import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/context/AuthContext';
import api from '../../shared/api/axios';
import Navbar from '../../shared/components/Navbar';

// ── Helpers ───────────────────────────────────────────────────────────────────
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
function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

// ── Spinner keyframe (injected once) ─────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('ll-admin-spin')) {
  const style = document.createElement('style');
  style.id = 'll-admin-spin';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ── Status badge colors ───────────────────────────────────────────────────────
const STATUS_COLORS = {
  OPEN:      { bg: '#FFF3E0', text: '#E65100', label: 'OPEN' },
  CLAIMED:   { bg: '#FFF8E1', text: '#F9A825', label: 'CLAIMED' },
  COMPLETED: { bg: '#E8F5E9', text: '#2E7D32', label: 'COMPLETED' },
};

// ── Category tag colors ───────────────────────────────────────────────────────
const CATEGORY_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

// ── Stat Card ─────────────────────────────────────────────────────────────────
const StatCard = ({ icon, value, label, loading }) => (
  <div style={s.statCard}>
    <div style={s.statIcon}>{icon}</div>
    <div>
      {loading
        ? <div style={s.statSkeleton} />
        : <div style={s.statValue}>{value}</div>
      }
      <div style={s.statLabel}>{label}</div>
    </div>
  </div>
);

// ── Progress bar row ──────────────────────────────────────────────────────────
const ProgressRow = ({ label, value, total, color }) => {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <div style={s.progressRow}>
      <div style={s.progressLabelRow}>
        <span style={s.progressLabel}>{label}</span>
        <span style={s.progressValue}>{value}</span>
      </div>
      <div style={s.progressTrack}>
        <div style={{ ...s.progressFill, width: `${pct}%`, background: color }} />
      </div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
const AdminDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [stats, setStats]             = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [recentFavors, setRecentFavors] = useState([]);
  const [favorsLoading, setFavorsLoading] = useState(true);
  const [announcements, setAnnouncements] = useState([]);
  const [annLoading, setAnnLoading]     = useState(true);

  // ── Fetch dashboard stats ─────────────────────────────────────────────────
  useEffect(() => {
    api.get('/admin/stats')
      .then(res => setStats(res.data?.data || res.data))
      .catch(() => setStats(null))
      .finally(() => setStatsLoading(false));
  }, []);

  // ── Fetch recent favor activity ───────────────────────────────────────────
  useEffect(() => {
    api.get('/admin/favors/recent')
      .then(res => {
        const d = res.data?.data;
        setRecentFavors(Array.isArray(d) ? d : []);
      })
      .catch(() => setRecentFavors([]))
      .finally(() => setFavorsLoading(false));
  }, []);

  // ── Fetch recent announcements (sidebar) ─────────────────────────────────
  useEffect(() => {
    api.get('/announcements', { params: { page: 0, size: 2 } })
      .then(res => {
        const d = res.data?.data;
        setAnnouncements(d?.content || d || []);
      })
      .catch(() => setAnnouncements([]))
      .finally(() => setAnnLoading(false));
  }, []);

  const totalFavors = stats
    ? (stats.openFavors + stats.claimedFavors + stats.completedFavors)
    : 0;

  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* ── Page title ──────────────────────────────────────────────────── */}
        <div style={s.pageHeader}>
          <h1 style={s.pageTitle}>Admin Dashboard</h1>
          <p style={s.pageSubtitle}>
            Manage {user?.barangay || 'your barangay'}'s LocalLoop platform.
          </p>
        </div>

        {/* ── Body: left + right ──────────────────────────────────────────── */}
        <div style={s.body}>

          {/* ════ LEFT COLUMN ═══════════════════════════════════════════════ */}
          <div style={s.leftCol}>

            {/* Stat cards row */}
            <div style={s.statsRow}>
              <StatCard
                loading={statsLoading}
                value={stats?.totalResidents ?? 0}
                label="Registered Residents"
                icon={
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                    stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                    <path d="M16 3.13a4 4 0 010 7.75"/>
                  </svg>
                }
              />
              <StatCard
                loading={statsLoading}
                value={stats?.totalFavors ?? 0}
                label="Total Favors Posted"
                icon={
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                    stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                    <rect x="9" y="3" width="6" height="4" rx="2"/>
                    <path d="M9 12h6M9 16h4"/>
                  </svg>
                }
              />
              <StatCard
                loading={statsLoading}
                value={stats?.totalAnnouncements ?? 0}
                label="Announcements"
                icon={
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                    stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 17H2a3 3 0 000 6h20v-6z"/>
                    <path d="M18 11V5a2 2 0 00-2-2H4a2 2 0 00-2 2v12"/>
                    <path d="M22 11V5"/>
                  </svg>
                }
              />
              <StatCard
                loading={statsLoading}
                value={stats?.totalReputationGiven ?? 0}
                label="Reputation Points Given"
                icon={
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                    stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                  </svg>
                }
              />
            </div>

            {/* Recent Favor Activity table */}
            <div style={s.card}>
              <div style={s.cardHeader}>
                <h2 style={s.cardTitle}>Recent Favor Activity</h2>
                <span style={s.viewAllLink} onClick={() => navigate('/admin/favors')}>
                  View all →
                </span>
              </div>

              {favorsLoading ? (
                <div style={s.loadingBox}><div style={s.spinner} /></div>
              ) : recentFavors.length === 0 ? (
                <div style={s.emptyBox}>No favor activity yet.</div>
              ) : (
                <table style={s.table}>
                  <thead>
                    <tr>
                      {['Favor', 'Resident', 'Category', 'Status', 'Posted'].map(h => (
                        <th key={h} style={s.th}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {recentFavors.map((favor, idx) => {
                      const status   = STATUS_COLORS[favor.status]   || STATUS_COLORS.OPEN;
                      const catColor = CATEGORY_COLORS[favor.category] || CATEGORY_COLORS.Other;
                      return (
                        <tr
                          key={favor.id || idx}
                          style={s.tr}
                          onMouseEnter={e => e.currentTarget.style.background = '#FAF7F2'}
                          onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                        >
                          {/* Favor title */}
                          <td style={s.td}>
                            <span style={s.favorTitle}>{favor.title}</span>
                          </td>

                          {/* Resident avatar + name */}
                          <td style={s.td}>
                            <div style={s.residentCell}>
                              <div style={{
                                ...s.miniAvatar,
                                background: avatarColor(favor.requesterName || '')
                              }}>
                                {initials(favor.requesterName || '?')}
                              </div>
                              <span style={s.residentName}>{favor.requesterName}</span>
                            </div>
                          </td>

                          {/* Category tag */}
                          <td style={s.td}>
                            <span style={{
                              ...s.tag,
                              background: catColor.bg,
                              color: catColor.text,
                            }}>
                              {favor.category}
                            </span>
                          </td>

                          {/* Status badge */}
                          <td style={s.td}>
                            <span style={{
                              ...s.statusBadge,
                              background: status.bg,
                              color: status.text,
                            }}>
                              {status.label}
                            </span>
                          </td>

                          {/* Date */}
                          <td style={{ ...s.td, color: '#888', fontSize: '13px' }}>
                            {formatDate(favor.createdAt)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* Recent Announcements */}
            <div style={s.card}>
              <div style={s.cardHeader}>
                <h2 style={s.cardTitle}>Recent Announcements</h2>
                <button
                  style={s.newAnnBtn}
                  onClick={() => navigate('/admin/announcements')}
                >
                  + New Announcement
                </button>
              </div>

              {annLoading ? (
                <div style={s.loadingBox}><div style={s.spinner} /></div>
              ) : announcements.length === 0 ? (
                <div style={s.emptyBox}>No announcements yet.</div>
              ) : (
                <div style={s.annList}>
                  {announcements.map((ann, i) => (
                    <div key={ann.id || i} style={s.annRow}>
                      <div>
                        <div style={s.annTitle}>{ann.title}</div>
                        <div style={s.annMeta}>
                          {formatDate(ann.createdAt)} · {ann.category || 'General'}
                        </div>
                      </div>
                      <div style={s.annActions}>
                        {/* Edit */}
                        <button
                          style={s.iconBtn}
                          title="Edit"
                          onClick={() => navigate('/admin/announcements')}
                        >
                          <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                            stroke="#888" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                          </svg>
                        </button>
                        {/* Delete */}
                        <button style={{ ...s.iconBtn, color: '#e53935' }} title="Delete">
                          <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                            stroke="#e53935" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                            <path d="M10 11v6M14 11v6"/>
                            <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>{/* end leftCol */}

          {/* ════ RIGHT COLUMN ══════════════════════════════════════════════ */}
          <div style={s.rightCol}>

            {/* Quick Actions */}
            <div style={s.card}>
              <h3 style={s.sideCardTitle}>⚡ Quick Actions</h3>
              <button
                style={s.quickActionPrimary}
                onClick={() => navigate('/admin/announcements')}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 17H2a3 3 0 000 6h20v-6z"/>
                  <path d="M18 11V5a2 2 0 00-2-2H4a2 2 0 00-2 2v12"/>
                  <path d="M22 11V5"/>
                </svg>
                Post New Announcement
              </button>
              <button
                style={s.quickActionSecondary}
                onClick={() => navigate('/admin/residents')}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="#444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                  <path d="M16 3.13a4 4 0 010 7.75"/>
                </svg>
                View All Residents
              </button>
              <button
                style={s.quickActionSecondary}
                onClick={() => navigate('/admin/favors')}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                  stroke="#444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                  <rect x="9" y="3" width="6" height="4" rx="2"/>
                </svg>
                View All Favors
              </button>
            </div>

            {/* Barangay Stats */}
            <div style={s.card}>
              <div style={s.barangayHeader}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
                <div>
                  <div style={s.barangayName}>{user?.barangay || 'Your Barangay'}</div>
                  <div style={s.barangayCity}>Cebu City, Philippines</div>
                </div>
              </div>

              <div style={s.statsList}>
                {[
                  { label: 'Active Residents', value: stats?.totalResidents,    accent: false },
                  { label: 'Open Favors',       value: stats?.openFavors,       accent: true  },
                  { label: 'Claimed Favors',    value: stats?.claimedFavors,    accent: true  },
                  { label: 'Completed Favors',  value: stats?.completedFavors,  accent: true  },
                  { label: 'Announcements',     value: stats?.totalAnnouncements, accent: false },
                ].map(({ label, value, accent }) => (
                  <div key={label} style={s.statsListRow}>
                    <span style={s.statsListLabel}>{label}</span>
                    {statsLoading
                      ? <div style={s.inlineSkeleton} />
                      : <span style={accent ? s.statsListValueAccent : s.statsListValue}>
                          {value ?? 0}
                        </span>
                    }
                  </div>
                ))}
              </div>
            </div>

            {/* Favor Status Breakdown */}
            <div style={s.card}>
              <h3 style={s.sideCardTitle}>Favor Status Breakdown</h3>
              {statsLoading ? (
                <div style={s.loadingBox}><div style={s.spinner} /></div>
              ) : (
                <div style={{ marginTop: '12px' }}>
                  <ProgressRow
                    label="Open"
                    value={stats?.openFavors ?? 0}
                    total={totalFavors}
                    color="#C8601A"
                  />
                  <ProgressRow
                    label="Claimed"
                    value={stats?.claimedFavors ?? 0}
                    total={totalFavors}
                    color="#F9A825"
                  />
                  <ProgressRow
                    label="Completed"
                    value={stats?.completedFavors ?? 0}
                    total={totalFavors}
                    color="#4CAF50"
                  />
                </div>
              )}
            </div>

          </div>{/* end rightCol */}
        </div>{/* end body */}
      </div>{/* end content */}
    </div>
  );
};

// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
  page: {
    minHeight: '100vh',
    background: '#FAF7F2',
    fontFamily: "'Segoe UI', sans-serif",
  },
  content: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '36px 32px',
  },

  // Header
  pageHeader: { marginBottom: '28px' },
  pageTitle: {
    fontSize: '26px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 4px 0',
  },
  pageSubtitle: {
    fontSize: '14px',
    color: '#888',
    margin: 0,
  },

  // Layout
  body: {
    display: 'flex',
    gap: '24px',
    alignItems: 'flex-start',
  },
  leftCol: {
    flex: 1,
    minWidth: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  rightCol: {
    width: '300px',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },

  // Stat cards
  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '14px',
  },
  statCard: {
    background: 'white',
    borderRadius: '16px',
    padding: '20px 18px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)',
    display: 'flex',
    alignItems: 'center',
    gap: '14px',
  },
  statIcon: {
    width: '44px',
    height: '44px',
    borderRadius: '12px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  statValue: {
    fontSize: '26px',
    fontWeight: '700',
    color: '#1a1a1a',
    lineHeight: 1,
    marginBottom: '4px',
  },
  statLabel: {
    fontSize: '12px',
    color: '#888',
    lineHeight: '1.3',
  },
  statSkeleton: {
    width: '60px',
    height: '26px',
    background: '#f0ece6',
    borderRadius: '6px',
    marginBottom: '4px',
    animation: 'pulse 1.5s ease-in-out infinite',
  },

  // Cards
  card: {
    background: 'white',
    borderRadius: '20px',
    padding: '22px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '18px',
  },
  cardTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: 0,
  },
  viewAllLink: {
    fontSize: '13px',
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
  },
  sideCardTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 14px 0',
  },

  // Table
  table: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    textAlign: 'left',
    fontSize: '12px',
    fontWeight: '600',
    color: '#aaa',
    padding: '0 12px 12px 0',
    borderBottom: '1px solid #f0ece6',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
  },
  tr: {
    borderBottom: '1px solid #f7f5f2',
    transition: 'background 0.15s',
    cursor: 'default',
  },
  td: {
    padding: '14px 12px 14px 0',
    fontSize: '13px',
    color: '#333',
    verticalAlign: 'middle',
  },
  favorTitle: {
    fontWeight: '500',
    color: '#1a1a1a',
    fontSize: '13px',
  },
  residentCell: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  miniAvatar: {
    width: '26px',
    height: '26px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '10px',
    fontWeight: '700',
    flexShrink: 0,
  },
  residentName: {
    fontSize: '13px',
    color: '#333',
    fontWeight: '500',
  },
  tag: {
    fontSize: '11px',
    fontWeight: '600',
    padding: '3px 9px',
    borderRadius: '20px',
    display: 'inline-block',
  },
  statusBadge: {
    fontSize: '11px',
    fontWeight: '700',
    padding: '3px 9px',
    borderRadius: '20px',
    display: 'inline-block',
    letterSpacing: '0.3px',
  },

  // Announcements mini list
  annList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0',
  },
  annRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '14px 0',
    borderBottom: '1px solid #f5f5f5',
    gap: '12px',
  },
  annTitle: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#1a1a1a',
    marginBottom: '2px',
  },
  annMeta: {
    fontSize: '11px',
    color: '#aaa',
  },
  annActions: {
    display: 'flex',
    gap: '4px',
    flexShrink: 0,
  },
  iconBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: '6px',
    borderRadius: '8px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  newAnnBtn: {
    padding: '8px 14px',
    borderRadius: '10px',
    background: '#C8601A',
    color: 'white',
    border: 'none',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },

  // Quick Actions
  quickActionPrimary: {
    width: '100%',
    padding: '12px 16px',
    borderRadius: '12px',
    background: '#C8601A',
    color: 'white',
    border: 'none',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '10px',
  },
  quickActionSecondary: {
    width: '100%',
    padding: '11px 16px',
    borderRadius: '12px',
    background: '#FAF7F2',
    color: '#333',
    border: '1.5px solid #f0ece6',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '8px',
  },

  // Barangay panel
  barangayHeader: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '8px',
    marginBottom: '16px',
    paddingBottom: '14px',
    borderBottom: '1px solid #f5f5f5',
  },
  barangayName: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  barangayCity: {
    fontSize: '12px',
    color: '#aaa',
    marginTop: '1px',
  },
  statsList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0',
  },
  statsListRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 0',
    borderBottom: '1px solid #f9f7f4',
  },
  statsListLabel: {
    fontSize: '13px',
    color: '#555',
  },
  statsListValue: {
    fontSize: '13px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  statsListValueAccent: {
    fontSize: '13px',
    fontWeight: '700',
    color: '#C8601A',
  },
  inlineSkeleton: {
    width: '28px',
    height: '14px',
    background: '#f0ece6',
    borderRadius: '4px',
  },

  // Progress bars
  progressRow: {
    marginBottom: '14px',
  },
  progressLabelRow: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: '6px',
  },
  progressLabel: {
    fontSize: '13px',
    color: '#555',
    fontWeight: '500',
  },
  progressValue: {
    fontSize: '13px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  progressTrack: {
    height: '8px',
    background: '#f0ece6',
    borderRadius: '20px',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: '20px',
    transition: 'width 0.6s ease',
  },

  // Utility
  loadingBox: {
    display: 'flex',
    justifyContent: 'center',
    padding: '24px 0',
  },
  spinner: {
    width: '22px',
    height: '22px',
    border: '3px solid #f0ece6',
    borderTop: '3px solid #C8601A',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
  },
  emptyBox: {
    textAlign: 'center',
    color: '#aaa',
    fontSize: '13px',
    padding: '20px 0',
  },
};

export default AdminDashboardPage;