import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import Navbar from '../components/Navbar';

// ── Utilities ─────────────────────────────────────────────────────────────────
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
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}
function formatDateShort(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
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

// ══════════════════════════════════════════════════════════════════════════════
const MyActivityPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('posted');

  // Data
  const [postedFavors, setPostedFavors]       = useState([]);
  const [claimedFavors, setClaimedFavors]     = useState([]);
  const [completedFavors, setCompletedFavors] = useState([]);
  const [reputation, setReputation]           = useState(null);

  // Loading / error
  const [loadingPosted,    setLoadingPosted]    = useState(true);
  const [loadingClaimed,   setLoadingClaimed]   = useState(true);
  const [loadingReputation,setLoadingReputation]= useState(true);
  const [error,            setError]            = useState('');

  // Action state
  const [actionLoading, setActionLoading] = useState(null); // stores favor id being acted on
  const [deleteConfirm, setDeleteConfirm] = useState(null); // stores favor id pending delete

  // ── Fetch posted favors ───────────────────────────────────────────────────
  const fetchPosted = useCallback(async () => {
    setLoadingPosted(true);
    try {
      const res = await api.get('/favors/my-posted', { params: { page: 0, size: 50 } });
      const data = res.data?.data;
      const list = data?.content || data || [];
      setPostedFavors(list);
    } catch {
      setError('Could not load your posted favors.');
    } finally {
      setLoadingPosted(false);
    }
  }, []);

  // ── Fetch claimed favors (split into claimed + completed) ─────────────────
  const fetchClaimed = useCallback(async () => {
    setLoadingClaimed(true);
    try {
      const res = await api.get('/favors/my-claimed', { params: { page: 0, size: 50 } });
      const data = res.data?.data;
      const list = data?.content || data || [];
      setClaimedFavors(list.filter(f => f.status === 'CLAIMED'));
      setCompletedFavors(list.filter(f => f.status === 'COMPLETED'));
    } catch {
      setError('Could not load your claimed favors.');
    } finally {
      setLoadingClaimed(false);
    }
  }, []);

  // ── Fetch reputation stats ────────────────────────────────────────────────
  const fetchReputation = useCallback(async () => {
    setLoadingReputation(true);
    try {
      const res = await api.get('/users/me/reputation');
      setReputation(res.data?.data || res.data);
    } catch {
      setReputation({ reputationScore: user?.reputationScore ?? 0, favorsPosted: 0, favorsCompleted: 0 });
    } finally {
      setLoadingReputation(false);
    }
  }, [user]);

  useEffect(() => {
    fetchPosted();
    fetchClaimed();
    fetchReputation();
  }, [fetchPosted, fetchClaimed, fetchReputation]);

  // ── Delete a favor ────────────────────────────────────────────────────────
  const handleDelete = async (favorId) => {
    setActionLoading(favorId);
    try {
      await api.delete(`/favors/${favorId}`);
      setPostedFavors(prev => prev.filter(f => f.id !== favorId));
      setDeleteConfirm(null);
      fetchReputation();
    } catch (err) {
      alert(err.response?.data?.error?.message || 'Could not delete this favor.');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Confirm completion ────────────────────────────────────────────────────
  const handleComplete = async (favorId) => {
    setActionLoading(favorId);
    try {
      const res = await api.put(`/favors/${favorId}/complete`);
      const updated = res.data?.data || res.data;
      setPostedFavors(prev => prev.map(f => f.id === favorId ? updated : f));
      fetchReputation();
      fetchClaimed();
    } catch (err) {
      alert(err.response?.data?.error?.message || 'Could not confirm completion.');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Derived stats ─────────────────────────────────────────────────────────
  const postedCount    = postedFavors.length;
  const claimedCount   = claimedFavors.length;
  const completedCount = completedFavors.length;
  const openCount      = postedFavors.filter(f => f.status === 'OPEN').length;
  const postedClaimedCount   = postedFavors.filter(f => f.status === 'CLAIMED').length;
  const postedCompletedCount = postedFavors.filter(f => f.status === 'COMPLETED').length;
  const completionRate = postedCount > 0 ? Math.round((postedCompletedCount / postedCount) * 100) : 0;

  // ── Tab config ────────────────────────────────────────────────────────────
  const tabs = [
    { key: 'posted',    label: 'Posted Favors',    count: postedCount },
    { key: 'claimed',   label: 'Claimed Favors',   count: claimedCount },
    { key: 'completed', label: 'Completed Favors', count: completedCount },
  ];

  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* ── MAIN COLUMN ── */}
        <div style={s.mainCol}>

          {/* Page heading */}
          <h1 style={s.pageTitle}>My Activity</h1>
          <p style={s.pageSubtitle}>Track all your posted, claimed, and completed favors.</p>

          {/* Stats row */}
          <div style={s.statsRow}>
            <StatCard
              icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>}
              value={loadingReputation ? '—' : (reputation?.reputationScore ?? user?.reputationScore ?? 0)}
              label="Reputation Score"
            />
            <StatCard
              icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>}
              value={loadingPosted ? '—' : postedCount}
              label="Favors Posted"
            />
            <StatCard
              icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>}
              value={loadingClaimed ? '—' : claimedCount}
              label="Favors Claimed"
            />
            <StatCard
              icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>}
              value={loadingClaimed ? '—' : completedCount}
              label="Favors Completed"
            />
          </div>

          {/* Tabs */}
          <div style={s.tabRow}>
            {tabs.map(tab => (
              <button
                key={tab.key}
                style={activeTab === tab.key ? s.tabActive : s.tab}
                onClick={() => setActiveTab(tab.key)}
              >
                {tab.label}
                <span style={activeTab === tab.key ? s.tabBadgeActive : s.tabBadge}>
                  {tab.count}
                </span>
              </button>
            ))}
          </div>

          {/* Tab content */}
          {activeTab === 'posted' && (
            <div>
              <div style={s.tabContentHeader}>
                <h2 style={s.tabContentTitle}>Your Posted Favors</h2>
                <span style={s.postNewLink} onClick={() => navigate('/favors/new')}>
                  Post a new favor →
                </span>
              </div>

              {loadingPosted ? (
                <LoadingBox />
              ) : postedFavors.length === 0 ? (
                <EmptyBox message="You haven't posted any favors yet." />
              ) : (
                postedFavors.map(favor => (
                  <PostedFavorCard
                    key={favor.id}
                    favor={favor}
                    actionLoading={actionLoading}
                    deleteConfirm={deleteConfirm}
                    onView={() => navigate(`/favors/${favor.id}`)}
                    onEdit={() => navigate(`/favors/${favor.id}/edit`)}
                    onDeleteRequest={() => setDeleteConfirm(favor.id)}
                    onDeleteCancel={() => setDeleteConfirm(null)}
                    onDeleteConfirm={() => handleDelete(favor.id)}
                    onComplete={() => handleComplete(favor.id)}
                  />
                ))
              )}
            </div>
          )}

          {activeTab === 'claimed' && (
            <div>
              <h2 style={s.tabContentTitle}>Favors You're Helping With</h2>
              <p style={{ ...s.pageSubtitle, marginBottom: '20px' }}>
                These are favors you claimed from neighbors.
              </p>

              {loadingClaimed ? (
                <LoadingBox />
              ) : claimedFavors.length === 0 ? (
                <EmptyBox message="You haven't claimed any favors yet." />
              ) : (
                claimedFavors.map(favor => (
                  <ClaimedFavorCard
                    key={favor.id}
                    favor={favor}
                    onView={() => navigate(`/favors/${favor.id}`)}
                  />
                ))
              )}
            </div>
          )}

          {activeTab === 'completed' && (
            <div>
              <h2 style={s.tabContentTitle}>Favors You've Completed</h2>
              <p style={{ ...s.pageSubtitle, marginBottom: '20px' }}>
                Favors you helped complete. Each one earned you +1 reputation.
              </p>

              {loadingClaimed ? (
                <LoadingBox />
              ) : completedFavors.length === 0 ? (
                <EmptyBox message="You haven't completed any favors yet." />
              ) : (
                completedFavors.map(favor => (
                  <CompletedFavorCard
                    key={favor.id}
                    favor={favor}
                    onView={() => navigate(`/favors/${favor.id}`)}
                  />
                ))
              )}
            </div>
          )}
        </div>

        {/* ── RIGHT SIDEBAR ── */}
        <div style={s.sidebar}>

          {/* Activity Summary */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>📊 Activity Summary</h3>
            <div style={s.summaryList}>
              {[
                { label: 'Open Favors',       value: openCount },
                { label: 'Claimed Favors',    value: postedClaimedCount },
                { label: 'Completed Favors',  value: postedCompletedCount },
                { label: 'Total Posted',      value: postedCount },
              ].map(({ label, value }) => (
                <div key={label} style={s.summaryRow}>
                  <span style={s.summaryLabel}>{label}</span>
                  <span style={s.summaryValue}>{loadingPosted ? '—' : value}</span>
                </div>
              ))}
              <div style={s.summaryRow}>
                <span style={s.summaryLabel}>Reputation Earned</span>
                <span style={{ ...s.summaryValue, color: '#C8601A', fontWeight: '700' }}>
                  ⭐ {loadingReputation ? '—' : (reputation?.reputationScore ?? 0)} pts
                </span>
              </div>
            </div>

            {/* Completion Rate */}
            <div style={s.completionSection}>
              <div style={s.completionHeader}>
                <span style={s.completionLabel}>Completion Rate</span>
              </div>
              <div style={s.progressBar}>
                <div style={{ ...s.progressFill, width: `${completionRate}%` }} />
              </div>
              <p style={s.completionText}>
                {loadingPosted ? '—' : `${completionRate}% of your posted favors were completed`}
              </p>
            </div>
          </div>

          {/* Reputation History */}
          <div style={s.sideCard}>
            <h3 style={s.sideCardTitle}>⭐ Reputation History</h3>
            {loadingClaimed ? (
              <LoadingBox small />
            ) : completedFavors.length === 0 ? (
              <p style={s.emptyText}>No reputation history yet. Help a neighbor!</p>
            ) : (
              <>
                <div style={s.repHistoryList}>
                  {completedFavors.slice(0, 5).map(favor => (
                    <div key={favor.id} style={s.repHistoryItem}>
                      <div style={s.repHistoryDot} />
                      <div style={s.repHistoryInfo}>
                        <div style={s.repHistoryTitle}>
                          Helped {favor.requesterName} with {(favor.category || 'favor').toLowerCase()}
                        </div>
                        <div style={s.repHistoryDate}>{formatDate(favor.completedAt)}</div>
                      </div>
                      <div style={s.repHistoryPoints}>+1 pt</div>
                    </div>
                  ))}
                </div>
                <div style={s.repHistoryFooter}>
                  You've earned <strong>{completedFavors.length} reputation point{completedFavors.length !== 1 ? 's' : ''}</strong> in total. Keep it up! 🎉
                </div>
              </>
            )}
          </div>

        </div>
      </div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
// SUB-COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

const StatCard = ({ icon, value, label }) => (
  <div style={s.statCard}>
    <div style={s.statIcon}>{icon}</div>
    <div style={s.statValue}>{value}</div>
    <div style={s.statLabel}>{label}</div>
  </div>
);

const LoadingBox = ({ small }) => (
  <div style={{ ...s.loadingBox, padding: small ? '12px 0' : '32px 0' }}>
    <div style={s.spinner} />
  </div>
);

const EmptyBox = ({ message }) => (
  <div style={s.emptyBox}>
    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ddd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/><path d="M8 15h8M9 9h.01M15 9h.01"/>
    </svg>
    <p style={s.emptyText}>{message}</p>
  </div>
);

// ── Posted Favor Card ─────────────────────────────────────────────────────────
const PostedFavorCard = ({
  favor, actionLoading, deleteConfirm,
  onView, onEdit, onDeleteRequest, onDeleteCancel, onDeleteConfirm, onComplete
}) => {
  const category = favor.category || 'Other';
  const statusColor = STATUS_COLORS[favor.status] || STATUS_COLORS.OPEN;
  const isDeleting = deleteConfirm === favor.id;
  const isActing   = actionLoading === favor.id;

  return (
    <div style={s.favorCard} onClick={onView}>
      <div style={s.favorCardLeft}>
        <div style={s.favorIconWrap}>
          {CATEGORY_ICONS[category] || CATEGORY_ICONS.Other}
        </div>
        <div style={s.favorCardBody}>
          <div style={s.favorCardTop}>
            <h3 style={s.favorCardTitle}>{favor.title}</h3>
            <span style={{ ...s.statusBadge, background: statusColor.bg, color: statusColor.text }}>
              {favor.status}
            </span>
          </div>
          <p style={s.favorCardDesc}>{favor.description}</p>
          <div style={s.favorCardMeta}>Posted {formatDateShort(favor.createdAt)}</div>
        </div>
      </div>

      <div style={s.favorCardActions} onClick={e => e.stopPropagation()}>
        {/* OPEN → Edit + Delete */}
        {favor.status === 'OPEN' && !isDeleting && (
          <div style={s.actionBtnRow}>
            <button style={s.editBtn} onClick={onEdit} disabled={isActing}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              Edit
            </button>
            <button style={s.deleteBtn} onClick={onDeleteRequest} disabled={isActing}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                <path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
              </svg>
              Delete
            </button>
          </div>
        )}

        {/* Delete confirm */}
        {favor.status === 'OPEN' && isDeleting && (
          <div style={s.deleteConfirmBox}>
            <p style={s.deleteConfirmText}>Delete this favor?</p>
            <div style={s.actionBtnRow}>
              <button style={s.deleteConfirmBtn} onClick={onDeleteConfirm} disabled={isActing}>
                {isActing ? 'Deleting…' : 'Yes, Delete'}
              </button>
              <button style={s.cancelBtn} onClick={onDeleteCancel} disabled={isActing}>Cancel</button>
            </div>
          </div>
        )}

        {/* CLAIMED → Claimer info + Confirm button */}
        {favor.status === 'CLAIMED' && (
          <div style={s.claimedActions}>
            <div style={s.claimedByRow}>
              <div style={{ ...s.miniAvatar, background: avatarColor(favor.claimerName || '') }}>
                {initials(favor.claimerName || '?')}
              </div>
              <span style={s.claimedByText}>Claimed by {favor.claimerName}</span>
            </div>
            <button
              style={{ ...s.confirmBtn, opacity: isActing ? 0.7 : 1 }}
              onClick={onComplete}
              disabled={isActing}
            >
              {isActing ? (
                'Confirming…'
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                  Confirm Completion
                </>
              )}
            </button>
          </div>
        )}

        {/* COMPLETED → Helper info */}
        {favor.status === 'COMPLETED' && (
          <div style={s.completedInfo}>
            <div style={s.claimedByRow}>
              <div style={{ ...s.miniAvatar, background: avatarColor(favor.claimerName || '') }}>
                {initials(favor.claimerName || '?')}
              </div>
              <span style={s.claimedByText}>Helped by {favor.claimerName}</span>
            </div>
            <span style={s.completedDate}>Completed {formatDateShort(favor.completedAt)}</span>
          </div>
        )}
      </div>
    </div>
  );
};

// ── Claimed Favor Card ────────────────────────────────────────────────────────
const ClaimedFavorCard = ({ favor, onView }) => {
  const category = favor.category || 'Other';
  const statusColor = STATUS_COLORS[favor.status] || STATUS_COLORS.CLAIMED;

  return (
    <div style={{ ...s.favorCard, cursor: 'pointer' }} onClick={onView}>
      <div style={s.favorCardLeft}>
        <div style={s.favorIconWrap}>
          {CATEGORY_ICONS[category] || CATEGORY_ICONS.Other}
        </div>
        <div style={s.favorCardBody}>
          <div style={s.favorCardTop}>
            <h3 style={s.favorCardTitle}>{favor.title}</h3>
            <span style={{ ...s.statusBadge, background: statusColor.bg, color: statusColor.text }}>
              {favor.status}
            </span>
          </div>
          <p style={s.favorCardDesc}>{favor.description}</p>
          <div style={s.favorCardMetaRow}>
            <div style={s.claimedByRow}>
              <div style={{ ...s.miniAvatar, background: avatarColor(favor.requesterName || '') }}>
                {initials(favor.requesterName || '?')}
              </div>
              <span style={s.favorCardMeta}>Requested by {favor.requesterName}</span>
            </div>
            <span style={s.favorCardMeta}>· {favor.barangay} · {category}</span>
          </div>
        </div>
      </div>
      <div style={{ alignSelf: 'center' }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ccc" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    </div>
  );
};

// ── Completed Favor Card ──────────────────────────────────────────────────────
const CompletedFavorCard = ({ favor, onView }) => {
  const category = favor.category || 'Other';

  return (
    <div style={{ ...s.favorCard, cursor: 'pointer' }} onClick={onView}>
      <div style={s.favorCardLeft}>
        <div style={s.favorIconWrap}>
          {CATEGORY_ICONS[category] || CATEGORY_ICONS.Other}
        </div>
        <div style={s.favorCardBody}>
          <div style={s.favorCardTop}>
            <h3 style={s.favorCardTitle}>{favor.title}</h3>
            <span style={{ ...s.statusBadge, background: STATUS_COLORS.COMPLETED.bg, color: STATUS_COLORS.COMPLETED.text }}>
              COMPLETED
            </span>
          </div>
          <p style={s.favorCardDesc}>{favor.description}</p>
          <div style={s.favorCardMetaRow}>
            <div style={s.claimedByRow}>
              <div style={{ ...s.miniAvatar, background: avatarColor(favor.requesterName || '') }}>
                {initials(favor.requesterName || '?')}
              </div>
              <span style={s.favorCardMeta}>Requested by {favor.requesterName}</span>
            </div>
            <span style={s.favorCardMeta}>· Completed {formatDateShort(favor.completedAt)}</span>
          </div>
        </div>
      </div>
      <div style={s.repBadge}>+1 pt</div>
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
// STYLES
// ══════════════════════════════════════════════════════════════════════════════
const s = {
  page: { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif" },
  content: { display: 'flex', gap: '24px', maxWidth: '1200px', margin: '0 auto', padding: '40px 32px', alignItems: 'flex-start' },
  mainCol: { flex: 1, minWidth: 0 },
  sidebar: { width: '300px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '16px' },

  pageTitle: { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 6px 0' },
  pageSubtitle: { fontSize: '14px', color: '#888', margin: '0 0 28px 0' },

  // Stats row
  statsRow: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', marginBottom: '28px' },
  statCard: { background: 'white', borderRadius: '16px', padding: '20px 16px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '8px', boxShadow: '0 2px 12px rgba(0,0,0,0.05)' },
  statIcon: { width: '36px', height: '36px', borderRadius: '10px', background: '#FFF3E0', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  statValue: { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', lineHeight: '1' },
  statLabel: { fontSize: '12px', color: '#888', fontWeight: '500' },

  // Tabs
  tabRow: { display: 'flex', gap: '0', marginBottom: '24px', borderBottom: '2px solid #f0ece6' },
  tab: { padding: '12px 20px', background: 'none', border: 'none', fontSize: '14px', color: '#888', cursor: 'pointer', fontWeight: '500', display: 'flex', alignItems: 'center', gap: '8px', borderBottom: '2px solid transparent', marginBottom: '-2px' },
  tabActive: { padding: '12px 20px', background: 'none', border: 'none', fontSize: '14px', color: '#C8601A', cursor: 'pointer', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px', borderBottom: '2px solid #C8601A', marginBottom: '-2px' },
  tabBadge: { background: '#f0ece6', color: '#888', fontSize: '11px', fontWeight: '700', padding: '2px 7px', borderRadius: '20px' },
  tabBadgeActive: { background: '#FFF3E0', color: '#C8601A', fontSize: '11px', fontWeight: '700', padding: '2px 7px', borderRadius: '20px' },

  // Tab content header
  tabContentHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' },
  tabContentTitle: { fontSize: '16px', fontWeight: '700', color: '#1a1a1a', margin: 0 },
  postNewLink: { fontSize: '13px', color: '#C8601A', cursor: 'pointer', fontWeight: '500' },

  // Favor cards
  favorCard: { background: 'white', borderRadius: '16px', padding: '18px', marginBottom: '12px', boxShadow: '0 2px 10px rgba(0,0,0,0.04)', border: '1.5px solid #f0ece6', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px' },
  favorCardLeft: { display: 'flex', gap: '14px', flex: 1, minWidth: 0 },
  favorIconWrap: { width: '44px', height: '44px', borderRadius: '12px', background: '#FFF3E0', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  favorCardBody: { flex: 1, minWidth: 0 },
  favorCardTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '4px' },
  favorCardTitle: { fontSize: '15px', fontWeight: '600', color: '#1a1a1a', margin: 0, flex: 1 },
  favorCardDesc: { fontSize: '13px', color: '#666', margin: '4px 0 8px 0', lineHeight: '1.5' },
  favorCardMeta: { fontSize: '12px', color: '#aaa' },
  favorCardMetaRow: { display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' },

  statusBadge: { fontSize: '11px', fontWeight: '700', padding: '3px 9px', borderRadius: '20px', flexShrink: 0, whiteSpace: 'nowrap' },

  // Actions (right side of card)
  favorCardActions: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px', flexShrink: 0, minWidth: '160px' },
  actionBtnRow: { display: 'flex', gap: '8px', alignItems: 'center' },

  editBtn: { display: 'flex', alignItems: 'center', gap: '5px', padding: '7px 14px', borderRadius: '8px', border: '1.5px solid #e8e8e8', background: 'white', color: '#555', fontSize: '13px', fontWeight: '500', cursor: 'pointer' },
  deleteBtn: { display: 'flex', alignItems: 'center', gap: '5px', padding: '7px 14px', borderRadius: '8px', border: '1.5px solid #ffcdd2', background: 'white', color: '#e53935', fontSize: '13px', fontWeight: '500', cursor: 'pointer' },
  cancelBtn: { padding: '7px 14px', borderRadius: '8px', border: '1.5px solid #e8e8e8', background: 'white', color: '#555', fontSize: '13px', fontWeight: '500', cursor: 'pointer' },

  deleteConfirmBox: { display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end' },
  deleteConfirmText: { fontSize: '13px', color: '#e53935', margin: 0, fontWeight: '500' },
  deleteConfirmBtn: { padding: '7px 14px', borderRadius: '8px', border: 'none', background: '#e53935', color: 'white', fontSize: '13px', fontWeight: '600', cursor: 'pointer' },

  claimedActions: { display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end' },
  claimedByRow: { display: 'flex', alignItems: 'center', gap: '6px' },
  claimedByText: { fontSize: '12px', color: '#555', fontWeight: '500' },
  miniAvatar: { width: '22px', height: '22px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '9px', fontWeight: '700', flexShrink: 0 },

  confirmBtn: { display: 'flex', alignItems: 'center', gap: '6px', padding: '9px 16px', borderRadius: '10px', border: 'none', background: '#2E7D32', color: 'white', fontSize: '13px', fontWeight: '600', cursor: 'pointer' },

  completedInfo: { display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end' },
  completedDate: { fontSize: '11px', color: '#aaa' },

  repBadge: { background: '#E8F5E9', color: '#2E7D32', fontSize: '12px', fontWeight: '700', padding: '4px 10px', borderRadius: '20px', flexShrink: 0, alignSelf: 'center' },

  // Loading / empty
  loadingBox: { display: 'flex', justifyContent: 'center', alignItems: 'center' },
  spinner: { width: '24px', height: '24px', border: '3px solid #f0ece6', borderTop: '3px solid #C8601A', borderRadius: '50%', animation: 'spin 0.7s linear infinite' },
  emptyBox: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '40px 0', gap: '10px' },
  emptyText: { fontSize: '13px', color: '#aaa', margin: 0, textAlign: 'center' },

  // Sidebar
  sideCard: { background: 'white', borderRadius: '16px', padding: '20px', boxShadow: '0 2px 12px rgba(0,0,0,0.05)' },
  sideCardTitle: { fontSize: '15px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 16px 0' },

  summaryList: { display: 'flex', flexDirection: 'column', gap: '0' },
  summaryRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f5f5f5' },
  summaryLabel: { fontSize: '13px', color: '#555' },
  summaryValue: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },

  completionSection: { marginTop: '16px' },
  completionHeader: { marginBottom: '8px' },
  completionLabel: { fontSize: '13px', fontWeight: '600', color: '#333' },
  progressBar: { height: '8px', background: '#f0ece6', borderRadius: '4px', overflow: 'hidden', marginBottom: '6px' },
  progressFill: { height: '100%', background: '#C8601A', borderRadius: '4px', transition: 'width 0.5s ease' },
  completionText: { fontSize: '12px', color: '#aaa', margin: 0 },

  repHistoryList: { display: 'flex', flexDirection: 'column', gap: '0' },
  repHistoryItem: { display: 'flex', alignItems: 'flex-start', gap: '10px', padding: '10px 0', borderBottom: '1px solid #f5f5f5' },
  repHistoryDot: { width: '8px', height: '8px', borderRadius: '50%', background: '#C8601A', marginTop: '4px', flexShrink: 0 },
  repHistoryInfo: { flex: 1, minWidth: 0 },
  repHistoryTitle: { fontSize: '13px', color: '#333', fontWeight: '500', marginBottom: '2px' },
  repHistoryDate: { fontSize: '11px', color: '#aaa' },
  repHistoryPoints: { background: '#E8F5E9', color: '#2E7D32', fontSize: '11px', fontWeight: '700', padding: '2px 8px', borderRadius: '20px', flexShrink: 0 },
  repHistoryFooter: { marginTop: '12px', fontSize: '13px', color: '#555', background: '#FAF7F2', borderRadius: '10px', padding: '10px 12px', lineHeight: '1.5' },
};

export default MyActivityPage;