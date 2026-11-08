import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useSession } from '../hooks/useSession';
import { useGameStore } from '../store/gameStore';

export default function RoomPage() {
  const { code } = useParams();
  const session = useSession(true);
  const navigate = useNavigate();

  const room = useGameStore((state) => state.room);
  const players = useGameStore((state) => state.players);
  const setRoom = useGameStore((state) => state.setRoom);
  const setPlayers = useGameStore((state) => state.setPlayers);

  const [loading, setLoading] = useState(!room);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const fetchRoom = async () => {
      try {
        const response = await api.get(`/rooms/${code.toUpperCase()}`);
        if (isMounted) {
          setRoom(response.data);
          setPlayers(response.data.players || []);
          setError('');
        }
      } catch (err) {
        console.error('Failed to load room:', err);
        if (isMounted) {
          setError(err.response?.data?.message || 'Room not found or expired.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchRoom();

    return () => {
      isMounted = false;
    };
  }, [code, setRoom, setPlayers]);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(code.toUpperCase());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLeaveRoom = async () => {
    setLeaving(true);
    try {
      await api.delete(`/rooms/${code.toUpperCase()}/leave`);
    } catch (err) {
      console.warn('Error while leaving room:', err);
    } finally {
      setRoom(null);
      setPlayers([]);
      navigate('/lobby');
    }
  };

  const isHost = room?.hostGuestId === session?.guestId;

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6 text-center">
        <div className="space-y-3">
          <div className="w-10 h-10 border-4 border-emerald-400 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p className="text-sm text-slate-400">Connecting to room {code}...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-slate-800 border border-slate-700 rounded-2xl p-8 text-center shadow-2xl">
          <div className="text-4xl mb-3">⚠️</div>
          <h2 className="text-xl font-bold text-white mb-2">Room Error</h2>
          <p className="text-sm text-slate-400 mb-6">{error}</p>
          <button
            onClick={() => navigate('/lobby')}
            className="w-full py-3 px-4 rounded-xl font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 transition"
          >
            Back to Lobby
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto flex flex-col justify-between">
      {/* Top Bar */}
      <header className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div className="flex items-center gap-4">
          <div>
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-widest">Room Code</div>
            <div className="flex items-center gap-2">
              <span className="text-3xl font-mono font-black text-emerald-400 tracking-wider">
                {code?.toUpperCase()}
              </span>
              <button
                onClick={handleCopyCode}
                className="text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 px-2.5 py-1 rounded-lg border border-slate-700 transition"
              >
                {copied ? '✓ Copied' : 'Copy'}
              </button>
            </div>
          </div>

          {/* Room Settings Badges */}
          <div className="hidden sm:flex items-center gap-2 pl-4 border-l border-slate-800">
            <span className="px-2.5 py-1 bg-slate-800 border border-slate-700 rounded-lg text-xs font-semibold text-slate-300">
              🎯 {room?.rounds || 3} Rounds
            </span>
            <span className="px-2.5 py-1 bg-slate-800 border border-slate-700 rounded-lg text-xs font-semibold text-slate-300">
              ⏱️ {room?.drawTime || 80}s
            </span>
            <span className="px-2.5 py-1 bg-slate-800 border border-slate-700 rounded-lg text-xs font-semibold text-slate-300">
              📚 {room?.category || 'General'}
            </span>
          </div>
        </div>

        <button
          onClick={handleLeaveRoom}
          disabled={leaving}
          className="text-xs text-red-400 hover:text-red-300 px-3.5 py-2 rounded-xl bg-red-500/10 border border-red-500/30 hover:bg-red-500/20 transition font-semibold"
        >
          {leaving ? 'Leaving...' : 'Leave Room'}
        </button>
      </header>

      {/* Main Waiting Area */}
      <main className="py-8 space-y-6">
        {/* Banner */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-xl backdrop-blur-sm">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <span>Waiting Lobby</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
                {players.length} / {room?.maxPlayers || 8} Players
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Share the 6-character code with your friends to let them join. Live real-time syncing arrives in Sprint 2!
            </p>
          </div>

          {isHost ? (
            <button
              onClick={() => alert('Sprint 2 will connect live WebSocket presence, and Sprint 4 will start the turn-based drawing loop!')}
              className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 transition shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2 text-sm"
            >
              <span>Start Game</span>
              <span>🎮</span>
            </button>
          ) : (
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs font-semibold text-slate-300">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Waiting for host to start
            </div>
          )}
        </div>

        {/* Players Grid */}
        <div>
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">
            Connected Players ({players.length})
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {players.map((player) => {
              const isCurrentUser = player.guestId === session?.guestId;
              return (
                <div
                  key={player.guestId}
                  className={`p-4 rounded-xl border flex items-center gap-3 shadow-md transition ${
                    isCurrentUser
                      ? 'bg-slate-800/90 border-emerald-500/50 ring-1 ring-emerald-500/30'
                      : 'bg-slate-800/50 border-slate-700'
                  }`}
                >
                  <div
                    className="w-11 h-11 rounded-full flex items-center justify-center text-white font-bold text-lg shadow shrink-0"
                    style={{ backgroundColor: player.color }}
                  >
                    {player.name ? player.name.charAt(0).toUpperCase() : '?'}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5">
                      <span className="font-bold text-white text-sm truncate">{player.name}</span>
                      {player.isHost && (
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30 font-bold shrink-0">
                          HOST
                        </span>
                      )}
                    </div>
                    <div className="text-[11px] text-slate-400 flex items-center gap-1">
                      {isCurrentUser ? (
                        <span className="text-emerald-400 font-semibold">(You)</span>
                      ) : (
                        <span>Ready</span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center text-xs text-slate-600 py-4 border-t border-slate-800">
        SkribblePro • Sprint 1 Room Management
      </footer>
    </div>
  );
}
