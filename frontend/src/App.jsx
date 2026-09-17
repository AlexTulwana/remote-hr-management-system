import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Profile from './pages/Profile';
import Messages from './pages/Messages';
import Announcements from './pages/Announcements';
import Calendar from './pages/Calendar';
import Documents from './pages/Documents';
import EmployeeDashboard from './pages/employee/EmployeeDashboard';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './auth/ProtectedRoute';

const TEMP_NAV = [
  { to: '/employee/dashboard', label: 'Dashboard' },
  { to: '/profile', label: 'Profile' },
  { to: '/messages', label: 'Messages' },
  { to: '/announcements', label: 'Announcements' },
  { to: '/calendar', label: 'Calendar' },
  { to: '/documents', label: 'Documents' },
];

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/employee/dashboard"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={TEMP_NAV}>
              <EmployeeDashboard />
            </AppLayout>
          </ProtectedRoute>
        }
      />
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
      <Route
        path="/messages"
        element={
          <ProtectedRoute>
            <AppLayout navItems={TEMP_NAV}>
              <Messages />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/announcements"
        element={
          <ProtectedRoute>
            <AppLayout navItems={TEMP_NAV}>
              <Announcements />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/calendar"
        element={
          <ProtectedRoute>
            <AppLayout navItems={TEMP_NAV}>
              <Calendar />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/documents"
        element={
          <ProtectedRoute>
            <AppLayout navItems={TEMP_NAV}>
              <Documents />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
