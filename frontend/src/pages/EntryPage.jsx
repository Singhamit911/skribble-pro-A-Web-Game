import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useGameStore } from '../store/gameStore';

const COLOR_SWATCHES = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#FED766',
  '#9B5DE5', '#F15BB5', '#00BBF9', '#00F5D4'
];

export default function EntryPage() {
  const navigate = useNavigate();
  const setSession = useGameStore((state) => state.setSession);

  const [name, setName] = useState('');
  const [selectedColor, setSelectedColor] = useState(COLOR_SWATCHES[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmed = name.trim();
    if (trimmed.length < 3 || trimmed.length > 20) {
      setError('Name must be between 3 and 20 characters');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post('/sessions', {
        name: trimmed,
        color: selectedColor
      });

      setSession(response.data);
      navigate('/lobby');
    } catch (err) {
      console.error('Session error:', err);
      setError(err.response?.data?.message || 'Failed to create guest session. Is the server running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-slate-800/90 border border-slate-700 rounded-2xl p-8 shadow-2xl backdrop-blur-sm">
        <div className="text-center mb-6">
          <div className="text-5xl mb-2">🎨</div>
          <h1 className="text-3xl font-black tracking-tight text-white">SkribblePro</h1>
          <p className="text-sm text-slate-400 mt-1">Multiplayer Drawing with Sabotage Power-ups</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/15 border border-red-500/30 rounded-xl text-red-400 text-xs font-semibold">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">
              Your Player Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. PixelMaster"
              maxLength={20}
              autoFocus
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-400 transition"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">
              Choose Avatar Color
            </label>
            <div className="grid grid-cols-4 gap-3">
              {COLOR_SWATCHES.map((color) => (
                <button
                  type="button"
                  key={color}
                  onClick={() => setSelectedColor(color)}
                  style={{ backgroundColor: color }}
                  className={`h-10 rounded-xl transition transform hover:scale-105 flex items-center justify-center ${
                    selectedColor === color ? 'ring-4 ring-white ring-offset-2 ring-offset-slate-800 scale-105' : 'opacity-85'
                  }`}
                >
                  {selectedColor === color && (
                    <span className="text-white text-xs font-black drop-shadow">✓</span>
                  )}
                </button>
              ))}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 px-4 rounded-xl font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 transition duration-150 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-emerald-500/20"
          >
            {loading ? 'Creating session...' : 'Play Now 🚀'}
          </button>
        </form>

        <p className="text-center text-xs text-slate-500 mt-6">
          No sign-in or password required. Pick a name and play instantly!
        </p>
      </div>
    </div>
  );
}
