import { Link } from 'react-router-dom';
import { FolderKanban } from 'lucide-react';

interface ModulePlaceholderProps {
  title: string;
  phase: string;
  description: string;
}

export function ModulePlaceholderPage({ title, phase, description }: ModulePlaceholderProps) {
  return (
    <div className="mx-auto max-w-2xl rounded-xl border border-slate-200 bg-white p-8 text-center dark:border-slate-800 dark:bg-slate-900">
      <FolderKanban className="mx-auto h-10 w-10 text-indigo-500" />
      <span className="mt-4 inline-block rounded-full bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
        {phase}
      </span>
      <h1 className="mt-3 text-xl font-semibold">{title}</h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{description}</p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
      >
        Back to Dashboard
      </Link>
    </div>
  );
}
