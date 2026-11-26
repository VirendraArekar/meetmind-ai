import { useQuery } from '@tanstack/react-query';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';
import { analyticsApi } from '../lib/api';

export function AnalyticsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-analytics'],
    queryFn: analyticsApi.dashboard,
  });

  if (isLoading || !data) {
    return <div className="p-8 text-sm text-slate-500">Loading analytics...</div>;
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Analytics &amp; AI Usage</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Meeting duration, task completion rate, and AI token telemetry.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500">ACTION ITEM COMPLETION RATE</p>
          <p className="mt-2 text-3xl font-semibold">{data.completionRatePercent}%</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500">TOTAL AI CALLS</p>
          <p className="mt-2 text-3xl font-semibold">{data.aiOperationsCount}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500">TOTAL TOKENS</p>
          <p className="mt-2 text-3xl font-semibold">{data.totalAiTokens}</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-base font-semibold">Meetings by Category</h2>
          <div className="mt-4 h-64">
            {data.categoryBreakdown.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.categoryBreakdown}>
                  <XAxis dataKey="category" stroke="#94a3b8" fontSize={12} />
                  <YAxis stroke="#94a3b8" fontSize={12} allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#6366f1" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">No data yet.</div>
            )}
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-base font-semibold">AI Usage by Operation</h2>
          <div className="mt-4 space-y-3">
            {data.aiUsageStats.length === 0 ? (
              <p className="text-sm text-slate-500">No AI operations recorded yet.</p>
            ) : (
              data.aiUsageStats.map((op) => (
                <div
                  key={op.operation}
                  className="flex items-center justify-between rounded-lg border border-slate-100 p-3 dark:border-slate-800"
                >
                  <div>
                    <p className="text-sm font-medium">{op.operation}</p>
                    <p className="text-xs text-slate-500">{op.calls} calls</p>
                  </div>
                  <span className="font-mono text-sm font-semibold">{op.totalTokens} tokens</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
