package com.meetmind.ai;

import java.util.List;

public interface TranscriptionProvider {

    record TranscriptSegmentResult(
            String speakerName,
            double startSeconds,
            double endSeconds,
            String content,
            double confidence
    ) {}

    record TranscriptionResult(
            String provider,
            String model,
            String language,
            String fullText,
            List<TranscriptSegmentResult> segments
    ) {}

    TranscriptionResult transcribe(String filename, String contentType, byte[] audioBytes, String meetingTitle);
}
