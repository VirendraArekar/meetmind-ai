package com.meetmind.export;

import com.meetmind.actionitem.ActionItemService;
import com.meetmind.meeting.MeetingDtos;
import com.meetmind.meeting.MeetingService;
import com.meetmind.summary.MeetingSummary;
import com.meetmind.summary.MeetingSummaryRepository;
import com.meetmind.summary.SummaryService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomExportService {

    public record GenerateMomRequest(String agenda, String nextMeetingDetails) {}

    public record MomDto(
            UUID meetingId,
            String title,
            Instant date,
            int durationSeconds,
            List<String> participants,
            String agenda,
            String discussionSummary,
            List<SummaryService.DecisionDto> decisions,
            List<ActionItemService.ActionItemDto> actionItems,
            String nextMeetingDetails
    ) {}

    private final MeetingService meetingService;
    private final SummaryService summaryService;
    private final ActionItemService actionItemService;
    private final MeetingSummaryRepository summaryRepository;

    @Transactional
    public MomDto generateMom(UUID userId, UUID meetingId, GenerateMomRequest req) {
        MeetingDtos.MeetingDto meeting = meetingService.getMeeting(userId, meetingId);
        SummaryService.SummaryDto summary = null;
        try {
            summary = summaryService.getSummary(userId, meetingId);
        } catch (Exception ignored) {
        }

        String agenda = (req != null && req.agenda() != null && !req.agenda().isBlank())
                ? req.agenda().trim()
                : (meeting.description() != null && !meeting.description().isBlank()
                ? meeting.description()
                : "Review progress, key decisions, and next steps for " + meeting.title());

        String nextMeeting = (req != null && req.nextMeetingDetails() != null && !req.nextMeetingDetails().isBlank())
                ? req.nextMeetingDetails().trim()
                : "Follow-up sync to be scheduled with action item owners.";

        summaryRepository.findFirstByMeetingIdOrderByUpdatedAtDesc(meetingId).ifPresent(ms -> {
            ms.setMomAgenda(agenda);
            ms.setMomNextMeeting(nextMeeting);
            summaryRepository.save(ms);
        });

        List<String> participants = meeting.participants().stream()
                .map(MeetingDtos.ParticipantDto::displayName)
                .toList();

        List<ActionItemService.ActionItemDto> items = summary != null
                ? summary.actionItems()
                : actionItemService.listActionItems(userId, meetingId, null);

        return new MomDto(
                meeting.id(),
                meeting.title(),
                meeting.scheduledAt() != null ? meeting.scheduledAt() : meeting.createdAt(),
                meeting.durationSeconds(),
                participants,
                agenda,
                summary != null ? summary.executiveSummary() : "Transcript summary pending.",
                summary != null ? summary.decisions() : List.of(),
                items,
                nextMeeting
        );
    }

    public byte[] exportTxt(UUID userId, UUID meetingId) {
        MomDto mom = generateMom(userId, meetingId, null);
        StringBuilder sb = new StringBuilder();
        sb.append("MINUTES OF MEETING (MOM)\n");
        sb.append("========================\n\n");
        sb.append("Title: ").append(mom.title()).append("\n");
        sb.append("Date: ").append(mom.date()).append("\n");
        sb.append("Duration: ").append(mom.durationSeconds()).append(" seconds\n");
        sb.append("Participants: ").append(mom.participants().isEmpty() ? "N/A" : String.join(", ", mom.participants())).append("\n\n");
        sb.append("AGENDA\n------\n").append(mom.agenda()).append("\n\n");
        sb.append("DISCUSSION SUMMARY\n------------------\n").append(mom.discussionSummary()).append("\n\n");
        sb.append("DECISIONS\n---------\n");
        if (mom.decisions().isEmpty()) {
            sb.append("- None recorded\n");
        } else {
            for (SummaryService.DecisionDto d : mom.decisions()) {
                sb.append("- ").append(d.title()).append(": ").append(d.description()).append("\n");
            }
        }
        sb.append("\nACTION ITEMS\n------------\n");
        if (mom.actionItems().isEmpty()) {
            sb.append("- None recorded\n");
        } else {
            for (ActionItemService.ActionItemDto a : mom.actionItems()) {
                sb.append("- [").append(a.status()).append("] ").append(a.title())
                        .append(" (Owner: ").append(a.assignedToName() != null ? a.assignedToName() : "Unassigned")
                        .append(", Priority: ").append(a.priority())
                        .append(a.dueDate() != null ? ", Due: " + a.dueDate() : "")
                        .append(")\n");
            }
        }
        sb.append("\nNEXT MEETING\n------------\n").append(mom.nextMeetingDetails()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(UUID userId, UUID meetingId) {
        MomDto mom = generateMom(userId, meetingId, null);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Minutes of Meeting: " + mom.title(), titleFont));
            doc.add(new Paragraph("Date: " + mom.date() + " | Duration: " + mom.durationSeconds() + "s", bodyFont));
            doc.add(new Paragraph("Participants: " + (mom.participants().isEmpty() ? "N/A" : String.join(", ", mom.participants())), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Agenda", headingFont));
            doc.add(new Paragraph(mom.agenda(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Discussion Summary", headingFont));
            doc.add(new Paragraph(mom.discussionSummary(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Decisions", headingFont));
            if (mom.decisions().isEmpty()) {
                doc.add(new Paragraph("None recorded.", bodyFont));
            } else {
                for (SummaryService.DecisionDto d : mom.decisions()) {
                    doc.add(new Paragraph("• " + d.title() + " — " + d.description(), bodyFont));
                }
            }
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Action Items", headingFont));
            if (mom.actionItems().isEmpty()) {
                doc.add(new Paragraph("None recorded.", bodyFont));
            } else {
                for (ActionItemService.ActionItemDto a : mom.actionItems()) {
                    doc.add(new Paragraph("• [" + a.status() + "] " + a.title()
                            + " (Owner: " + (a.assignedToName() != null ? a.assignedToName() : "Unassigned")
                            + ", Priority: " + a.priority() + ")", bodyFont));
                }
            }
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Next Meeting", headingFont));
            doc.add(new Paragraph(mom.nextMeetingDetails(), bodyFont));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF export", e);
        }
    }

    public byte[] exportDocx(UUID userId, UUID meetingId) {
        MomDto mom = generateMom(userId, meetingId, null);
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addHeading(doc, "Minutes of Meeting: " + mom.title(), 16);
            addBody(doc, "Date: " + mom.date() + " | Duration: " + mom.durationSeconds() + "s");
            addBody(doc, "Participants: " + (mom.participants().isEmpty() ? "N/A" : String.join(", ", mom.participants())));

            addHeading(doc, "Agenda", 13);
            addBody(doc, mom.agenda());

            addHeading(doc, "Discussion Summary", 13);
            addBody(doc, mom.discussionSummary());

            addHeading(doc, "Decisions", 13);
            if (mom.decisions().isEmpty()) {
                addBody(doc, "None recorded.");
            } else {
                for (SummaryService.DecisionDto d : mom.decisions()) {
                    addBody(doc, "• " + d.title() + " — " + d.description());
                }
            }

            addHeading(doc, "Action Items", 13);
            if (mom.actionItems().isEmpty()) {
                addBody(doc, "None recorded.");
            } else {
                for (ActionItemService.ActionItemDto a : mom.actionItems()) {
                    addBody(doc, "• [" + a.status() + "] " + a.title()
                            + " (Owner: " + (a.assignedToName() != null ? a.assignedToName() : "Unassigned")
                            + ", Priority: " + a.priority() + ")");
                }
            }

            addHeading(doc, "Next Meeting", 13);
            addBody(doc, mom.nextMeetingDetails());

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate DOCX export", e);
        }
    }

    private void addHeading(XWPFDocument doc, String text, int size) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(size);
        run.setText(text);
    }

    private void addBody(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setFontSize(11);
        run.setText(text);
    }
}
