import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../app/store';
import { updateUser } from '../app/authSlice';
import { authApi } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

export function ProfilePage() {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const user = useSelector((state: RootState) => state.auth.user);

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [jobTitle, setJobTitle] = useState(user?.jobTitle || '');
  const [saving, setSaving] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await authApi.updateProfile({ fullName, jobTitle });
      dispatch(updateUser(updated));
      toast('Profile updated');
    } catch {
      toast('Failed to update profile', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">User Profile</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">Manage your personal account details.</p>
      </div>

      <form
        onSubmit={handleSave}
        className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
      >
        <div>
          <label className="block text-xs font-medium">Email</label>
          <input
            disabled
            value={user?.email || ''}
            className="mt-1 w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2 text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-800"
          />
        </div>
        <div>
          <label className="block text-xs font-medium">Full Name</label>
          <input
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
          />
        </div>
        <div>
          <label className="block text-xs font-medium">Job Title</label>
          <input
            value={jobTitle}
            onChange={(e) => setJobTitle(e.target.value)}
            placeholder="Senior Engineering Manager"
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
          />
        </div>
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
        >
          {saving ? 'Saving...' : 'Save Changes'}
        </button>
      </form>
    </div>
  );
}
