import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, CheckSquare } from 'lucide-react';
import { actionItemsApi } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

export function ActionItemsPage() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [statusFilter, setStatusFilter] = useState<string>('');

  const { data: items, isLoading } = useQuery({
    queryKey: ['action-items', 'all', statusFilter],
    queryFn: () => actionItemsApi.list({ status: statusFilter || undefined }),
  });

  const toggleMutation = useMutation({
    mutationFn: (item: { id: string; title: string; status: string }) =>
      actionItemsApi.update(item.id, {
        title: item.title,
        status: item.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['action-items'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => actionItemsApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['action-items'] });
      toast('Action item deleted');
    },
  });

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Action Items</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Track tasks, owners, priorities, and deadlines across all meetings.
          </p>
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900"
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        {isLoading ? (
          <p className="text-sm text-slate-500">Loading action items...</p>
        ) : !items || items.length === 0 ? (
          <div className="py-10 text-center">
            <CheckSquare className="mx-auto h-9 w-9 text-slate-400" />
            <p className="mt-2 text-sm text-slate-500">No action items found.</p>
          </div>
        ) : (
          <div className="space-y-2.5">
            {items.map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between rounded-lg border border-slate-100 p-3.5 dark:border-slate-800"
              >
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={item.status === 'COMPLETED'}
                    onChange={() => toggleMutation.mutate(item)}
                    className="h-4 w-4 rounded"
                  />
                  <div>
                    <p className={`text-sm font-medium ${item.status === 'COMPLETED' ? 'line-through text-slate-400' : ''}`}>
                      {item.title}
                    </p>
                    <p className="text-xs text-slate-500">
                      Owner: {item.assignedToName || 'Unassigned'} · Priority: {item.priority}
                      {item.dueDate ? ` · Due ${item.dueDate}` : ''}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => deleteMutation.mutate(item.id)}
                  aria-label="Delete action item"
                  className="text-slate-400 hover:text-rose-500"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
