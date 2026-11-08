import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useSession } from '../hooks/useSession';
import { useGameStore } from '../store/gameStore';

export default function LobbyPage() {
  const session = useSession(true);
  const clearSession = useGameStore((state) => state.clearSession);
  const setRoom = useGameStore((state) => state.setRoom);
  const setPlayers = useGameStore((state) => state.setPlayers);
  const navigate = useNavigate();

  // Create room options
  const [rounds, setRounds] = useState(3);
  const [drawTime, setDrawTime] = useState(80);
  const [category, setCategory] = useState('General');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState('');

  // Join room state
  const [roomCode, setRoomCode] = useState('');
  const [joinLoading, setJoinLoading] = useState(false);
  const [joinError, setJoinError] = useState('');

  const handleLogout = () => {
    clearSession();
    navigate('/');
  };

  const handleCreateRoom = async () => {
    setCreateError('');
    setCreateLoading(true);

    try {
      const response = await api.post('/rooms', {
        rounds: Number(rounds),
        drawTime: Number(drawTime),
        maxPlayers: 8,
        category
      });

      setRoom(response.data);
      setPlayers(response.data.players || []);
      navigate(`/room/${response.data.code}`);
    } catch (err) {
      console.error('Create room error:', err);
      setCreateError(err.response?.data?.message || 'Failed to create room. Please try again.');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleJoin = async (e) => {
    e.preventDefault();
    setJoinError('');

    const code = roomCode.trim().toUpperCase();
    if (code.length !== 6) {
      setJoinError('Room code must be exactly 6 characters');
      return;
    }

    setJoinLoading(true);
    try {
      const response = await api.post(`/rooms/${code}/join`);
      setRoom(response.data);
      setPlayers(response.data.players || []);
      navigate(`/room/${response.data.code}`);
    } catch (err) {
      console.error('Join room error:', err);
      setJoinError(err.response?.data?.message || 'Room not found or unable to join.');
    } finally {
      setJoinLoading(false);
    }
  };

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto flex flex-col justify-between">
      {/* Top Bar */}
      <header className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shadow-md ring-2 ring-slate-700"
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
          Change Avatar
        </button>
      </header>

      {/* Main Lobby Cards */}
      <main className="py-10 grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Create Room Card */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col justify-between shadow-xl backdrop-blur-sm">
          <div>
            <div className="text-3xl mb-2">🏠</div>
            <h2 className="text-xl font-bold text-white">Create a Private Room</h2>
            <p className="text-xs text-slate-400 mt-1 mb-5">
              Host a new room with custom rules and invite your friends.
            </p>

            {createError && (
              <div className="mb-4 p-2.5 bg-red-500/15 border border-red-500/30 rounded-xl text-red-400 text-xs font-semibold">
                {createError}
              </div>
            )}

            {/* Room Settings Controls */}
            <div className="space-y-4">
              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Rounds: <span className="text-emerald-400 font-black">{rounds}</span>
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[3, 5, 8].map((r) => (
                    <button
                      key={r}
                      type="button"
                      onClick={() => setRounds(r)}
                      className={`py-1.5 text-xs font-bold rounded-lg border transition ${
                        rounds === r
                          ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300 shadow-sm'
                          : 'bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {r} Rounds
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Draw Time: <span className="text-emerald-400 font-black">{drawTime}s</span>
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[60, 80, 100].map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setDrawTime(t)}
                      className={`py-1.5 text-xs font-bold rounded-lg border transition ${
                        drawTime === t
                          ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300 shadow-sm'
                          : 'bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {t} Seconds
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Word Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-400"
                >
                  <option value="General">🌍 General Mix</option>
                  <option value="Animals">🦁 Animals &amp; Nature</option>
                  <option value="Food">🍕 Food &amp; Cooking</option>
                  <option value="Gaming">🎮 Gaming &amp; Tech</option>
                </select>
              </div>
            </div>
          </div>

          <div className="mt-6">
            <button
              onClick={handleCreateRoom}
              disabled={createLoading}
              className="w-full py-3 px-4 rounded-xl font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 disabled:opacity-50 transition shadow-lg shadow-emerald-500/20"
            >
              {createLoading ? 'Generating Room...' : '+ Create Room 🚀'}
            </button>
          </div>
        </div>

        {/* Join Room Card */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col justify-between shadow-xl backdrop-blur-sm">
          <div>
            <div className="text-3xl mb-2">🔑</div>
            <h2 className="text-xl font-bold text-white">Join by Room Code</h2>
            <p className="text-xs text-slate-400 mt-1 mb-5">
              Have a 6-character room code from a friend? Enter it below.
            </p>

            {joinError && (
              <div className="mb-4 p-2.5 bg-red-500/15 border border-red-500/30 rounded-xl text-red-400 text-xs font-semibold">
                {joinError}
              </div>
            )}
          </div>

          <form onSubmit={handleJoin} className="space-y-4">
            <div>
              <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                6-Character Room Code
              </label>
              <input
                type="text"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                placeholder="e.g. XK92TZ"
                maxLength={6}
                className="w-full text-center tracking-widest font-mono text-xl font-black uppercase px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-400 transition"
              />
            </div>

            <button
              type="submit"
              disabled={joinLoading || roomCode.trim().length !== 6}
              className="w-full py-3 px-4 rounded-xl font-bold text-white bg-slate-700 hover:bg-slate-600 disabled:opacity-40 disabled:cursor-not-allowed transition"
            >
              {joinLoading ? 'Connecting...' : 'Join Room 🎮'}
            </button>
          </form>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center text-xs text-slate-600 py-4 border-t border-slate-800">
        SkribblePro • Sprint 1 Room Service Active
      </footer>
    </div>
  );
}
