import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import ApiClient from '../api/ApiClient';
const api = ApiClient.getInstance();
import Navbar from '../components/Navbar';

// ─── Utility: time ago ────────────────────────────────────────────────────────
function timeAgo(dateStr) {
  const diff = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} hr ago`;
  return `${Math.floor(diff / 86400)} d ago`;
}
function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
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

const CATEGORY_ICONS = {
  Errand: (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
      <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 001.99-1.84L23 6H6"/>
    </svg>
  ),
  'Pet Care': (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10 5.172C10 3.782 8.423 2.679 6.5 3c-2.823.47-4.113 6.006-4 7 .08.703.75 1.5 1.5 2h7c.75-.5 1.42-1.297 1.5-2 .113-.994-1.177-6.53-4-7-.267-.044-.537-.054-.8-.028"/>
      <path d="M14.267 5.172c0-1.39 1.577-2.493 3.5-2.172 2.823.47 4.113 6.006 4 7-.08.703-.75 1.5-1.5 2h-7c-.75-.5-1.42-1.297-1.5-2-.113-.994 1.177-6.53 4-7 .267-.044.537-.054.8-.028"/>
      <path d="M8 14v4c0 1.1.9 2 2 2h4c1.1 0 2-.9 2-2v-4"/>
    </svg>
  ),
  'Tool Borrowing': (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/>
    </svg>
  ),
  'Plant Watering': (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22V12"/><path d="M12 12C12 12 7 10 5 6c4 0 7 2 7 6z"/>
      <path d="M12 12C12 12 17 10 19 6c-4 0-7 2-7 6z"/>
    </svg>
  ),
  Other: (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" y1="8" x2="12" y2="12"/>
      <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
  ),
};

function WeatherIcon({ condition = '' }) {
  const c = condition.toLowerCase();
  if (c.includes('sunny') || c.includes('clear')) {
    return (
      <svg width="72" height="72" viewBox="0 0 72 72">
        <circle cx="36" cy="36" r="16" fill="#FFC107"/>
        {[0,45,90,135,180,225,270,315].map((deg, i) => (
          <line key={i}
            x1={36 + 22 * Math.cos(deg * Math.PI / 180)}
            y1={36 + 22 * Math.sin(deg * Math.PI / 180)}
            x2={36 + 30 * Math.cos(deg * Math.PI / 180)}
            y2={36 + 30 * Math.sin(deg * Math.PI / 180)}
            stroke="#FFC107" strokeWidth="3" strokeLinecap="round"/>
        ))}
      </svg>
    );
  }
  if (c.includes('rain') || c.includes('drizzle')) {
    return (
      <svg width="72" height="72" viewBox="0 0 72 72">
        <ellipse cx="36" cy="28" rx="18" ry="12" fill="#90A4AE"/>
        <ellipse cx="22" cy="32" rx="10" ry="8" fill="#B0BEC5"/>
        <ellipse cx="50" cy="32" rx="10" ry="8" fill="#B0BEC5"/>
        {[24,34,44].map((x, i) => (
          <line key={i} x1={x} y1="46" x2={x - 4} y2="58" stroke="#42A5F5" strokeWidth="2.5" strokeLinecap="round"/>
        ))}
      </svg>
    );
  }
  return (
    <svg width="80" height="60" viewBox="0 0 80 60">
      <circle cx="34" cy="24" r="14" fill="#FFC107"/>
      {[0,60,120,180,240,300].map((deg, i) => (
        <line key={i}
          x1={34 + 18 * Math.cos(deg * Math.PI / 180)}
          y1={24 + 18 * Math.sin(deg * Math.PI / 180)}
          x2={34 + 24 * Math.cos(deg * Math.PI / 180)}
          y2={24 + 24 * Math.sin(deg * Math.PI / 180)}
          stroke="#FFC107" strokeWidth="2.5" strokeLinecap="round"/>
      ))}
      <ellipse cx="44" cy="40" rx="20" ry="12" fill="#90A4AE"/>
      <ellipse cx="28" cy="44" rx="14" ry="10" fill="#B0BEC5"/>
      <ellipse cx="58" cy="44" rx="12" ry="9" fill="#B0BEC5"/>
    </svg>
  );
}

const CATEGORY_TAG_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ══════════════════════════════════════════════════════════════════════════════
const DashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [activeCategory, setActiveCategory] = useState('All');
  const [favors, setFavors] = useState([]);
  const [favorsLoading, setFavorsLoading] = useState(true);
  const [favorsError, setFavorsError] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [weather, setWeather] = useState(null);
  const [weatherLoading, setWeatherLoading] = useState(true);
  const [announcements, setAnnouncements] = useState([]);
  const [announcementsLoading, setAnnouncementsLoading] = useState(true);
  const [reputation, setReputation] = useState(null);
  const [reputationLoading, setReputationLoading] = useState(true);

  const PAGE_SIZE = 5;
  const categories = ['All', 'Errand', 'Pet Care', 'Tool Borrowing', 'Plant Watering', 'Other'];

  useEffect(() => {
    setFavors([]); setPage(0); setHasMore(true);
    fetchFavors(0, true);
    // eslint-disable-next-line
  }, [activeCategory]);

  const fetchFavors = async (pageNum, reset = false) => {
    try {
      if (pageNum === 0) setFavorsLoading(true); else setLoadingMore(true);
      const params = { page: pageNum, size: PAGE_SIZE, status: 'OPEN' };
      if (activeCategory !== 'All') params.category = activeCategory;
      const res = await api.get('/favors', { params });
      const data = res.data?.data;
      const list = data?.content || data || [];
      const totalPages = data?.totalPages;
      if (reset) setFavors(list); else setFavors(prev => [...prev, ...list]);
      setHasMore(totalPages !== undefined ? pageNum + 1 < totalPages : list.length === PAGE_SIZE);
      setFavorsError('');
    } catch { setFavorsError('Could not load favors.'); }
    finally { setFavorsLoading(false); setLoadingMore(false); }
  };

  const handleClaim = async (favorId) => {
    try {
      await api.post(`/favors/${favorId}/claim`);
      setFavors(prev => prev.filter(f => f.id !== favorId));
    } catch (err) {
      alert(err.response?.data?.error?.message || 'Could not claim this favor.');
    }
  };

  useEffect(() => {
    api.get('/weather')
      .then(res => setWeather(res.data?.data || res.data))
      .catch(() => setWeather(null))
      .finally(() => setWeatherLoading(false));
  }, []);

  useEffect(() => {
    api.get('/announcements', { params: { page: 0, size: 3 } })
      .then(res => { const d = res.data?.data; setAnnouncements(d?.content || d || []); })
      .catch(() => setAnnouncements([]))
      .finally(() => setAnnouncementsLoading(false));
  }, []);

  useEffect(() => {
    api.get('/users/me/reputation')
      .then(res => setReputation(res.data?.data || res.data))
      .catch(() => setReputation({ reputationScore: user?.reputationScore ?? 0, favorsPosted: 0, favorsCompleted: 0 }))
      .finally(() => setReputationLoading(false));
  }, [user]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* LEFT */}
        <div style={s.leftCol}>
          <div style={s.greetingSection}>
            <h1 style={s.greetingText}>{getGreeting()}, {user?.name?.split(' ')[0] || 'there'}! 👋</h1>
            <p style={s.greetingSubtext}>Here's what's happening in {user?.barangay || 'your barangay'} today.</p>
          </div>

          <div style={s.favorsSection}>
            <div style={s.sectionHeader}>
              <h2 style={s.sectionTitle}>Open Favors Near You</h2>
              <span style={s.viewAllLink} onClick={() => navigate('/favor-feed')}>View all →</span>
            </div>
            <div style={s.categoryRow}>
              {categories.map(cat => (
                <button key={cat} onClick={() => setActiveCategory(cat)}
                  style={cat === activeCategory ? s.catBtnActive : s.catBtn}>{cat}</button>
              ))}
            </div>
            {favorsLoading ? (
              <div style={s.loadingBox}><div style={s.spinner} /><span style={s.loadingText}>Loading favors…</span></div>
            ) : favorsError ? (
              <div style={s.errorBox}>{favorsError}</div>
            ) : favors.length === 0 ? (
              <div style={s.emptyBox}>
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ccc" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 15h8M9 9h.01M15 9h.01"/></svg>
                <p style={{ color: '#aaa', fontSize: '14px', margin: '8px 0 0 0' }}>No open favors in this category yet.</p>
              </div>
            ) : (
              <>
                {favors.map(favor => <FavorCard key={favor.id} favor={favor} onClaim={handleClaim} />)}
                {hasMore && (
                  <button style={s.loadMoreBtn} onClick={() => { const n = page + 1; setPage(n); fetchFavors(n); }} disabled={loadingMore}>
                    {loadingMore ? 'Loading…' : 'Load more favors'}
                  </button>
                )}
              </>
            )}
          </div>
        </div>

        {/* RIGHT */}
        <div style={s.rightCol}>
          {/* Weather */}
          <div style={s.card}>
            <div style={s.weatherHeader}>
              <div style={s.weatherLocation}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
                <span style={s.weatherLocationText}>{user?.barangay || 'Your Barangay'}</span>
              </div>
              <div style={s.liveBadge}><div style={s.liveDot} />Live</div>
            </div>
            {weatherLoading ? (
              <div style={{ ...s.loadingBox, padding: '32px 0' }}><div style={s.spinner} /></div>
            ) : weather ? (
              <>
                <div style={s.weatherMain}>
                  <WeatherIcon condition={weather.condition || weather.description || ''} />
                  <div style={s.weatherTemp}>{Math.round(weather.temperature ?? weather.temp ?? 0)}°C</div>
                  <div style={s.weatherCondition}>{weather.condition || weather.description || 'Clear'}</div>
                </div>
                <div style={s.weatherStats}>
                  <div style={s.weatherStatItem}><span style={s.weatherStatIcon}>💧</span><span style={s.weatherStatValue}>{weather.humidity ?? '--'}%</span><span style={s.weatherStatLabel}>Humidity</span></div>
                  <div style={s.weatherStatDivider} />
                  <div style={s.weatherStatItem}><span style={s.weatherStatIcon}>💨</span><span style={s.weatherStatValue}>{weather.windSpeed ?? weather.wind_speed ?? '--'} km/h</span><span style={s.weatherStatLabel}>Wind</span></div>
                  <div style={s.weatherStatDivider} />
                  <div style={s.weatherStatItem}><span style={s.weatherStatIcon}>🌡️</span><span style={s.weatherStatValue}>{Math.round(weather.feelsLike ?? weather.feels_like ?? weather.temperature ?? 0)}°C</span><span style={s.weatherStatLabel}>Feels like</span></div>
                </div>
              </>
            ) : <div style={s.emptyBox}>Weather unavailable</div>}
          </div>

          {/* Announcements */}
          <div style={s.card}>
            <div style={s.sectionHeader}>
              <h3 style={s.cardTitle}>📢 Announcements</h3>
              <span style={s.viewAllLink} onClick={() => navigate('/announcements')}>View all</span>
            </div>
            {announcementsLoading ? (
              <div style={s.loadingBox}><div style={s.spinner} /></div>
            ) : announcements.length === 0 ? (
              <p style={{ color: '#aaa', fontSize: '13px', margin: '12px 0 0 0' }}>No announcements yet.</p>
            ) : (
              <div style={s.announcementList}>
                {announcements.map((ann, i) => (
                  <div key={ann.id || i} style={s.announcementItem}>
                    <div style={s.announcementBody}>
                      <div style={s.announcementTitle}>{ann.title}</div>
                      <div style={s.announcementMeta}>{formatDate(ann.createdAt || ann.date)} · Posted by {ann.postedBy || 'Admin'}</div>
                    </div>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ccc" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Reputation */}
          <div style={s.card}>
            <h3 style={s.cardTitle}>⭐ My Reputation</h3>
            <p style={s.reputationSubtext}>Keep helping your neighbors!</p>
            {reputationLoading ? (
              <div style={s.loadingBox}><div style={s.spinner} /></div>
            ) : (
              <div style={s.reputationGrid}>
                <div style={s.reputationStat}><span style={s.reputationValue}>{reputation?.reputationScore ?? user?.reputationScore ?? 0}</span><span style={s.reputationLabel}>Reputation</span></div>
                <div style={s.reputationStat}><span style={s.reputationValue}>{reputation?.favorsPosted ?? 0}</span><span style={s.reputationLabel}>Posted</span></div>
                <div style={s.reputationStat}><span style={s.reputationValue}>{reputation?.favorsCompleted ?? 0}</span><span style={s.reputationLabel}>Completed</span></div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* FAB */}
      <button style={s.fab} onClick={() => navigate('/favors/new')} title="Post a new favor">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>
    </div>
  );
};

// ── FavorCard sub-component ────────────────────────────────────────────────────
const FavorCard = ({ favor, onClaim }) => {
  const [hovered, setHovered] = useState(false);
  const category = favor.category || 'Other';
  const tagColor = CATEGORY_TAG_COLORS[category] || CATEGORY_TAG_COLORS.Other;
  return (
    <div style={{ ...s.favorCard, ...(hovered ? s.favorCardHover : {}) }}
      onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>
      <div style={s.favorIcon}>{CATEGORY_ICONS[category] || CATEGORY_ICONS.Other}</div>
      <div style={s.favorContent}>
        <div style={s.favorTopRow}>
          <h3 style={s.favorTitle}>{favor.title}</h3>
          <span style={{ ...s.categoryTag, background: tagColor.bg, color: tagColor.text }}>{category}</span>
        </div>
        <p style={s.favorDescription}>{favor.description}</p>
        <div style={s.favorMeta}>
          <div style={s.requesterAvatar}>
            <div style={{ ...s.miniAvatar, background: avatarColor(favor.requesterName || '') }}>{initials(favor.requesterName || '?')}</div>
            <span style={s.favorMetaText}>{favor.requesterName}</span>
          </div>
          <span style={s.favorDot}>·</span>
          <span style={s.favorMetaText}>{favor.barangay}</span>
          <span style={s.favorDot}>·</span>
          <span style={s.favorMetaText}>{timeAgo(favor.createdAt)}</span>
        </div>
      </div>
      <button style={s.claimBtn} onClick={() => onClaim(favor.id)}>Claim</button>
    </div>
  );
};

// ── Styles (identical to original DashboardPage styles) ───────────────────────
const s = {
  page: { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif", position: 'relative' },
  content: { display: 'flex', gap: '24px', maxWidth: '1200px', margin: '0 auto', padding: '36px 32px' },
  leftCol: { flex: 1, minWidth: 0 },
  rightCol: { width: '320px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '16px' },
  greetingSection: { marginBottom: '32px' },
  greetingText: { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 6px 0' },
  greetingSubtext: { fontSize: '14px', color: '#888', margin: 0 },
  favorsSection: { background: 'white', borderRadius: '20px', padding: '24px', boxShadow: '0 2px 16px rgba(0,0,0,0.05)' },
  sectionHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' },
  sectionTitle: { fontSize: '17px', fontWeight: '700', color: '#1a1a1a', margin: 0 },
  viewAllLink: { fontSize: '13px', color: '#C8601A', cursor: 'pointer', fontWeight: '500' },
  categoryRow: { display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' },
  catBtn: { padding: '6px 14px', borderRadius: '20px', border: '1.5px solid #e8e8e8', background: 'white', fontSize: '13px', color: '#555', cursor: 'pointer', fontWeight: '500' },
  catBtnActive: { padding: '6px 14px', borderRadius: '20px', border: '1.5px solid #C8601A', background: '#C8601A', fontSize: '13px', color: 'white', cursor: 'pointer', fontWeight: '600' },
  favorCard: { display: 'flex', alignItems: 'flex-start', gap: '14px', padding: '18px 16px', borderRadius: '14px', border: '1.5px solid #f0ece6', marginBottom: '12px', background: 'white', transition: 'box-shadow 0.18s, transform 0.18s' },
  favorCardHover: { boxShadow: '0 4px 20px rgba(200,96,26,0.10)', transform: 'translateY(-1px)' },
  favorIcon: { width: '44px', height: '44px', borderRadius: '12px', background: '#FFF3E0', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  favorContent: { flex: 1, minWidth: 0 },
  favorTopRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '4px' },
  favorTitle: { fontSize: '15px', fontWeight: '600', color: '#1a1a1a', margin: 0, flex: 1 },
  categoryTag: { fontSize: '11px', fontWeight: '600', padding: '3px 9px', borderRadius: '20px', flexShrink: 0 },
  favorDescription: { fontSize: '13px', color: '#666', margin: '4px 0 10px 0', lineHeight: '1.5' },
  favorMeta: { display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' },
  requesterAvatar: { display: 'flex', alignItems: 'center', gap: '5px' },
  miniAvatar: { width: '22px', height: '22px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '9px', fontWeight: '700', flexShrink: 0 },
  favorMetaText: { fontSize: '12px', color: '#888' },
  favorDot: { fontSize: '12px', color: '#ccc' },
  claimBtn: { padding: '8px 18px', borderRadius: '10px', border: 'none', background: '#C8601A', color: 'white', fontSize: '13px', fontWeight: '600', cursor: 'pointer', flexShrink: 0, alignSelf: 'center' },
  loadMoreBtn: { width: '100%', padding: '12px', background: 'none', border: 'none', color: '#C8601A', fontSize: '14px', fontWeight: '500', cursor: 'pointer', marginTop: '4px' },
  loadingBox: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '20px 0' },
  spinner: { width: '24px', height: '24px', border: '3px solid #f0ece6', borderTop: '3px solid #C8601A', borderRadius: '50%', animation: 'spin 0.7s linear infinite' },
  loadingText: { fontSize: '13px', color: '#aaa' },
  errorBox: { padding: '16px', background: '#fff5f5', border: '1px solid #ffcdd2', borderRadius: '10px', color: '#e53935', fontSize: '13px' },
  emptyBox: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '32px 0', color: '#aaa', fontSize: '14px' },
  card: { background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 2px 16px rgba(0,0,0,0.05)' },
  cardTitle: { fontSize: '15px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  weatherHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' },
  weatherLocation: { display: 'flex', alignItems: 'center', gap: '4px' },
  weatherLocationText: { fontSize: '13px', fontWeight: '600', color: '#333' },
  liveBadge: { display: 'flex', alignItems: 'center', gap: '5px', background: '#E8F5E9', color: '#2E7D32', fontSize: '11px', fontWeight: '600', padding: '3px 9px', borderRadius: '20px' },
  liveDot: { width: '6px', height: '6px', borderRadius: '50%', background: '#4CAF50' },
  weatherMain: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '8px 0 16px 0' },
  weatherTemp: { fontSize: '48px', fontWeight: '700', color: '#1a1a1a', lineHeight: '1', marginTop: '8px' },
  weatherCondition: { fontSize: '14px', color: '#888', marginTop: '4px' },
  weatherStats: { display: 'flex', justifyContent: 'space-around', padding: '14px 0 4px 0', borderTop: '1px solid #f5f5f5' },
  weatherStatItem: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px', flex: 1 },
  weatherStatDivider: { width: '1px', background: '#f0ece6', alignSelf: 'stretch' },
  weatherStatIcon: { fontSize: '14px' },
  weatherStatValue: { fontSize: '13px', fontWeight: '700', color: '#333' },
  weatherStatLabel: { fontSize: '11px', color: '#aaa' },
  announcementList: { display: 'flex', flexDirection: 'column', gap: '0', marginTop: '8px' },
  announcementItem: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid #f5f5f5', cursor: 'pointer', gap: '10px' },
  announcementBody: { flex: 1, minWidth: 0 },
  announcementTitle: { fontSize: '13px', fontWeight: '600', color: '#1a1a1a', marginBottom: '2px' },
  announcementMeta: { fontSize: '11px', color: '#aaa' },
  reputationSubtext: { fontSize: '12px', color: '#aaa', margin: '0 0 14px 0' },
  reputationGrid: { display: 'flex', gap: '10px' },
  reputationStat: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '14px 8px', background: '#FAF7F2', borderRadius: '12px', gap: '4px' },
  reputationValue: { fontSize: '24px', fontWeight: '700', color: '#C8601A' },
  reputationLabel: { fontSize: '11px', color: '#888' },
  fab: { position: 'fixed', bottom: '32px', right: '32px', width: '52px', height: '52px', borderRadius: '50%', background: '#C8601A', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 4px 20px rgba(200,96,26,0.40)', zIndex: 300 },
};

export default DashboardPage;