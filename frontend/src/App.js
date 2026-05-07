import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import SelectBarangayPage from './pages/SelectBarangayPage';
import FavorFeedPage from './pages/FavorFeedPage';
import CreateFavorPage from './pages/CreateFavorPage';
import FavorDetailPage from './pages/FavorDetailPage';
import MyActivityPage from './pages/MyActivityPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import AdminAnnouncementsPage from './pages/AdminAnnouncementsPage';
import ProfilePage from './pages/ProfilePage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminResidentsPage from './pages/AdminResidentsPage';
import AdminFavorOverviewPage from './pages/AdminFavorOverviewPage';

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