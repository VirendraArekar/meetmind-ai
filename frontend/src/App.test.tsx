import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import App from './App';
import { store } from './app/store';
import { clearCredentials, setCredentials } from './app/authSlice';

vi.mock('./lib/api', () => ({
  fetchApiHealth: vi.fn().mockResolvedValue({
    status: 'UP',
    service: 'meetmind-backend',
    version: '0.2.0',
    checkedAt: '2026-11-26T00:00:00Z',
  }),
  analyticsApi: {
    dashboard: vi.fn().mockResolvedValue({
      totalMeetings: 3,
      totalDurationSeconds: 900,
      totalActionItems: 5,
      pendingTasks: 2,
      completedTasks: 3,
      completionRatePercent: 60,
      aiOperationsCount: 4,
      totalAiTokens: 1200,
      activityTimeline: [],
      categoryBreakdown: [],
      aiUsageStats: [],
    }),
  },
  meetingsApi: {
    list: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
  },
  authApi: {
    logout: vi.fn().mockResolvedValue({}),
  },
}));

describe('App', () => {
  beforeEach(() => {
    store.dispatch(clearCredentials());
    window.history.pushState({}, '', '/');
  });

  it('redirects unauthenticated users to login page', async () => {
    render(<App />);
    expect(await screen.findByText(/sign in to meetmind ai/i)).toBeInTheDocument();
  });

  it('renders authenticated dashboard and toggles theme', async () => {
    store.dispatch(
      setCredentials({
        accessToken: 'test-jwt',
        refreshToken: 'test-refresh',
        user: {
          id: 'user-1',
          email: 'alice@meetmind.ai',
          fullName: 'Alice Chen',
          emailVerified: true,
          roles: ['ROLE_ADMIN'],
          defaultOrganizationName: 'Acme AI',
        },
      })
    );

    render(<App />);
    expect(await screen.findByText('TOTAL MEETINGS')).toBeInTheDocument();
    expect(await screen.findByText('3')).toBeInTheDocument();

    const themeButton = screen.getByRole('button', { name: /switch to light theme/i });
    fireEvent.click(themeButton);
    expect(screen.getByRole('button', { name: /switch to dark theme/i })).toBeInTheDocument();
  });
});
