import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Clock, FileAudio, CheckSquare, Sparkles, ArrowRight, Mic, Plus } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';
import { analyticsApi, meetingsApi } from '../lib/api';

export function DashboardPage() {
  const { data: analytics, isLoading: loadingAnalytics } = useQuery({
    queryKey: ['dashboard-analytics'],
    queryFn: analyticsApi.dashboard,
  });

  const { data: meetingsPage, isLoading: loadingMeetings } = useQuery({
    queryKey: ['recent-meetings'],
    queryFn: () => meetingsApi.list(),
  });

  const recentMeetings = meetingsPage?.content?.slice(0, 5) || [];
  const totalMinutes = Math.round((analytics?.totalDurationSeconds || 0) / 60);

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Overview of your meetings, action items, and AI workspace activity.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/record"
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-sm font-medium hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:hover:bg-slate-800"
          >
            <Mic className="h-4 w-4 text-rose-500" />
            <span>Record Live</span>
          </Link>
          <Link
            to="/meetings"
            className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-indigo-500"
          >
            <Plus className="h-4 w-4" />
            <span>New Meeting</span>
          </Link>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span>TOTAL MEETINGS</span>
            <FileAudio className="h-4 w-4 text-indigo-500" />
          </div>
          <p className="mt-3 text-2xl font-semibold">
            {loadingAnalytics ? '—' : analytics?.totalMeetings ?? 0}
          </p>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span>MEETING DURATION</span>
            <Clock className="h-4 w-4 text-indigo-500" />
          </div>
          <p className="mt-3 text-2xl font-semibold">
            {loadingAnalytics ? '—' : `${totalMinutes} min`}
          </p>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span>ACTION ITEMS</span>
            <CheckSquare className="h-4 w-4 text-emerald-500" />
          </div>
          <p className="mt-3 text-2xl font-semibold">
            {loadingAnalytics ? '—' : analytics?.totalActionItems ?? 0}
          </p>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {analytics?.pendingTasks ?? 0} pending · {analytics?.completedTasks ?? 0} done
          </p>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between text-xs font-medium text-slate-500 dark:text-slate-400">
            <span>AI OPERATIONS</span>
            <Sparkles className="h-4 w-4 text-amber-500" />
          </div>
          <p className="mt-3 text-2xl font-semibold">
            {loadingAnalytics ? '—' : analytics?.aiOperationsCount ?? 0}
          </p>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {analytics?.totalAiTokens ?? 0} tokens processed
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-6 lg:col-span-2 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-base font-semibold">Meeting Activity</h2>
          <div className="mt-4 h-64">
            {analytics?.activityTimeline && analytics.activityTimeline.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={analytics.activityTimeline}>
                  <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} />
                  <YAxis stroke="#94a3b8" fontSize={12} allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="meetings" fill="#6366f1" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                Create or upload your first meeting to see activity trends.
              </div>
            )}
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold">Recent Meetings</h2>
            <Link to="/meetings" className="text-xs font-medium text-indigo-600 hover:underline dark:text-indigo-400">
              View all
            </Link>
          </div>

          <div className="mt-4 space-y-3">
            {loadingMeetings ? (
              <p className="text-sm text-slate-500">Loading meetings...</p>
            ) : recentMeetings.length === 0 ? (
              <p className="text-sm text-slate-500">No meetings yet.</p>
            ) : (
              recentMeetings.map((m) => (
                <Link
                  key={m.id}
                  to={`/meetings/${m.id}`}
                  className="flex items-center justify-between rounded-lg border border-slate-100 p-3 hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/60"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{m.title}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {m.category} · {m.status}
                    </p>
                  </div>
                  <ArrowRight className="h-4 w-4 shrink-0 text-slate-400" />
                </Link>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
