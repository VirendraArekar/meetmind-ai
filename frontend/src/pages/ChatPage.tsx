import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MessageSquare, Send } from 'lucide-react';
import { meetingsApi } from '../lib/api';

export function ChatPage() {
  const queryClient = useQueryClient();
  const [selectedMeetingId, setSelectedMeetingId] = useState('');
  const [question, setQuestion] = useState('');

  const { data: meetingsPage } = useQuery({
    queryKey: ['meetings'],
    queryFn: () => meetingsApi.list(),
  });

  const meetings = meetingsPage?.content || [];
  const activeId = selectedMeetingId || meetings[0]?.id || '';

  const { data: history } = useQuery({
    queryKey: ['chat-history', activeId],
    queryFn: () => meetingsApi.chatHistory(activeId),
    enabled: !!activeId,
  });

  const chatMutation = useMutation({
    mutationFn: (q: string) => meetingsApi.chat(activeId, q),
    onSuccess: () => {
      setQuestion('');
      queryClient.invalidateQueries({ queryKey: ['chat-history', activeId] });
    },
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">AI Meeting Chat</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Ask grounded questions across any transcribed meeting using RAG.
          </p>
        </div>

        <select
          value={activeId}
          onChange={(e) => setSelectedMeetingId(e.target.value)}
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900"
        >
          {meetings.map((m) => (
            <option key={m.id} value={m.id}>
              {m.title}
            </option>
          ))}
        </select>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        {!activeId ? (
          <div className="py-12 text-center text-sm text-slate-500">
            <MessageSquare className="mx-auto mb-2 h-8 w-8 text-slate-400" />
            Create and transcribe a meeting first to start chatting.
          </div>
        ) : (
          <>
            <div className="max-h-96 space-y-3 overflow-y-auto">
              {(!history || history.length === 0) ? (
                <p className="text-sm text-slate-500">Ask a question about this meeting&apos;s transcript.</p>
              ) : (
                history.map((msg) => (
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
                        {msg.citations.map((c, idx) => (
                          <span
                            key={idx}
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
                placeholder="What decisions or deadlines were discussed?"
                className="flex-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
              />
              <button
                type="submit"
                disabled={chatMutation.isPending}
                className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
              >
                <Send className="h-4 w-4" />
                <span>Ask</span>
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
