import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ApiClient from '../api/ApiClient';
const api = ApiClient.getInstance();
import LeftPanel from '../components/LeftPanel';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login', {
        email: form.email,
        password: form.password,
      });
      const { user, accessToken } = res.data.data;
      login(user, accessToken);
      navigate('/dashboard');
    } catch (err) {
      const msg =
        err.response?.data?.error?.message ||
        'Invalid email or password. Please try again.';
      setError(msg);
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
          <h1 style={styles.heading}>Welcome back 👋</h1>
          <p style={styles.subheading}>Log in to your LocalLoop account.</p>

          <div style={styles.card}>
            <form onSubmit={handleSubmit}>
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
                    style={styles.input}
                    required
                  />
                </div>
              </div>

              {/* Password */}
              <div style={styles.fieldGroup}>
                <div style={styles.passwordLabelRow}>
                  <label style={styles.label}>Password</label>
                  <span style={styles.forgotLink}>Forgot password?</span>
                </div>
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
                    placeholder="••••••••"
                    value={form.password}
                    onChange={handleChange}
                    style={styles.input}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={styles.eyeBtn}
                  >
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
              </div>

              {/* Error */}
              {error && <p style={styles.errorMsg}>{error}</p>}

              {/* Login Button */}
              <button type="submit" style={styles.submitBtn} disabled={loading}>
                {loading ? 'Logging in...' : 'Log In'}
              </button>

              {/* Divider */}
              <div style={styles.dividerRow}>
                <div style={styles.dividerLine} />
                <span style={styles.dividerText}>or</span>
                <div style={styles.dividerLine} />
              </div>

              {/* Google Button */}
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
            Don't have an account?{' '}
            <Link to="/register" style={styles.link}>Sign up</Link>
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
  },
  formWrapper: {
    width: '100%',
    maxWidth: '420px',
  },
  heading: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 6px 0',
  },
  subheading: {
    fontSize: '14px',
    color: '#888',
    margin: '0 0 28px 0',
  },
  card: {
    background: 'white',
    borderRadius: '16px',
    padding: '28px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.06)',
    marginBottom: '20px',
  },
  fieldGroup: {
    marginBottom: '18px',
  },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '600',
    color: '#333',
    marginBottom: '6px',
  },
  passwordLabelRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '6px',
  },
  forgotLink: {
    fontSize: '12px',
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
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
    transition: 'border-color 0.2s',
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
  errorMsg: {
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
    transition: 'background 0.2s',
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
    transition: 'border-color 0.2s',
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

export default LoginPage;