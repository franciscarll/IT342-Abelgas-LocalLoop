import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from '../../shared/context/AuthContext';
import api from '../../shared/api/axios';
import Navbar from '../../shared/components/Navbar';

const AVATAR_COLORS = [
  '#C8601A', '#2E86AB', '#A23B72', '#F18F01',
  '#44BBA4', '#E94F37', '#6B4226', '#3A86FF',
];
function avatarColor(name = '') {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}
function initials(name = '') {
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
}
function formatMonthYear(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
}
function formatDateShort(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });
}
function reputationToStars(score = 0) {
  return Math.min(5, Math.floor(score / 5));
}
const STATUS_COLORS = {
  OPEN:      { bg: '#FFF3E0', text: '#E65100' },
  CLAIMED:   { bg: '#FFF8E1', text: '#F57F17' },
  COMPLETED: { bg: '#E8F5E9', text: '#2E7D32' },
};
if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

const ProfilePage = () => {
  const { user, updateUser } = useAuth();
  const fileInputRef = useRef(null);

  const updateUserRef = useRef(updateUser);
  useEffect(() => { updateUserRef.current = updateUser; }, [updateUser]);

  const userRef = useRef(user);
  useEffect(() => { userRef.current = user; }, [user]);

  const [profile, setProfile]               = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [activity, setActivity]               = useState([]);
  const [activityLoading, setActivityLoading] = useState(true);
  const [name, setName]                         = useState('');
  const [currentPassword, setCurrentPassword]   = useState('');
  const [newPassword, setNewPassword]           = useState('');
  const [confirmPassword, setConfirmPassword]   = useState('');
  const [showCurrentPw, setShowCurrentPw]       = useState(false);
  const [showNewPw, setShowNewPw]               = useState(false);
  const [showConfirmPw, setShowConfirmPw]       = useState(false);
  const [saving, setSaving]           = useState(false);
  const [saveSuccess, setSaveSuccess] = useState('');
  const [saveError, setSaveError]     = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadError, setUploadError]     = useState('');

  const fetchProfile = useCallback(async () => {
    setProfileLoading(true);
    try {
      const res = await api.get('/profile');
      const data = res.data?.data || res.data;
      setProfile(data);
      setName(data.name || '');
      updateUserRef.current({ hasPassword: data.hasPassword });
    } catch {
      const u = userRef.current;
      if (u) {
        setProfile({ ...u, favorsPosted: 0, favorsClaimed: 0, favorsCompleted: 0 });
        setName(u.name || '');
      }
    } finally {
      setProfileLoading(false);
    }
  }, []);

  const profileRef = useRef(null);
  const fetchActivity = useCallback(async () => {
    setActivityLoading(true);
    try {
      const [postedRes, claimedRes] = await Promise.all([
        api.get('/favors/my-posted',  { params: { page: 0, size: 20 } }),
        api.get('/favors/my-claimed', { params: { page: 0, size: 20 } }),
      ]);
      const posted = (postedRes.data?.data?.content || postedRes.data?.data || [])
        .map(f => ({
          id:     f.id,
          type:   f.status === 'COMPLETED' ? 'Completed' : f.status === 'CLAIMED' ? 'Claimed' : 'Posted',
          label:  f.title,
          date:   f.completedAt || f.updatedAt || f.createdAt,
          status: f.status,
        }));
      const claimed = (claimedRes.data?.data?.content || claimedRes.data?.data || [])
        .map(f => ({
          id:     f.id,
          type:   f.status === 'COMPLETED' ? 'Completed' : 'Claimed',
          label:  f.title,
          date:   f.completedAt || f.claimedAt || f.updatedAt,
          status: f.status,
        }));
      const joined = {
        id:     'joined',
        type:   'Joined',
        label:  'Joined LocalLoop — Welcome! 🎉',
        date:   profileRef.current?.createdAt || userRef.current?.createdAt,
        status: null,
      };
      const merged = [...posted, ...claimed]
        .sort((a, b) => new Date(b.date) - new Date(a.date))
        .slice(0, 5);
      setActivity([...merged, joined]);
    } catch {
      setActivity([]);
    } finally {
      setActivityLoading(false);
    }
  }, []);

  useEffect(() => { fetchProfile(); }, [fetchProfile]);
  useEffect(() => { if (!profileLoading) { fetchActivity(); } }, [profileLoading, fetchActivity]);
  useEffect(() => { profileRef.current = profile; }, [profile]);

  const validate = () => {
    const errs = {};
    if (!name.trim()) errs.name = 'Full name is required.';
    else if (name.trim().length > 100) errs.name = 'Name must be 100 characters or less.';
    const userHasPassword = profile?.hasPassword ?? userRef.current?.hasPassword ?? false;
    const wantsPassword = newPassword || confirmPassword || currentPassword;
    if (wantsPassword) {
      if (!newPassword) {
        errs.newPassword = 'New password is required.';
      } else if (newPassword.length < 8) {
        errs.newPassword = 'Must be at least 8 characters.';
      }
      if (newPassword && newPassword !== confirmPassword) {
        errs.confirmPassword = 'Passwords do not match.';
      }
      if (userHasPassword && (!currentPassword || currentPassword.trim() === '')) {
        errs.currentPassword = 'Current password is required.';
      }
    }
    return errs;
  };

  const handleSave = async () => {
    setSaveSuccess('');
    setSaveError('');
    const errs = validate();
    if (Object.keys(errs).length > 0) { setFieldErrors(errs); return; }
    setFieldErrors({});
    setSaving(true);
    try {
      const payload = { name: name.trim() };
      const wantsPassword = newPassword || confirmPassword || currentPassword;
      if (wantsPassword) {
        payload.newPassword     = newPassword;
        payload.confirmPassword = confirmPassword;
        const userHasPassword = profile?.hasPassword ?? userRef.current?.hasPassword ?? false;
        if (userHasPassword) {
          payload.currentPassword = currentPassword;
        }
      }
      const res = await api.put('/profile', payload);
      const updated = res.data?.data || res.data;
      updateUserRef.current({
        name:            updated.name,
        profileImageUrl: updated.profileImageUrl,
        hasPassword:     updated.hasPassword,
      });
      setProfile(prev => ({
        ...prev,
        name:        updated.name,
        hasPassword: updated.hasPassword,
      }));
      setName(updated.name);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSaveSuccess(
        updated.hasPassword && !profile?.hasPassword
          ? 'Password set! You can now log in with email and password.'
          : 'Profile updated successfully!'
      );
      setTimeout(() => setSaveSuccess(''), 5000);
    } catch (err) {
      // ── FIXED: route backend errors to inline fields instead of top banner ──
      const message =
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        'Could not save profile. Please try again.';

      const status = err.response?.status;

      if (status === 400) {
        const lower = message.toLowerCase();

        if (lower.includes('current password is incorrect')) {
          setFieldErrors(prev => ({ ...prev, currentPassword: 'Current password is incorrect.' }));
        } else if (lower.includes('current password is required')) {
          setFieldErrors(prev => ({ ...prev, currentPassword: 'Current password is required.' }));
        } else if (lower.includes('at least 8 characters')) {
          setFieldErrors(prev => ({ ...prev, newPassword: 'Must be at least 8 characters.' }));
        } else if (lower.includes('do not match')) {
          setFieldErrors(prev => ({ ...prev, confirmPassword: 'Passwords do not match.' }));
        } else {
          setSaveError(message);
        }
      } else {
        // 500 or network error → top banner only
        setSaveError(message);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDiscard = () => {
    setName(profile?.name || userRef.current?.name || '');
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setFieldErrors({});
    setSaveError('');
    setSaveSuccess('');
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setUploadError('Only JPG and PNG images are allowed.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setUploadError('File size must be 5MB or less.');
      return;
    }
    setUploadError('');
    setUploadLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/profile/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const updated = res.data?.data || res.data;
      updateUserRef.current({ profileImageUrl: updated.profileImageUrl });
      setProfile(prev => ({ ...prev, profileImageUrl: updated.profileImageUrl }));
    } catch (err) {
      setUploadError(
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        'Could not upload image. Please try again.'
      );
    } finally {
      setUploadLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const displayName       = profile?.name           || user?.name           || '';
  const displayEmail      = profile?.email          || user?.email          || '';
  const displayBarangay   = profile?.barangay       || user?.barangay       || '';
  const displayPhoto      = profile?.profileImageUrl || user?.profileImageUrl;
  const displayReputation = profile?.reputationScore ?? user?.reputationScore ?? 0;
  const starsCount        = reputationToStars(displayReputation);
  const userHasPassword   = profile?.hasPassword ?? user?.hasPassword ?? false;

  const activityDotColor = (type) => {
    if (type === 'Completed') return '#2E7D32';
    if (type === 'Claimed')   return '#F57F17';
    if (type === 'Posted')    return '#C8601A';
    return '#aaa';
  };

  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.content}>
        <div style={s.leftCol}>
          <div style={s.card}>
            {profileLoading ? (
              <div style={s.loadingBox}><div style={s.spinner} /></div>
            ) : (
              <>
                <div style={s.avatarSection}>
                  <div style={s.avatarWrap}>
                    {displayPhoto ? (
                      <img src={displayPhoto} alt="Profile" style={s.avatarImg} />
                    ) : (
                      <div style={{ ...s.avatarCircle, background: avatarColor(displayName) }}>
                        {initials(displayName)}
                      </div>
                    )}
                    <div
                      style={s.cameraOverlay}
                      onClick={() => fileInputRef.current?.click()}
                      title="Change photo"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                           stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                        <circle cx="12" cy="13" r="4"/>
                      </svg>
                    </div>
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png"
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                  />
                  <h2 style={s.profileName}>{displayName}</h2>
                  <p style={s.profileEmail}>{displayEmail}</p>
                  <div style={s.barangayBadge}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                    </svg>
                    {displayBarangay}
                  </div>
                  <p style={s.memberSince}>Member since {formatMonthYear(profile?.createdAt)}</p>
                  <button
                    style={{ ...s.uploadBtn, opacity: uploadLoading ? 0.7 : 1 }}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploadLoading}
                  >
                    {uploadLoading ? (
                      <><div style={s.btnSpinnerDark} /> Uploading…</>
                    ) : (
                      <>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                             stroke="#C8601A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                          <circle cx="12" cy="13" r="4"/>
                        </svg>
                        Upload New Photo
                      </>
                    )}
                  </button>
                  {uploadError && <p style={s.uploadError}>{uploadError}</p>}
                </div>
                <div style={s.divider} />
                <div style={s.reputationSection}>
                  <div style={s.reputationHeader}>
                    <span style={s.reputationTitle}>⭐ Reputation</span>
                  </div>
                  <p style={s.reputationSubtext}>Your standing in {displayBarangay}</p>
                  <div style={s.reputationScore}>{displayReputation}</div>
                  <p style={s.reputationLabel}>Reputation Points</p>
                  <div style={s.starsRow}>
                    {[1, 2, 3, 4, 5].map(i => (
                      <svg key={i} width="20" height="20" viewBox="0 0 24 24"
                           fill={i <= starsCount ? '#C8601A' : 'none'}
                           stroke="#C8601A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                    ))}
                  </div>
                  <p style={s.reputationMotivation}>Building trust in the community 🌱</p>
                  <div style={s.statsRow}>
                    <div style={s.statItem}>
                      <span style={s.statValue}>{profile?.favorsPosted ?? 0}</span>
                      <span style={s.statLabel}>Posted</span>
                    </div>
                    <div style={s.statItem}>
                      <span style={s.statValue}>{profile?.favorsClaimed ?? 0}</span>
                      <span style={s.statLabel}>Claimed</span>
                    </div>
                    <div style={s.statItem}>
                      <span style={s.statValue}>{profile?.favorsCompleted ?? 0}</span>
                      <span style={s.statLabel}>Completed</span>
                    </div>
                  </div>
                </div>
                <div style={s.divider} />
                <div style={s.barangaySection}>
                  <div style={s.barangaySectionHeader}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                    </svg>
                    <span style={s.barangaySectionLabel}>Your Barangay</span>
                  </div>
                  <p style={s.barangaySectionName}>{displayBarangay}</p>
                  <p style={s.barangaySectionCity}>Cebu City, Philippines</p>
                  <div style={s.barangayNote}>
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
                         stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="12" y1="8" x2="12" y2="12"/>
                      <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <span style={s.barangayNoteText}>
                      Your barangay cannot be changed after registration.
                    </span>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        <div style={s.rightCol}>
          <div style={s.card}>
            <div style={s.editHeading}>
              <h2 style={s.editTitle}>Edit Profile</h2>
              <p style={s.editSubtitle}>Update your personal information.</p>
            </div>
            {saveSuccess && <div style={s.successBanner}>{saveSuccess}</div>}
            {saveError   && <div style={s.errorBanner}>{saveError}</div>}

            <div style={s.fieldGroup}>
              <label style={s.fieldLabel}>Full Name</label>
              <div style={{ ...s.inputWrap, ...(fieldErrors.name ? s.inputWrapError : {}) }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                     stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                <input
                  type="text"
                  value={name}
                  onChange={e => { setName(e.target.value); if (fieldErrors.name) setFieldErrors(p => ({ ...p, name: '' })); setSaveError(''); }}
                  style={s.fieldInput}
                  placeholder="Your full name"
                  maxLength={100}
                />
              </div>
              {fieldErrors.name && <span style={s.fieldError}>{fieldErrors.name}</span>}
            </div>

            <div style={s.fieldGroup}>
              <label style={s.fieldLabel}>Email Address</label>
              <div style={{ ...s.inputWrap, ...s.inputWrapReadOnly }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                     stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                <input type="email" value={displayEmail} readOnly style={{ ...s.fieldInput, color: '#aaa' }} />
                <span style={s.readOnlyBadge}>Cannot be changed</span>
              </div>
            </div>

            <div style={s.fieldGroup}>
              <label style={s.fieldLabel}>Barangay</label>
              <div style={{ ...s.inputWrap, ...s.inputWrapReadOnly }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
                <input type="text" value={displayBarangay} readOnly style={{ ...s.fieldInput, color: '#aaa' }} />
                <span style={s.readOnlyBadge}>Cannot be changed</span>
              </div>
            </div>

            <div style={s.passwordDivider}>
              <div style={s.passwordDividerLine} />
              <span style={s.passwordDividerLabel}>
                {userHasPassword ? 'Change Password' : 'Set Password'}
              </span>
              <div style={s.passwordDividerLine} />
            </div>

            {!userHasPassword && (
              <div style={s.googleAccountNote}>
                <svg width="16" height="16" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                <span style={s.googleAccountNoteText}>
                  You signed in with Google. You can set a password to also log in with email.
                </span>
              </div>
            )}

            {userHasPassword && (
              <div style={s.fieldGroup}>
                <label style={s.fieldLabel}>Current Password</label>
                <div style={{ ...s.inputWrap, ...(fieldErrors.currentPassword ? s.inputWrapError : {}) }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                  </svg>
                  <input type={showCurrentPw ? 'text' : 'password'} value={currentPassword}
                    onChange={e => { setCurrentPassword(e.target.value); if (fieldErrors.currentPassword) setFieldErrors(p => ({ ...p, currentPassword: '' })); setSaveError(''); }}
                    placeholder="Enter current password" style={s.fieldInput} />
                  <button type="button" style={s.eyeBtn} onClick={() => setShowCurrentPw(v => !v)}>
                    {showCurrentPw
                      ? <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                      : <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    }
                  </button>
                </div>
                {fieldErrors.currentPassword && <span style={s.fieldError}>{fieldErrors.currentPassword}</span>}
              </div>
            )}

            <div style={s.fieldGroup}>
              <label style={s.fieldLabel}>{userHasPassword ? 'New Password' : 'Password'}</label>
              <div style={{ ...s.inputWrap, ...(fieldErrors.newPassword ? s.inputWrapError : {}) }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
                <input type={showNewPw ? 'text' : 'password'} value={newPassword}
                  onChange={e => { setNewPassword(e.target.value); if (fieldErrors.newPassword) setFieldErrors(p => ({ ...p, newPassword: '' })); setSaveError(''); }}
                  placeholder="Min. 8 characters" style={s.fieldInput} />
                <button type="button" style={s.eyeBtn} onClick={() => setShowNewPw(v => !v)}>
                  {showNewPw
                    ? <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    : <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  }
                </button>
              </div>
              {fieldErrors.newPassword
                ? <span style={s.fieldError}>{fieldErrors.newPassword}</span>
                : <span style={s.fieldHint}>Must be at least 8 characters long.</span>
              }
            </div>

            <div style={s.fieldGroup}>
              <label style={s.fieldLabel}>{userHasPassword ? 'Confirm New Password' : 'Confirm Password'}</label>
              <div style={{ ...s.inputWrap, ...(fieldErrors.confirmPassword ? s.inputWrapError : {}) }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
                <input type={showConfirmPw ? 'text' : 'password'} value={confirmPassword}
                  onChange={e => { setConfirmPassword(e.target.value); if (fieldErrors.confirmPassword) setFieldErrors(p => ({ ...p, confirmPassword: '' })); setSaveError(''); }}
                  placeholder="Re-enter password" style={s.fieldInput} />
                <button type="button" style={s.eyeBtn} onClick={() => setShowConfirmPw(v => !v)}>
                  {showConfirmPw
                    ? <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    : <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  }
                </button>
              </div>
              {fieldErrors.confirmPassword && <span style={s.fieldError}>{fieldErrors.confirmPassword}</span>}
            </div>

            <div style={s.formActions}>
              <button style={s.discardBtn} onClick={handleDiscard} disabled={saving}>
                Discard Changes
              </button>
              <button style={{ ...s.saveBtn, opacity: saving ? 0.7 : 1 }} onClick={handleSave} disabled={saving}>
                {saving ? (
                  <span style={s.btnInner}><div style={s.btnSpinner} /> Saving…</span>
                ) : (
                  <span style={s.btnInner}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    Save Changes
                  </span>
                )}
              </button>
            </div>

            <div style={s.activitySection}>
              <h3 style={s.activityTitle}>🕐 Recent Activity</h3>
              {activityLoading ? (
                <div style={s.loadingBox}><div style={s.spinner} /></div>
              ) : activity.length === 0 ? (
                <p style={s.activityEmpty}>No activity yet.</p>
              ) : (
                <div style={s.activityList}>
                  {activity.map((item, i) => (
                    <div key={item.id ?? i} style={s.activityItem}>
                      <div style={{ ...s.activityDot, background: activityDotColor(item.type) }} />
                      <div style={s.activityInfo}>
                        <div style={s.activityLabel}>
                          {item.type !== 'Joined' && <span style={s.activityType}>{item.type}: </span>}
                          {item.label}
                        </div>
                        <div style={s.activityDate}>{formatDateShort(item.date)}</div>
                      </div>
                      {item.status ? (
                        <span style={{ ...s.statusBadge, background: STATUS_COLORS[item.status]?.bg || '#f5f5f5', color: STATUS_COLORS[item.status]?.text || '#888' }}>
                          {item.status}
                        </span>
                      ) : (
                        <span style={s.joinedBadge}>Joined</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const s = {
  page: { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif" },
  content: { display: 'flex', gap: '24px', maxWidth: '1100px', margin: '0 auto', padding: '40px 32px', alignItems: 'flex-start' },
  leftCol:  { width: '320px', flexShrink: 0 },
  rightCol: { flex: 1, minWidth: 0 },
  card: { background: 'white', borderRadius: '20px', padding: '28px', boxShadow: '0 2px 16px rgba(0,0,0,0.05)' },
  avatarSection: { display: 'flex', flexDirection: 'column', alignItems: 'center', paddingBottom: '4px' },
  avatarWrap: { position: 'relative', marginBottom: '16px' },
  avatarImg: { width: '100px', height: '100px', borderRadius: '50%', objectFit: 'cover', border: '3px solid #FFF3E0' },
  avatarCircle: { width: '100px', height: '100px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '32px', fontWeight: '700', border: '3px solid #FFF3E0' },
  cameraOverlay: { position: 'absolute', bottom: '4px', right: '4px', width: '28px', height: '28px', borderRadius: '50%', background: '#C8601A', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', border: '2px solid white' },
  profileName: { fontSize: '20px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0', textAlign: 'center' },
  profileEmail: { fontSize: '13px', color: '#888', margin: '0 0 12px 0', textAlign: 'center' },
  barangayBadge: { display: 'flex', alignItems: 'center', gap: '5px', background: '#FFF3E0', color: '#C8601A', fontSize: '12px', fontWeight: '600', padding: '5px 12px', borderRadius: '20px', marginBottom: '8px' },
  memberSince: { fontSize: '12px', color: '#aaa', margin: '0 0 16px 0', textAlign: 'center' },
  uploadBtn: { display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '10px', borderRadius: '10px', border: '1.5px solid #f0ece6', background: '#FAF7F2', color: '#C8601A', fontSize: '13px', fontWeight: '600', cursor: 'pointer', justifyContent: 'center' },
  uploadError: { fontSize: '12px', color: '#e53935', margin: '6px 0 0 0', textAlign: 'center' },
  divider: { height: '1px', background: '#f5f5f5', margin: '20px 0' },
  reputationSection: { textAlign: 'center' },
  reputationHeader: { marginBottom: '4px' },
  reputationTitle: { fontSize: '15px', fontWeight: '700', color: '#1a1a1a' },
  reputationSubtext: { fontSize: '12px', color: '#888', margin: '0 0 12px 0' },
  reputationScore: { fontSize: '48px', fontWeight: '700', color: '#C8601A', lineHeight: '1', marginBottom: '4px' },
  reputationLabel: { fontSize: '12px', color: '#aaa', margin: '0 0 10px 0' },
  starsRow: { display: 'flex', justifyContent: 'center', gap: '4px', marginBottom: '6px' },
  reputationMotivation: { fontSize: '12px', color: '#888', margin: '0 0 16px 0' },
  statsRow: { display: 'flex', borderRadius: '12px', overflow: 'hidden', border: '1.5px solid #f0ece6' },
  statItem: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 8px', background: '#FAF7F2', borderRight: '1px solid #f0ece6', gap: '2px' },
  statValue: { fontSize: '20px', fontWeight: '700', color: '#C8601A' },
  statLabel: { fontSize: '11px', color: '#888' },
  barangaySection: {},
  barangaySectionHeader: { display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' },
  barangaySectionLabel: { fontSize: '12px', color: '#888', fontWeight: '500' },
  barangaySectionName: { fontSize: '18px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 2px 0' },
  barangaySectionCity: { fontSize: '13px', color: '#aaa', margin: '0 0 10px 0' },
  barangayNote: { display: 'flex', alignItems: 'flex-start', gap: '6px', background: '#FAF7F2', borderRadius: '8px', padding: '10px 12px' },
  barangayNoteText: { fontSize: '12px', color: '#888', lineHeight: '1.5' },
  editHeading: { marginBottom: '20px' },
  editTitle: { fontSize: '20px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  editSubtitle: { fontSize: '13px', color: '#888', margin: 0 },
  successBanner: { background: '#E8F5E9', border: '1px solid #C8E6C9', borderRadius: '10px', padding: '12px 14px', fontSize: '13px', color: '#2E7D32', marginBottom: '16px' },
  errorBanner: { background: '#fff5f5', border: '1px solid #ffcdd2', borderRadius: '10px', padding: '12px 14px', fontSize: '13px', color: '#e53935', marginBottom: '16px' },
  fieldGroup: { display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '16px' },
  fieldLabel: { fontSize: '13px', fontWeight: '600', color: '#333' },
  inputWrap: { display: 'flex', alignItems: 'center', gap: '10px', border: '1.5px solid #e8e8e8', borderRadius: '10px', padding: '0 14px', height: '46px', background: 'white' },
  inputWrapError: { borderColor: '#ffcdd2', background: '#fff5f5' },
  inputWrapReadOnly: { background: '#fafafa' },
  fieldInput: { flex: 1, border: 'none', outline: 'none', fontSize: '14px', color: '#333', background: 'transparent', fontFamily: "'Segoe UI', sans-serif" },
  readOnlyBadge: { fontSize: '11px', color: '#aaa', background: '#f0f0f0', padding: '2px 8px', borderRadius: '6px', whiteSpace: 'nowrap', flexShrink: 0 },
  fieldError: { fontSize: '12px', color: '#e53935' },
  fieldHint: { fontSize: '12px', color: '#aaa' },
  eyeBtn: { background: 'none', border: 'none', cursor: 'pointer', padding: '0', display: 'flex', alignItems: 'center', flexShrink: 0 },
  passwordDivider: { display: 'flex', alignItems: 'center', gap: '12px', margin: '8px 0 20px 0' },
  passwordDividerLine: { flex: 1, height: '1px', background: '#f0ece6' },
  passwordDividerLabel: { fontSize: '12px', color: '#aaa', fontWeight: '500', whiteSpace: 'nowrap' },
  googleAccountNote: { display: 'flex', alignItems: 'flex-start', gap: '10px', background: '#F8F9FF', border: '1.5px solid #E8EAFF', borderRadius: '10px', padding: '12px 14px', marginBottom: '16px' },
  googleAccountNoteText: { fontSize: '13px', color: '#555', lineHeight: '1.5' },
  formActions: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px', marginBottom: '28px' },
  discardBtn: { padding: '10px 20px', borderRadius: '10px', border: '1.5px solid #e8e8e8', background: 'white', fontSize: '14px', color: '#555', cursor: 'pointer', fontWeight: '500' },
  saveBtn: { padding: '10px 24px', borderRadius: '10px', border: 'none', background: '#C8601A', color: 'white', fontSize: '14px', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  btnInner: { display: 'flex', alignItems: 'center', gap: '8px' },
  btnSpinner: { width: '14px', height: '14px', border: '2px solid rgba(255,255,255,0.4)', borderTop: '2px solid white', borderRadius: '50%', animation: 'spin 0.7s linear infinite', flexShrink: 0 },
  btnSpinnerDark: { width: '14px', height: '14px', border: '2px solid rgba(200,96,26,0.3)', borderTop: '2px solid #C8601A', borderRadius: '50%', animation: 'spin 0.7s linear infinite', flexShrink: 0 },
  activitySection: { borderTop: '1px solid #f5f5f5', paddingTop: '24px' },
  activityTitle: { fontSize: '15px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 16px 0' },
  activityEmpty: { fontSize: '13px', color: '#aaa', margin: 0 },
  activityList: { display: 'flex', flexDirection: 'column' },
  activityItem: { display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 0', borderBottom: '1px solid #f5f5f5' },
  activityDot: { width: '10px', height: '10px', borderRadius: '50%', flexShrink: 0 },
  activityInfo: { flex: 1, minWidth: 0 },
  activityLabel: { fontSize: '13px', color: '#333', fontWeight: '500', marginBottom: '2px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  activityType: { color: '#888', fontWeight: '400' },
  activityDate: { fontSize: '11px', color: '#aaa' },
  statusBadge: { fontSize: '11px', fontWeight: '700', padding: '3px 9px', borderRadius: '20px', flexShrink: 0, whiteSpace: 'nowrap' },
  joinedBadge: { fontSize: '11px', color: '#aaa', flexShrink: 0 },
  loadingBox: { display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '24px 0' },
  spinner: { width: '24px', height: '24px', border: '3px solid #f0ece6', borderTop: '3px solid #C8601A', borderRadius: '50%', animation: 'spin 0.7s linear infinite' },
};

export default ProfilePage;