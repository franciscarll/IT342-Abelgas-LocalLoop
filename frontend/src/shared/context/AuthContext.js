import React, { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [token, setToken] = useState(() => {
    return localStorage.getItem('token') || null;
  });

  const login = (userData, accessToken) => {
    localStorage.setItem('token', accessToken);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setToken(accessToken);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setToken(null);
  };

  // ── NEW: update user fields in state + localStorage without re-login ────────
  // Called after profile save or photo upload so Navbar and all pages
  // immediately reflect the new name / profileImageUrl.
  const updateUser = (updatedFields) => {
    const merged = { ...user, ...updatedFields };
    localStorage.setItem('user', JSON.stringify(merged));
    setUser(merged);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);