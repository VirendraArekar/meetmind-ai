import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { UserPlus, Users } from 'lucide-react';
import { orgApi } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

export function TeamPage() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('MEMBER');
  const [teamName, setTeamName] = useState('');

  const { data: org, isLoading } = useQuery({
    queryKey: ['current-org'],
    queryFn: orgApi.current,
  });

  const inviteMutation = useMutation({
    mutationFn: () => orgApi.invite({ email: inviteEmail, workspaceRole: inviteRole }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['current-org'] });
      setInviteEmail('');
      toast('Member added to workspace');
    },
    onError: (err: unknown) => toast(err instanceof Error ? err.message : 'Unable to add member', 'error'),
  });

  const teamMutation = useMutation({
    mutationFn: () => orgApi.createTeam({ name: teamName }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['current-org'] });
      setTeamName('');
      toast('Team created');
    },
  });

  if (isLoading || !org) {
    return <div className="p-8 text-sm text-slate-500">Loading workspace...</div>;
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{org.name}</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Manage workspace members, roles, and teams. Your role: <strong>{org.myRole}</strong>
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="flex items-center gap-2 text-base font-semibold">
            <UserPlus className="h-4 w-4 text-indigo-500" />
            <span>Workspace Members ({org.members.length})</span>
          </h2>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (inviteEmail.trim()) inviteMutation.mutate();
            }}
            className="flex gap-2"
          >
            <input
              type="email"
              required
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="colleague@company.com"
              className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
            />
            <select
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value)}
              className="rounded-lg border border-slate-200 px-2.5 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
            >
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Admin</option>
              <option value="VIEWER">Viewer</option>
            </select>
            <button
              type="submit"
              className="rounded-lg bg-indigo-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-indigo-500"
            >
              Add
            </button>
          </form>

          <div className="space-y-2">
            {org.members.map((m) => (
              <div key={m.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                <div>
                  <p className="text-sm font-medium">{m.fullName}</p>
                  <p className="text-xs text-slate-500">{m.email}</p>
                </div>
                <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium dark:bg-slate-800">
                  {m.workspaceRole}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="flex items-center gap-2 text-base font-semibold">
            <Users className="h-4 w-4 text-indigo-500" />
            <span>Teams ({org.teams.length})</span>
          </h2>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (teamName.trim()) teamMutation.mutate();
            }}
            className="flex gap-2"
          >
            <input
              value={teamName}
              onChange={(e) => setTeamName(e.target.value)}
              placeholder="e.g. Platform Engineering"
              className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
            />
            <button
              type="submit"
              className="rounded-lg bg-indigo-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-indigo-500"
            >
              Create
            </button>
          </form>

          <div className="space-y-2">
            {org.teams.length === 0 ? (
              <p className="text-sm text-slate-500">No teams created yet.</p>
            ) : (
              org.teams.map((t) => (
                <div key={t.id} className="rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                  <p className="text-sm font-medium">{t.name}</p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
