import { describe, it, expect, beforeEach } from 'vitest';
import { useGameStore } from './gameStore';

describe('gameStore (Zustand)', () => {
  beforeEach(() => {
    localStorage.clear();
    useGameStore.getState().clearSession();
    useGameStore.getState().setRoom(null);
    useGameStore.getState().setPlayers([]);
    useGameStore.getState().setGameState(null);
  });

  it('should initialize with empty or null session when localStorage is clear', () => {
    const session = useGameStore.getState().session;
    expect(session.sessionToken).toBeNull();
    expect(session.guestId).toBeNull();
    expect(session.name).toBe('');
    expect(session.color).toBe('#FF6B6B');
  });

  it('should setSession and persist all fields to localStorage', () => {
    const newSession = {
      sessionToken: 'token-abc-123',
      guestId: 'guest-uuid-1',
      name: 'PixelArtist',
      color: '#4ECDC4'
    };

    useGameStore.getState().setSession(newSession);

    // Verify Zustand state
    expect(useGameStore.getState().session).toEqual(newSession);

    // Verify localStorage persistence
    expect(localStorage.getItem('skribblepro_session_token')).toBe('token-abc-123');
    expect(localStorage.getItem('skribblepro_guest_id')).toBe('guest-uuid-1');
    expect(localStorage.getItem('skribblepro_name')).toBe('PixelArtist');
    expect(localStorage.getItem('skribblepro_color')).toBe('#4ECDC4');
  });

  it('should clearSession and purge localStorage keys', () => {
    const session = {
      sessionToken: 'token-xyz',
      guestId: 'guest-xyz',
      name: 'ToClear',
      color: '#00F5D4'
    };

    useGameStore.getState().setSession(session);
    expect(localStorage.getItem('skribblepro_session_token')).toBe('token-xyz');

    useGameStore.getState().clearSession();

    expect(useGameStore.getState().session.sessionToken).toBeNull();
    expect(useGameStore.getState().session.guestId).toBeNull();
    expect(localStorage.getItem('skribblepro_session_token')).toBeNull();
    expect(localStorage.getItem('skribblepro_guest_id')).toBeNull();
  });

  it('should update room and players state', () => {
    const mockRoom = { code: 'XK92TZ', status: 'WAITING', rounds: 3 };
    const mockPlayers = [
      { guestId: 'g1', name: 'Sangi', color: '#FF6B6B', isHost: true },
      { guestId: 'g2', name: 'Alex', color: '#4ECDC4', isHost: false }
    ];

    useGameStore.getState().setRoom(mockRoom);
    useGameStore.getState().setPlayers(mockPlayers);

    expect(useGameStore.getState().room).toEqual(mockRoom);
    expect(useGameStore.getState().players).toHaveLength(2);
    expect(useGameStore.getState().players[0].name).toBe('Sangi');
  });

  it('should update gameState correctly', () => {
    const mockGameState = { phase: 'DRAWING', currentRound: 1, timeLeft: 80 };
    useGameStore.getState().setGameState(mockGameState);

    expect(useGameStore.getState().gameState).toEqual(mockGameState);
  });
});

