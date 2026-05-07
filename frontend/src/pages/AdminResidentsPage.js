import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import Navbar from '../components/Navbar';

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
function formatJoined(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
}

// ── Spinner keyframe ──────────────────────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('ll-res-spin')) {
  const style = document.createElement('style');
  style.id = 'll-res-spin';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

const SEARCH_BY_OPTIONS = [
  { value: 'both',  label: 'Name or Email' },
  { value: 'name',  label: 'Name only' },
  { value: 'email', label: 'Email only' },
];

// ── Stat Card ─────────────────────────────────────────────────────────────────
const StatCard = ({ icon, value, label, loading }) => (
  <div style={s.statCard}>
    <div style={s.statIcon}>{icon}</div>
    <div>
      {loading ? <div style={s.statSkeleton} /> : <div style={s.statValue}>{value ?? 0}</div>}
      <div style={s.statLabel}>{label}</div>
    </div>
  </div>
);

// ── Confirmation Modal ────────────────────────────────────────────────────────
const ConfirmModal = ({ resident, onConfirm, onCancel, loading }) => {
  const isDeactivating = resident?.active;
  return (
    <div style={s.modalOverlay}>
      <div style={s.modal}>
        {/* Icon */}
        <div style={{
          ...s.modalIcon,
          background: isDeactivating ? '#FFF3E0' : '#E8F5E9',
        }}>
          {isDeactivating ? (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
              stroke="#C8601A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
          ) : (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
              stroke="#2E7D32" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          )}
        </div>

        <h3 style={s.modalTitle}>
          {isDeactivating ? 'Remove Resident' : 'Reactivate Resident'}
        </h3>

        <p style={s.modalBody}>
          {isDeactivating ? (
            <>
              Are you sure you want to remove <strong>{resident?.name}</strong>?
              They will be blocked from logging in and will need to select a new
              barangay. Their past activity and history will be preserved.
            </>
          ) : (
            <>
              Are you sure you want to reactivate <strong>{resident?.name}</strong>?
              They will be able to log in again.
            </>
          )}
        </p>

        <div style={s.modalActions}>
          <button style={s.modalCancelBtn} onClick={onCancel} disabled={loading}>
            Cancel
          </button>
          <button
            style={{
              ...s.modalConfirmBtn,
              background: isDeactivating ? '#C8601A' : '#2E7D32',
              opacity: loading ? 0.7 : 1,
            }}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading
              ? 'Processing...'
              : isDeactivating ? 'Yes, Remove' : 'Yes, Reactivate'
            }
          </button>
        </div>
      </div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
const AdminResidentsPage = () => {
  const { user } = useAuth();

  // ── State ─────────────────────────────────────────────────────────────────
  const [residents, setResidents]           = useState([]);
  const [loading, setLoading]               = useState(true);
  const [stats, setStats]                   = useState(null);
  const [statsLoading, setStatsLoading]     = useState(true);

  const [search, setSearch]                 = useState('');
  const [searchBy, setSearchBy]             = useState('both');
  const [inputValue, setInputValue]         = useState('');

  const [page, setPage]                     = useState(0);
  const [totalPages, setTotalPages]         = useState(1);
  const [totalElements, setTotalElements]   = useState(0);

  // Modal state
  const [modalResident, setModalResident]   = useState(null);
  const [actionLoading, setActionLoading]   = useState(false);
  const [toast, setToast]                   = useState(null); // { message, type }

  const PAGE_SIZE = 10;

  // ── Toast helper ─────────────────────────────────────────────────────────
  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3500);
  };

  // ── Fetch stats ───────────────────────────────────────────────────────────
  const fetchStats = useCallback(() => {
    api.get('/admin/residents/stats')
      .then(res => setStats(res.data?.data || res.data))
      .catch(() => setStats(null))
      .finally(() => setStatsLoading(false));
  }, []);

  useEffect(() => { fetchStats(); }, [fetchStats]);

  // ── Fetch residents ───────────────────────────────────────────────────────
  const fetchResidents = useCallback((pageNum, searchVal, searchByVal) => {
    setLoading(true);
    const params = { page: pageNum, size: PAGE_SIZE, searchBy: searchByVal };
    if (searchVal) params.search = searchVal;

    api.get('/admin/residents', { params })
      .then(res => {
        const d = res.data?.data;
        setResidents(d?.content || []);
        setTotalPages(d?.totalPages ?? 1);
        setTotalElements(d?.totalElements ?? 0);
      })
      .catch(() => setResidents([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchResidents(page, search, searchBy);
  }, [page, search, searchBy, fetchResidents]);

  // ── Search ────────────────────────────────────────────────────────────────
  const handleSearch = () => { setPage(0); setSearch(inputValue.trim()); };
  const handleKeyDown = (e) => { if (e.key === 'Enter') handleSearch(); };
  const handleSearchByChange = (val) => { setSearchBy(val); setPage(0); };
  const handleClearSearch = () => { setInputValue(''); setSearch(''); setPage(0); };

  // ── Pagination ────────────────────────────────────────────────────────────
  const goToPage = (p) => { if (p >= 0 && p < totalPages) setPage(p); };
  const getPageNumbers = () => {
    const pages = [];
    const start = Math.max(0, page - 2);
    const end   = Math.min(totalPages - 1, page + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  // ── Deactivate / Reactivate ───────────────────────────────────────────────
  const handleActionClick = (resident) => {
    // Prevent admin from acting on themselves
    if (resident.id === user?.id) return;
    setModalResident(resident);
  };

  const handleConfirm = async () => {
    if (!modalResident) return;
    setActionLoading(true);
    const endpoint = modalResident.active
      ? `/admin/residents/${modalResident.id}/deactivate`
      : `/admin/residents/${modalResident.id}/reactivate`;

    try {
      await api.patch(endpoint);
      showToast(
        modalResident.active
          ? `${modalResident.name} has been removed.`
          : `${modalResident.name} has been reactivated.`,
        modalResident.active ? 'warning' : 'success'
      );
      setModalResident(null);
      // Refresh both table and stats
      fetchResidents(page, search, searchBy);
      fetchStats();
    } catch (err) {
      showToast(err.response?.data?.error?.message || 'Action failed.', 'error');
      setModalResident(null);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div style={s.page}>
      <Navbar />

      {/* Toast notification */}
      {toast && (
        <div style={{
          ...s.toast,
          background: toast.type === 'success' ? '#2E7D32'
                    : toast.type === 'warning'  ? '#C8601A'
                    : '#c62828',
        }}>
          {toast.message}
        </div>
      )}

      {/* Confirmation modal */}
      {modalResident && (
        <ConfirmModal
          resident={modalResident}
          onConfirm={handleConfirm}
          onCancel={() => setModalResident(null)}
          loading={actionLoading}
        />
      )}

      <div style={s.content}>
        {/* Header */}
        <div style={s.pageHeader}>
          <h1 style={s.pageTitle}>Residents</h1>
          <p style={s.pageSubtitle}>
            All registered residents of {user?.barangay || 'your barangay'}.
          </p>
        </div>

        {/* Stat Cards */}
        <div style={s.statsRow}>
          <StatCard loading={statsLoading} value={stats?.totalUsers} label="Total Residents"
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
          <StatCard loading={statsLoading} value={stats?.totalReputation} label="Total Reputation Given"
            icon={
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            }
          />
          <StatCard loading={statsLoading} value={stats?.totalCompleted} label="Favors Completed"
            icon={
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            }
          />
        </div>

        {/* Table Card */}
        <div style={s.tableCard}>

          {/* Search row */}
          <div style={s.searchRow}>
            <div style={s.searchBox}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                style={{ flexShrink: 0 }}>
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                style={s.searchInput}
                type="text"
                placeholder="Search residents by name or email..."
                value={inputValue}
                onChange={e => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
              />
              {inputValue && (
                <button style={s.clearBtn} onClick={handleClearSearch}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                    stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              )}
            </div>

            <div style={s.selectWrapper}>
              <select
                style={s.select}
                value={searchBy}
                onChange={e => handleSearchByChange(e.target.value)}
              >
                {SEARCH_BY_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                stroke="#888" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                style={s.selectChevron}>
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>

            <button style={s.searchBtn} onClick={handleSearch}>Search</button>
          </div>

          {/* Result count */}
          {!loading && (
            <div style={s.resultCount}>
              {search
                ? `${totalElements} result${totalElements !== 1 ? 's' : ''} for "${search}"`
                : `${totalElements} total resident${totalElements !== 1 ? 's' : ''}`
              }
            </div>
          )}

          {/* Table */}
          {loading ? (
            <div style={s.loadingBox}><div style={s.spinner} /></div>
          ) : residents.length === 0 ? (
            <div style={s.emptyBox}>
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
                stroke="#ddd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
              <p style={{ color: '#aaa', fontSize: '14px', margin: '10px 0 0 0' }}>
                No residents found.
              </p>
            </div>
          ) : (
            <table style={s.table}>
              <thead>
                <tr>
                  {['Resident', 'Email', 'Role', 'Reputation', 'Joined', 'Favors', 'Status', 'Action'].map(h => (
                    <th key={h} style={s.th}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {residents.map((r, idx) => {
                  const isAdmin   = r.role === 'ROLE_ADMIN';
                  const isSelf    = r.id === user?.id;
                  const isActive  = r.active !== false; // default true

                  return (
                    <tr
                      key={r.id || idx}
                      style={{
                        ...s.tr,
                        opacity: isActive ? 1 : 0.6,
                      }}
                      onMouseEnter={e => e.currentTarget.style.background = '#FAF7F2'}
                      onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                    >
                      {/* Resident */}
                      <td style={s.td}>
                        <div style={s.residentCell}>
                          <div style={{
                            ...s.avatar,
                            background: isAdmin ? '#1a1a1a' : avatarColor(r.name || ''),
                          }}>
                            {r.profileImageUrl
                              ? <img src={r.profileImageUrl} alt={r.name} style={s.avatarImg} />
                              : initials(r.name || '?')
                            }
                          </div>
                          <div>
                            <div style={s.residentName}>{r.name}</div>
                            <div style={s.residentBarangay}>{r.barangay}</div>
                          </div>
                        </div>
                      </td>

                      {/* Email */}
                      <td style={{ ...s.td, color: '#555', fontSize: '13px' }}>{r.email}</td>

                      {/* Role badge */}
                      <td style={s.td}>
                        {isAdmin
                          ? <span style={s.adminBadge}>ADMIN</span>
                          : <span style={s.userBadge}>USER</span>
                        }
                      </td>

                      {/* Reputation */}
                      <td style={s.td}>
                        {isAdmin ? (
                          <span style={s.dash}>—</span>
                        ) : (
                          <div style={s.repCell}>
                            <svg width="13" height="13" viewBox="0 0 24 24"
                              fill="#F9A825" stroke="none">
                              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                            </svg>
                            <span style={s.repValue}>{r.reputationScore ?? 0}</span>
                          </div>
                        )}
                      </td>

                      {/* Joined */}
                      <td style={{ ...s.td, color: '#888', fontSize: '13px' }}>
                        {formatJoined(r.createdAt)}
                      </td>

                      {/* Favors */}
                      <td style={{ ...s.td, fontWeight: '600', color: '#333', fontSize: '13px' }}>
                        {isAdmin ? <span style={s.dash}>—</span> : r.favorsPosted}
                      </td>

                      {/* Active/Inactive status */}
                      <td style={s.td}>
                        {isActive ? (
                          <span style={s.activeBadge}>Active</span>
                        ) : (
                          <span style={s.inactiveBadge}>Removed</span>
                        )}
                      </td>

                      {/* Action button */}
                      <td style={s.td}>
                        {isSelf || isAdmin ? (
                          <span style={s.dash}>—</span>
                        ) : isActive ? (
                          <button
                            style={s.removeBtn}
                            onClick={() => handleActionClick(r)}
                            title="Remove resident from barangay"
                          >
                            Remove
                          </button>
                        ) : (
                          <button
                            style={s.reactivateBtn}
                            onClick={() => handleActionClick(r)}
                            title="Reactivate resident"
                          >
                            Reactivate
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          {/* Pagination */}
          {!loading && totalPages > 1 && (
            <div style={s.pagination}>
              <button
                style={{ ...s.pageBtn, ...(page === 0 ? s.pageBtnDisabled : {}) }}
                onClick={() => goToPage(page - 1)}
                disabled={page === 0}
              >
                ‹ Previous
              </button>
              {getPageNumbers().map(p => (
                <button
                  key={p}
                  style={{ ...s.pageBtn, ...(p === page ? s.pageBtnActive : {}) }}
                  onClick={() => goToPage(p)}
                >
                  {p + 1}
                </button>
              ))}
              <button
                style={{ ...s.pageBtn, ...(page >= totalPages - 1 ? s.pageBtnDisabled : {}) }}
                onClick={() => goToPage(page + 1)}
                disabled={page >= totalPages - 1}
              >
                Next ›
              </button>
            </div>
          )}

        </div>
      </div>
    </div>
  );
};

// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
  page: { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif" },
  content: { maxWidth: '1200px', margin: '0 auto', padding: '36px 32px' },

  pageHeader: { marginBottom: '28px' },
  pageTitle:  { fontSize: '26px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  pageSubtitle: { fontSize: '14px', color: '#888', margin: 0 },

  statsRow: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '16px', marginBottom: '24px',
  },
  statCard: {
    background: 'white', borderRadius: '16px', padding: '22px 20px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)', display: 'flex',
    alignItems: 'center', gap: '16px',
  },
  statIcon: {
    width: '48px', height: '48px', borderRadius: '14px', background: '#FFF3E0',
    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
  },
  statValue:   { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', lineHeight: 1, marginBottom: '4px' },
  statLabel:   { fontSize: '13px', color: '#888' },
  statSkeleton:{ width: '64px', height: '28px', background: '#f0ece6', borderRadius: '6px', marginBottom: '4px' },

  tableCard: {
    background: 'white', borderRadius: '20px', padding: '24px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },

  searchRow: { display: 'flex', gap: '10px', marginBottom: '8px', alignItems: 'center' },
  searchBox: {
    flex: 1, display: 'flex', alignItems: 'center', gap: '10px',
    padding: '0 14px', border: '1.5px solid #e8e8e8', borderRadius: '12px',
    background: 'white', height: '42px',
  },
  searchInput: { flex: 1, border: 'none', outline: 'none', fontSize: '14px', color: '#333', background: 'transparent' },
  clearBtn:    { background: 'none', border: 'none', cursor: 'pointer', padding: '2px', display: 'flex', alignItems: 'center' },
  selectWrapper: { position: 'relative', display: 'flex', alignItems: 'center' },
  select: {
    appearance: 'none', padding: '0 36px 0 14px', height: '42px',
    border: '1.5px solid #e8e8e8', borderRadius: '12px', fontSize: '13px',
    color: '#333', background: 'white', cursor: 'pointer', outline: 'none',
    fontFamily: "'Segoe UI', sans-serif",
  },
  selectChevron: { position: 'absolute', right: '12px', pointerEvents: 'none' },
  searchBtn: {
    padding: '0 20px', height: '42px', borderRadius: '12px',
    background: '#C8601A', color: 'white', border: 'none',
    fontSize: '13px', fontWeight: '600', cursor: 'pointer', flexShrink: 0,
  },
  resultCount: { fontSize: '12px', color: '#aaa', marginBottom: '16px' },

  table:  { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#aaa',
    padding: '0 12px 12px 0', borderBottom: '1px solid #f0ece6',
    textTransform: 'uppercase', letterSpacing: '0.4px',
  },
  tr: { borderBottom: '1px solid #f7f5f2', transition: 'background 0.15s' },
  td: { padding: '14px 12px 14px 0', fontSize: '13px', color: '#333', verticalAlign: 'middle' },

  residentCell: { display: 'flex', alignItems: 'center', gap: '12px' },
  avatar: {
    width: '36px', height: '36px', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: 'white', fontSize: '12px', fontWeight: '700', flexShrink: 0, overflow: 'hidden',
  },
  avatarImg:       { width: '100%', height: '100%', objectFit: 'cover' },
  residentName:    { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },
  residentBarangay:{ fontSize: '12px', color: '#aaa', marginTop: '1px' },

  userBadge: {
    fontSize: '11px', fontWeight: '700', padding: '3px 10px',
    borderRadius: '20px', background: '#FFF3E0', color: '#C8601A', letterSpacing: '0.3px',
  },
  adminBadge: {
    fontSize: '11px', fontWeight: '700', padding: '3px 10px',
    borderRadius: '20px', background: '#1a1a1a', color: 'white', letterSpacing: '0.3px',
  },
  activeBadge: {
    fontSize: '11px', fontWeight: '600', padding: '3px 10px',
    borderRadius: '20px', background: '#E8F5E9', color: '#2E7D32',
  },
  inactiveBadge: {
    fontSize: '11px', fontWeight: '600', padding: '3px 10px',
    borderRadius: '20px', background: '#FFEBEE', color: '#c62828',
  },

  repCell:  { display: 'flex', alignItems: 'center', gap: '5px' },
  repValue: { fontSize: '13px', fontWeight: '600', color: '#333' },
  dash:     { color: '#ccc', fontSize: '14px' },

  removeBtn: {
    padding: '6px 14px', borderRadius: '8px', border: '1.5px solid #FFCDD2',
    background: '#FFF5F5', color: '#c62828', fontSize: '12px',
    fontWeight: '600', cursor: 'pointer',
  },
  reactivateBtn: {
    padding: '6px 14px', borderRadius: '8px', border: '1.5px solid #C8E6C9',
    background: '#F1F8E9', color: '#2E7D32', fontSize: '12px',
    fontWeight: '600', cursor: 'pointer',
  },

  pagination: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    gap: '6px', marginTop: '24px', paddingTop: '20px', borderTop: '1px solid #f5f5f5',
  },
  pageBtn: {
    minWidth: '36px', height: '36px', padding: '0 12px', borderRadius: '10px',
    border: '1.5px solid #e8e8e8', background: 'white', fontSize: '13px',
    fontWeight: '500', color: '#333', cursor: 'pointer',
  },
  pageBtnActive:   { background: '#C8601A', color: 'white', border: '1.5px solid #C8601A', fontWeight: '700' },
  pageBtnDisabled: { color: '#ccc', cursor: 'not-allowed', background: '#fafafa' },

  loadingBox: { display: 'flex', justifyContent: 'center', padding: '48px 0' },
  spinner: {
    width: '24px', height: '24px', border: '3px solid #f0ece6',
    borderTop: '3px solid #C8601A', borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
  },
  emptyBox: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '48px 0' },

  // Modal
  modalOverlay: {
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 500,
  },
  modal: {
    background: 'white', borderRadius: '20px', padding: '32px 28px',
    width: '420px', maxWidth: '90vw', boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
    display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center',
  },
  modalIcon: {
    width: '56px', height: '56px', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px',
  },
  modalTitle: { fontSize: '18px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 10px 0' },
  modalBody:  { fontSize: '14px', color: '#666', lineHeight: '1.6', margin: '0 0 24px 0' },
  modalActions: { display: 'flex', gap: '12px', width: '100%' },
  modalCancelBtn: {
    flex: 1, padding: '12px', borderRadius: '12px', border: '1.5px solid #e8e8e8',
    background: 'white', fontSize: '14px', fontWeight: '600', color: '#555', cursor: 'pointer',
  },
  modalConfirmBtn: {
    flex: 1, padding: '12px', borderRadius: '12px', border: 'none',
    color: 'white', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
  },

  // Toast
  toast: {
    position: 'fixed', bottom: '28px', left: '50%', transform: 'translateX(-50%)',
    color: 'white', padding: '12px 24px', borderRadius: '12px',
    fontSize: '14px', fontWeight: '500', zIndex: 600,
    boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
  },
};

export default AdminResidentsPage;