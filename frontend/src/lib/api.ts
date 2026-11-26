import axios from 'axios';
import { store } from '../app/store';
import { clearCredentials, setCredentials, type UserSummary } from '../app/authSlice';

export interface ErrorDetails {
  code?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: ErrorDetails;
  timestamp: string;
}

export interface HealthStatus {
  status: string;
  service: string;
  version: string;
  checkedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserSummary;
}

export interface ParticipantDto {
  id: string;
  displayName: string;
  email?: string;
  role: string;
}

export interface MeetingFileDto {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSizeBytes: number;
  storageProvider: string;
  createdAt: string;
}

export interface MeetingDto {
  id: string;
  organizationId: string;
  createdBy: string;
  title: string;
  description?: string;
  category: string;
  visibility: string;
  status: string;
  sourceType: string;
  scheduledAt?: string;
  durationSeconds: number;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
  participants: ParticipantDto[];
  files: MeetingFileDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TranscriptSegmentDto {
  id: string;
  speakerName: string;
  segmentIndex: number;
  startSeconds: number;
  endSeconds: number;
  content: string;
  confidence: number;
}

export interface TranscriptDto {
  id: string;
  meetingId: string;
  language: string;
  provider: string;
  status: string;
  fullText: string;
  segments: TranscriptSegmentDto[];
  updatedAt: string;
}

export interface ActionItemDto {
  id: string;
  meetingId: string;
  title: string;
  description?: string;
  assignedToName?: string;
  priority: string;
  dueDate?: string;
  status: string;
  transcriptTimestamp?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DecisionDto {
  id: string;
  title: string;
  description?: string;
  decidedBy?: string;
  transcriptTimestamp?: string;
}

export interface SummaryDto {
  id: string;
  meetingId: string;
  summaryLength: string;
  provider: string;
  executiveSummary: string;
  objectives?: string;
  discussionPoints?: string;
  highlights?: string;
  decisionsSummary?: string;
  problemsIdentified?: string;
  risksDependencies?: string;
  nextSteps?: string;
  followUpRequirements?: string;
  decisions: DecisionDto[];
  actionItems: ActionItemDto[];
  updatedAt: string;
}

export interface CitationDto {
  timestamp: string;
  excerpt: string;
  score: number;
}

export interface ChatMessageDto {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations: CitationDto[];
  createdAt: string;
}

export interface ChatResponseDto {
  sessionId: string;
  answer: ChatMessageDto;
  history: ChatMessageDto[];
}

export interface MomDto {
  meetingId: string;
  title: string;
  date: string;
  durationSeconds: number;
  participants: string[];
  agenda: string;
  discussionSummary: string;
  decisions: DecisionDto[];
  actionItems: ActionItemDto[];
  nextMeetingDetails: string;
}

export interface SearchHitDto {
  meetingId: string;
  meetingTitle: string;
  category: string;
  snippet: string;
  timestamp?: string;
  score: number;
}

export interface DashboardAnalyticsDto {
  totalMeetings: number;
  totalDurationSeconds: number;
  totalActionItems: number;
  pendingTasks: number;
  completedTasks: number;
  completionRatePercent: number;
  aiOperationsCount: number;
  totalAiTokens: number;
  activityTimeline: { date: string; meetings: number; durationMinutes: number }[];
  categoryBreakdown: { category: string; count: number }[];
  aiUsageStats: { operation: string; calls: number; totalTokens: number }[];
}

export interface OrgDto {
  id: string;
  name: string;
  slug: string;
  myRole: string;
  members: { id: string; userId: string; fullName: string; email: string; workspaceRole: string; status: string }[];
  teams: { id: string; name: string; description?: string }[];
}

const baseURL = import.meta.env.VITE_API_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = store.getState().auth.refreshToken;
      if (refreshToken) {
        try {
          const refreshRes = await axios.post<ApiResponse<AuthResponse>>(`${baseURL}/auth/refresh`, {
            refreshToken,
          });
          if (refreshRes.data.success && refreshRes.data.data) {
            store.dispatch(
              setCredentials({
                accessToken: refreshRes.data.data.accessToken,
                refreshToken: refreshRes.data.data.refreshToken,
                user: refreshRes.data.data.user,
              })
            );
            originalRequest.headers.Authorization = `Bearer ${refreshRes.data.data.accessToken}`;
            return apiClient(originalRequest);
          }
        } catch {
          store.dispatch(clearCredentials());
        }
      } else {
        store.dispatch(clearCredentials());
      }
    }
    return Promise.reject(error);
  }
);

function unwrap<T>(res: { data: ApiResponse<T> }): T {
  if (!res.data.success || res.data.data === undefined) {
    throw new Error(res.data.message || 'Request failed');
  }
  return res.data.data;
}

export async function fetchApiHealth(): Promise<HealthStatus> {
  return unwrap(await apiClient.get<ApiResponse<HealthStatus>>('/health'));
}

export const authApi = {
  register: async (payload: { email: string; password: string; fullName: string; organizationName?: string }) =>
    unwrap(await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', payload)),
  login: async (payload: { email: string; password: string }) =>
    unwrap(await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload)),
  logout: async (refreshToken?: string | null) =>
    apiClient.post('/auth/logout', { refreshToken }),
  forgotPassword: async (email: string) =>
    apiClient.post('/auth/forgot-password', { email }),
  resetPassword: async (payload: { token: string; newPassword: string }) =>
    apiClient.post('/auth/reset-password', payload),
  getProfile: async () => unwrap(await apiClient.get<ApiResponse<UserSummary>>('/users/me')),
  updateProfile: async (payload: { fullName: string; jobTitle?: string; avatarUrl?: string }) =>
    unwrap(await apiClient.put<ApiResponse<UserSummary>>('/users/me', payload)),
};

export const meetingsApi = {
  list: async (params?: { category?: string; archived?: boolean; page?: number; size?: number }) =>
    unwrap(await apiClient.get<ApiResponse<PageResponse<MeetingDto>>>('/meetings', { params })),
  get: async (id: string) => unwrap(await apiClient.get<ApiResponse<MeetingDto>>(`/meetings/${id}`)),
  create: async (payload: {
    title: string;
    description?: string;
    category?: string;
    visibility?: string;
    sourceType?: string;
    durationSeconds?: number;
    participants?: { displayName: string; email?: string; role?: string }[];
  }) => unwrap(await apiClient.post<ApiResponse<MeetingDto>>('/meetings', payload)),
  update: async (id: string, payload: Record<string, unknown>) =>
    unwrap(await apiClient.put<ApiResponse<MeetingDto>>(`/meetings/${id}`, payload)),
  remove: async (id: string) => apiClient.delete(`/meetings/${id}`),
  upload: async (id: string, file: File | Blob, filename: string, durationSeconds?: number, onProgress?: (pct: number) => void) => {
    const form = new FormData();
    form.append('file', file, filename);
    if (durationSeconds !== undefined) {
      form.append('durationSeconds', String(durationSeconds));
    }
    const res = await apiClient.post<ApiResponse<MeetingFileDto>>(`/meetings/${id}/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (evt) => {
        if (onProgress && evt.total) {
          onProgress(Math.round((evt.loaded * 100) / evt.total));
        }
      },
    });
    return unwrap(res);
  },
  transcribe: async (id: string) =>
    unwrap(await apiClient.post<ApiResponse<TranscriptDto>>(`/meetings/${id}/transcribe`)),
  getTranscript: async (id: string) =>
    unwrap(await apiClient.get<ApiResponse<TranscriptDto>>(`/meetings/${id}/transcript`)),
  updateTranscript: async (id: string, segments: { id?: string; speakerName: string; content: string; startSeconds: number; endSeconds: number }[]) =>
    unwrap(await apiClient.put<ApiResponse<TranscriptDto>>(`/meetings/${id}/transcript`, { segments })),
  summarize: async (id: string, summaryLength: 'SHORT' | 'DETAILED' | 'EXECUTIVE') =>
    unwrap(await apiClient.post<ApiResponse<SummaryDto>>(`/meetings/${id}/summarize`, { summaryLength })),
  getSummary: async (id: string) =>
    unwrap(await apiClient.get<ApiResponse<SummaryDto>>(`/meetings/${id}/summary`)),
  chat: async (id: string, question: string) =>
    unwrap(await apiClient.post<ApiResponse<ChatResponseDto>>(`/meetings/${id}/chat`, { question })),
  chatHistory: async (id: string) =>
    unwrap(await apiClient.get<ApiResponse<ChatMessageDto[]>>(`/meetings/${id}/chat`)),
  generateMom: async (id: string, payload?: { agenda?: string; nextMeetingDetails?: string }) =>
    unwrap(await apiClient.post<ApiResponse<MomDto>>(`/meetings/${id}/generate-mom`, payload || {})),
  exportMom: async (id: string, format: 'pdf' | 'docx' | 'txt') => {
    const res = await apiClient.get(`/meetings/${id}/export`, {
      params: { format },
      responseType: 'blob',
    });
    return res.data as Blob;
  },
};

export const actionItemsApi = {
  list: async (params?: { meetingId?: string; status?: string }) =>
    unwrap(await apiClient.get<ApiResponse<ActionItemDto[]>>('/action-items', { params })),
  create: async (payload: { meetingId: string; title: string; description?: string; assignedToName?: string; priority?: string; dueDate?: string }) =>
    unwrap(await apiClient.post<ApiResponse<ActionItemDto>>('/action-items', payload)),
  update: async (id: string, payload: Record<string, unknown>) =>
    unwrap(await apiClient.put<ApiResponse<ActionItemDto>>(`/action-items/${id}`, payload)),
  remove: async (id: string) => apiClient.delete(`/action-items/${id}`),
};

export const searchApi = {
  fullText: async (q: string, category?: string) =>
    unwrap(await apiClient.get<ApiResponse<SearchHitDto[]>>('/search', { params: { q, category } })),
  semantic: async (query: string, limit = 8) =>
    unwrap(await apiClient.post<ApiResponse<SearchHitDto[]>>('/search/semantic', { query, limit })),
};

export const analyticsApi = {
  dashboard: async () => unwrap(await apiClient.get<ApiResponse<DashboardAnalyticsDto>>('/analytics/dashboard')),
};

export const orgApi = {
  current: async () => unwrap(await apiClient.get<ApiResponse<OrgDto>>('/organizations/current')),
  invite: async (payload: { email: string; workspaceRole: string }) =>
    unwrap(await apiClient.post('/organizations/current/members', payload)),
  createTeam: async (payload: { name: string; description?: string }) =>
    unwrap(await apiClient.post('/organizations/current/teams', payload)),
};
