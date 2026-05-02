import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ApiClient from '../api/ApiClient';
import Navbar from '../components/Navbar';

const api = ApiClient.getInstance();
// ── Utilities ──────────────────────────────────────────────────────────────────
function timeAgo(dateStr) {
  if (!dateStr) return '';
  const diff = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} hr ago`;
  return `${Math.floor(diff / 86400)} d ago`;
}

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

// ── Constants ──────────────────────────────────────────────────────────────────
const CATEGORIES = ['All', 'Errand', 'Pet Care', 'Tool Borrowing', 'Plant Watering', 'Other'];
const PAGE_SIZE = 5;

const CATEGORY_TAG_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

const STATUS_COLORS = {
  OPEN:      { bg: '#FFF3E0', text: '#E65100' },
  CLAIMED:   { bg: '#FFF8E1', text: '#F57F17' },
  COMPLETED: { bg: '#E8F5E9', text: '#2E7D32' },
};

const CATEGORY_ICONS = {
  Errand: '🛒',
  'Pet Care': '🐾',
  'Tool Borrowing': '🔧',
  'Plant Watering': '🌿',
  Other: '📦',
};

// ── CSS keyframes injection ────────────────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN PAGE
// ══════════════════════════════════════════════════════════════════════════════
const FavorFeedPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // ── Filters & state ──────────────────────────────────────────────────────
  const [favors, setFavors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [activeCategory, setActiveCategory] = useState('All');
  const [statusFilter, setStatusFilter] = useState('');   // '' = all statuses
  const [sortBy, setSortBy] = useState('newest');         // newest | oldest | category

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Sidebar counts (loaded once)
  const [statusCounts, setStatusCounts] = useState({ OPEN: 0, CLAIMED: 0, COMPLETED: 0 });
  const [categoryCounts, setCategoryCounts] = useState({});

  // ── Fetch favors ──────────────────────────────────────────────────────────
  const fetchFavors = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (activeCategory !== 'All') params.category = activeCategory;
      if (statusFilter) params.status = statusFilter;
      if (search) params.search = search;

      // Sort mapping
      if (sortBy === 'oldest') params.sort = 'createdAt,asc';
      else if (sortBy === 'category') params.sort = 'category,asc';
      else params.sort = 'createdAt,desc';

      const res = await api.get('/favors', { params });
      const data = res.data?.data;
      const list = data?.content || data || [];
      setFavors(list);
      setTotalPages(data?.totalPages ?? 1);
    } catch (err) {
      setError('Could not load favors. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [activeCategory, statusFilter, search, sortBy]);

  useEffect(() => {
    setCurrentPage(0);
    fetchFavors(0);
  }, [fetchFavors]);

  // ── Fetch sidebar counts ──────────────────────────────────────────────────
  useEffect(() => {
    // Status counts
    Promise.all([
      api.get('/favors', { params: { status: 'OPEN',      page: 0, size: 1 } }),
      api.get('/favors', { params: { status: 'CLAIMED',   page: 0, size: 1 } }),
      api.get('/favors', { params: { status: 'COMPLETED', page: 0, size: 1 } }),
    ]).then(([open, claimed, completed]) => {
      setStatusCounts({
        OPEN:      open.data?.data?.totalElements      ?? open.data?.data?.length      ?? 0,
        CLAIMED:   claimed.data?.data?.totalElements   ?? claimed.data?.data?.length   ?? 0,
        COMPLETED: completed.data?.data?.totalElements ?? completed.data?.data?.length ?? 0,
      });
    }).catch(() => {});

    // Category counts
    const cats = ['Errand', 'Pet Care', 'Tool Borrowing', 'Plant Watering', 'Other'];
    Promise.all(
      cats.map(cat => api.get('/favors', { params: { category: cat, page: 0, size: 1 } }))
    ).then(results => {
      const counts = {};
      cats.forEach((cat, i) => {
        counts[cat] = results[i].data?.data?.totalElements ?? results[i].data?.data?.length ?? 0;
      });
      setCategoryCounts(counts);
    }).catch(() => {});
  }, []);

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setSearch(searchInput);
  };

  const handleClaim = async (favorId) => {
    const favor = favors.find(f => f.id === favorId);
    if (favor && favor.requesterId === user?.id) return;
    try {
      await api.post(`/favors/${favorId}/claim`);
      setFavors(prev => prev.filter(f => f.id !== favorId));
      setStatusCounts(prev => ({ ...prev, OPEN: Math.max(0, prev.OPEN - 1), CLAIMED: prev.CLAIMED + 1 }));
    } catch (err) {
      alert(err.response?.data?.error?.message || 'Could not claim this favor.');
    }
  };

  const handlePageChange = (page) => {
    if (page < 0 || page >= totalPages) return;
    setCurrentPage(page);
    fetchFavors(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* ── LEFT / MAIN COLUMN ─────────────────────────────────────── */}
        <div style={s.mainCol}>
          {/* Heading */}
          <h1 style={s.pageTitle}>Favor Feed</h1>
          <p style={s.pageSubtitle}>
            Browse open favor requests in {user?.barangay || 'your barangay'}.
          </p>

          {/* Search bar */}
          <form onSubmit={handleSearchSubmit} style={s.searchForm}>
            <div style={s.searchWrapper}>
              <svg style={s.searchIcon} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                type="text"
                placeholder="Search favors..."
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                style={s.searchInput}
              />
              <span style={s.searchHint}>⌘K</span>
            </div>
          </form>

          {/* Category tabs */}
          <div style={s.categoryRow}>
            {CATEGORIES.map(cat => (
              <button
                key={cat}
                onClick={() => { setActiveCategory(cat); setCurrentPage(0); }}
                style={cat === activeCategory ? s.catBtnActive : s.catBtn}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Favor list */}
          {loading ? (
            <div style={s.loadingBox}>
              <div style={s.spinner} />
              <span style={s.loadingText}>Loading favors…</span>
            </div>
          ) : error ? (
            <div style={s.errorBox}>{error}</div>
          ) : favors.length === 0 ? (
            <div style={s.emptyBox}>
              <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#ddd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><path d="M8 15h8M9 9h.01M15 9h.01"/>
              </svg>
              <p style={{ color: '#aaa', fontSize: '14px', margin: '10px 0 0 0' }}>
                No favors found. Try a different filter or search.
              </p>
            </div>
          ) : (
            favors.map(favor => (
              <FeedFavorCard key={favor.id} favor={favor} onClaim={handleClaim} currentUserId={user?.id} />
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

        {/* ── RIGHT SIDEBAR ──────────────────────────────────────────── */}
        <div style={s.sidebar}>
          {/* Post CTA */}
          <div style={s.ctaCard}>
            <p style={s.ctaTitle}>Got a small task you need help with?</p>
            <p style={s.ctaSubtitle}>Post it and let your neighbors lend a hand.</p>
            <button style={s.ctaBtn} onClick={() => navigate('/favors/new')}>
              + Post a Favor
            </button>
          </div>

          {/* Filter by Status */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>Filter by Status</h3>
            <div style={s.statusList}>
              {[
                { key: '',          label: 'All',       dot: '#aaa' },
                { key: 'OPEN',      label: 'Open',      dot: '#E65100' },
                { key: 'CLAIMED',   label: 'Claimed',   dot: '#F57F17' },
                { key: 'COMPLETED', label: 'Completed', dot: '#2E7D32' },
              ].map(({ key, label, dot }) => (
                <div
                  key={key}
                  style={{
                    ...s.statusItem,
                    background: statusFilter === key ? '#FFF3E0' : 'transparent',
                  }}
                  onClick={() => { setStatusFilter(key); setCurrentPage(0); }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: dot }} />
                    <span style={s.statusLabel}>{label}</span>
                  </div>
                  <span style={s.statusCount}>
                    {key === ''        ? (statusCounts.OPEN + statusCounts.CLAIMED + statusCounts.COMPLETED)
                     : key === 'OPEN'      ? statusCounts.OPEN
                     : key === 'CLAIMED'   ? statusCounts.CLAIMED
                     : statusCounts.COMPLETED}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Filter by Category */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>Filter by Category</h3>
            <div style={s.categoryList}>
              {['Errand', 'Pet Care', 'Tool Borrowing', 'Plant Watering', 'Other'].map(cat => (
                <div
                  key={cat}
                  style={{
                    ...s.categoryItem,
                    background: activeCategory === cat ? '#FFF3E0' : 'transparent',
                  }}
                  onClick={() => { setActiveCategory(cat); setCurrentPage(0); }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '16px' }}>{CATEGORY_ICONS[cat]}</span>
                    <span style={s.categoryItemLabel}>{cat}</span>
                  </div>
                  <span style={s.categoryItemCount}>{categoryCounts[cat] ?? 0}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Sort By */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>Sort By</h3>
            <div style={s.sortList}>
              {[
                { key: 'newest',   label: 'Newest First' },
                { key: 'oldest',   label: 'Oldest First' },
                { key: 'category', label: 'Category A–Z' },
              ].map(({ key, label }) => (
                <label key={key} style={s.sortItem}>
                  <div style={s.radioOuter} onClick={() => setSortBy(key)}>
                    {sortBy === key && <div style={s.radioInner} />}
                  </div>
                  <span
                    style={{ ...s.sortLabel, fontWeight: sortBy === key ? '600' : '400' }}
                    onClick={() => setSortBy(key)}
                  >
                    {label}
                  </span>
                </label>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
// FAVOR CARD (Feed version — shows status badge, Claim Favor button)
// ══════════════════════════════════════════════════════════════════════════════
const FeedFavorCard = ({ favor, onClaim, currentUserId }) => {
  const [hovered, setHovered] = useState(false);
  const category = favor.category || 'Other';
  const status = favor.status || 'OPEN';
  const tagColor = CATEGORY_TAG_COLORS[category] || CATEGORY_TAG_COLORS.Other;
  const statusColor = STATUS_COLORS[status] || STATUS_COLORS.OPEN;
  const isOwn = favor.requesterId === currentUserId;

  return (
    <div
      style={{ ...s.favorCard, ...(hovered ? s.favorCardHover : {}) }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* Category icon */}
      <div style={s.favorIconBox}>
        <span style={{ fontSize: '18px' }}>{CATEGORY_ICONS[category] || '📦'}</span>
      </div>

      {/* Main content */}
      <div style={s.favorContent}>
        {/* Top row: title + status badge */}
        <div style={s.favorTopRow}>
          <h3 style={s.favorTitle}>{favor.title}</h3>
          <span style={{ ...s.statusBadge, background: statusColor.bg, color: statusColor.text }}>
            {status}
          </span>
        </div>

        <p style={s.favorDescription}>{favor.description}</p>

        {/* Meta row */}
        <div style={s.favorMeta}>
          <div style={{ ...s.miniAvatar, background: avatarColor(favor.requesterName || '') }}>
            {initials(favor.requesterName || '?')}
          </div>
          <span style={s.metaText}>{favor.requesterName}</span>
          <span style={s.metaDot}>·</span>
          <span style={s.metaText}>{favor.barangay}</span>
          <span style={s.metaDot}>·</span>
          <span style={s.metaText}>{timeAgo(favor.createdAt)}</span>
        </div>
      </div>

      {/* Right: category tag + claim button */}
      <div style={s.favorActions}>
        <span style={{ ...s.categoryTag, background: tagColor.bg, color: tagColor.text }}>
          {category}
        </span>
        {status === 'OPEN' && !isOwn && (
          <button style={s.claimBtn} onClick={() => onClaim(favor.id)}>
            Claim Favor
          </button>
        )}
        {status === 'OPEN' && isOwn && (
          <button style={s.ownFavorBtn} disabled>Your Favor</button>
        )}
        {status !== 'OPEN' && (
          <button style={s.claimedBtn} disabled>{status === 'CLAIMED' ? 'Claimed' : 'Completed'}</button>
        )}
      </div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
// STYLES
// ══════════════════════════════════════════════════════════════════════════════
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
  mainCol: {
    flex: 1,
    minWidth: 0,
  },
  sidebar: {
    width: '300px',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },

  // Heading
  pageTitle: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 6px 0',
  },
  pageSubtitle: {
    fontSize: '14px',
    color: '#888',
    margin: '0 0 24px 0',
  },

  // Search
  searchForm: { marginBottom: '16px' },
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
  searchHint: {
    fontSize: '12px',
    color: '#bbb',
    background: '#f5f5f5',
    padding: '2px 7px',
    borderRadius: '6px',
  },

  // Category tabs
  categoryRow: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap',
    marginBottom: '20px',
  },
  catBtn: {
    padding: '6px 14px',
    borderRadius: '20px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    fontSize: '13px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
  },
  catBtnActive: {
    padding: '6px 14px',
    borderRadius: '20px',
    border: '1.5px solid #C8601A',
    background: '#C8601A',
    fontSize: '13px',
    color: 'white',
    cursor: 'pointer',
    fontWeight: '600',
  },

  // Favor card
  favorCard: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '14px',
    padding: '20px 18px',
    borderRadius: '16px',
    border: '1.5px solid #f0ece6',
    marginBottom: '12px',
    background: 'white',
    transition: 'box-shadow 0.18s, transform 0.18s',
  },
  favorCardHover: {
    boxShadow: '0 4px 20px rgba(200,96,26,0.10)',
    transform: 'translateY(-1px)',
  },
  favorIconBox: {
    width: '44px',
    height: '44px',
    borderRadius: '12px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  favorContent: { flex: 1, minWidth: 0 },
  favorTopRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '8px',
    marginBottom: '4px',
  },
  favorTitle: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#1a1a1a',
    margin: 0,
    flex: 1,
  },
  statusBadge: {
    fontSize: '11px',
    fontWeight: '700',
    padding: '3px 9px',
    borderRadius: '20px',
    flexShrink: 0,
    letterSpacing: '0.3px',
  },
  favorDescription: {
    fontSize: '13px',
    color: '#666',
    margin: '4px 0 10px 0',
    lineHeight: '1.5',
  },
  favorMeta: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    flexWrap: 'wrap',
  },
  miniAvatar: {
    width: '22px',
    height: '22px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '9px',
    fontWeight: '700',
    flexShrink: 0,
  },
  metaText: { fontSize: '12px', color: '#888' },
  metaDot:  { fontSize: '12px', color: '#ccc' },

  // Right side of card
  favorActions: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    gap: '8px',
    flexShrink: 0,
  },
  categoryTag: {
    fontSize: '11px',
    fontWeight: '600',
    padding: '3px 10px',
    borderRadius: '20px',
  },
  claimBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: 'none',
    background: '#C8601A',
    color: 'white',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  ownFavorBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    color: '#aaa',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'not-allowed',
  },
  claimedBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: 'none',
    background: '#f0f0f0',
    color: '#888',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'not-allowed',
  },

  // Pagination
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
  pageBtnDisabled: {
    color: '#ccc',
    borderColor: '#f0f0f0',
    cursor: 'not-allowed',
  },

  // Loading / Empty / Error
  loadingBox: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '10px',
    padding: '48px 0',
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
    borderRadius: '10px',
    color: '#e53935',
    fontSize: '13px',
  },
  emptyBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '48px 0',
  },

  // Sidebar cards
  ctaCard: {
    background: '#C8601A',
    borderRadius: '16px',
    padding: '20px',
  },
  ctaTitle: {
    fontSize: '15px',
    fontWeight: '700',
    color: 'white',
    margin: '0 0 4px 0',
  },
  ctaSubtitle: {
    fontSize: '13px',
    color: 'rgba(255,255,255,0.85)',
    margin: '0 0 16px 0',
  },
  ctaBtn: {
    width: '100%',
    padding: '11px',
    borderRadius: '10px',
    border: 'none',
    background: 'white',
    color: '#C8601A',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
  },
  sideCard: {
    background: 'white',
    borderRadius: '16px',
    padding: '18px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)',
  },
  sideCardTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 14px 0',
  },

  // Status filter
  statusList: { display: 'flex', flexDirection: 'column', gap: '4px' },
  statusItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '8px 10px',
    borderRadius: '10px',
    cursor: 'pointer',
    transition: 'background 0.15s',
  },
  statusLabel: { fontSize: '13px', color: '#333', fontWeight: '500' },
  statusCount: {
    fontSize: '12px',
    fontWeight: '700',
    color: '#C8601A',
    background: '#FFF3E0',
    padding: '2px 8px',
    borderRadius: '20px',
  },

  // Category filter
  categoryList: { display: 'flex', flexDirection: 'column', gap: '4px' },
  categoryItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '8px 10px',
    borderRadius: '10px',
    cursor: 'pointer',
    transition: 'background 0.15s',
  },
  categoryItemLabel: { fontSize: '13px', color: '#333', fontWeight: '500' },
  categoryItemCount: { fontSize: '12px', color: '#aaa', fontWeight: '600' },

  // Sort
  sortList: { display: 'flex', flexDirection: 'column', gap: '8px' },
  sortItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    cursor: 'pointer',
  },
  radioOuter: {
    width: '18px',
    height: '18px',
    borderRadius: '50%',
    border: '2px solid #C8601A',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    cursor: 'pointer',
  },
  radioInner: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: '#C8601A',
  },
  sortLabel: { fontSize: '13px', color: '#333' },
};

export default FavorFeedPage;