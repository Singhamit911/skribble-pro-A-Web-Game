import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSession } from '../hooks/useSession';

export default function GamePage() {
  const { id } = useParams();
  const session = useSession(true);

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto flex flex-col justify-between">
      <header className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div>
          <div className="text-xs font-bold text-slate-400 uppercase tracking-widest">Active Game</div>
          <div className="text-xl font-mono font-bold text-white">ID: {id}</div>
        </div>
        <Link
          to="/lobby"
          className="text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-lg border border-slate-700 hover:border-slate-500 transition"
        >
          Exit Game
        </Link>
      </header>

      <main className="py-16 bg-slate-800/60 border border-slate-700/80 rounded-2xl p-8 text-center shadow-xl">
        <div className="text-5xl mb-3">🎨</div>
        <h2 className="text-xl font-bold text-white mb-2">Game Canvas Arena</h2>
        <p className="text-sm text-slate-400 max-w-md mx-auto mb-6">
          Real-time synchronized drawing canvas with power-ups (Freeze, Erase, Fake Lines) will be activated in Sprint 3 &amp; 4.
        </p>

        <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-full text-emerald-400 text-xs font-semibold">
          Canvas Engine Ready for Sprint 3
        </div>
      </main>

      <footer className="text-center text-xs text-slate-600 py-4 border-t border-slate-800">
        SkribblePro • Sprint 0 Placeholder
      </footer>
    </div>
  );
}
