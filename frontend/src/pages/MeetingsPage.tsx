import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Upload, Trash2, Search, Sparkles, FileAudio, ArrowRight } from 'lucide-react';
import { meetingsApi, searchApi } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

export function MeetingsPage() {
  const [searchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') || '';
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('GENERAL');
  const [visibility, setVisibility] = useState('PRIVATE');
  const [participantsText, setParticipantsText] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);

  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [semanticMode, setSemanticMode] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const { data: meetingsPage, isLoading } = useQuery({
    queryKey: ['meetings', page],
    queryFn: () => meetingsApi.list({ page, size: 20 }),
  });

  const { data: searchResults } = useQuery({
    queryKey: ['search', searchQuery, semanticMode],
    queryFn: () =>
      semanticMode ? searchApi.semantic(searchQuery) : searchApi.fullText(searchQuery),
    enabled: searchQuery.trim().length > 0,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      const participants = participantsText
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .map((displayName) => ({ displayName, role: 'PARTICIPANT' }));

      const meeting = await meetingsApi.create({
        title,
        description,
        category,
        visibility,
        sourceType: selectedFile ? 'UPLOAD' : 'MANUAL',
        participants,
      });

      if (selectedFile) {
        await meetingsApi.upload(meeting.id, selectedFile, selectedFile.name, 120, setUploadProgress);
      }
      return meeting;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meetings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] });
      toast('Meeting created successfully');
      setShowCreate(false);
      setTitle('');
      setDescription('');
      setParticipantsText('');
      setSelectedFile(null);
      setUploadProgress(0);
    },
    onError: (err: unknown) => {
      toast(err instanceof Error ? err.message : 'Failed to create meeting', 'error');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => meetingsApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meetings'] });
      toast('Meeting deleted');
      setConfirmDeleteId(null);
    },
    onError: () => toast('Only the meeting creator can delete this meeting', 'error'),
  });

  const meetings = meetingsPage?.content || [];

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Meetings</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Upload audio/video recordings, transcribe speakers, and generate AI insights.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreate((v) => !v)}
          className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
        >
          <Plus className="h-4 w-4" />
          <span>Create Meeting</span>
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
        <div className="relative flex-1 min-w-60">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={semanticMode ? 'Semantic search by concept or topic...' : 'Filter meetings by title or transcript...'}
            className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-800"
          />
        </div>
        <button
          type="button"
          onClick={() => setSemanticMode((v) => !v)}
          className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-medium ${
            semanticMode
              ? 'border-indigo-500 bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
              : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'
          }`}
        >
          <Sparkles className="h-3.5 w-3.5" />
          <span>{semanticMode ? 'Semantic AI Search: ON' : 'Full-Text Search'}</span>
        </button>
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
        >
          <h2 className="text-base font-semibold">New Meeting</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium">Title *</label>
              <input
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Weekly Product Sync"
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
                >
                  <option value="GENERAL">General</option>
                  <option value="ENGINEERING">Engineering</option>
                  <option value="PRODUCT">Product</option>
                  <option value="SALES">Sales</option>
                  <option value="STANDUP">Standup</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium">Visibility</label>
                <select
                  value={visibility}
                  onChange={(e) => setVisibility(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
                >
                  <option value="PRIVATE">Private</option>
                  <option value="SHARED">Shared with Workspace</option>
                </select>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium">Description / Agenda</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Meeting objectives and topics..."
              className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium">Participants (comma-separated)</label>
              <input
                value={participantsText}
                onChange={(e) => setParticipantsText(e.target.value)}
                placeholder="Alice Chen, Bob Patel"
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
              />
            </div>
            <div>
              <label className="block text-xs font-medium">Audio/Video Recording (MP3, WAV, M4A, MP4, WEBM)</label>
              <label className="mt-1 flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-slate-300 px-3 py-2 text-sm text-slate-600 hover:border-indigo-500 dark:border-slate-700 dark:text-slate-300">
                <Upload className="h-4 w-4" />
                <span className="truncate">{selectedFile ? selectedFile.name : 'Choose recording file...'}</span>
                <input
                  type="file"
                  accept=".mp3,.wav,.m4a,.mp4,.webm"
                  className="hidden"
                  onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                />
              </label>
            </div>
          </div>

          {uploadProgress > 0 && uploadProgress < 100 && (
            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
              <div className="h-full bg-indigo-600 transition-all" style={{ width: `${uploadProgress}%` }} />
            </div>
          )}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm dark:border-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Saving...' : 'Save Meeting'}
            </button>
          </div>
        </form>
      )}

      {searchQuery.trim().length > 0 && searchResults ? (
        <div className="space-y-3">
          <h2 className="text-sm font-medium text-slate-500">
            Search results ({searchResults.length})
          </h2>
          {searchResults.map((hit, idx) => (
            <Link
              key={`${hit.meetingId}-${idx}`}
              to={`/meetings/${hit.meetingId}`}
              className="block rounded-xl border border-slate-200 bg-white p-4 hover:border-indigo-400 dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">{hit.meetingTitle}</span>
                {hit.timestamp && (
                  <span className="rounded bg-indigo-50 px-2 py-0.5 text-xs text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300">
                    {hit.timestamp}
                  </span>
                )}
              </div>
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{hit.snippet}</p>
            </Link>
          ))}
        </div>
      ) : isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-28 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
          ))}
        </div>
      ) : meetings.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 p-12 text-center dark:border-slate-700">
          <FileAudio className="mx-auto h-10 w-10 text-slate-400" />
          <h2 className="mt-3 text-base font-medium">No meetings yet</h2>
          <p className="mt-1 text-sm text-slate-500">Create a meeting or record live audio to get started.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {meetings.map((m) => (
            <div
              key={m.id}
              className="flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <Link to={`/meetings/${m.id}`} className="text-base font-semibold hover:text-indigo-600">
                    {m.title}
                  </Link>
                  <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                    {m.status}
                  </span>
                </div>
                <p className="mt-1 line-clamp-2 text-sm text-slate-600 dark:text-slate-400">
                  {m.description || 'No description'}
                </p>
              </div>

              <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500 dark:border-slate-800">
                <span>
                  {m.category} · {m.visibility}
                </span>
                <div className="flex items-center gap-2">
                  {confirmDeleteId === m.id ? (
                    <>
                      <button
                        type="button"
                        onClick={() => deleteMutation.mutate(m.id)}
                        className="font-medium text-rose-600 hover:underline"
                      >
                        Confirm
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmDeleteId(null)}
                        className="text-slate-500 hover:underline"
                      >
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmDeleteId(m.id)}
                      aria-label={`Delete ${m.title}`}
                      className="text-slate-400 hover:text-rose-500"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                  <Link
                    to={`/meetings/${m.id}`}
                    className="inline-flex items-center gap-1 font-medium text-indigo-600 hover:underline dark:text-indigo-400"
                  >
                    <span>Open</span>
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {searchQuery.trim().length === 0 && meetingsPage && meetingsPage.totalPages > 1 && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm dark:border-slate-800 dark:bg-slate-900">
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Page {meetingsPage.number + 1} of {meetingsPage.totalPages} ({meetingsPage.totalElements} meetings)
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={meetingsPage.number === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium hover:bg-slate-50 disabled:opacity-40 dark:border-slate-700 dark:hover:bg-slate-800"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={meetingsPage.number + 1 >= meetingsPage.totalPages}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium hover:bg-slate-50 disabled:opacity-40 dark:border-slate-700 dark:hover:bg-slate-800"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
