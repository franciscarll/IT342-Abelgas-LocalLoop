import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * AdminRoute — wraps admin-only pages.
 * - No token → /login
 * - Token but ROLE_USER → /login  (admin accounts only per spec)
 * - Token + ROLE_ADMIN → render children
 */
const AdminRoute = ({ children }) => {
  const { token, user } = useAuth();

  if (!token) return <Navigate to="/login" replace />;
  if (user?.role !== 'ROLE_ADMIN') return <Navigate to="/login" replace />;

  return children;
};

export default AdminRoute;