import { describe, it, expect, beforeEach } from 'vitest';
import api from './api';

describe('API service (Axios interceptor)', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should not attach Authorization header if session token is absent', () => {
    // Invoke request interceptor directly
    const interceptor = api.interceptors.request.handlers[0].fulfilled;
    const config = { headers: {} };

    const modifiedConfig = interceptor(config);
    expect(modifiedConfig.headers.Authorization).toBeUndefined();
  });

  it('should automatically attach Authorization Bearer token when stored in localStorage', () => {
    localStorage.setItem('skribblepro_session_token', 'test-token-777');

    const interceptor = api.interceptors.request.handlers[0].fulfilled;
    const config = { headers: {} };

    const modifiedConfig = interceptor(config);
    expect(modifiedConfig.headers.Authorization).toBe('Bearer test-token-777');
  });
});

