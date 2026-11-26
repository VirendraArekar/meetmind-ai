# MeetMind AI — System Architecture

## 1. High-Level Overview

MeetMind AI is a modular SaaS platform built with a **React 19 + TypeScript** frontend and a **Java 21 / Spring Boot 4** backend, backed by **PostgreSQL (with pgvector)**, **Redis**, and **S3-compatible object storage**.

```
[Browser / React 19 + Vite]
         │  HTTPS / REST (/api/v1) + WebSocket
         ▼
   [Nginx Reverse Proxy]
         │
         ▼
[Spring Boot 4 Backend (com.meetmind)]
  ├── Security & JWT Auth (Phase 2)
  ├── Meeting & S3 Media Pipeline (Phase 3)
  ├── Interchangeable AI Providers (Phases 4–6)
  │     ├── Transcription: OpenAI Whisper / Whisper-compatible
  │     ├── LLM: OpenAI / Google Gemini / Local Ollama
  │     └── Embeddings: OpenAI / Ollama -> pgvector
  ├── Async Background Workers (Redis-coordinated)
  └── Export Engine: PDF / DOCX / TXT (Phase 7)
         │
         ├──► PostgreSQL 16 + pgvector
         ├──► Redis 7
         └──► S3 / MinIO Object Storage
```

## 2. Backend Modular Package Layout

```
com.meetmind
├── config          # Security, CORS, OpenAPI, Async, AI provider beans
├── common
│   ├── exception   # ApiException hierarchy & GlobalExceptionHandler
│   └── response    # ApiResponse<T> & ErrorDetails envelope
└── health          # Service health & readiness controller
```

Upcoming domain modules (added incrementally per phase):
- `auth`, `user`, `organization` (Phase 2)
- `meeting`, `recording`, `storage` (Phase 3)
- `transcription` (Phase 4)
- `summary`, `actionitem` (Phase 5)
- `rag`, `embedding`, `chat`, `search` (Phase 6)
- `export` (Phase 7)
- `analytics`, `notification` (Phase 8)

## 3. Frontend Architecture

- **State & Caching**: Redux Toolkit (`uiSlice` for theme & layout; auth slice in Phase 2) + TanStack Query for server state.
- **Routing**: React Router v7 with `AppShell` layout and `ErrorBoundary`.
- **Styling**: Tailwind CSS v4 with class-based dark/light theme switching.
- **API Client**: Centralized Axios instance (`src/lib/api.ts`) targeting `/api/v1`.
