import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// ── Helpers ────────────────────────────────────────────────────────────────────
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

// ── Route map ──────────────────────────────────────────────────────────────────
const NAV_LINKS = [
  { label: 'Dashboard',    path: '/dashboard' },
  { label: 'Favor Feed',   path: '/favor-feed' },
  { label: 'Announcements',path: '/announcements' },
  { label: 'My Activity',  path: '/my-activity' },
];

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav style={s.navbar}>
      {/* Logo */}
      <div style={s.navLogo} onClick={() => navigate('/dashboard')} role="button">
        <svg width="28" height="22" viewBox="0 0 72 56" fill="none">
          <path d="M24 4C16.268 4 10 10.268 10 18C10 26.5 20 38 24 42C28 38 38 26.5 38 18C38 10.268 31.732 4 24 4Z" fill="#C8601A"/>
          <circle cx="24" cy="18" r="5" fill="white"/>
          <path d="M48 8C41.373 8 36 13.373 36 20C36 27.5 44.5 38 48 42C51.5 38 60 27.5 60 20C60 13.373 54.627 8 48 8Z" fill="#C8601A" fillOpacity="0.6"/>
          <path d="M48 16.5C48 16.5 45 14 43.5 16C42 18 44 20 48 23C52 20 54 18 52.5 16C51 14 48 16.5 48 16.5Z" fill="white"/>
        </svg>
        <span style={s.navLogoText}>LocalLoop</span>
      </div>

      {/* Nav Links */}
      <div style={s.navLinks}>
        {NAV_LINKS.map(({ label, path }) => (
          <span
            key={label}
            style={isActive(path) ? s.navLinkActive : s.navLink}
            onClick={() => navigate(path)}
          >
            {label}
            {isActive(path) && <div style={s.navLinkUnderline} />}
          </span>
        ))}
      </div>

      {/* Avatar + Dropdown */}
      <div style={s.navRight} ref={dropdownRef}>
        <div style={s.avatarBtn} onClick={() => setDropdownOpen(o => !o)}>
          {user?.profileImageUrl ? (
            <img src={user.profileImageUrl} alt="avatar" style={s.avatarImg} />
          ) : (
            <div style={{ ...s.avatarCircle, background: avatarColor(user?.name || '') }}>
              {initials(user?.name || 'U')}
            </div>
          )}
          <svg
            width="14" height="14" viewBox="0 0 24 24"
            fill="none" stroke="#555" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round"
            style={{ transition: 'transform 0.2s', transform: dropdownOpen ? 'rotate(180deg)' : 'none' }}
          >
            <polyline points="6 9 12 15 18 9"/>
          </svg>
        </div>

        {dropdownOpen && (
          <div style={s.dropdown}>
            <div style={s.dropdownHeader}>
              <div style={{ ...s.dropdownAvatar, background: avatarColor(user?.name || '') }}>
                {initials(user?.name || 'U')}
              </div>
              <div>
                <div style={s.dropdownName}>{user?.name}</div>
                <div style={s.dropdownEmail}>{user?.email}</div>
              </div>
            </div>
            <div style={s.dropdownDivider} />
            <div style={s.dropdownItem} onClick={() => { setDropdownOpen(false); navigate('/profile'); }}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              My Profile
            </div>
            <div style={s.dropdownDivider} />
            <div style={{ ...s.dropdownItem, color: '#e53935' }} onClick={handleLogout}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              Log Out
            </div>
          </div>
        )}
      </div>
    </nav>
  );
};

const s = {
  navbar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 40px',
    height: '60px',
    background: 'white',
    borderBottom: '1px solid #f0ece6',
    position: 'sticky',
    top: 0,
    zIndex: 100,
    boxShadow: '0 1px 8px rgba(0,0,0,0.05)',
  },
  navLogo: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    cursor: 'pointer',
  },
  navLogoText: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#1a1a1a',
    fontFamily: "'Georgia', serif",
  },
  navLinks: {
    display: 'flex',
    alignItems: 'center',
    gap: '32px',
  },
  navLink: {
    fontSize: '14px',
    color: '#666',
    cursor: 'pointer',
    fontWeight: '500',
    position: 'relative',
    padding: '4px 0',
  },
  navLinkActive: {
    fontSize: '14px',
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '600',
    position: 'relative',
    padding: '4px 0',
  },
  navLinkUnderline: {
    position: 'absolute',
    bottom: '-2px',
    left: 0,
    right: 0,
    height: '2px',
    background: '#C8601A',
    borderRadius: '2px',
  },
  navRight: { position: 'relative' },
  avatarBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    cursor: 'pointer',
    padding: '4px 8px',
    borderRadius: '8px',
  },
  avatarImg: {
    width: '34px',
    height: '34px',
    borderRadius: '50%',
    objectFit: 'cover',
  },
  avatarCircle: {
    width: '34px',
    height: '34px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '13px',
    fontWeight: '700',
  },
  dropdown: {
    position: 'absolute',
    top: 'calc(100% + 8px)',
    right: 0,
    background: 'white',
    borderRadius: '14px',
    boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
    minWidth: '220px',
    padding: '6px 0',
    zIndex: 200,
    border: '1px solid #f0ece6',
  },
  dropdownHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px 16px',
  },
  dropdownAvatar: {
    width: '38px',
    height: '38px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '14px',
    fontWeight: '700',
    flexShrink: 0,
  },
  dropdownName: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },
  dropdownEmail: { fontSize: '12px', color: '#888', marginTop: '1px' },
  dropdownDivider: { height: '1px', background: '#f5f5f5', margin: '4px 0' },
  dropdownItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '10px 16px',
    fontSize: '13px',
    color: '#333',
    cursor: 'pointer',
  },
};

export default Navbar;