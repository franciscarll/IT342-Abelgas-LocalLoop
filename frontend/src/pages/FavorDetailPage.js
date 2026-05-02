import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import Navbar from '../components/Navbar';

// ── Shared helpers (consistent with DashboardPage / FavorFeedPage) ─────────────
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

const CATEGORY_TAG_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

const CATEGORY_ICONS = {
  Errand: (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
      <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 001.99-1.84L23 6H6"/>
    </svg>
  ),
  'Pet Care': (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10 5.172C10 3.782 8.423 2.679 6.5 3c-2.823.47-4.113 6.006-4 7
               .08.703.75 1.5 1.5 2h7c.75-.5 1.42-1.297 1.5-2
               .113-.994-1.177-6.53-4-7-.267-.044-.537-.054-.8-.028"/>
      <path d="M8 14v4c0 1.1.9 2 2 2h4c1.1 0 2-.9 2-2v-4"/>
    </svg>
  ),
  'Tool Borrowing': (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0
               01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0
               017.94-7.94l-3.76 3.76z"/>
    </svg>
  ),
  'Plant Watering': (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22V12"/>
      <path d="M12 12C12 12 7 10 5 6c4 0 7 2 7 6z"/>
      <path d="M12 12C12 12 17 10 19 6c-4 0-7 2-7 6z"/>
    </svg>
  ),
  Other: (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C8601A"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" y1="8" x2="12" y2="12"/>
      <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
  ),
};

// ── Date formatters ────────────────────────────────────────────────────────────
function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return (
    d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) +
    ' · ' +
    d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true })
  );
}

function formatDateShort(dateStr) {
  if (!dateStr) return 'Pending';
  const d = new Date(dateStr);
  return (
    d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) +
    ', ' +
    d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true })
  );
}

function formatDateLong(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'long', day: 'numeric', year: 'numeric',
  });
}

// Inject spinner keyframe once
if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ══════════════════════════════════════════════════════════════════════════════
const FavorDetailPage = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [favor, setFavor] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // View mode is derived from role — not user-controlled

  const [otherFavors, setOtherFavors] = useState([]);
  const [requesterStats, setRequesterStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);

  // ── Fetch favor detail ─────────────────────────────────────────────────────
  useEffect(() => {
    setLoading(true);
    setError('');
    api.get(`/favors/${id}`)
      .then(res => {
        const data = res.data?.data || res.data;
        setFavor(data);
      })
      .catch(() => setError('Could not load favor details. Please try again.'))
      .finally(() => setLoading(false));
  }, [id, user]);

  // ── Fetch other open favors for sidebar ───────────────────────────────────
  useEffect(() => {
    api.get('/favors', { params: { page: 0, size: 4, status: 'OPEN' } })
      .then(res => {
        const data = res.data?.data;
        const list = data?.content || data || [];
        // Exclude the current favor from the list
        setOtherFavors(list.filter(f => String(f.id) !== String(id)).slice(0, 3));
      })
      .catch(() => setOtherFavors([]));
  }, [id]);

  // ── Fetch requester reputation stats ──────────────────────────────────────
  useEffect(() => {
    if (!favor?.requesterId) return;
    setStatsLoading(true);
    api.get(`/users/${favor.requesterId}/reputation`)
      .then(res => setRequesterStats(res.data?.data || res.data))
      .catch(() => setRequesterStats(null))
      .finally(() => setStatsLoading(false));
  }, [favor?.requesterId]);

  // ── Claim handler ──────────────────────────────────────────────────────────
  const handleClaim = async () => {
    setActionLoading(true);
    setActionError('');
    setActionSuccess('');
    try {
      const res = await api.post(`/favors/${id}/claim`);
      const updated = res.data?.data || res.data;
      setFavor(updated);
      setActionSuccess('Favor claimed! The requester has been notified.');
    } catch (err) {
      setActionError(
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        'Could not claim this favor. Please try again.'
      );
    } finally {
      setActionLoading(false);
    }
  };

  // ── Complete handler ───────────────────────────────────────────────────────
  const handleComplete = async () => {
    setActionLoading(true);
    setActionError('');
    setActionSuccess('');
    try {
      const res = await api.put(`/favors/${id}/complete`);
      const updated = res.data?.data || res.data;
      setFavor(updated);
      setActionSuccess('Favor marked as completed! The helper earned +1 reputation point.');
    } catch (err) {
      setActionError(
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        'Could not complete this favor. Please try again.'
      );
    } finally {
      setActionLoading(false);
    }
  };

  // ── Role flags ─────────────────────────────────────────────────────────────
  const isOwner   = user && favor && user.id === favor.requesterId;
  const isClaimer = user && favor && user.id === favor.claimerId;

  // ── Loading state ──────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div style={s.page}>
        <Navbar />
        <div style={s.centered}>
          <div style={s.spinner} />
          <span style={s.loadingText}>Loading favor details…</span>
        </div>
      </div>
    );
  }

  // ── Error state ────────────────────────────────────────────────────────────
  if (error || !favor) {
    return (
      <div style={s.page}>
        <Navbar />
        <div style={s.centered}>
          <div style={s.errorBox}>{error || 'Favor not found.'}</div>
          <button style={s.backBtn} onClick={() => navigate('/favor-feed')}>
            ← Back to Favor Feed
          </button>
        </div>
      </div>
    );
  }

  // ── Derived display values ─────────────────────────────────────────────────
  const category = favor.category || 'Other';
  const tagColor  = CATEGORY_TAG_COLORS[category] || CATEGORY_TAG_COLORS.Other;

  const STATUS_META = {
    OPEN:      { bg: '#FFF3E0', text: '#E65100', dot: '#FF9800' },
    CLAIMED:   { bg: '#E3F2FD', text: '#1565C0', dot: '#2196F3' },
    COMPLETED: { bg: '#E8F5E9', text: '#2E7D32', dot: '#4CAF50' },
  };
  const sc = STATUS_META[favor.status] || STATUS_META.OPEN;

  // Timeline steps — uses claimedAt from backend (now available)
  const timelineSteps = [
    {
      label: 'Posted',
      date:  favor.createdAt,
      done:  true,
    },
    {
      label: 'Claimed',
      // claimedAt is now a real field on FavorResponse; fallback only for legacy rows
      date:  favor.claimedAt || (favor.status !== 'OPEN' ? favor.updatedAt : null),
      done:  favor.status === 'CLAIMED' || favor.status === 'COMPLETED',
    },
    {
      label: 'Completed',
      date:  favor.completedAt,
      done:  favor.status === 'COMPLETED',
    },
  ];

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>

        {/* ══ LEFT MAIN COLUMN ══════════════════════════════════════════ */}
        <div style={s.leftCol}>

          {/* Breadcrumb */}
          <div style={s.breadcrumb}>
            <span style={s.breadcrumbLink} onClick={() => navigate('/favor-feed')}>
              Favor Feed
            </span>
            <span style={s.breadcrumbArrow}>→</span>
            <span style={s.breadcrumbCurrent}>{favor.title}</span>
          </div>

          {/* View / Status Toggle Row — both are read-only indicators */}
          <div style={s.toggleRow}>

            {/* View as — locked, derived from whether logged-in user is the requester */}
            <div style={s.toggleGroup}>
              <span style={s.toggleLabel}>View as:</span>
              <div style={s.toggleBtnGroup}>
                {['Claimer', 'Owner'].map(mode => {
                  const isActive = isOwner ? mode === 'Owner' : mode === 'Claimer';
                  return (
                    <button
                      key={mode}
                      style={isActive ? s.toggleBtnActive : s.toggleBtn}
                      disabled
                    >
                      {mode}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Status indicator — read-only */}
            <div style={s.toggleGroup}>
              <span style={s.toggleLabel}>Status:</span>
              <div style={s.toggleBtnGroup}>
                {['Open', 'Claimed'].map(st => {
                  const isCurrentStatus =
                    favor.status.toLowerCase() === st.toLowerCase() ||
                    (st === 'Claimed' && favor.status === 'COMPLETED');
                  return (
                    <button
                      key={st}
                      style={isCurrentStatus ? s.toggleBtnActive : s.toggleBtn}
                      disabled
                    >
                      {st}
                    </button>
                  );
                })}
              </div>
            </div>

          </div>

          {/* ── Main Favor Card ─────────────────────────────────────────── */}
          <div style={s.favorCard}>

            {/* Card Header: icon + title + status badge */}
            <div style={s.favorCardHeader}>
              <div style={s.favorIconWrap}>
                {CATEGORY_ICONS[category] || CATEGORY_ICONS.Other}
              </div>
              <div style={s.favorTitleBlock}>
                <h1 style={s.favorTitle}>{favor.title}</h1>
                <span style={{ ...s.categoryBadge, background: tagColor.bg, color: tagColor.text }}>
                  {category}
                </span>
              </div>
              <div style={{ ...s.statusBadge, background: sc.bg, color: sc.text }}>
                <div style={{ ...s.statusDot, background: sc.dot }} />
                {favor.status}
              </div>
            </div>

            <div style={s.divider} />

            {/* Description */}
            <div style={s.section}>
              <div style={s.sectionLabel}>Description</div>
              <p style={s.descriptionText}>{favor.description}</p>
            </div>

            {/* Meta Grid */}
            <div style={s.metaGrid}>
              <div style={s.metaItem}>
                <div style={s.metaLabel}>Date Needed</div>
                <div style={s.metaValue}>
                  {favor.dateNeeded ? formatDateLong(favor.dateNeeded) : '—'}
                </div>
              </div>
              <div style={s.metaItem}>
                <div style={s.metaLabel}>Date Posted</div>
                <div style={s.metaValue}>{formatDateTime(favor.createdAt)}</div>
              </div>
              <div style={s.metaItem}>
                <div style={s.metaLabel}>Barangay</div>
                <div style={s.metaValue}>{favor.barangay}</div>
              </div>
              <div style={s.metaItem}>
                <div style={s.metaLabel}>Category</div>
                <div style={s.metaValue}>{category}</div>
              </div>
            </div>

            <div style={s.divider} />

            {/* Status Timeline */}
            <div style={s.section}>
              <div style={s.sectionLabel}>Status Timeline</div>
              <div style={s.timeline}>
                {timelineSteps.map((step, i) => (
                  <React.Fragment key={step.label}>
                    <div style={s.timelineStep}>
                      <div style={step.done ? s.timelineCircleDone : s.timelineCircle}>
                        {step.done ? (
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                               stroke="white" strokeWidth="2.5"
                               strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                          </svg>
                        ) : (
                          <span style={s.timelineNumber}>{i + 1}</span>
                        )}
                      </div>
                      <div style={s.timelineStepLabel(step.done)}>{step.label}</div>
                      <div style={s.timelineDate}>
                        {step.done && step.date
                          ? formatDateShort(step.date)
                          : 'Pending'}
                      </div>
                    </div>
                    {i < timelineSteps.length - 1 && (
                      <div style={timelineSteps[i + 1].done
                        ? s.timelineLineDone
                        : s.timelineLine}
                      />
                    )}
                  </React.Fragment>
                ))}
              </div>
            </div>

          </div>{/* end favorCard */}
        </div>{/* end leftCol */}

        {/* ══ RIGHT SIDEBAR ════════════════════════════════════════════ */}
        <div style={s.rightCol}>

          {/* Requester Info Card */}
          <div style={s.card}>
            <div style={s.cardSectionLabel}>Requester</div>
            <div style={s.requesterRow}>
              <div style={{
                ...s.requesterAvatar,
                background: avatarColor(favor.requesterName || ''),
              }}>
                {initials(favor.requesterName || '?')}
              </div>
              <div>
                <div style={s.requesterName}>{favor.requesterName || '—'}</div>
                {statsLoading ? (
                  <div style={s.statsLoading}>Loading…</div>
                ) : (
                  <>
                    <div style={s.requesterRep}>
                      ⭐ {requesterStats?.reputationScore ?? favor.requesterStats ?? 0} reputation points
                    </div>
                    <div style={s.requesterMeta}>
                      Member since{' '}
                      {requesterStats?.memberSince
                        ? new Date(requesterStats.memberSince).toLocaleDateString('en-US', {
                            month: 'short', year: 'numeric',
                          })
                        : '—'}
                    </div>
                  </>
                )}
              </div>
            </div>

            {!statsLoading && (
              <div style={s.requesterStats}>
                <div style={s.requesterStatItem}>
                  <span style={s.requesterStatValue}>
                    {requesterStats?.favorsPosted ?? '—'}
                  </span>
                  <span style={s.requesterStatLabel}>Favors Posted</span>
                </div>
                <div style={s.requesterStatDivider} />
                <div style={s.requesterStatItem}>
                  <span style={s.requesterStatValue}>
                    {requesterStats?.favorsCompleted ?? '—'}
                  </span>
                  <span style={s.requesterStatLabel}>Completed</span>
                </div>
              </div>
            )}
          </div>

          {/* Take Action Card */}
          <div style={s.card}>
            <div style={s.cardSectionLabel}>Take Action</div>

            {/* ── OPEN + not owner → Claim button ── */}
            {favor.status === 'OPEN' && !isOwner && (
              <>
                <p style={s.actionSubtext}>
                  This favor is open and waiting for a neighbor to help.
                </p>
                {actionSuccess ? (
                  <div style={s.successBox}>{actionSuccess}</div>
                ) : (
                  <>
                    {actionError && <div style={s.inlineError}>{actionError}</div>}
                    <button
                      style={{ ...s.claimBtn, opacity: actionLoading ? 0.7 : 1 }}
                      onClick={handleClaim}
                      disabled={actionLoading}
                    >
                      <span style={s.btnInner}>
                        {actionLoading ? (
                          <><div style={s.btnSpinner} /> Claiming…</>
                        ) : (
                          <>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                 stroke="white" strokeWidth="2"
                                 strokeLinecap="round" strokeLinejoin="round">
                              <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67
                                       l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06
                                       1.06L12 21.23l7.78-7.78 1.06-1.06a5.5
                                       5.5 0 000-7.78z"/>
                            </svg>
                            Claim This Favor
                          </>
                        )}
                      </span>
                    </button>
                    <p style={s.actionNote}>
                      Once claimed, the requester will be notified.
                    </p>
                  </>
                )}
              </>
            )}

            {/* ── OPEN + is owner → can't claim own favor ── */}
            {favor.status === 'OPEN' && isOwner && (
              <div style={s.ownerNote}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                     stroke="#888" strokeWidth="2"
                     strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                This is your favor. Wait for a neighbor to claim it.
              </div>
            )}

            {/* ── CLAIMED + is owner → Confirm Completion ── */}
            {favor.status === 'CLAIMED' && isOwner && (
              <>
                <p style={s.actionSubtext}>
                  <strong>{favor.claimerName}</strong> has claimed your favor.
                  Confirm once they're done!
                </p>
                {actionSuccess ? (
                  <div style={s.successBox}>{actionSuccess}</div>
                ) : (
                  <>
                    {actionError && <div style={s.inlineError}>{actionError}</div>}
                    <button
                      style={{ ...s.completeBtn, opacity: actionLoading ? 0.7 : 1 }}
                      onClick={handleComplete}
                      disabled={actionLoading}
                    >
                      <span style={s.btnInner}>
                        {actionLoading ? (
                          <><div style={s.btnSpinner} /> Confirming…</>
                        ) : (
                          <>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                 stroke="white" strokeWidth="2.5"
                                 strokeLinecap="round" strokeLinejoin="round">
                              <polyline points="20 6 9 17 4 12"/>
                            </svg>
                            Confirm Completion
                          </>
                        )}
                      </span>
                    </button>
                    <p style={s.actionNote}>
                      Helper earns +1 reputation on confirmation.
                    </p>
                  </>
                )}
              </>
            )}

            {/* ── CLAIMED + is claimer → you claimed it ── */}
            {favor.status === 'CLAIMED' && isClaimer && !isOwner && (
              <div style={s.successBox}>
                ✅ You claimed this favor. Complete the task and wait for the
                requester to confirm.
              </div>
            )}

            {/* ── CLAIMED + neither owner nor claimer → already claimed ── */}
            {favor.status === 'CLAIMED' && !isOwner && !isClaimer && (
              <div style={s.ownerNote}>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                     stroke="#888" strokeWidth="2"
                     strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                This favor has already been claimed.
              </div>
            )}

            {/* ── COMPLETED ── */}
            {favor.status === 'COMPLETED' && (
              <div style={s.successBox}>
                ✅ This favor has been completed!
                {favor.claimerName && (
                  <> Helped by <strong>{favor.claimerName}</strong>.</>
                )}
              </div>
            )}
          </div>

          {/* Other Open Favors */}
          <div style={s.card}>
            <div style={s.cardSectionLabel}>Other Open Favors</div>
            {otherFavors.length === 0 ? (
              <p style={s.emptyOther}>No other open favors right now.</p>
            ) : (
              <div style={s.otherFavorsList}>
                {otherFavors.map(f => {
                  const fCat = f.category || 'Other';
                  return (
                    <div
                      key={f.id}
                      style={s.otherFavorItem}
                      onClick={() => navigate(`/favors/${f.id}`)}
                    >
                      <div style={s.otherFavorIcon}>
                        {CATEGORY_ICONS[fCat] || CATEGORY_ICONS.Other}
                      </div>
                      <div style={s.otherFavorInfo}>
                        <div style={s.otherFavorTitle}>{f.title}</div>
                        <div style={s.otherFavorCategory}>{fCat}</div>
                      </div>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                           stroke="#ccc" strokeWidth="2"
                           strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="9 18 15 12 9 6"/>
                      </svg>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

        </div>{/* end rightCol */}
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
    display: 'flex',
    gap: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '32px 32px',
    alignItems: 'flex-start',
  },
  leftCol:  { flex: 1, minWidth: 0 },
  rightCol: {
    width: '320px',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },

  // ── Loading / error states ─────────────────────────────────────────────────
  centered: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    gap: '16px',
  },
  spinner: {
    width: '32px',
    height: '32px',
    border: '3px solid #f0ece6',
    borderTop: '3px solid #C8601A',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
  },
  loadingText: { fontSize: '14px', color: '#aaa' },
  errorBox: {
    padding: '16px 20px',
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '12px',
    color: '#e53935',
    fontSize: '14px',
  },
  backBtn: {
    marginTop: '8px',
    padding: '10px 20px',
    background: 'none',
    border: '1.5px solid #C8601A',
    borderRadius: '10px',
    color: '#C8601A',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
  },

  // ── Breadcrumb ─────────────────────────────────────────────────────────────
  breadcrumb: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '20px',
    flexWrap: 'wrap',
  },
  breadcrumbLink: {
    fontSize: '13px',
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
  },
  breadcrumbArrow:   { fontSize: '13px', color: '#ccc' },
  breadcrumbCurrent: { fontSize: '13px', color: '#888' },

  // ── Toggle row ─────────────────────────────────────────────────────────────
  toggleRow: {
    display: 'flex',
    gap: '20px',
    marginBottom: '20px',
    flexWrap: 'wrap',
  },
  toggleGroup: { display: 'flex', alignItems: 'center', gap: '8px' },
  toggleLabel: { fontSize: '13px', color: '#888', fontWeight: '500' },
  toggleBtnGroup: {
    display: 'flex',
    background: 'white',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    overflow: 'hidden',
  },
  toggleBtn: {
    padding: '6px 16px',
    border: 'none',
    background: 'transparent',
    fontSize: '13px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
  },
  toggleBtnActive: {
    padding: '6px 16px',
    border: 'none',
    background: '#C8601A',
    fontSize: '13px',
    color: 'white',
    cursor: 'pointer',
    fontWeight: '600',
    borderRadius: '8px',
  },

  // ── Favor card ─────────────────────────────────────────────────────────────
  favorCard: {
    background: 'white',
    borderRadius: '20px',
    padding: '28px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },
  favorCardHeader: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '16px',
    marginBottom: '20px',
  },
  favorIconWrap: {
    width: '52px',
    height: '52px',
    borderRadius: '14px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  favorTitleBlock: { flex: 1, minWidth: 0 },
  favorTitle: {
    fontSize: '22px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 8px 0',
    lineHeight: '1.3',
  },
  categoryBadge: {
    display: 'inline-block',
    fontSize: '12px',
    fontWeight: '600',
    padding: '4px 12px',
    borderRadius: '20px',
  },
  statusBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '6px 14px',
    borderRadius: '20px',
    fontSize: '12px',
    fontWeight: '700',
    flexShrink: 0,
    height: 'fit-content',
    letterSpacing: '0.3px',
  },
  statusDot: { width: '7px', height: '7px', borderRadius: '50%' },

  divider: { height: '1px', background: '#f5f5f5', margin: '20px 0' },

  section: { marginBottom: '20px' },
  sectionLabel: {
    fontSize: '11px',
    fontWeight: '600',
    color: '#aaa',
    textTransform: 'uppercase',
    letterSpacing: '0.6px',
    marginBottom: '10px',
  },
  descriptionText: {
    fontSize: '14px',
    color: '#444',
    lineHeight: '1.75',
    margin: 0,
  },

  metaGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '20px',
    marginBottom: '0',
  },
  metaItem: {},
  metaLabel: {
    fontSize: '11px',
    fontWeight: '600',
    color: '#aaa',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    marginBottom: '4px',
  },
  metaValue: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },

  // ── Timeline ───────────────────────────────────────────────────────────────
  timeline: { display: 'flex', alignItems: 'flex-start' },
  timelineStep: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
    width: '90px',
    flexShrink: 0,
  },
  timelineCircleDone: {
    width: '36px',
    height: '36px',
    borderRadius: '50%',
    background: '#C8601A',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  timelineCircle: {
    width: '36px',
    height: '36px',
    borderRadius: '50%',
    background: 'white',
    border: '2px solid #e0e0e0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  timelineNumber: { fontSize: '13px', fontWeight: '600', color: '#bbb' },
  timelineStepLabel: (done) => ({
    fontSize: '13px',
    fontWeight: done ? '700' : '500',
    color: done ? '#C8601A' : '#aaa',
    textAlign: 'center',
  }),
  timelineDate: { fontSize: '11px', color: '#aaa', textAlign: 'center' },
  timelineLine: {
    flex: 1,
    height: '2px',
    background: '#e8e8e8',
    marginTop: '17px',
    alignSelf: 'flex-start',
  },
  timelineLineDone: {
    flex: 1,
    height: '2px',
    background: '#C8601A',
    marginTop: '17px',
    alignSelf: 'flex-start',
  },

  // ── Sidebar cards ──────────────────────────────────────────────────────────
  card: {
    background: 'white',
    borderRadius: '20px',
    padding: '20px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },
  cardSectionLabel: {
    fontSize: '11px',
    fontWeight: '700',
    color: '#aaa',
    textTransform: 'uppercase',
    letterSpacing: '0.6px',
    marginBottom: '14px',
  },

  // ── Requester ──────────────────────────────────────────────────────────────
  requesterRow: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '12px',
    marginBottom: '16px',
  },
  requesterAvatar: {
    width: '44px',
    height: '44px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '15px',
    fontWeight: '700',
    flexShrink: 0,
  },
  requesterName: {
    fontSize: '15px',
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: '3px',
  },
  requesterRep: {
    fontSize: '12px',
    color: '#C8601A',
    fontWeight: '500',
    marginBottom: '2px',
  },
  requesterMeta: { fontSize: '12px', color: '#aaa' },
  statsLoading: { fontSize: '12px', color: '#bbb' },
  requesterStats: {
    display: 'flex',
    gap: '0',
    paddingTop: '14px',
    borderTop: '1px solid #f5f5f5',
  },
  requesterStatItem: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
  },
  requesterStatDivider: {
    width: '1px',
    background: '#f0ece6',
    margin: '0 12px',
  },
  requesterStatValue: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1a1a1a',
  },
  requesterStatLabel: { fontSize: '11px', color: '#aaa' },

  // ── Take Action ────────────────────────────────────────────────────────────
  actionSubtext: {
    fontSize: '13px',
    color: '#666',
    margin: '0 0 14px 0',
    lineHeight: '1.6',
  },
  claimBtn: {
    width: '100%',
    padding: '13px',
    borderRadius: '12px',
    border: 'none',
    background: '#C8601A',
    color: 'white',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
    marginBottom: '10px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  completeBtn: {
    width: '100%',
    padding: '13px',
    borderRadius: '12px',
    border: 'none',
    background: '#2E7D32',
    color: 'white',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
    marginBottom: '10px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnInner: { display: 'flex', alignItems: 'center', gap: '8px' },
  btnSpinner: {
    width: '14px',
    height: '14px',
    border: '2px solid rgba(255,255,255,0.4)',
    borderTop: '2px solid white',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
    flexShrink: 0,
  },
  actionNote: {
    fontSize: '12px',
    color: '#aaa',
    textAlign: 'center',
    margin: '4px 0 0 0',
  },
  ownerNote: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '8px',
    fontSize: '13px',
    color: '#888',
    background: '#f9f9f9',
    borderRadius: '10px',
    padding: '12px',
    lineHeight: '1.5',
  },
  successBox: {
    background: '#E8F5E9',
    border: '1px solid #C8E6C9',
    borderRadius: '10px',
    padding: '12px 14px',
    fontSize: '13px',
    color: '#2E7D32',
    lineHeight: '1.6',
  },
  inlineError: {
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '10px',
    padding: '10px 14px',
    fontSize: '13px',
    color: '#e53935',
    marginBottom: '12px',
  },

  // ── Other favors ───────────────────────────────────────────────────────────
  emptyOther: { fontSize: '13px', color: '#aaa', margin: '4px 0 0 0' },
  otherFavorsList: { display: 'flex', flexDirection: 'column' },
  otherFavorItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 0',
    borderBottom: '1px solid #f5f5f5',
    cursor: 'pointer',
    transition: 'opacity 0.15s',
  },
  otherFavorIcon: {
    width: '36px',
    height: '36px',
    borderRadius: '10px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  otherFavorInfo: { flex: 1, minWidth: 0 },
  otherFavorTitle: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#1a1a1a',
    marginBottom: '2px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  otherFavorCategory: { fontSize: '11px', color: '#aaa' },
};

export default FavorDetailPage;