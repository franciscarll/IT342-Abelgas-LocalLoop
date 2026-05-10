import React, { useState, useEffect, useCallback } from 'react';
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
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

// ── Spinner keyframe ──────────────────────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('ll-fav-spin')) {
  const style = document.createElement('style');
  style.id = 'll-fav-spin';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ── Constants ─────────────────────────────────────────────────────────────────
const STATUS_OPTIONS = [
  { value: '',          label: 'All Statuses' },
  { value: 'OPEN',      label: 'Open' },
  { value: 'CLAIMED',   label: 'Claimed' },
  { value: 'COMPLETED', label: 'Completed' },
];

const CATEGORY_OPTIONS = [
  { value: '',               label: 'All Categories' },
  { value: 'Errand',         label: 'Errand' },
  { value: 'Pet Care',       label: 'Pet Care' },
  { value: 'Tool Borrowing', label: 'Tool Borrowing' },
  { value: 'Plant Watering', label: 'Plant Watering' },
  { value: 'Other',          label: 'Other' },
];

const SORT_OPTIONS = [
  { value: '',       label: 'Sort by Date' },
  { value: 'newest', label: 'Newest First' },
  { value: 'oldest', label: 'Oldest First' },
];

// ── Color maps ────────────────────────────────────────────────────────────────
const STATUS_COLORS = {
  OPEN:      { bg: '#FFF3E0', text: '#E65100' },
  CLAIMED:   { bg: '#FFF8E1', text: '#F9A825' },
  COMPLETED: { bg: '#E8F5E9', text: '#2E7D32' },
};

const CATEGORY_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

// ── Stat Card ─────────────────────────────────────────────────────────────────
const StatCard = ({ icon, iconBg, value, label, loading }) => (
  <div style={s.statCard}>
    <div style={{ ...s.statIcon, background: iconBg || '#FFF3E0' }}>{icon}</div>
    <div>
      {loading
        ? <div style={s.statSkeleton} />
        : <div style={s.statValue}>{value ?? 0}</div>
      }
      <div style={s.statLabel}>{label}</div>
    </div>
  </div>
);

// ── Person cell (avatar + name) ───────────────────────────────────────────────
const PersonCell = ({ name }) => {
  if (!name) return <span style={s.dash}>—</span>;
  return (
    <div style={s.personCell}>
      <div style={{ ...s.avatar, background: avatarColor(name) }}>
        {initials(name)}
      </div>
      <span style={s.personName}>{name}</span>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
const AdminFavorOverviewPage = () => {
  const { user } = useAuth();

  // ── State ─────────────────────────────────────────────────────────────────
  const [favors, setFavors]               = useState([]);
  const [loading, setLoading]             = useState(true);
  const [stats, setStats]                 = useState(null);
  const [statsLoading, setStatsLoading]   = useState(true);

  // Filter / search state (applied values)
  const [search, setSearch]               = useState('');
  const [statusFilter, setStatusFilter]   = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sortFilter, setSortFilter]       = useState('');

  // Input buffer for search (only applied on Enter / button click)
  const [inputValue, setInputValue]       = useState('');

  // Pagination
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const PAGE_SIZE = 10;

  // ── Fetch stats ───────────────────────────────────────────────────────────
  useEffect(() => {
    api.get('/admin/stats')
      .then(res => setStats(res.data?.data || res.data))
      .catch(() => setStats(null))
      .finally(() => setStatsLoading(false));
  }, []);

  // ── Fetch favors ──────────────────────────────────────────────────────────
  const fetchFavors = useCallback((pageNum, searchVal, status, category, sort) => {
    setLoading(true);
    const params = { page: pageNum, size: PAGE_SIZE };
    if (searchVal)  params.search   = searchVal;
    if (status)     params.status   = status;
    if (category)   params.category = category;
    if (sort)       params.sort     = sort;

    api.get('/admin/favors', { params })
      .then(res => {
        const d = res.data?.data;
        setFavors(d?.content || []);
        setTotalPages(d?.totalPages ?? 1);
        setTotalElements(d?.totalElements ?? 0);
      })
      .catch(() => setFavors([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchFavors(page, search, statusFilter, categoryFilter, sortFilter);
  }, [page, search, statusFilter, categoryFilter, sortFilter, fetchFavors]);

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleSearch = () => {
    setPage(0);
    setSearch(inputValue.trim());
  };
  const handleKeyDown = (e) => { if (e.key === 'Enter') handleSearch(); };
  const handleClearSearch = () => { setInputValue(''); setSearch(''); setPage(0); };

  const handleStatusChange = (val) => { setStatusFilter(val); setPage(0); };
  const handleCategoryChange = (val) => { setCategoryFilter(val); setPage(0); };
  const handleSortChange = (val) => { setSortFilter(val); setPage(0); };

  // ── Pagination ────────────────────────────────────────────────────────────
  const goToPage = (p) => { if (p >= 0 && p < totalPages) setPage(p); };
  const getPageNumbers = () => {
    const pages = [];
    const start = Math.max(0, page - 2);
    const end   = Math.min(totalPages - 1, page + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* Header */}
        <div style={s.pageHeader}>
          <h1 style={s.pageTitle}>Favor Overview</h1>
          <p style={s.pageSubtitle}>
            All favor requests across {user?.barangay || 'your barangay'}.
          </p>
        </div>

        {/* Stat Cards */}
        <div style={s.statsRow}>
          <StatCard
            loading={statsLoading}
            value={stats?.totalFavors}
            label="Total Favors"
            iconBg="#FFF3E0"
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
            value={stats?.openFavors}
            label="Open"
            iconBg="#FFF3E0"
            icon={<div style={{ width: 16, height: 16, borderRadius: '50%', background: '#E65100' }} />}
          />
          <StatCard
            loading={statsLoading}
            value={stats?.claimedFavors}
            label="Claimed"
            iconBg="#FFF8E1"
            icon={<div style={{ width: 16, height: 16, borderRadius: '50%', background: '#F9A825' }} />}
          />
          <StatCard
            loading={statsLoading}
            value={stats?.completedFavors}
            label="Completed"
            iconBg="#E8F5E9"
            icon={<div style={{ width: 16, height: 16, borderRadius: '50%', background: '#4CAF50' }} />}
          />
        </div>

        {/* Table Card */}
        <div style={s.tableCard}>

          {/* Search + Filter row */}
          <div style={s.filterRow}>
            {/* Search */}
            <div style={s.searchBox}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                style={{ flexShrink: 0 }}>
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                style={s.searchInput}
                type="text"
                placeholder="Search favors by title or resident..."
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

            {/* Status dropdown */}
            <Dropdown
              value={statusFilter}
              options={STATUS_OPTIONS}
              onChange={handleStatusChange}
            />

            {/* Category dropdown */}
            <Dropdown
              value={categoryFilter}
              options={CATEGORY_OPTIONS}
              onChange={handleCategoryChange}
            />

            {/* Sort dropdown */}
            <Dropdown
              value={sortFilter}
              options={SORT_OPTIONS}
              onChange={handleSortChange}
            />
          </div>

          {/* Result count */}
          {!loading && (
            <div style={s.resultCount}>
              {totalElements} favor{totalElements !== 1 ? 's' : ''}
              {search && ` matching "${search}"`}
              {statusFilter && ` · ${statusFilter}`}
              {categoryFilter && ` · ${categoryFilter}`}
            </div>
          )}

          {/* Table */}
          {loading ? (
            <div style={s.loadingBox}><div style={s.spinner} /></div>
          ) : favors.length === 0 ? (
            <div style={s.emptyBox}>
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
                stroke="#ddd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                <rect x="9" y="3" width="6" height="4" rx="2"/>
              </svg>
              <p style={{ color: '#aaa', fontSize: '14px', margin: '10px 0 0 0' }}>
                No favors found.
              </p>
            </div>
          ) : (
            <table style={s.table}>
              <thead>
                <tr>
                  {['Favor', 'Requester', 'Helper', 'Category', 'Status', 'Date'].map(h => (
                    <th key={h} style={s.th}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {favors.map((favor, idx) => {
                  const statusColor   = STATUS_COLORS[favor.status]   || STATUS_COLORS.OPEN;
                  const categoryColor = CATEGORY_COLORS[favor.category] || CATEGORY_COLORS.Other;
                  return (
                    <tr
                      key={favor.id || idx}
                      style={s.tr}
                      onMouseEnter={e => e.currentTarget.style.background = '#FAF7F2'}
                      onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                    >
                      {/* Favor title */}
                      <td style={{ ...s.td, maxWidth: '220px' }}>
                        <span style={s.favorTitle}>{favor.title}</span>
                      </td>

                      {/* Requester */}
                      <td style={s.td}>
                        <PersonCell name={favor.requesterName} />
                      </td>

                      {/* Helper */}
                      <td style={s.td}>
                        <PersonCell name={favor.claimerName} />
                      </td>

                      {/* Category */}
                      <td style={s.td}>
                        <span style={{
                          ...s.tag,
                          background: categoryColor.bg,
                          color: categoryColor.text,
                        }}>
                          {favor.category}
                        </span>
                      </td>

                      {/* Status */}
                      <td style={s.td}>
                        <span style={{
                          ...s.statusBadge,
                          background: statusColor.bg,
                          color: statusColor.text,
                        }}>
                          {favor.status}
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

// ── Reusable Dropdown ─────────────────────────────────────────────────────────
const Dropdown = ({ value, options, onChange }) => (
  <div style={s.selectWrapper}>
    <select
      style={s.select}
      value={value}
      onChange={e => onChange(e.target.value)}
    >
      {options.map(opt => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
      stroke="#888" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      style={s.selectChevron}>
      <polyline points="6 9 12 15 18 9"/>
    </svg>
  </div>
);

// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
  page:    { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif" },
  content: { maxWidth: '1200px', margin: '0 auto', padding: '36px 32px' },

  pageHeader:   { marginBottom: '28px' },
  pageTitle:    { fontSize: '26px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  pageSubtitle: { fontSize: '14px', color: '#888', margin: 0 },

  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '14px',
    marginBottom: '24px',
  },
  statCard: {
    background: 'white', borderRadius: '16px', padding: '20px 18px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)',
    display: 'flex', alignItems: 'center', gap: '14px',
  },
  statIcon: {
    width: '44px', height: '44px', borderRadius: '12px',
    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
  },
  statValue:   { fontSize: '26px', fontWeight: '700', color: '#1a1a1a', lineHeight: 1, marginBottom: '4px' },
  statLabel:   { fontSize: '12px', color: '#888' },
  statSkeleton:{ width: '56px', height: '26px', background: '#f0ece6', borderRadius: '6px', marginBottom: '4px' },

  tableCard: {
    background: 'white', borderRadius: '20px', padding: '24px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },

  filterRow: {
    display: 'flex', gap: '10px', marginBottom: '8px', alignItems: 'center',
  },
  searchBox: {
    flex: 1, display: 'flex', alignItems: 'center', gap: '10px',
    padding: '0 14px', border: '1.5px solid #e8e8e8', borderRadius: '12px',
    background: 'white', height: '42px',
  },
  searchInput: {
    flex: 1, border: 'none', outline: 'none',
    fontSize: '14px', color: '#333', background: 'transparent',
  },
  clearBtn: {
    background: 'none', border: 'none', cursor: 'pointer',
    padding: '2px', display: 'flex', alignItems: 'center',
  },
  selectWrapper: { position: 'relative', display: 'flex', alignItems: 'center' },
  select: {
    appearance: 'none', padding: '0 36px 0 14px', height: '42px',
    border: '1.5px solid #e8e8e8', borderRadius: '12px',
    fontSize: '13px', color: '#333', background: 'white',
    cursor: 'pointer', outline: 'none', fontFamily: "'Segoe UI', sans-serif",
    whiteSpace: 'nowrap',
  },
  selectChevron: { position: 'absolute', right: '12px', pointerEvents: 'none' },

  resultCount: { fontSize: '12px', color: '#aaa', marginBottom: '16px' },

  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#aaa',
    padding: '0 12px 12px 0', borderBottom: '1px solid #f0ece6',
    textTransform: 'uppercase', letterSpacing: '0.4px',
  },
  tr: { borderBottom: '1px solid #f7f5f2', transition: 'background 0.15s' },
  td: { padding: '14px 12px 14px 0', fontSize: '13px', color: '#333', verticalAlign: 'middle' },

  favorTitle: { fontWeight: '600', color: '#1a1a1a', fontSize: '14px' },

  personCell: { display: 'flex', alignItems: 'center', gap: '8px' },
  avatar: {
    width: '28px', height: '28px', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: 'white', fontSize: '10px', fontWeight: '700', flexShrink: 0,
  },
  personName: { fontSize: '13px', color: '#333', fontWeight: '500' },
  dash: { color: '#ccc', fontSize: '14px' },

  tag: {
    fontSize: '11px', fontWeight: '600', padding: '3px 9px',
    borderRadius: '20px', display: 'inline-block',
  },
  statusBadge: {
    fontSize: '11px', fontWeight: '700', padding: '3px 10px',
    borderRadius: '20px', display: 'inline-block', letterSpacing: '0.3px',
  },

  pagination: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    gap: '6px', marginTop: '24px', paddingTop: '20px', borderTop: '1px solid #f5f5f5',
  },
  pageBtn: {
    minWidth: '36px', height: '36px', padding: '0 12px', borderRadius: '10px',
    border: '1.5px solid #e8e8e8', background: 'white',
    fontSize: '13px', fontWeight: '500', color: '#333', cursor: 'pointer',
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
};

export default AdminFavorOverviewPage;