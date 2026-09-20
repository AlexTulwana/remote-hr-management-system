import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Profile from './pages/Profile';
import Messages from './pages/Messages';
import Announcements from './pages/Announcements';
import Calendar from './pages/Calendar';
import Documents from './pages/Documents';
import EmployeeDashboard from './pages/employee/EmployeeDashboard';
import LeaveRequests from './pages/employee/LeaveRequests';
import Attendance from './pages/employee/Attendance';
import MyRequests from './pages/employee/MyRequests';
import Payslips from './pages/employee/Payslips';
import ManagerDashboard from './pages/manager/ManagerDashboard';
import LeaveApprovals from './pages/manager/LeaveApprovals';
import TeamRequests from './pages/manager/TeamRequests';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './auth/ProtectedRoute';
import { useAuth } from './auth/AuthContext';

function navItemsFor(role) {
  const items = [{ to: '/profile', label: 'Profile' }];
  if (role === 'EMPLOYEE') {
    items.push({ to: '/employee/dashboard', label: 'Dashboard' });
    items.push({ to: '/employee/leave-requests', label: 'Leave Requests' });
    items.push({ to: '/employee/attendance', label: 'Attendance' });
    items.push({ to: '/employee/my-requests', label: 'My Requests' });
    items.push({ to: '/employee/payslips', label: 'Payslips' });
  }
  if (role === 'MANAGER') {
    items.push({ to: '/manager/dashboard', label: 'Dashboard' });
    items.push({ to: '/manager/leave-approvals', label: 'Leave Approvals' });
    items.push({ to: '/manager/team-requests', label: 'Team Requests' });
  }
  items.push(
    { to: '/messages', label: 'Messages' },
    { to: '/announcements', label: 'Announcements' },
    { to: '/calendar', label: 'Calendar' },
    { to: '/documents', label: 'Documents' },
  );
  return items;
}

function App() {
  const { user } = useAuth();
  const navItems = navItemsFor(user?.role);

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/employee/dashboard"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <EmployeeDashboard />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/employee/leave-requests"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <LeaveRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/employee/attendance"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <Attendance />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/employee/my-requests"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <MyRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/employee/payslips"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <Payslips />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager/dashboard"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <ManagerDashboard />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager/leave-approvals"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <LeaveApprovals />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager/team-requests"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <TeamRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
              <Profile />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/messages"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
              <Messages />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/announcements"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
              <Announcements />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/calendar"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
              <Calendar />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/documents"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
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
