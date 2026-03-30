import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';

const DashboardPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {}
    logout();
    navigate('/login');
  };

  return (
    <div style={styles.page}>
      <div style={styles.navbar}>
        <div style={styles.logo}>
          <svg width="28" height="22" viewBox="0 0 72 56" fill="none">
            <path d="M24 4C16.268 4 10 10.268 10 18C10 26.5 20 38 24 42C28 38 38 26.5 38 18C38 10.268 31.732 4 24 4Z" fill="#C8601A" />
            <circle cx="24" cy="18" r="5" fill="white" />
            <path d="M48 8C41.373 8 36 13.373 36 20C36 27.5 44.5 38 48 42C51.5 38 60 27.5 60 20C60 13.373 54.627 8 48 8Z" fill="#C8601A" fillOpacity="0.6" />
          </svg>
          <span style={styles.logoText}>LocalLoop</span>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Log out</button>
      </div>

      <div style={styles.content}>
        <div style={styles.card}>
          <h1 style={styles.welcome}>
            Welcome, {user?.name}! 👋
          </h1>
          <p style={styles.sub}>
            You are logged in to <strong>Barangay {user?.barangay}</strong>.
          </p>
          <div style={styles.infoGrid}>
            <div style={styles.infoItem}>
              <span style={styles.infoLabel}>Email</span>
              <span style={styles.infoValue}>{user?.email}</span>
            </div>
            <div style={styles.infoItem}>
              <span style={styles.infoLabel}>Role</span>
              <span style={styles.infoValue}>{user?.role}</span>
            </div>
            <div style={styles.infoItem}>
              <span style={styles.infoLabel}>Reputation</span>
              <span style={styles.infoValue}>{user?.reputationScore} pts</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  page: {
    minHeight: '100vh',
    background: '#FAF7F2',
    fontFamily: "'Segoe UI', sans-serif",
  },
  navbar: {
    background: 'white',
    padding: '14px 40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    boxShadow: '0 1px 8px rgba(0,0,0,0.06)',
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  logoText: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#C8601A',
  },
  logoutBtn: {
    background: 'none',
    border: '1.5px solid #C8601A',
    color: '#C8601A',
    padding: '8px 20px',
    borderRadius: '8px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
  },
  content: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: 'calc(100vh - 60px)',
    padding: '40px',
  },
  card: {
    background: 'white',
    borderRadius: '20px',
    padding: '48px',
    boxShadow: '0 4px 24px rgba(0,0,0,0.07)',
    maxWidth: '500px',
    width: '100%',
    textAlign: 'center',
  },
  welcome: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 10px 0',
  },
  sub: {
    fontSize: '15px',
    color: '#888',
    margin: '0 0 32px 0',
  },
  infoGrid: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    textAlign: 'left',
  },
  infoItem: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '12px 16px',
    background: '#FAF7F2',
    borderRadius: '10px',
  },
  infoLabel: {
    fontSize: '13px',
    color: '#aaa',
    fontWeight: '500',
  },
  infoValue: {
    fontSize: '13px',
    color: '#333',
    fontWeight: '600',
  },
};

export default DashboardPage;