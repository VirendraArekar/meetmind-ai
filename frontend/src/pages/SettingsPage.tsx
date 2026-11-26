import { useQuery } from '@tanstack/react-query';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../app/store';
import { toggleTheme } from '../app/uiSlice';
import { fetchApiHealth } from '../lib/api';

export function SettingsPage() {
  const dispatch = useDispatch();
  const theme = useSelector((state: RootState) => state.ui.theme);
  const { data: health } = useQuery({ queryKey: ['api-health'], queryFn: fetchApiHealth });

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Workspace appearance and system diagnostics.
        </p>
      </div>

      <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">Interface Theme</p>
            <p className="text-xs text-slate-500">Current mode: {theme}</p>
          </div>
          <button
            type="button"
            onClick={() => dispatch(toggleTheme())}
            className="rounded-lg border border-slate-200 px-3.5 py-2 text-xs font-medium dark:border-slate-700"
          >
            Toggle Theme
          </button>
        </div>

        <div className="border-t border-slate-100 pt-4 dark:border-slate-800">
          <p className="text-sm font-medium">Backend API Status</p>
          <p className="mt-1 text-xs text-slate-500">
            {health ? `${health.status} — ${health.service} (${health.version})` : 'Checking...'}
          </p>
        </div>
      </div>
    </div>
  );
}
