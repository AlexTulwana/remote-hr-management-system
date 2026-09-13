import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Profile from './pages/Profile';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './auth/ProtectedRoute';

const TEMP_NAV = [{ to: '/profile', label: 'Profile' }];

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <AppLayout navItems={TEMP_NAV}>
              <Profile />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
