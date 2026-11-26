import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mic, Square, Upload, ShieldAlert } from 'lucide-react';
import { meetingsApi } from '../lib/api';
import { useToast } from '../components/common/ToastContext';

export function LiveRecordingPage() {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [consentGranted, setConsentGranted] = useState(false);
  const [title, setTitle] = useState('');
  const [recording, setRecording] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
      if (audioUrl) URL.revokeObjectURL(audioUrl);
    };
  }, [audioUrl]);

  const startRecording = async () => {
    if (!consentGranted) {
      toast('Confirm participant recording consent first', 'error');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
      chunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };

      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
        setAudioBlob(blob);
        setAudioUrl(URL.createObjectURL(blob));
        stream.getTracks().forEach((t) => t.stop());
      };

      recorder.start(1000);
      mediaRecorderRef.current = recorder;
      setSeconds(0);
      setRecording(true);
      timerRef.current = window.setInterval(() => setSeconds((s) => s + 1), 1000);
    } catch {
      toast('Microphone permission denied or unavailable in this browser', 'error');
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setRecording(false);
  };

  const saveAndTranscribe = async () => {
    if (!audioBlob) return;
    setSaving(true);
    try {
      const meeting = await meetingsApi.create({
        title: title.trim() || `Live Recording ${new Date().toLocaleString()}`,
        category: 'GENERAL',
        visibility: 'PRIVATE',
        sourceType: 'LIVE_RECORDING',
        durationSeconds: seconds,
      });
      await meetingsApi.upload(meeting.id, audioBlob, 'live-recording.webm', seconds);
      await meetingsApi.transcribe(meeting.id);
      toast('Recording uploaded and transcribed');
      navigate(`/meetings/${meeting.id}`);
    } catch (err: unknown) {
      toast(err instanceof Error ? err.message : 'Failed to save recording', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Live Meeting Recording</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Capture audio directly in your browser using the MediaRecorder API.
        </p>
      </div>

      <div className="space-y-5 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        <div>
          <label className="block text-xs font-medium">Meeting Title</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Design Sync — Nov 26"
            className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800"
          />
        </div>

        <label className="flex items-start gap-2.5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-200">
          <input
            type="checkbox"
            checked={consentGranted}
            onChange={(e) => setConsentGranted(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded"
          />
          <span className="flex items-center gap-1.5">
            <ShieldAlert className="h-4 w-4 shrink-0" />
            I confirm all meeting participants have been notified and consented to this audio recording.
          </span>
        </label>

        <div className="flex flex-col items-center justify-center rounded-xl border border-slate-200 bg-slate-50 py-10 dark:border-slate-800 dark:bg-slate-950/60">
          <p className="font-mono text-3xl font-semibold">
            {String(Math.floor(seconds / 60)).padStart(2, '0')}:{String(seconds % 60).padStart(2, '0')}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {recording ? 'Recording in progress...' : 'Ready to record'}
          </p>

          <div className="mt-6 flex gap-3">
            {!recording ? (
              <button
                type="button"
                onClick={startRecording}
                className="inline-flex items-center gap-2 rounded-full bg-rose-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-rose-500"
              >
                <Mic className="h-4 w-4" />
                <span>Start Recording</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={stopRecording}
                className="inline-flex items-center gap-2 rounded-full bg-slate-900 px-6 py-2.5 text-sm font-medium text-white dark:bg-slate-100 dark:text-slate-900"
              >
                <Square className="h-4 w-4" />
                <span>Stop Recording</span>
              </button>
            )}
          </div>
        </div>

        {audioUrl && (
          <div className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800">
            <audio controls src={audioUrl} className="w-full" />
            <button
              type="button"
              onClick={saveAndTranscribe}
              disabled={saving}
              className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600 py-2.5 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              <Upload className="h-4 w-4" />
              <span>{saving ? 'Uploading & Transcribing...' : 'Save Meeting & Transcribe'}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
