import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import LeftPanel from '../components/LeftPanel';

const CEBU_BARANGAYS = [
  'Adlaon', 'Agsungot', 'Apas', 'Bacayan', 'Banilad', 'Basak Pardo',
  'Basak San Nicolas', 'Binaliw', 'Bonbon', 'Budla-an', 'Buhisan',
  'Bulacao', 'Buot-Taup', 'Busay', 'Calamba', 'Cambinocot', 'Camputhaw',
  'Capitol Site', 'Carreta', 'Central Pardo', 'Cogon Pardo', 'Cogon Ramos',
  'Day-as', 'Duljo', 'Ermita', 'Guadalupe', 'Guba', 'Hipodromo',
  'Inayawan', 'Kalubihan', 'Kalunasan', 'Kamagayan', 'Kasambagan',
  'Kinasang-an', 'Labangon', 'Lahug', 'Lorega', 'Lusaran', 'Luz',
  'Mabini', 'Mabolo', 'Malubog', 'Mambaling', 'Mining', 'Mohon',
  'Motivoon', 'Nasipit', 'Nga-an', 'Opon', 'Pahina Central',
  'Pahina San Nicolas', 'Pamutan', 'Pardo', 'Pari-an', 'Paril',
  'Pasil', 'Pit-os', 'Poblacion Pardo', 'Pulangbato', 'Pung-ol-Sibugay',
  'Punta Princesa', 'Quiot Pardo', 'Sambag I', 'Sambag II',
  'San Antonio', 'San Jose', 'San Nicolas Central', 'San Roque',
  'Santa Cruz', 'Santo Nino', 'Sapangdaku', 'Sawang Calero',
  'Sinsin', 'Sirao', 'Suba', 'Sudlon I', 'Sudlon II', 'T. Padilla',
  'Tabunan', 'Tagbao', 'Talamban', 'Taptap', 'Tejero', 'Tinago',
  'Tisa', 'To-ong', 'Toong', 'Tugbongan', 'Camputhaw', 'Zapatera',
];

const RegisterPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    barangay: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
    setApiError('');
  };

  const validate = () => {
    const newErrors = {};
    if (!form.name.trim()) newErrors.name = 'Full name is required';
    if (!form.email.trim()) newErrors.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) newErrors.email = 'Invalid email format';
    if (!form.password) newErrors.password = 'Password is required';
    else if (form.password.length < 8) newErrors.password = 'Must be at least 8 characters';
    if (!form.confirmPassword) newErrors.confirmPassword = 'Please confirm your password';
    else if (form.password !== form.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';
    if (!form.barangay) newErrors.barangay = 'Please select your barangay';
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    setLoading(true);
    setApiError('');
    try {
      const res = await api.post('/auth/register', {
        name: form.name,
        email: form.email,
        password: form.password,
        barangay: form.barangay,
      });
      const { user, accessToken } = res.data.data;
      login(user, accessToken);
      navigate('/dashboard');
    } catch (err) {
      const msg =
        err.response?.data?.error?.message ||
        'Registration failed. Please try again.';
      if (err.response?.status === 409) {
        setApiError('An account with this email already exists.');
      } else {
        setApiError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <LeftPanel />

      {/* Right Panel */}
      <div style={styles.rightPanel}>
        <div style={styles.formWrapper}>
          <h1 style={styles.heading}>Join your barangay 🏠</h1>
          <p style={styles.subheading}>Create your LocalLoop account.</p>

          <div style={styles.card}>
            <form onSubmit={handleSubmit}>

              {/* Full Name */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>Full Name</label>
                <div style={styles.inputWrapper}>
                  <span style={styles.inputIcon}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                  </span>
                  <input
                    type="text"
                    name="name"
                    placeholder="Juan dela Cruz"
                    value={form.name}
                    onChange={handleChange}
                    style={{
                      ...styles.input,
                      borderColor: errors.name ? '#e53935' : '#e8e8e8',
                    }}
                  />
                </div>
                {errors.name && <p style={styles.fieldError}>{errors.name}</p>}
              </div>

              {/* Email */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>Email address</label>
                <div style={styles.inputWrapper}>
                  <span style={styles.inputIcon}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                      <polyline points="22,6 12,13 2,6" />
                    </svg>
                  </span>
                  <input
                    type="email"
                    name="email"
                    placeholder="you@email.com"
                    value={form.email}
                    onChange={handleChange}
                    style={{
                      ...styles.input,
                      borderColor: errors.email ? '#e53935' : '#e8e8e8',
                    }}
                  />
                </div>
                {errors.email && <p style={styles.fieldError}>{errors.email}</p>}
              </div>

              {/* Password */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>Password</label>
                <div style={styles.inputWrapper}>
                  <span style={styles.inputIcon}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0110 0v4" />
                    </svg>
                  </span>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    placeholder="Min. 8 characters"
                    value={form.password}
                    onChange={handleChange}
                    style={{
                      ...styles.input,
                      borderColor: errors.password ? '#e53935' : '#e8e8e8',
                    }}
                  />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} style={styles.eyeBtn}>
                    {showPassword ? (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94" />
                        <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
                {errors.password
                  ? <p style={styles.fieldError}>{errors.password}</p>
                  : <p style={styles.hint}>Must be at least 8 characters</p>
                }
              </div>

              {/* Confirm Password */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>Confirm Password</label>
                <div style={styles.inputWrapper}>
                  <span style={styles.inputIcon}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0110 0v4" />
                    </svg>
                  </span>
                  <input
                    type={showConfirm ? 'text' : 'password'}
                    name="confirmPassword"
                    placeholder="Re-enter your password"
                    value={form.confirmPassword}
                    onChange={handleChange}
                    style={{
                      ...styles.input,
                      borderColor: errors.confirmPassword ? '#e53935' : '#e8e8e8',
                    }}
                  />
                  <button type="button" onClick={() => setShowConfirm(!showConfirm)} style={styles.eyeBtn}>
                    {showConfirm ? (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94" />
                        <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
                {errors.confirmPassword && <p style={styles.fieldError}>{errors.confirmPassword}</p>}
              </div>

              {/* Barangay */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>Your Barangay</label>
                <div style={styles.inputWrapper}>
                  <span style={styles.inputIcon}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                  </span>
                  <select
                    name="barangay"
                    value={form.barangay}
                    onChange={handleChange}
                    style={{
                      ...styles.input,
                      ...styles.select,
                      borderColor: errors.barangay ? '#e53935' : '#e8e8e8',
                      color: form.barangay ? '#333' : '#aaa',
                    }}
                  >
                    <option value="">Select your barangay</option>
                    {CEBU_BARANGAYS.map((b) => (
                      <option key={b} value={b}>{b}</option>
                    ))}
                  </select>
                  <span style={styles.selectArrow}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="6 9 12 15 18 9" />
                    </svg>
                  </span>
                </div>
                {errors.barangay && <p style={styles.fieldError}>{errors.barangay}</p>}
              </div>

              {/* API Error */}
              {apiError && <p style={styles.apiError}>{apiError}</p>}

              {/* Submit */}
              <button type="submit" style={styles.submitBtn} disabled={loading}>
                {loading ? 'Creating account...' : 'Create Account'}
              </button>

              {/* Divider */}
              <div style={styles.dividerRow}>
                <div style={styles.dividerLine} />
                <span style={styles.dividerText}>or</span>
                <div style={styles.dividerLine} />
              </div>

              {/* Google */}
                <button 
                  type="button" 
                  style={styles.googleBtn}
                  onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                >
                <svg width="18" height="18" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                </svg>
                Continue with Google
              </button>
            </form>
          </div>

          <p style={styles.bottomText}>
            Already have an account?{' '}
            <Link to="/login" style={styles.link}>Log in</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

const styles = {
  page: {
    display: 'flex',
    minHeight: '100vh',
    fontFamily: "'Segoe UI', sans-serif",
  },
  rightPanel: {
    flex: 1,
    background: '#FAF7F2',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 60px',
    overflowY: 'auto',
  },
  formWrapper: {
    width: '100%',
    maxWidth: '420px',
    paddingTop: '20px',
    paddingBottom: '20px',
  },
  heading: {
    fontSize: '26px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 6px 0',
  },
  subheading: {
    fontSize: '14px',
    color: '#888',
    margin: '0 0 24px 0',
  },
  card: {
    background: 'white',
    borderRadius: '16px',
    padding: '28px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.06)',
    marginBottom: '20px',
  },
  fieldGroup: {
    marginBottom: '16px',
  },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '600',
    color: '#333',
    marginBottom: '6px',
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  inputIcon: {
    position: 'absolute',
    left: '12px',
    display: 'flex',
    alignItems: 'center',
    pointerEvents: 'none',
    zIndex: 1,
  },
  input: {
    width: '100%',
    height: '44px',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '0 40px 0 38px',
    fontSize: '14px',
    color: '#333',
    background: 'white',
    outline: 'none',
    boxSizing: 'border-box',
    appearance: 'none',
  },
  select: {
    cursor: 'pointer',
    paddingRight: '36px',
  },
  selectArrow: {
    position: 'absolute',
    right: '12px',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
  },
  eyeBtn: {
    position: 'absolute',
    right: '12px',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    padding: 0,
  },
  hint: {
    fontSize: '11px',
    color: '#aaa',
    margin: '4px 0 0 0',
  },
  fieldError: {
    fontSize: '11px',
    color: '#e53935',
    margin: '4px 0 0 0',
  },
  apiError: {
    color: '#e53935',
    fontSize: '13px',
    marginBottom: '12px',
    padding: '10px 12px',
    background: '#fff5f5',
    borderRadius: '8px',
    border: '1px solid #ffcdd2',
  },
  submitBtn: {
    width: '100%',
    height: '46px',
    background: '#C8601A',
    color: 'white',
    border: 'none',
    borderRadius: '10px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    marginBottom: '16px',
  },
  dividerRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '16px',
  },
  dividerLine: {
    flex: 1,
    height: '1px',
    background: '#eee',
  },
  dividerText: {
    fontSize: '13px',
    color: '#aaa',
  },
  googleBtn: {
    width: '100%',
    height: '46px',
    background: 'white',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    fontSize: '14px',
    fontWeight: '500',
    color: '#333',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '10px',
  },
  bottomText: {
    textAlign: 'center',
    fontSize: '13px',
    color: '#888',
    margin: 0,
  },
  link: {
    color: '#C8601A',
    fontWeight: '600',
    textDecoration: 'none',
  },
};

export default RegisterPage;