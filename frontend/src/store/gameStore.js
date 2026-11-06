import { create } from 'zustand';

export const useGameStore = create((set) => ({
  // Player session info
  session: {
    sessionToken: localStorage.getItem('skribblepro_session_token') || null,
    guestId: localStorage.getItem('skribblepro_guest_id') || null,
    name: localStorage.getItem('skribblepro_name') || '',
    color: localStorage.getItem('skribblepro_color') || '#FF6B6B'
  },

  setSession: (sessionData) => {
    localStorage.setItem('skribblepro_session_token', sessionData.sessionToken);
    localStorage.setItem('skribblepro_guest_id', sessionData.guestId);
    localStorage.setItem('skribblepro_name', sessionData.name);
    localStorage.setItem('skribblepro_color', sessionData.color);

    set({ session: sessionData });
  },

  clearSession: () => {
    localStorage.removeItem('skribblepro_session_token');
    localStorage.removeItem('skribblepro_guest_id');
    localStorage.removeItem('skribblepro_name');
    localStorage.removeItem('skribblepro_color');

    set({
      session: { sessionToken: null, guestId: null, name: '', color: '#FF6B6B' }
    });
  },

  // Active room data
  room: null,
  setRoom: (room) => set({ room }),

  // Players in current room
  players: [],
  setPlayers: (players) => set({ players }),

  // Live game state
  gameState: null,
  setGameState: (gameState) => set({ gameState })
}));
