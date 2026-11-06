import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGameStore } from '../store/gameStore';

export const useSession = (requireAuth = true) => {
  const navigate = useNavigate();
  const session = useGameStore((state) => state.session);

  useEffect(() => {
    if (requireAuth && !session?.sessionToken) {
      navigate('/', { replace: true });
    }
  }, [requireAuth, session?.sessionToken, navigate]);

  return session;
};
