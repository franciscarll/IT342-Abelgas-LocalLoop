import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './shared/context/AuthContext';
import ProtectedRoute from './shared/components/ProtectedRoute';
import AdminRoute from './shared/components/AdminRoute';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import OAuth2CallbackPage from './features/auth/OAuth2CallbackPage';
import SelectBarangayPage from './features/auth/SelectBarangayPage';
import DashboardPage from './features/dashboard/DashboardPage';
import FavorFeedPage from './features/favors/FavorFeedPage';
import CreateFavorPage from './features/favors/CreateFavorPage';
import FavorDetailPage from './features/favors/FavorDetailPage';
import MyActivityPage from './features/profile/MyActivityPage';
import AnnouncementsPage from './features/announcements/AnnouncementsPage';
import ProfilePage from './features/profile/ProfilePage';
import AdminDashboardPage from './features/admin/AdminDashboardPage';
import AdminAnnouncementsPage from './features/admin/AdminAnnouncementsPage';
import AdminResidentsPage from './features/admin/AdminResidentsPage';
import AdminFavorOverviewPage from './features/admin/AdminFavorOverviewPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
          <Route path="/select-barangay" element={<SelectBarangayPage />} />

          {/* Resident-protected routes */}
          <Route path="/dashboard" element={
            <ProtectedRoute><DashboardPage /></ProtectedRoute>
          } />
          <Route path="/favor-feed" element={
            <ProtectedRoute><FavorFeedPage /></ProtectedRoute>
          } />
          <Route path="/favors/new" element={
            <ProtectedRoute><CreateFavorPage /></ProtectedRoute>
          } />
          <Route path="/favors/:id" element={
            <ProtectedRoute><FavorDetailPage /></ProtectedRoute>
          } />
          <Route path="/my-activity" element={
            <ProtectedRoute><MyActivityPage /></ProtectedRoute>
          } />
          <Route path="/announcements" element={
            <ProtectedRoute><AnnouncementsPage /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />

          {/* Admin-only routes */}
          <Route path="/admin/dashboard" element={
            <AdminRoute><AdminDashboardPage /></AdminRoute>
          } />
          <Route path="/admin/announcements" element={
            <AdminRoute><AdminAnnouncementsPage /></AdminRoute>
          } />
          <Route path="/admin/residents" element={
            <AdminRoute><AdminResidentsPage /></AdminRoute>
          } />
          <Route path="/admin/favors" element={
            <AdminRoute><AdminFavorOverviewPage /></AdminRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;