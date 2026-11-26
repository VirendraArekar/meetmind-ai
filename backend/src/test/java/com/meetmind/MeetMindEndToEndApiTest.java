package com.meetmind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetMindEndToEndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullMeetingLifecycleAuthUploadTranscribeSummarizeChatMomExportAndIsolation() throws Exception {
        // 1. Register user A
        MvcResult regA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@meetmind.ai",
                                  "password": "Password123!",
                                  "fullName": "Alice Chen",
                                  "organizationName": "Acme AI"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode regAJson = objectMapper.readTree(regA.getResponse().getContentAsString());
        String tokenA = regAJson.path("data").path("accessToken").asString();
        String refreshA = regAJson.path("data").path("refreshToken").asString();

        // 2. Refresh token rotation
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 3. Create a private meeting
        MvcResult meetingRes = mockMvc.perform(post("/api/v1/meetings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Q4 Architecture Review",
                                  "description": "Review payment gateway migration and release timeline",
                                  "category": "ENGINEERING",
                                  "visibility": "PRIVATE",
                                  "durationSeconds": 180,
                                  "participants": [
                                    {"displayName": "Alice Chen", "email": "alice@meetmind.ai", "role": "HOST"},
                                    {"displayName": "Bob Patel", "email": "bob@meetmind.ai", "role": "PARTICIPANT"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Q4 Architecture Review"))
                .andReturn();

        String meetingId = objectMapper.readTree(meetingRes.getResponse().getContentAsString())
                .path("data").path("id").asString();

        // 4. Upload audio recording
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "sprint-sync.mp3",
                "audio/mpeg",
                "fake-mp3-audio-bytes-for-testing".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/meetings/" + meetingId + "/upload")
                        .file(audioFile)
                        .param("durationSeconds", "180")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename").value("sprint-sync.mp3"));

        // 5. Transcribe
        mockMvc.perform(post("/api/v1/meetings/" + meetingId + "/transcribe")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.segments").isArray());

        // 6. Summarize & extract action items
        mockMvc.perform(post("/api/v1/meetings/" + meetingId + "/summarize")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summaryLength\":\"EXECUTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executiveSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.actionItems").isArray());

        // 7. RAG Chat
        mockMvc.perform(post("/api/v1/meetings/" + meetingId + "/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What did we decide about QA testing?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer.content").isNotEmpty())
                .andExpect(jsonPath("$.data.answer.citations").isArray());

        // 8. Semantic Search
        mockMvc.perform(post("/api/v1/search/semantic")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"QA testing release\",\"limit\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 9. MOM & Exports (PDF, DOCX, TXT)
        mockMvc.perform(post("/api/v1/meetings/" + meetingId + "/generate-mom")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Q4 Architecture Review"));

        byte[] pdfBytes = mockMvc.perform(get("/api/v1/meetings/" + meetingId + "/export?format=pdf")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(100);

        byte[] docxBytes = mockMvc.perform(get("/api/v1/meetings/" + meetingId + "/export?format=docx")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(docxBytes.length).isGreaterThan(100);

        byte[] txtBytes = mockMvc.perform(get("/api/v1/meetings/" + meetingId + "/export?format=txt")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(txtBytes, StandardCharsets.UTF_8)).contains("Q4 Architecture Review");

        // 10. Analytics
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMeetings").value(1));

        // 11. Organization isolation: User B cannot access User A's private meeting
        MvcResult regB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "eve@otherorg.ai",
                                  "password": "Password123!",
                                  "fullName": "Eve Other",
                                  "organizationName": "Other Org"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readTree(regB.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();

        mockMvc.perform(get("/api/v1/meetings/" + meetingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
