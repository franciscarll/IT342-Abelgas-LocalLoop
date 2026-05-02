import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';

const CEBU_BARANGAYS = [
  'Adlaon', 'Agsungot', 'Apas', 'Bacayan', 'Banilad',
  'Basak Pardo', 'Basak San Nicolas', 'Binaliw', 'Bonbon',
  'Budla-an', 'Buhisan', 'Bulacao', 'Buot-Taup', 'Busay',
  'Calamba', 'Cambinocot', 'Camputhaw', 'Capitol Site', 'Carreta',
  'Central Pardo', 'Cogon Pardo', 'Cogon Ramos', 'Day-as', 'Duljo',
  'Ermita', 'Guadalupe', 'Guba', 'Hipodromo', 'Inayawan',
  'Kalubihan', 'Kalunasan', 'Kamagayan', 'Kasambagan', 'Kinasang-an',
  'Labangon', 'Lahug', 'Lorega', 'Lusaran', 'Luz', 'Mabini',
  'Mabolo', 'Malubog', 'Mambaling', 'Mining', 'Mohon',
  'Motivoon', 'Nasipit', 'Nga-an', 'Pahina Central',
  'Pahina San Nicolas', 'Pamutan', 'Pardo', 'Pari-an', 'Paril',
  'Pasil', 'Pit-os', 'Poblacion Pardo', 'Pulangbato',
  'Pung-ol-Sibugay', 'Punta Princesa', 'Quiot Pardo',
  'Sambag I', 'Sambag II', 'San Antonio', 'San Jose',
  'San Nicolas Central', 'San Roque', 'Santa Cruz', 'Santo Nino',
  'Sapangdaku', 'Sawang Calero', 'Sinsin', 'Sirao', 'Suba',
  'Sudlon I', 'Sudlon II', 'T. Padilla', 'Tabunan', 'Tagbao',
  'Talamban', 'Taptap', 'Tejero', 'Tinago', 'Tisa',
  'To-ong', 'Toong', 'Tugbongan', 'Zapatera',
];

const SelectBarangayPage = () => {
  const { user, token, login } = useAuth();
  const navigate = useNavigate();
  const [barangay, setBarangay] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // If no token at all, redirect to login
  if (!token) {
    navigate('/login');
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!barangay) {
      setError('Please select your barangay.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      // ── FIX: Actually save to the database ──────────────────────────────
      // Calls PUT /api/users/profile — saves barangay to the users table.
      // This was silently failing before because the endpoint didn't exist,
      // meaning the DB kept "Not set" and caused the redirect loop on every login.
      const res = await api.put('/users/profile', { barangay });

      // Use the fresh user data returned from the server if available,
      // otherwise fall back to merging the selected barangay into local user.
      const savedUser = res.data?.data || { ...user, barangay };

      // ── FIX: Update AuthContext + localStorage with real saved barangay ─
      // Ensures future logins read the correct barangay from localStorage
      // until the JWT is refreshed.
      login(savedUser, token);

      navigate('/dashboard', { replace: true });
    } catch (err) {
      // ── FIX: Show the error instead of silently swallowing it ────────────
      // Previously the catch block said "Continue even if API fails" which
      // hid the 404 and let the user appear to succeed while DB was unchanged.
      console.error('Failed to save barangay:', err);
      setError(
        err.response?.data?.error?.message ||
        'Failed to save your barangay. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        {/* Logo */}
        <div style={styles.logoRow}>
          <svg width="32" height="26" viewBox="0 0 72 56" fill="none">
            <path
              d="M24 4C16.268 4 10 10.268 10 18C10 26.5 20 38 24 42C28 38 38 26.5 38 18C38 10.268 31.732 4 24 4Z"
              fill="#C8601A"
            />
            <circle cx="24" cy="18" r="5" fill="white" />
            <path
              d="M48 8C41.373 8 36 13.373 36 20C36 27.5 44.5 38 48 42C51.5 38 60 27.5 60 20C60 13.373 54.627 8 48 8Z"
              fill="#C8601A"
              fillOpacity="0.6"
            />
          </svg>
          <span style={styles.logoText}>LocalLoop</span>
        </div>

        <h1 style={styles.heading}>One more step! 📍</h1>
        <p style={styles.sub}>
          {user?.name ? (
            <>Hi <strong>{user.name}</strong>! Please select your barangay to continue.</>
          ) : (
            <>Please select your barangay to continue.</>
          )}
        </p>

        <form onSubmit={handleSubmit}>
          <div style={styles.fieldGroup}>
            <label style={styles.label}>Your Barangay</label>
            <div style={styles.inputWrapper}>
              <span style={styles.icon}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
              </span>
              <select
                value={barangay}
                onChange={(e) => { setBarangay(e.target.value); setError(''); }}
                style={{
                  ...styles.select,
                  borderColor: error ? '#e53935' : '#e8e8e8',
                  color: barangay ? '#333' : '#aaa',
                }}
              >
                <option value="">Select your barangay</option>
                {CEBU_BARANGAYS.map((b) => (
                  <option key={b} value={b}>{b}</option>
                ))}
              </select>
              <span style={styles.arrow}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                  stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </div>
            {error && <p style={styles.errorMsg}>{error}</p>}
          </div>

          <button type="submit" style={styles.btn} disabled={loading}>
            {loading ? 'Saving...' : 'Continue to Dashboard →'}
          </button>
        </form>
      </div>
    </div>
  );
};

const styles = {
  page: {
    minHeight: '100vh',
    background: '#FAF7F2',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: "'Segoe UI', sans-serif",
    padding: '20px',
  },
  card: {
    background: 'white',
    borderRadius: '20px',
    padding: '48px',
    maxWidth: '440px',
    width: '100%',
    boxShadow: '0 4px 24px rgba(0,0,0,0.07)',
  },
  logoRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    marginBottom: '28px',
    justifyContent: 'center',
  },
  logoText: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#C8601A',
  },
  heading: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 8px 0',
    textAlign: 'center',
  },
  sub: {
    fontSize: '14px',
    color: '#888',
    margin: '0 0 32px 0',
    textAlign: 'center',
    lineHeight: '1.5',
  },
  fieldGroup: {
    marginBottom: '24px',
  },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '600',
    color: '#333',
    marginBottom: '8px',
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  icon: {
    position: 'absolute',
    left: '12px',
    display: 'flex',
    alignItems: 'center',
    pointerEvents: 'none',
    zIndex: 1,
  },
  select: {
    width: '100%',
    height: '46px',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '0 36px 0 38px',
    fontSize: '14px',
    background: 'white',
    outline: 'none',
    appearance: 'none',
    cursor: 'pointer',
    boxSizing: 'border-box',
  },
  arrow: {
    position: 'absolute',
    right: '12px',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
  },
  errorMsg: {
    fontSize: '12px',
    color: '#e53935',
    margin: '6px 0 0 0',
  },
  btn: {
    width: '100%',
    height: '46px',
    background: '#C8601A',
    color: 'white',
    border: 'none',
    borderRadius: '10px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
  },
};

export default SelectBarangayPage;