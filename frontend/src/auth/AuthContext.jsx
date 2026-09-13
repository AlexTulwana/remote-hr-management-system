import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { apiFetch, setUnauthorizedHandler } from '../api/client';
import { getMe } from '../api/users';

const AuthContext = createContext(null);

function decodeToken(token) {
  try {
    const payload = jwtDecode(token);
    if (payload.exp && payload.exp * 1000 < Date.now()) return null;
    return { username: payload.sub, role: payload.role };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
    navigate('/login');
  }, [navigate]);

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
