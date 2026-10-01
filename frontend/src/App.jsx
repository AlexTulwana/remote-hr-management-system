import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import JobApply from './pages/public/JobApply';
import SetPassword from './pages/public/SetPassword';
import ForgotPassword from './pages/public/ForgotPassword';
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
import MyReviews from './pages/employee/MyReviews';
import ManagerDashboard from './pages/manager/ManagerDashboard';
import MyTeam from './pages/manager/MyTeam';
import LeaveApprovals from './pages/manager/LeaveApprovals';
import TeamRequests from './pages/manager/TeamRequests';
import TeamDisciplinary from './pages/manager/TeamDisciplinary';
import TeamReviews from './pages/manager/TeamReviews';
import EmployeeDirectory from './pages/hr/EmployeeDirectory';
import Recruitment from './pages/hr/Recruitment';
import Onboarding from './pages/hr/Onboarding';
import Offboarding from './pages/hr/Offboarding';
import PayslipManagement from './pages/hr/PayslipManagement';
import LeaveManagement from './pages/hr/LeaveManagement';
import HrDisciplinaryCases from './pages/hr/HrDisciplinaryCases';
import HrHearings from './pages/hr/HrHearings';
import HrEscalations from './pages/hr/HrEscalations';
import HrPerformanceReviews from './pages/hr/HrPerformanceReviews';
import HrEmployeeRequests from './pages/hr/HrEmployeeRequests';
import Branches from './pages/hr/Branches';
import ExecutiveOverview from './pages/executive/ExecutiveOverview';
import TurnoverAnalytics from './pages/executive/TurnoverAnalytics';
import ReportConcern from './pages/ReportConcern';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './auth/ProtectedRoute';
import { useAuth } from './auth/AuthContext';
import {
  User, LayoutDashboard, CalendarDays, Clock, ClipboardList, Wallet, Star,
  Users, UserCheck, LogOut, LogIn, FileWarning, Gavel, AlertTriangle,
  Briefcase, Building2, TrendingUp, MessageSquare, Megaphone, FolderOpen, Flag,
} from 'lucide-react';

function navItemsFor(role) {
  const pinned = [{ to: '/profile', label: 'Profile', icon: User }];
  const rest = [];

  if (role === 'EMPLOYEE') {
    rest.push({ to: '/employee/dashboard', label: 'Dashboard', icon: LayoutDashboard });
    rest.push({ to: '/employee/leave-requests', label: 'Leave Requests', icon: CalendarDays });
    rest.push({ to: '/employee/attendance', label: 'Attendance', icon: Clock });
    rest.push({ to: '/employee/my-requests', label: 'My Requests', icon: ClipboardList });
    rest.push({ to: '/employee/payslips', label: 'Payslips', icon: Wallet });
    rest.push({ to: '/employee/performance-reviews', label: 'Performance Reviews', icon: Star });
  }
  if (role === 'MANAGER') {
    rest.push({ to: '/manager/dashboard', label: 'Dashboard', icon: LayoutDashboard });
    rest.push({ to: '/manager/my-team', label: 'My Team', icon: Users });
    rest.push({ to: '/manager/leave-approvals', label: 'Leave Approvals', icon: UserCheck });
    rest.push({ to: '/manager/team-requests', label: 'Team Requests', icon: ClipboardList });
    rest.push({ to: '/manager/disciplinary-cases', label: 'Disciplinary Cases', icon: FileWarning });
    rest.push({ to: '/manager/performance-reviews', label: 'Performance Reviews', icon: Star });
    rest.push({ to: '/manager/my-requests', label: 'My Requests', icon: ClipboardList });
  }
  if (role === 'HR' || role === 'ADMIN') {
    rest.push({ to: '/hr/employee-directory', label: 'Employee Directory', icon: Users });
    rest.push({ to: '/hr/recruitment', label: 'Recruitment', icon: Briefcase });
    rest.push({ to: '/hr/onboarding', label: 'Onboarding', icon: LogIn });
    rest.push({ to: '/hr/offboarding', label: 'Offboarding', icon: LogOut });
    rest.push({ to: '/hr/payslip-management', label: 'Payslip Management', icon: Wallet });
    rest.push({ to: '/hr/leave-management', label: 'Leave Management', icon: CalendarDays });
    rest.push({ to: '/hr/disciplinary-cases', label: 'Disciplinary Cases', icon: FileWarning });
    rest.push({ to: '/hr/hearings', label: 'Hearings', icon: Gavel });
    rest.push({ to: '/hr/escalations', label: 'Escalations', icon: AlertTriangle });
    rest.push({ to: '/hr/performance-reviews', label: 'Performance Reviews', icon: Star });
    rest.push({ to: '/hr/employee-requests', label: 'Employee Requests', icon: ClipboardList });
    rest.push({ to: '/hr/my-requests', label: 'My Requests', icon: ClipboardList });
    rest.push({ to: '/hr/branches', label: 'Branches', icon: Building2 });
  }
  if (role === 'ADMIN') {
    rest.push({ to: '/executive/overview', label: 'Executive Overview', icon: LayoutDashboard });
    rest.push({ to: '/executive/turnover-analytics', label: 'Turnover & Analytics', icon: TrendingUp });
  }
  rest.push(
    { to: '/report-concern', label: 'Report a Concern', icon: Flag },
    { to: '/messages', label: 'Messages', icon: MessageSquare },
    { to: '/announcements', label: 'Announcements', icon: Megaphone },
    { to: '/calendar', label: 'Calendar', icon: CalendarDays },
    { to: '/documents', label: 'Documents', icon: FolderOpen },
  );

  rest.sort((a, b) => a.label.localeCompare(b.label));
  return [...pinned, ...rest];
}

function App() {
  const { user } = useAuth();
  const navItems = navItemsFor(user?.role);

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/apply/:id" element={<JobApply />} />
      <Route path="/set-password" element={<SetPassword />} />
      <Route path="/reset-password" element={<SetPassword />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
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
        path="/employee/performance-reviews"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYEE']}>
            <AppLayout navItems={navItems}>
              <MyReviews />
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
        path="/manager/my-team"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <MyTeam />
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
        path="/manager/disciplinary-cases"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <TeamDisciplinary />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager/performance-reviews"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <TeamReviews />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager/my-requests"
        element={
          <ProtectedRoute allowedRoles={['MANAGER']}>
            <AppLayout navItems={navItems}>
              <MyRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/employee-directory"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <EmployeeDirectory />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/recruitment"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <Recruitment />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/onboarding"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <Onboarding />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/offboarding"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <Offboarding />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/payslip-management"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <PayslipManagement />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/leave-management"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <LeaveManagement />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/employee-requests"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <HrEmployeeRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/my-requests"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <MyRequests />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/branches"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <Branches />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/disciplinary-cases"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <HrDisciplinaryCases />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/hearings"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <HrHearings />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/escalations"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <HrEscalations />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/hr/performance-reviews"
        element={
          <ProtectedRoute allowedRoles={['HR', 'ADMIN']}>
            <AppLayout navItems={navItems}>
              <HrPerformanceReviews />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/executive/overview"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AppLayout navItems={navItems}>
              <ExecutiveOverview />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/executive/turnover-analytics"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AppLayout navItems={navItems}>
              <TurnoverAnalytics />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/report-concern"
        element={
          <ProtectedRoute>
            <AppLayout navItems={navItems}>
              <ReportConcern />
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
