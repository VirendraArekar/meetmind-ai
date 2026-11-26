# MeetMind AI — Production Deployment Guide

## 1. Frontend (Vercel)

1. Import the repository into Vercel and set the **Root Directory** to `frontend`.
2. Build command: `npm run build`
3. Output directory: `dist`
4. Environment variables:
   - `VITE_API_URL=https://api.your-domain.com/api/v1`

## 2. Backend (Render, Railway, or VPS)

1. Point the service to `backend/Dockerfile`.
2. Expose port `8080` and configure health check path `/api/v1/health`.
3. Set production environment variables:
   - `APP_ENV=production`
   - `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
   - `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
   - `S3_ENABLED=true`, `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`
   - `JWT_SECRET` (strong 256-bit+ base64 secret)
   - `CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app`
   - `OPENAI_API_KEY` / `GEMINI_API_KEY`

## 3. Managed PostgreSQL (`pgvector`)

- Use any managed PostgreSQL 16+ provider with the `vector` extension enabled (Neon, Supabase, AWS RDS, or Railway Postgres).
- Flyway automatically executes `backend/src/main/resources/db/migration/V1__init_schema.sql` on startup.

## 4. S3-Compatible Storage

- Use AWS S3, Cloudflare R2, Backblaze B2, or self-hosted MinIO.
- Ensure the configured bucket exists or the IAM user has `s3:CreateBucket`, `s3:PutObject`, and `s3:GetObject` permissions.
