import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSession } from '../hooks/useSession';

export default function RoomPage() {
  const { code } = useParams();
  const session = useSession(true);

  return (
    <div className="min-h-screen p-6 max-w-3xl mx-auto flex flex-col justify-between">
      <header className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div>
          <div className="text-xs font-bold text-slate-400 uppercase tracking-widest">Room Code</div>
          <div className="text-2xl font-mono font-black text-emerald-400">{code}</div>
        </div>
        <Link
          to="/lobby"
          className="text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-lg border border-slate-700 hover:border-slate-500 transition"
        >
          ← Leave Room
        </Link>
      </header>

      <main className="py-12 bg-slate-800/60 border border-slate-700/80 rounded-2xl p-8 text-center shadow-xl">
        <div className="text-4xl mb-3">👥</div>
        <h2 className="text-xl font-bold text-white mb-2">Waiting Lobby for {code}</h2>
        <p className="text-sm text-slate-400 max-w-md mx-auto mb-6">
          Players will join this lobby via WebSocket in Sprint 2. You are connected as{' '}
          <span className="font-bold text-white" style={{ color: session.color }}>
            {session.name}
          </span>
          .
        </p>

        <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-full text-emerald-400 text-xs font-semibold">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          Room Module Ready for Sprint 1 &amp; 2
        </div>
      </main>

      <footer className="text-center text-xs text-slate-600 py-4 border-t border-slate-800">
        SkribblePro • Sprint 0 Placeholder
      </footer>
    </div>
  );
}
