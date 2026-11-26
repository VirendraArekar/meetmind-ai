package com.meetmind.rag;

import com.meetmind.ai.AiUsageService;
import com.meetmind.ai.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagIndexingService {

    public record ChunkInput(double startSeconds, double endSeconds, String content) {}
    public record ScoredChunk(TranscriptEmbedding chunk, double score) {}

    private final TranscriptEmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void indexTranscriptChunks(UUID organizationId, UUID meetingId, UUID userId, List<ChunkInput> chunks) {
        embeddingRepository.deleteByMeetingId(meetingId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();
        List<ChunkInput> grouped = groupChunks(chunks, 500);
        for (int i = 0; i < grouped.size(); i++) {
            ChunkInput chunk = grouped.get(i);
            float[] vector = embeddingProvider.embed(chunk.content());
            String json = serializeVector(vector);
            embeddingRepository.save(TranscriptEmbedding.builder()
                    .meetingId(meetingId)
                    .organizationId(organizationId)
                    .chunkIndex(i)
                    .startSeconds(chunk.startSeconds())
                    .endSeconds(chunk.endSeconds())
                    .content(chunk.content())
                    .embeddingJson(json)
                    .build());
        }
        aiUsageService.record(
                organizationId,
                meetingId,
                userId,
                "EMBEDDING_INDEX",
                embeddingProvider.providerName(),
                embeddingProvider.modelName(),
                grouped.size() * 32,
                System.currentTimeMillis() - start,
                true
        );
    }

    @Transactional(readOnly = true)
    public List<ScoredChunk> searchMeeting(UUID meetingId, String query, int topK) {
        List<TranscriptEmbedding> chunks = embeddingRepository.findByMeetingIdOrderByChunkIndexAsc(meetingId);
        return rankChunks(chunks, query, topK);
    }

    @Transactional(readOnly = true)
    public List<ScoredChunk> searchOrganization(UUID organizationId, String query, int topK) {
        List<TranscriptEmbedding> chunks = embeddingRepository.findByOrganizationId(organizationId);
        return rankChunks(chunks, query, topK);
    }

    private List<ScoredChunk> rankChunks(List<TranscriptEmbedding> chunks, String query, int topK) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        float[] queryVec = embeddingProvider.embed(query);
        return chunks.stream()
                .map(chunk -> {
                    float[] vec = deserializeVector(chunk.getEmbeddingJson());
                    double sim = cosineSimilarity(queryVec, vec);
                    return new ScoredChunk(chunk, sim);
                })
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private List<ChunkInput> groupChunks(List<ChunkInput> raw, int maxChars) {
        List<ChunkInput> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        double start = raw.getFirst().startSeconds();
        double end = raw.getFirst().endSeconds();

        for (ChunkInput item : raw) {
            if (buf.length() + item.content().length() > maxChars && !buf.isEmpty()) {
                result.add(new ChunkInput(start, end, buf.toString().trim()));
                buf.setLength(0);
                start = item.startSeconds();
            }
            if (!buf.isEmpty()) {
                buf.append("\n");
            }
            buf.append(item.content());
            end = item.endSeconds();
        }
        if (!buf.isEmpty()) {
            result.add(new ChunkInput(start, end, buf.toString().trim()));
        }
        return result;
    }

    private String serializeVector(float[] vec) {
        try {
            return objectMapper.writeValueAsString(vec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    private float[] deserializeVector(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return new float[0];
        }
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }
}
