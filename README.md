# MeetMind AI

Production-grade AI-powered Meeting Assistant SaaS platform for recording, transcribing, summarizing, extracting action items, generating Minutes of Meeting (MOM), and querying meeting knowledge with Retrieval-Augmented Generation (RAG).

---

## How It Works & Step-by-Step User Guide

### 1. Start the Application
```bash
cp .env.example .env
docker compose up -d --build
```
Open **http://localhost:5173** in your browser (desktop, tablet, or mobile — the UI is fully responsive).

### 2. Sign In (Demo Workspace with 250 Pre-Seeded Meetings)
On first startup, MeetMind AI automatically seeds **250 realistic meetings**, transcripts, AI summaries, action items, decisions, and analytics telemetry.
- Click **"Try Demo Workspace (250 Meetings)"** on the Login page, **or** sign in manually with:
  - **Email**: `demo@meetmind.ai`
  - **Password**: `Demo@123456`
- Or click **Create workspace** (`/register`) to create your own private organization.

### 3. Explore the Dashboard & Analytics
- **Dashboard (`/`)**: View total meetings, total meeting duration, pending vs. completed action items, AI token usage, a meeting activity bar chart, and your 5 most recent meetings.
- **Analytics (`/analytics`)**: Inspect action item completion rate, meetings by category, and AI operations breakdown.

### 4. Create, Upload, or Record a Meeting
- **Upload a Recording (`/meetings`)**:
  1. Click **Create Meeting**, enter a title, category (`ENGINEERING`, `PRODUCT`, `SALES`, `STANDUP`, `GENERAL`), visibility (`Private` or `Shared with Workspace`), and participants.
  2. Attach an audio or video file (`MP3`, `WAV`, `M4A`, `MP4`, or `WEBM`, up to `500MB`).
  3. Click **Save Meeting**. Files are stored in S3/MinIO.
- **Record Live in Browser (`/record`)**:
  1. Enter a meeting title and check the **participant recording consent** box.
  2. Click **Start Recording**, speak, then click **Stop Recording**.
  3. Preview the audio and click **Save Meeting & Transcribe**.

### 5. Transcribe, Summarize & Extract Action Items
Open any meeting (`/meetings/:id`):
1. **Transcribe**: Click **Transcribe** to convert audio into speaker-diarized, timestamped segments (`Speaker 1`, `Speaker 2`, etc.) and automatically index chunks for RAG search. You can click **Edit Transcript** to fix speaker names or text anytime.
2. **AI Summary**: Click **Generate AI Summary** and choose `Short`, `Detailed`, or `Executive`. MeetMind AI generates all 9 structured sections (Executive Summary, Objectives, Discussion Points, Highlights, Decisions, Problems, Risks & Dependencies, Next Steps, Follow-up Requirements) and extracts **Action Items** automatically.
3. **Action Items (`/action-items`)**: Check off completed tasks, filter by status, or delete tasks.

### 6. Ask Questions with AI Meeting Chat (RAG) & Semantic Search
- **AI Chat tab** (inside a meeting or via `/chat`): Ask questions like *"What did we decide about QA testing?"* or *"Who owns the release checklist?"*. Answers are strictly grounded in the meeting's transcript chunks with `[MM:SS]` timestamp citations.
- **Global & Semantic Search (`/meetings`)**: Use the search bar and toggle **Semantic AI Search: ON** to search across meeting concepts using vector similarity.

### 7. Generate Minutes of Meeting (MOM) & Export Reports
In any meeting's **MOM & Export** tab:
- Optionally customize the Agenda and Next Meeting details and click **Preview MOM**.
- Click **Export PDF**, **Export DOCX**, or **Export TXT** to download a formatted report.

### 8. Team Collaboration (`/team`)
- Invite registered users to your workspace with `ADMIN`, `MEMBER`, or `VIEWER` roles, and organize members into teams. Private meetings remain accessible only to their creator; Shared meetings are visible to workspace members.

---

## Tech Stack

- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS v4, Redux Toolkit, React Router, TanStack Query, React Hook Form, Zod, Axios, Recharts, Lucide React
- **Backend**: Java 21+, Spring Boot 4, Spring Web, Spring Security (JWT + Refresh Token Rotation + RBAC), Spring Data JPA, Flyway, Spring Validation, Spring Actuator, SpringDoc OpenAPI (Swagger)
- **Database & Storage**: PostgreSQL 16 + `pgvector`, Redis 7, S3-compatible Object Storage (MinIO locally / AWS S3 in production)
- **AI Providers (Interchangeable)**: OpenAI (Whisper, GPT, Embeddings), Google Gemini, Whisper-compatible endpoints, Local Ollama (configured via `.env`, with automatic local fallback when keys are omitted)
- **Exports**: PDF (`OpenPDF`), DOCX (`Apache POI`), TXT

## Endpoints & Tools

| Service | URL |
| :--- | :--- |
| **Frontend** | http://localhost:5173 |
| **Backend Health** | http://localhost:8080/api/v1/health |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **MinIO Console** | http://localhost:9001 |

## Running Tests

```bash
# Backend (JUnit 5 + MockMvc + H2)
cd backend && mvn test

# Frontend (TypeScript + Vitest + React Testing Library + Vite Build)
cd frontend && npm run lint && npm test && npm run build
```

## Documentation

- [System Architecture](docs/architecture.md)
- [Deployment Guide](docs/deployment.md)
