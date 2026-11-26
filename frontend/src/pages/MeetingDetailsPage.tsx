import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FileText,
  Sparkles,
  CheckSquare,
  MessageSquare,
  Download,
  Upload,
  Send,
  Save,
  Loader2,
} from 'lucide-react';
import { meetingsApi, actionItemsApi, type TranscriptSegmentDto } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

type TabKey = 'transcript' | 'summary' | 'actions' | 'chat' | 'mom';

function formatSec(s: number) {
  const mins = Math.floor(s / 60);
  const secs = Math.floor(s % 60);
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

export function MeetingDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const [tab, setTab] = useState<TabKey>('transcript');
  const [summaryLength, setSummaryLength] = useState<'SHORT' | 'DETAILED' | 'EXECUTIVE'>('DETAILED');
  const [question, setQuestion] = useState('');
  const [editingSegments, setEditingSegments] = useState<TranscriptSegmentDto[] | null>(null);
  const [agenda, setAgenda] = useState('');
  const [nextMeeting, setNextMeeting] = useState('');

  const { data: meeting, isLoading } = useQuery({
    queryKey: ['meeting', id],
    queryFn: () => meetingsApi.get(id!),
    enabled: !!id,
  });

  const { data: transcript } = useQuery({
    queryKey: ['transcript', id],
    queryFn: () => meetingsApi.getTranscript(id!),
    enabled: !!id,
    retry: false,
  });

  const { data: summary } = useQuery({
    queryKey: ['summary', id],
    queryFn: () => meetingsApi.getSummary(id!),
    enabled: !!id,
    retry: false,
  });

  const { data: actionItems } = useQuery({
    queryKey: ['action-items', id],
    queryFn: () => actionItemsApi.list({ meetingId: id }),
    enabled: !!id,
  });

  const { data: chatHistory } = useQuery({
    queryKey: ['chat-history', id],
    queryFn: () => meetingsApi.chatHistory(id!),
    enabled: !!id,
  });

  const { data: mom, refetch: refetchMom } = useQuery({
    queryKey: ['mom', id],
    queryFn: () => meetingsApi.generateMom(id!, { agenda, nextMeetingDetails: nextMeeting }),
    enabled: false,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => meetingsApi.upload(id!, file, file.name, 120),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meeting', id] });
      toast('Recording uploaded');
    },
    onError: (err: unknown) => toast(err instanceof Error ? err.message : 'Upload failed', 'error'),
  });

  const transcribeMutation = useMutation({
    mutationFn: () => meetingsApi.transcribe(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meeting', id] });
      queryClient.invalidateQueries({ queryKey: ['transcript', id] });
      toast('Transcription completed and indexed for RAG');
    },
    onError: (err: unknown) => toast(err instanceof Error ? err.message : 'Transcription failed', 'error'),
  });

  const saveTranscriptMutation = useMutation({
    mutationFn: (segments: TranscriptSegmentDto[]) =>
      meetingsApi.updateTranscript(
        id!,
        segments.map((s) => ({
          id: s.id,
          speakerName: s.speakerName,
          content: s.content,
          startSeconds: s.startSeconds,
          endSeconds: s.endSeconds,
        }))
      ),
    onSuccess: () => {
      setEditingSegments(null);
      queryClient.invalidateQueries({ queryKey: ['transcript', id] });
      toast('Transcript updated and re-indexed');
    },
  });

  const summarizeMutation = useMutation({
    mutationFn: () => meetingsApi.summarize(id!, summaryLength),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['summary', id] });
      queryClient.invalidateQueries({ queryKey: ['action-items', id] });
      queryClient.invalidateQueries({ queryKey: ['meeting', id] });
      toast('AI summary & action items generated');
      setTab('summary');
    },
    onError: (err: unknown) => toast(err instanceof Error ? err.message : 'Summarization failed', 'error'),
  });

  const chatMutation = useMutation({
    mutationFn: (q: string) => meetingsApi.chat(id!, q),
    onSuccess: () => {
      setQuestion('');
      queryClient.invalidateQueries({ queryKey: ['chat-history', id] });
    },
    onError: (err: unknown) => toast(err instanceof Error ? err.message : 'Chat failed', 'error'),
  });

  const toggleItemMutation = useMutation({
    mutationFn: (item: { id: string; title: string; status: string }) =>
      actionItemsApi.update(item.id, {
        title: item.title,
        status: item.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED',
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['action-items', id] }),
  });

  const handleDownload = async (format: 'pdf' | 'docx' | 'txt') => {
    try {
      const blob = await meetingsApi.exportMom(id!, format);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `meeting-${id}-mom.${format}`;
      a.click();
      URL.revokeObjectURL(url);
      toast(`Exported ${format.toUpperCase()} report`);
    } catch {
      toast('Export failed', 'error');
    }
  };

  if (isLoading || !meeting) {
    return <div className="p-8 text-sm text-slate-500">Loading meeting workspace...</div>;
  }

  const segmentsToRender = editingSegments || transcript?.segments || [];

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
              {meeting.category}
            </span>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium dark:bg-slate-800">
              {meeting.status}
            </span>
          </div>
          <h1 className="mt-2 text-2xl font-semibold">{meeting.title}</h1>
          {meeting.description && (
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{meeting.description}</p>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800">
            <Upload className="h-3.5 w-3.5" />
            <span>Upload Media</span>
            <input
              type="file"
              accept=".mp3,.wav,.m4a,.mp4,.webm"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) uploadMutation.mutate(file);
              }}
            />
          </label>

          <button
            type="button"
            onClick={() => transcribeMutation.mutate()}
            disabled={transcribeMutation.isPending || meeting.files.length === 0}
            className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3.5 py-2 text-xs font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
          >
            {transcribeMutation.isPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <FileText className="h-3.5 w-3.5" />}
            <span>{transcript ? 'Re-Transcribe' : 'Transcribe'}</span>
          </button>

          <button
            type="button"
            onClick={() => summarizeMutation.mutate()}
            disabled={summarizeMutation.isPending || !transcript}
            className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-3.5 py-2 text-xs font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
          >
            {summarizeMutation.isPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Sparkles className="h-3.5 w-3.5" />}
            <span>Generate AI Summary</span>
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-2 dark:border-slate-800">
        {[
          { key: 'transcript', label: 'Transcript', icon: FileText },
          { key: 'summary', label: 'AI Summary', icon: Sparkles },
          { key: 'actions', label: 'Action Items', icon: CheckSquare },
          { key: 'chat', label: 'AI Chat (RAG)', icon: MessageSquare },
          { key: 'mom', label: 'MOM & Export', icon: Download },
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key as TabKey)}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium ${
              tab === key
                ? 'bg-indigo-600 text-white'
                : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
            }`}
          >
            <Icon className="h-4 w-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {tab === 'transcript' && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold">
              Timestamped Transcript {transcript ? `(${transcript.provider})` : ''}
            </h2>
            {transcript && (
              editingSegments ? (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setEditingSegments(null)}
                    className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs dark:border-slate-700"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={() => saveTranscriptMutation.mutate(editingSegments)}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white"
                  >
                    <Save className="h-3.5 w-3.5" />
                    <span>Save Changes</span>
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setEditingSegments(transcript.segments)}
                  className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
                >
                  Edit Transcript
                </button>
              )
            )}
          </div>

          {!transcript ? (
            <p className="mt-4 text-sm text-slate-500">
              Upload an audio/video file above and click <strong>Transcribe</strong> to generate speaker-diarized segments.
            </p>
          ) : (
            <div className="mt-4 space-y-3">
              {segmentsToRender.map((seg, index) => (
                <div key={seg.id || index} className="rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                  <div className="flex items-center gap-2 text-xs text-slate-500">
                    <span className="rounded bg-slate-100 px-2 py-0.5 font-mono dark:bg-slate-800">
                      {formatSec(seg.startSeconds)}
                    </span>
                    {editingSegments ? (
                      <input
                        value={seg.speakerName}
                        onChange={(e) => {
                          const copy = [...editingSegments];
                          copy[index] = { ...seg, speakerName: e.target.value };
                          setEditingSegments(copy);
                        }}
                        className="rounded border border-slate-300 px-2 py-0.5 text-xs font-semibold dark:border-slate-700 dark:bg-slate-800"
                      />
                    ) : (
                      <span className="font-semibold text-indigo-600 dark:text-indigo-400">{seg.speakerName}</span>
                    )}
                  </div>
                  {editingSegments ? (
                    <textarea
                      rows={2}
                      value={seg.content}
                      onChange={(e) => {
                        const copy = [...editingSegments];
                        copy[index] = { ...seg, content: e.target.value };
                        setEditingSegments(copy);
                      }}
                      className="mt-2 w-full rounded border border-slate-300 p-2 text-sm dark:border-slate-700 dark:bg-slate-800"
                    />
                  ) : (
                    <p className="mt-1.5 text-sm leading-relaxed">{seg.content}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'summary' && (
        <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-base font-semibold">Structured AI Summary</h2>
            <div className="flex items-center gap-2">
              <select
                value={summaryLength}
                onChange={(e) => setSummaryLength(e.target.value as 'SHORT' | 'DETAILED' | 'EXECUTIVE')}
                className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs dark:border-slate-700 dark:bg-slate-800"
              >
                <option value="SHORT">Short</option>
                <option value="DETAILED">Detailed</option>
                <option value="EXECUTIVE">Executive</option>
              </select>
              <button
                type="button"
                onClick={() => summarizeMutation.mutate()}
                disabled={!transcript || summarizeMutation.isPending}
                className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50"
              >
                Regenerate
              </button>
            </div>
          </div>

          {!summary ? (
            <p className="text-sm text-slate-500">Click Generate AI Summary once the meeting is transcribed.</p>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {[
                ['Executive Summary', summary.executiveSummary],
                ['Meeting Objectives', summary.objectives],
                ['Main Discussion Points', summary.discussionPoints],
                ['Important Highlights', summary.highlights],
                ['Decisions Made', summary.decisionsSummary],
                ['Problems Identified', summary.problemsIdentified],
                ['Risks & Dependencies', summary.risksDependencies],
                ['Next Steps', summary.nextSteps],
                ['Follow-up Requirements', summary.followUpRequirements],
              ].map(([label, text]) => (
                <div key={label} className="rounded-lg border border-slate-100 p-4 dark:border-slate-800">
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                    {label}
                  </h3>
                  <p className="mt-2 whitespace-pre-line text-sm text-slate-700 dark:text-slate-300">
                    {text || '—'}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'actions' && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-base font-semibold">Action Items ({actionItems?.length || 0})</h2>
          <div className="mt-4 space-y-2.5">
            {(!actionItems || actionItems.length === 0) ? (
              <p className="text-sm text-slate-500">No action items extracted yet.</p>
            ) : (
              actionItems.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-between rounded-lg border border-slate-100 p-3 dark:border-slate-800"
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      checked={item.status === 'COMPLETED'}
                      onChange={() => toggleItemMutation.mutate(item)}
                      className="h-4 w-4 rounded border-slate-300 text-indigo-600"
                    />
                    <div>
                      <p className={`text-sm font-medium ${item.status === 'COMPLETED' ? 'line-through text-slate-400' : ''}`}>
                        {item.title}
                      </p>
                      <p className="text-xs text-slate-500">
                        Owner: {item.assignedToName || 'Unassigned'} · Priority: {item.priority}
                        {item.transcriptTimestamp ? ` · @${item.transcriptTimestamp}` : ''}
                      </p>
                    </div>
                  </div>
                  <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs dark:bg-slate-800">
                    {item.status}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {tab === 'chat' && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 className="text-base font-semibold">AI Meeting Chat (RAG Grounded)</h2>
          <div className="mt-4 max-h-96 space-y-3 overflow-y-auto pr-1">
            {(!chatHistory || chatHistory.length === 0) ? (
              <p className="text-sm text-slate-500">
                Ask anything about this meeting, e.g. &ldquo;What deadlines were agreed upon?&rdquo;
              </p>
            ) : (
              chatHistory.map((msg) => (
                <div
                  key={msg.id}
                  className={`rounded-xl p-4 text-sm ${
                    msg.role === 'USER'
                      ? 'ml-auto max-w-xl bg-indigo-600 text-white'
                      : 'max-w-2xl border border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/70'
                  }`}
                >
                  <p className="whitespace-pre-line">{msg.content}</p>
                  {msg.citations && msg.citations.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {msg.citations.map((c, i) => (
                        <span
                          key={i}
                          className="rounded bg-indigo-100 px-2 py-0.5 text-[11px] font-medium text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300"
                        >
                          [{c.timestamp}]
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (question.trim()) chatMutation.mutate(question.trim());
            }}
            className="mt-4 flex gap-2"
          >
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask a question about this meeting transcript..."
              className="flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-800"
            />
            <button
              type="submit"
              disabled={chatMutation.isPending}
              className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              <Send className="h-4 w-4" />
              <span>Ask</span>
            </button>
          </form>
        </div>
      )}

      {tab === 'mom' && (
        <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-base font-semibold">Minutes of Meeting (MOM) &amp; Exports</h2>
            <div className="flex gap-2">
              {(['pdf', 'docx', 'txt'] as const).map((fmt) => (
                <button
                  key={fmt}
                  type="button"
                  onClick={() => handleDownload(fmt)}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
                >
                  <Download className="h-3.5 w-3.5" />
                  <span>Export {fmt.toUpperCase()}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium">Custom Agenda (optional)</label>
              <input
                value={agenda}
                onChange={(e) => setAgenda(e.target.value)}
                placeholder="Sprint review & architecture sign-off"
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
              />
            </div>
            <div>
              <label className="block text-xs font-medium">Next Meeting Details (optional)</label>
              <input
                value={nextMeeting}
                onChange={(e) => setNextMeeting(e.target.value)}
                placeholder="Next Friday 10:00 AM UTC"
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
              />
            </div>
          </div>

          <button
            type="button"
            onClick={() => refetchMom()}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
          >
            Preview MOM
          </button>

          {mom && (
            <div className="space-y-3 rounded-lg border border-slate-200 p-4 text-sm dark:border-slate-800">
              <h3 className="font-semibold">{mom.title}</h3>
              <p><strong>Agenda:</strong> {mom.agenda}</p>
              <p><strong>Discussion Summary:</strong> {mom.discussionSummary}</p>
              <p><strong>Next Meeting:</strong> {mom.nextMeetingDetails}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
