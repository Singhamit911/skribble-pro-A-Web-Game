import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '../hooks/useSession';
import { useGameStore } from '../store/gameStore';

export default function LobbyPage() {
  const session = useSession(true);
  const clearSession = useGameStore((state) => state.clearSession);
  const navigate = useNavigate();

  const [roomCode, setRoomCode] = useState('');

  const handleLogout = () => {
    clearSession();
    navigate('/');
  };

  const handleJoin = (e) => {
    e.preventDefault();
    if (roomCode.trim().length === 6) {
      navigate(`/room/${roomCode.trim().toUpperCase()}`);
    }
  };

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto flex flex-col justify-between">
      {/* Top Bar */}
      <header className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shadow-md"
            style={{ backgroundColor: session.color }}
          >
            {session.name ? session.name.charAt(0).toUpperCase() : '?'}
          </div>
          <div>
            <div className="font-bold text-white text-base leading-tight">{session.name}</div>
            <div className="text-xs text-slate-400">Guest Player</div>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-lg border border-slate-700 hover:border-slate-500 transition"
        >
          Change Name
        </button>
      </header>

      {/* Main Lobby Cards */}
      <main className="py-12 grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Create Room Card */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col justify-between shadow-xl">
          <div>
            <div className="text-3xl mb-2">🏠</div>
            <h2 className="text-xl font-bold text-white">Create a Private Room</h2>
            <p className="text-sm text-slate-400 mt-2">
              Start a custom game with your friends. Choose rounds, drawing time, and category.
            </p>
          </div>

          <div className="mt-6">
            <button
              onClick={() => alert('Sprint 1 will connect room creation!')}
              className="w-full py-3 px-4 rounded-xl font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 transition"
            >
              + Create Room
            </button>
            <p className="text-[11px] text-slate-500 text-center mt-2">Sprint 1 implementation</p>
          </div>
        </div>

        {/* Join Room Card */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col justify-between shadow-xl">
          <div>
            <div className="text-3xl mb-2">🔑</div>
            <h2 className="text-xl font-bold text-white">Join by Room Code</h2>
            <p className="text-sm text-slate-400 mt-2">
              Have a 6-character room code from a friend? Enter it below to join their lobby.
            </p>
          </div>

          <form onSubmit={handleJoin} className="mt-6 space-y-3">
            <input
              type="text"
              value={roomCode}
              onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
              placeholder="e.g. XK92TZ"
              maxLength={6}
              className="w-full text-center tracking-widest font-mono text-lg uppercase px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-400 transition"
            />
            <button
              type="submit"
              disabled={roomCode.trim().length !== 6}
              className="w-full py-3 px-4 rounded-xl font-bold text-white bg-slate-700 hover:bg-slate-600 disabled:opacity-40 disabled:cursor-not-allowed transition"
            >
              Join Room
            </button>
            <p className="text-[11px] text-slate-500 text-center mt-2">Sprint 1 implementation</p>
          </form>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center text-xs text-slate-600 py-4 border-t border-slate-800">
        SkribblePro Sprint 0 Scaffolding • Spring Boot 3.3.5 &amp; Spring Cloud Gateway
      </footer>
    </div>
  );
}
