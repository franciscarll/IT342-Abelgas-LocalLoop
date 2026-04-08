import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import SelectBarangayPage from './pages/SelectBarangayPage';
import FavorFeedPage from './pages/FavorFeedPage';
import CreateFavorPage from './pages/CreateFavorPage';
import FavorDetailPage from './pages/FavorDetailPage';

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

          {/* Protected routes */}
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
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;