import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import Navbar from '../components/Navbar';

// ── Helpers ───────────────────────────────────────────────────────────────────
function formatDateBadge(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });
}

function formatDateLong(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric', month: 'long', day: 'numeric',
  }) + ' · ' + new Date(dateStr).toLocaleTimeString('en-US', {
    hour: 'numeric', minute: '2-digit', hour12: true,
  });
}

const MONTH_NAMES = [
  '', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

// Category icons matching the Figma sidebar
const CATEGORY_ICONS = {
  Event: (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  ),
  Health: (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
    </svg>
  ),
  Reminder: (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
      <path d="M13.73 21a2 2 0 01-3.46 0"/>
    </svg>
  ),
  General: (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" y1="8" x2="12" y2="12"/>
      <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
  ),
};

const CATEGORY_TAG_COLORS = {
  Event:    { bg: '#EEF2FF', text: '#3730A3' },
  Health:   { bg: '#FDF2F8', text: '#9D174D' },
  Reminder: { bg: '#F0FDF4', text: '#166534' },
  General:  { bg: '#F5F5F5', text: '#424242' },
};

// CSS keyframes
if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

const PAGE_SIZE = 4;

// ══════════════════════════════════════════════════════════════════════════════
const AnnouncementsPage = () => {
  const { user } = useAuth();

  // ── List state ─────────────────────────────────────────────────────────────
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState('');
  const [currentPage, setCurrentPage]     = useState(0);
  const [totalPages, setTotalPages]       = useState(1);

  // ── Filters ────────────────────────────────────────────────────────────────
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch]           = useState('');
  const [activeCategory, setActiveCategory] = useState('');
  const [activeYear, setActiveYear]   = useState(null);
  const [activeMonth, setActiveMonth] = useState(null);

  // ── Expanded "Read more" state ─────────────────────────────────────────────
  const [expandedId, setExpandedId]   = useState(null);

  // ── Sidebar state ──────────────────────────────────────────────────────────
  const [pinned, setPinned]                 = useState(null);
  const [monthCounts, setMonthCounts]       = useState([]);
  const [categoryCounts, setCategoryCounts] = useState([]);

  // ── Fetch main list ────────────────────────────────────────────────────────
  const fetchAnnouncements = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (search)        params.search   = search;
      if (activeCategory) params.category = activeCategory;
      if (activeYear && activeMonth) {
        params.year  = activeYear;
        params.month = activeMonth;
      }
      const res  = await api.get('/announcements', { params });
      const data = res.data?.data;
      const list = data?.content || data || [];
      setAnnouncements(list);
      setTotalPages(data?.totalPages ?? 1);
    } catch {
      setError('Could not load announcements. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [search, activeCategory, activeYear, activeMonth]);

  useEffect(() => {
    setCurrentPage(0);
    fetchAnnouncements(0);
  }, [fetchAnnouncements]);

  // ── Fetch sidebar data once ────────────────────────────────────────────────
  useEffect(() => {
    api.get('/announcements/pinned')
      .then(res => setPinned(res.data?.data || null))
      .catch(() => setPinned(null));

    api.get('/announcements/months')
      .then(res => setMonthCounts(res.data?.data || []))
      .catch(() => setMonthCounts([]));

    api.get('/announcements/categories')
      .then(res => setCategoryCounts(res.data?.data || []))
      .catch(() => setCategoryCounts([]));
  }, []);

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setSearch(searchInput.trim());
    setActiveCategory('');
    setActiveYear(null);
    setActiveMonth(null);
  };

  const handleCategoryClick = (cat) => {
    setActiveCategory(prev => prev === cat ? '' : cat);
    setSearch('');
    setSearchInput('');
    setActiveYear(null);
    setActiveMonth(null);
  };

  const handleMonthClick = (year, month) => {
    if (activeYear === year && activeMonth === month) {
      setActiveYear(null);
      setActiveMonth(null);
    } else {
      setActiveYear(year);
      setActiveMonth(month);
      setActiveCategory('');
      setSearch('');
      setSearchInput('');
    }
  };

  const handlePageChange = (page) => {
    if (page < 0 || page >= totalPages) return;
    setCurrentPage(page);
    fetchAnnouncements(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const clearFilters = () => {
    setSearch('');
    setSearchInput('');
    setActiveCategory('');
    setActiveYear(null);
    setActiveMonth(null);
  };

  const hasFilter = search || activeCategory || activeYear;

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div style={s.page}>
      <Navbar />
      <div style={s.content}>

        {/* ══ MAIN COLUMN ══════════════════════════════════════════════════ */}
        <div style={s.mainCol}>

          {/* Page heading */}
          <div style={s.pageHeading}>
            <div>
              <h1 style={s.pageTitle}>📢 Announcements</h1>
              <p style={s.pageSubtitle}>
                Official updates from {user?.barangay || 'your barangay'}.
              </p>
            </div>
            {/* Admin view toggle — only shown if admin */}
            {user?.role === 'ROLE_ADMIN' && (
              <a href="/admin/announcements" style={s.adminToggle}>
                Admin view
                <div style={s.adminToggleSwitch} />
              </a>
            )}
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearchSubmit} style={s.searchForm}>
            <div style={s.searchWrapper}>
              <svg style={s.searchIcon} width="16" height="16" viewBox="0 0 24 24"
                   fill="none" stroke="#aaa" strokeWidth="2"
                   strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                type="text"
                placeholder="Search announcements..."
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                style={s.searchInput}
              />
              {hasFilter && (
                <button type="button" style={s.clearBtn} onClick={clearFilters}>
                  Clear
                </button>
              )}
            </div>
          </form>

          {/* Active filter indicator */}
          {hasFilter && (
            <div style={s.filterIndicator}>
              Showing:{' '}
              {activeCategory && <strong>{activeCategory}</strong>}
              {activeYear && activeMonth && (
                <strong>{MONTH_NAMES[activeMonth]} {activeYear}</strong>
              )}
              {search && <strong>"{search}"</strong>}
              <span style={s.filterClear} onClick={clearFilters}> · Clear filter</span>
            </div>
          )}

          {/* Announcement cards */}
          {loading ? (
            <LoadingBox />
          ) : error ? (
            <div style={s.errorBox}>{error}</div>
          ) : announcements.length === 0 ? (
            <EmptyBox />
          ) : (
            announcements.map(ann => (
              <AnnouncementCard
                key={ann.id}
                ann={ann}
                expanded={expandedId === ann.id}
                onToggle={() => setExpandedId(prev => prev === ann.id ? null : ann.id)}
              />
            ))
          )}

          {/* Pagination */}
          {!loading && totalPages > 1 && (
            <div style={s.pagination}>
              <button
                style={{ ...s.pageBtn, ...(currentPage === 0 ? s.pageBtnDisabled : {}) }}
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                ← Previous
              </button>

              {Array.from({ length: totalPages }, (_, i) => (
                <button
                  key={i}
                  style={i === currentPage ? s.pageBtnActive : s.pageBtn}
                  onClick={() => handlePageChange(i)}
                >
                  {i + 1}
                </button>
              ))}

              <button
                style={{ ...s.pageBtn, ...(currentPage >= totalPages - 1 ? s.pageBtnDisabled : {}) }}
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Next →
              </button>
            </div>
          )}
        </div>

        {/* ══ RIGHT SIDEBAR ════════════════════════════════════════════════ */}
        <div style={s.sidebar}>

          {/* Pinned announcement */}
          {pinned && (
            <div style={s.pinnedCard}>
              <div style={s.pinnedLabel}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="white"
                     stroke="none">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
                Pinned
              </div>
              <h3 style={s.pinnedTitle}>{pinned.title}</h3>
              <p style={s.pinnedMeta}>
                {formatDateLong(pinned.createdAt)}
              </p>
              <button
                style={s.pinnedReadMore}
                onClick={() => setExpandedId(prev => prev === pinned.id ? null : pinned.id)}
              >
                Read full announcement →
              </button>
            </div>
          )}

          {/* Browse by Month */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>📅 Browse by Month</h3>
            {monthCounts.length === 0 ? (
              <p style={s.sideEmpty}>No data yet.</p>
            ) : (
              <div style={s.monthList}>
                {monthCounts.map(({ year, month, count }) => {
                  const isActive = activeYear === year && activeMonth === month;
                  return (
                    <div
                      key={`${year}-${month}`}
                      style={{ ...s.monthRow, background: isActive ? '#FFF3E0' : 'transparent' }}
                      onClick={() => handleMonthClick(year, month)}
                    >
                      <span style={s.monthLabel}>
                        {MONTH_NAMES[month]} {year}
                      </span>
                      <span style={s.monthCount}>{count} post{count !== 1 ? 's' : ''}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Browse by Category */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>Browse by Category</h3>
            {categoryCounts.length === 0 ? (
              <p style={s.sideEmpty}>No categories yet.</p>
            ) : (
              <div style={s.catList}>
                {categoryCounts.map(({ category, count }) => {
                  const isActive = activeCategory === category;
                  return (
                    <div
                      key={category}
                      style={{ ...s.catRow, background: isActive ? '#FFF3E0' : 'transparent' }}
                      onClick={() => handleCategoryClick(category)}
                    >
                      <div style={s.catRowLeft}>
                        <div style={s.catIcon}>
                          {CATEGORY_ICONS[category] || CATEGORY_ICONS.General}
                        </div>
                        <span style={s.catLabel}>{category}</span>
                      </div>
                      <span style={s.catCount}>{count}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

        </div>{/* end sidebar */}
      </div>
    </div>
  );
};

// ── Announcement Card ──────────────────────────────────────────────────────────
const AnnouncementCard = ({ ann, expanded, onToggle }) => {
  const tagColor = CATEGORY_TAG_COLORS[ann.category] || CATEGORY_TAG_COLORS.General;
  const PREVIEW_LENGTH = 160;
  const isLong = ann.content && ann.content.length > PREVIEW_LENGTH;
  const preview = isLong && !expanded
    ? ann.content.slice(0, PREVIEW_LENGTH) + '…'
    : ann.content;

  return (
    <div style={s.annCard}>
      {/* Top row: date badge + category tag */}
      <div style={s.annCardTop}>
        <span style={s.dateBadge}>{formatDateBadge(ann.createdAt)}</span>
        <span style={{ ...s.categoryTag, background: tagColor.bg, color: tagColor.text }}>
          {ann.category}
        </span>
      </div>

      {/* Title */}
      <h2 style={s.annTitle}>{ann.title}</h2>

      {/* Content */}
      <p style={s.annContent}>{preview}</p>

      {/* Footer: posted by + read more */}
      <div style={s.annFooter}>
        <div style={s.annPostedBy}>
          <div style={s.postedByAvatar}>A</div>
          <span style={s.postedByText}>Posted by {ann.postedBy || 'Admin'}</span>
        </div>
        {isLong && (
          <button style={s.readMoreBtn} onClick={onToggle}>
            {expanded ? 'Show less ←' : 'Read more →'}
          </button>
        )}
      </div>
    </div>
  );
};

// ── Loading / Empty ────────────────────────────────────────────────────────────
const LoadingBox = () => (
  <div style={s.loadingBox}>
    <div style={s.spinner} />
    <span style={s.loadingText}>Loading announcements…</span>
  </div>
);

const EmptyBox = () => (
  <div style={s.emptyBox}>
    <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#ddd"
         strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
      <path d="M13.73 21a2 2 0 01-3.46 0"/>
    </svg>
    <p style={s.emptyText}>No announcements found.</p>
  </div>
);

// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
  page: {
    minHeight: '100vh',
    background: '#FAF7F2',
    fontFamily: "'Segoe UI', sans-serif",
  },
  content: {
    display: 'flex',
    gap: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '40px 32px',
    alignItems: 'flex-start',
  },
  mainCol: { flex: 1, minWidth: 0 },
  sidebar: {
    width: '300px',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },

  // ── Heading ────────────────────────────────────────────────────────────────
  pageHeading: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '24px',
  },
  pageTitle: { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  pageSubtitle: { fontSize: '14px', color: '#888', margin: 0 },
  adminToggle: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '13px',
    color: '#555',
    textDecoration: 'none',
    background: 'white',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '8px 14px',
    fontWeight: '500',
    cursor: 'pointer',
    flexShrink: 0,
  },
  adminToggleSwitch: {
    width: '32px',
    height: '18px',
    borderRadius: '9px',
    background: '#e8e8e8',
    position: 'relative',
  },

  // ── Search ─────────────────────────────────────────────────────────────────
  searchForm: { marginBottom: '20px' },
  searchWrapper: {
    display: 'flex',
    alignItems: 'center',
    background: 'white',
    border: '1.5px solid #e8e8e8',
    borderRadius: '12px',
    padding: '0 14px',
    height: '46px',
    gap: '8px',
  },
  searchIcon: { flexShrink: 0 },
  searchInput: {
    flex: 1,
    border: 'none',
    outline: 'none',
    fontSize: '14px',
    color: '#333',
    background: 'transparent',
  },
  clearBtn: {
    padding: '4px 10px',
    borderRadius: '6px',
    border: '1px solid #e8e8e8',
    background: '#f5f5f5',
    fontSize: '12px',
    color: '#666',
    cursor: 'pointer',
    flexShrink: 0,
  },

  // Filter indicator
  filterIndicator: {
    fontSize: '13px',
    color: '#888',
    marginBottom: '16px',
    marginTop: '-8px',
  },
  filterClear: {
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
  },

  // ── Announcement card ──────────────────────────────────────────────────────
  annCard: {
    background: 'white',
    borderRadius: '16px',
    padding: '24px',
    marginBottom: '12px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.04)',
    borderLeft: '4px solid #C8601A',
  },
  annCardTop: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
  },
  dateBadge: {
    fontSize: '12px',
    fontWeight: '600',
    color: '#C8601A',
    background: '#FFF3E0',
    padding: '4px 10px',
    borderRadius: '20px',
  },
  categoryTag: {
    fontSize: '11px',
    fontWeight: '600',
    padding: '4px 10px',
    borderRadius: '20px',
  },
  annTitle: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 10px 0',
    lineHeight: '1.3',
  },
  annContent: {
    fontSize: '14px',
    color: '#555',
    lineHeight: '1.7',
    margin: '0 0 16px 0',
  },
  annFooter: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: '12px',
    borderTop: '1px solid #f5f5f5',
  },
  annPostedBy: { display: 'flex', alignItems: 'center', gap: '8px' },
  postedByAvatar: {
    width: '26px',
    height: '26px',
    borderRadius: '50%',
    background: '#C8601A',
    color: 'white',
    fontSize: '11px',
    fontWeight: '700',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  postedByText: { fontSize: '12px', color: '#888' },
  readMoreBtn: {
    background: 'none',
    border: 'none',
    fontSize: '13px',
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
    padding: 0,
  },

  // ── Pagination ─────────────────────────────────────────────────────────────
  pagination: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '6px',
    marginTop: '28px',
    paddingBottom: '8px',
  },
  pageBtn: {
    padding: '7px 13px',
    borderRadius: '8px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    fontSize: '13px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
  },
  pageBtnActive: {
    padding: '7px 13px',
    borderRadius: '8px',
    border: '1.5px solid #C8601A',
    background: '#C8601A',
    fontSize: '13px',
    color: 'white',
    cursor: 'pointer',
    fontWeight: '600',
  },
  pageBtnDisabled: { color: '#ccc', borderColor: '#f0f0f0', cursor: 'not-allowed' },

  // ── Loading / Empty ─────────────────────────────────────────────────────────
  loadingBox: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '10px',
    padding: '60px 0',
  },
  spinner: {
    width: '24px',
    height: '24px',
    border: '3px solid #f0ece6',
    borderTop: '3px solid #C8601A',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
  },
  loadingText: { fontSize: '13px', color: '#aaa' },
  errorBox: {
    padding: '16px',
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '12px',
    color: '#e53935',
    fontSize: '14px',
  },
  emptyBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '60px 0',
    gap: '12px',
  },
  emptyText: { fontSize: '14px', color: '#aaa', margin: 0 },

  // ── Sidebar ─────────────────────────────────────────────────────────────────
  pinnedCard: {
    background: '#C8601A',
    borderRadius: '16px',
    padding: '20px',
  },
  pinnedLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: '5px',
    fontSize: '11px',
    fontWeight: '700',
    color: 'rgba(255,255,255,0.85)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    marginBottom: '10px',
  },
  pinnedTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: 'white',
    margin: '0 0 6px 0',
    lineHeight: '1.4',
  },
  pinnedMeta: {
    fontSize: '12px',
    color: 'rgba(255,255,255,0.75)',
    margin: '0 0 14px 0',
  },
  pinnedReadMore: {
    background: 'none',
    border: 'none',
    fontSize: '13px',
    color: 'white',
    fontWeight: '600',
    cursor: 'pointer',
    padding: 0,
    textDecoration: 'underline',
    textUnderlineOffset: '3px',
  },

  sideCard: {
    background: 'white',
    borderRadius: '16px',
    padding: '20px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)',
  },
  sideCardTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 14px 0',
  },
  sideEmpty: { fontSize: '13px', color: '#aaa', margin: 0 },

  // Month list
  monthList: { display: 'flex', flexDirection: 'column', gap: '0' },
  monthRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 8px',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'background 0.15s',
  },
  monthLabel: { fontSize: '13px', color: '#333', fontWeight: '500' },
  monthCount: { fontSize: '12px', color: '#C8601A', fontWeight: '600' },

  // Category list
  catList: { display: 'flex', flexDirection: 'column', gap: '0' },
  catRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 8px',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'background 0.15s',
  },
  catRowLeft: { display: 'flex', alignItems: 'center', gap: '8px' },
  catIcon: {
    width: '24px',
    height: '24px',
    borderRadius: '6px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  catLabel: { fontSize: '13px', color: '#333', fontWeight: '500' },
  catCount: { fontSize: '13px', color: '#aaa', fontWeight: '600' },
};

export default AnnouncementsPage;