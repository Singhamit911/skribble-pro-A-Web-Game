import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import EntryPage from './pages/EntryPage';
import LobbyPage from './pages/LobbyPage';
import RoomPage from './pages/RoomPage';
import GamePage from './pages/GamePage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<EntryPage />} />
        <Route path="/lobby" element={<LobbyPage />} />
        <Route path="/room/:code" element={<RoomPage />} />
        <Route path="/game/:id" element={<GamePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
