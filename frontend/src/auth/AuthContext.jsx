import { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate, useLocation, useNavigationType } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { apiFetch, setUnauthorizedHandler } from '../api/client';
import { getMe } from '../api/users';

const AuthContext = createContext(null);

// Routes reachable without being logged in. Landing back on one of these
// via browser back/forward counts as "leaving the app" (see below).
const PUBLIC_PATHS = ['/login'];

function decodeToken(token) {
  try {
    const payload = jwtDecode(token);
    if (payload.exp && payload.exp * 1000 < Date.now()) return null;
    return { username: payload.sub, role: payload.role };
  } catch {
    return null;
  }
}

function hasValidSession() {
  const token = localStorage.getItem('token');
  return Boolean(token && decodeToken(token));
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();
  const navigationType = useNavigationType(); // 'POP' | 'PUSH' | 'REPLACE'
  const userRef = useRef(null);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
    navigate('/login');
  }, [navigate]);

  useEffect(() => {
    userRef.current = user;
  }, [user]);

  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  useEffect(() => {
    async function restoreSession() {
      const token = localStorage.getItem('token');
      if (!token) {
        setLoading(false);
        return;
      }
      const decoded = decodeToken(token);
      if (!decoded) {
        localStorage.removeItem('token');
        setLoading(false);
        return;
      }
      try {
        const profile = await getMe();
        setUser({ ...decoded, ...profile });
      } catch {
        localStorage.removeItem('token');
      }
      setLoading(false);
    }
    restoreSession();
  }, []);

  // Guard against bfcache restoring a frozen authenticated snapshot after
  // logout: if a bfcache restore happens with no valid session, force a
  // hard reload so the app re-runs its real auth check.
  useEffect(() => {
    function handlePageShow(event) {
      if (event.persisted && !hasValidSession()) {
        window.location.reload();
      }
    }
    window.addEventListener('pageshow', handlePageShow);
    return () => window.removeEventListener('pageshow', handlePageShow);
  }, []);

  // Deliberate policy: browser back/forward navigation is only allowed to
  // move between pages INSIDE the app while authenticated. If back/forward
  // lands on a public route (e.g. /login) while a session was active, that
  // counts as leaving the app, and the session is ended — so forward can
  // never be used afterwards to slip back into an authenticated page.
  useEffect(() => {
    if (
      navigationType === 'POP' &&
      PUBLIC_PATHS.includes(location.pathname) &&
      userRef.current
    ) {
      logout();
    }
  }, [location, navigationType, logout]);

  async function login(username, password) {
    const { token } = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    localStorage.setItem('token', token);
    const decoded = decodeToken(token);
    const profile = await getMe();
    const fullUser = { ...decoded, ...profile };
    setUser(fullUser);
    return fullUser.role;
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
