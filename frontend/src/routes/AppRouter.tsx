import { Navigate, Route, Routes } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../app/store';
import { AppShell } from '../components/layout/AppShell';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { DashboardPage } from '../pages/DashboardPage';
import { MeetingsPage } from '../pages/MeetingsPage';
import { MeetingDetailsPage } from '../pages/MeetingDetailsPage';
import { LiveRecordingPage } from '../pages/LiveRecordingPage';
import { ActionItemsPage } from '../pages/ActionItemsPage';
import { ChatPage } from '../pages/ChatPage';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { TeamPage } from '../pages/TeamPage';
import { ProfilePage } from '../pages/ProfilePage';
import { SettingsPage } from '../pages/SettingsPage';

function ProtectedApp() {
  const accessToken = useSelector((state: RootState) => state.auth.accessToken);
  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/meetings" element={<MeetingsPage />} />
        <Route path="/meetings/:id" element={<MeetingDetailsPage />} />
        <Route path="/record" element={<LiveRecordingPage />} />
        <Route path="/action-items" element={<ActionItemsPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/team" element={<TeamPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/*" element={<ProtectedApp />} />
    </Routes>
  );
}
