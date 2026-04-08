import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ApiClient from '../api/ApiClient';
const api = ApiClient.getInstance();
import Navbar from '../components/Navbar';

// ── Constants ──────────────────────────────────────────────────────────────────
const CATEGORIES = ['Errand', 'Pet Care', 'Tool Borrowing', 'Plant Watering', 'Other'];

const CATEGORY_TAG_COLORS = {
  Errand:           { bg: '#FFF3E0', text: '#E65100' },
  'Pet Care':       { bg: '#F3E5F5', text: '#7B1FA2' },
  'Tool Borrowing': { bg: '#E8F5E9', text: '#2E7D32' },
  'Plant Watering': { bg: '#E0F7FA', text: '#00695C' },
  Other:            { bg: '#F5F5F5', text: '#424242' },
};

const CATEGORY_ICONS = {
  Errand: '🛒',
  'Pet Care': '🐾',
  'Tool Borrowing': '🔧',
  'Plant Watering': '🌿',
  Other: '📦',
};

const TIPS = [
  { icon: '✅', text: 'Be specific about what you need and when.' },
  { icon: '📍', text: 'Mention the location if relevant.' },
  { icon: '🕐', text: 'Set a realistic date needed.' },
  { icon: '🤝', text: 'Be polite — your neighbor is doing you a kindness!' },
];

// ── CSS injection ──────────────────────────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('ll-spin-style')) {
  const style = document.createElement('style');
  style.id = 'll-spin-style';
  style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
  document.head.appendChild(style);
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN PAGE
// ══════════════════════════════════════════════════════════════════════════════
const CreateFavorPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    title: '',
    description: '',
    category: 'Errand',
    dateNeeded: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [apiError, setApiError] = useState('');

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
    setApiError('');
  };

  const validate = () => {
    const newErrors = {};
    if (!form.title.trim()) newErrors.title = 'Favor title is required.';
    else if (form.title.length > 200) newErrors.title = 'Title must be 200 characters or less.';
    if (!form.description.trim()) newErrors.description = 'Description is required.';
    else if (form.description.length > 1000) newErrors.description = 'Description must be 1000 characters or less.';
    if (!form.category) newErrors.category = 'Please select a category.';
    return newErrors;
  };

  const handleSubmit = async () => {
    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    setApiError('');
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        category: form.category,
      };
      if (form.dateNeeded) payload.dateNeeded = form.dateNeeded + 'T00:00:00';

      await api.post('/favors', payload);
      setSuccessMsg('Your favor has been posted! Neighbors can now see and claim it.');
      setTimeout(() => navigate('/favor-feed'), 2000);
    } catch (err) {
      setApiError(err.response?.data?.error?.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => navigate('/favor-feed');

  // ── Preview helpers ───────────────────────────────────────────────────────
  const previewTitle = form.title.trim() || 'Your favor title will appear here';
  const previewDesc  = form.description.trim()
    ? form.description.substring(0, 60) + (form.description.length > 60 ? '…' : '')
    : 'Your description will appear here...';
  const tagColor = CATEGORY_TAG_COLORS[form.category] || CATEGORY_TAG_COLORS.Other;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>
        {/* ── LEFT: FORM ─────────────────────────────────────────────── */}
        <div style={s.formCol}>
          {/* Breadcrumb */}
          <div style={s.breadcrumb}>
            <span style={s.breadcrumbLink} onClick={() => navigate('/favor-feed')}>
              Favor Feed
            </span>
            <span style={s.breadcrumbArrow}> → </span>
            <span style={s.breadcrumbCurrent}>Post a Favor</span>
          </div>

          <h1 style={s.pageTitle}>Post a Favor 🤝</h1>
          <p style={s.pageSubtitle}>Ask your neighbors for a small hand. Be specific and friendly!</p>

          {/* Success message */}
          {successMsg && (
            <div style={s.successBox}>
              <span>✅</span> {successMsg}
            </div>
          )}

          {/* API error */}
          {apiError && (
            <div style={s.errorBox}>
              <span>⚠️</span> {apiError}
            </div>
          )}

          <div style={s.formCard}>
            {/* ── Favor Title ───────────────────────────────────────── */}
            <div style={s.fieldSection}>
              <div style={s.fieldHeader}>
                <span style={s.fieldTitle}>Favor Title</span>
                <span style={s.fieldHint}>Be clear and concise.</span>
              </div>
              <input
                type="text"
                placeholder="e.g. Pick up medicine from Mercury Drug"
                value={form.title}
                onChange={e => handleChange('title', e.target.value)}
                style={{ ...s.input, ...(errors.title ? s.inputError : {}) }}
                maxLength={200}
              />
              <div style={s.fieldFooter}>
                {errors.title && <span style={s.errorText}>{errors.title}</span>}
                <span style={s.charCount}>{form.title.length} / 200</span>
              </div>
            </div>

            <div style={s.fieldDivider} />

            {/* ── Category ──────────────────────────────────────────── */}
            <div style={s.fieldSection}>
              <div style={s.fieldHeader}>
                <span style={s.fieldTitle}>Category</span>
                <span style={s.fieldHint}>Choose the most relevant type.</span>
              </div>
              <div style={s.categoryGrid}>
                {CATEGORIES.map(cat => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => handleChange('category', cat)}
                    style={{
                      ...s.catPill,
                      ...(form.category === cat ? s.catPillActive : {}),
                    }}
                  >
                    <span style={{ fontSize: '14px' }}>{CATEGORY_ICONS[cat]}</span>
                    {cat}
                  </button>
                ))}
              </div>
              {errors.category && <span style={s.errorText}>{errors.category}</span>}
            </div>

            <div style={s.fieldDivider} />

            {/* ── Description ───────────────────────────────────────── */}
            <div style={s.fieldSection}>
              <div style={s.fieldHeader}>
                <span style={s.fieldTitle}>Description</span>
                <span style={s.fieldHint}>Give your neighbor enough detail to help you.</span>
              </div>
              <textarea
                placeholder="e.g. Need someone to pick up paracetamol 500mg from the Mercury Drug on the main road. Payment via GCash. Should take around 15 minutes."
                value={form.description}
                onChange={e => handleChange('description', e.target.value)}
                style={{ ...s.textarea, ...(errors.description ? s.inputError : {}) }}
                rows={5}
                maxLength={1000}
              />
              <div style={s.fieldFooter}>
                {errors.description && <span style={s.errorText}>{errors.description}</span>}
                <span style={s.charCount}>{form.description.length} / 1000</span>
              </div>
            </div>

            <div style={s.fieldDivider} />

            {/* ── Date Needed ───────────────────────────────────────── */}
            <div style={s.fieldSection}>
              <div style={s.fieldHeader}>
                <span style={s.fieldTitle}>Date Needed</span>
                <span style={s.fieldHint}>Optional — when do you need this done?</span>
              </div>
              <div style={s.dateWrapper}>
                <svg style={{ flexShrink: 0 }} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <input
                  type="date"
                  value={form.dateNeeded}
                  onChange={e => handleChange('dateNeeded', e.target.value)}
                  style={s.dateInput}
                  min={new Date().toISOString().split('T')[0]}
                />
                {!form.dateNeeded && <span style={s.optionalBadge}>Optional</span>}
              </div>
            </div>

            <div style={s.fieldDivider} />

            {/* ── Actions ───────────────────────────────────────────── */}
            <div style={s.formActions}>
              <button type="button" style={s.cancelBtn} onClick={handleCancel} disabled={loading}>
                Cancel
              </button>
              <button
                type="button"
                style={{ ...s.submitBtn, ...(loading ? s.submitBtnDisabled : {}) }}
                onClick={handleSubmit}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <div style={s.btnSpinner} />
                    Posting…
                  </>
                ) : (
                  <>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="22" y1="2" x2="11" y2="13"/>
                      <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                    Post Favor
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* ── RIGHT: PREVIEW + TIPS + BARANGAY ─────────────────────── */}
        <div style={s.rightCol}>
          {/* Live Preview */}
          <div style={s.sideCard}>
            <div style={s.previewHeader}>
              <span style={s.sideCardTitle}>Preview</span>
              <span style={s.previewBadge}>Updates as you type</span>
            </div>

            {/* Mini card preview */}
            <div style={s.previewCard}>
              <div style={s.previewIconBox}>
                <span style={{ fontSize: '16px' }}>{CATEGORY_ICONS[form.category] || '📦'}</span>
              </div>
              <div style={s.previewContent}>
                <div style={s.previewTopRow}>
                  <span style={s.previewTitle}>{previewTitle}</span>
                  <span style={s.previewStatusBadge}>OPEN</span>
                </div>
                <p style={s.previewDesc}>{previewDesc}</p>
                <div style={s.previewMeta}>
                  <div style={s.previewAvatar}>
                    {(user?.name || 'Y').charAt(0).toUpperCase()}
                  </div>
                  <span style={s.previewMetaText}>You</span>
                  <span style={s.previewMetaDot}>·</span>
                  <span style={s.previewMetaText}>{user?.barangay || 'Your Barangay'}</span>
                  <span style={s.previewMetaDot}>·</span>
                  <span style={s.previewMetaText}>Just now</span>
                </div>
              </div>
              <span style={{ ...s.previewCatTag, background: tagColor.bg, color: tagColor.text }}>
                {form.category}
              </span>
            </div>

            {/* Title char progress bar */}
            <div style={s.progressBarBg}>
              <div style={{ ...s.progressBarFill, width: `${(form.title.length / 200) * 100}%` }} />
            </div>
            <span style={s.progressLabel}>{form.title.length} / 200</span>
          </div>

          {/* Tips */}
          <div style={s.sideCard}>
            <p style={s.tipsTitle}>✏️ Tips for a great favor post</p>
            <div style={s.tipsList}>
              {TIPS.map((tip, i) => (
                <div key={i} style={s.tipItem}>
                  <span style={s.tipIcon}>{tip.icon}</span>
                  <span style={s.tipText}>{tip.text}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Your Barangay */}
          <div style={s.sideCard}>
            <div style={s.barangayHeader}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="#C8601A" stroke="none">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
              </svg>
              <span style={s.barangayLabel}>Your Barangay</span>
            </div>
            <p style={s.barangayName}>{user?.barangay || 'Not set'}</p>
            <p style={s.barangayInfo}>Your favor will only be visible to residents of your barangay.</p>
            <div style={s.barangayNeighbors}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#aaa" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                <path d="M16 3.13a4 4 0 010 7.75"/>
              </svg>
              <span style={s.barangayNeighborsText}>~120 active neighbors can see this</span>
            </div>

            {/* Description char progress bar */}
            <div style={{ marginTop: '12px' }}>
              <div style={s.progressBarBg}>
                <div style={{ ...s.progressBarFill, width: `${(form.description.length / 1000) * 100}%` }} />
              </div>
              <span style={s.progressLabel}>{form.description.length} / 1000</span>
            </div>
          </div>
        </div>
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
    gap: '28px',
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '40px 32px',
    alignItems: 'flex-start',
  },

  // Form column
  formCol: { flex: 1, minWidth: 0 },

  breadcrumb: {
    display: 'flex',
    alignItems: 'center',
    fontSize: '13px',
    marginBottom: '16px',
  },
  breadcrumbLink: {
    color: '#C8601A',
    cursor: 'pointer',
    fontWeight: '500',
  },
  breadcrumbArrow: { color: '#aaa', margin: '0 4px' },
  breadcrumbCurrent: { color: '#888' },

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

  successBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '14px 16px',
    background: '#E8F5E9',
    border: '1px solid #A5D6A7',
    borderRadius: '10px',
    color: '#2E7D32',
    fontSize: '14px',
    marginBottom: '16px',
  },
  errorBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '14px 16px',
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '10px',
    color: '#e53935',
    fontSize: '14px',
    marginBottom: '16px',
  },

  formCard: {
    background: 'white',
    borderRadius: '20px',
    padding: '28px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
  },
  fieldSection: { marginBottom: '4px' },
  fieldDivider: {
    height: '1px',
    background: '#f5f5f5',
    margin: '20px 0',
  },
  fieldHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'baseline',
    marginBottom: '6px',
  },
  fieldTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#1a1a1a',
  },
  fieldHint: {
    fontSize: '12px',
    color: '#aaa',
  },
  input: {
    width: '100%',
    height: '44px',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '0 14px',
    fontSize: '14px',
    color: '#333',
    background: 'white',
    outline: 'none',
    boxSizing: 'border-box',
  },
  inputError: {
    borderColor: '#ffcdd2',
    background: '#fff5f5',
  },
  textarea: {
    width: '100%',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '12px 14px',
    fontSize: '14px',
    color: '#333',
    background: 'white',
    outline: 'none',
    boxSizing: 'border-box',
    resize: 'vertical',
    lineHeight: '1.6',
    fontFamily: "'Segoe UI', sans-serif",
  },
  fieldFooter: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '4px',
    minHeight: '18px',
  },
  errorText: {
    fontSize: '12px',
    color: '#e53935',
  },
  charCount: {
    fontSize: '11px',
    color: '#bbb',
    marginLeft: 'auto',
  },

  // Category pills
  categoryGrid: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
  },
  catPill: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '7px 14px',
    borderRadius: '20px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    fontSize: '13px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
    transition: 'all 0.15s',
  },
  catPillActive: {
    border: '1.5px solid #C8601A',
    background: '#FFF3E0',
    color: '#C8601A',
    fontWeight: '600',
  },

  // Date picker
  dateWrapper: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '0 14px',
    height: '44px',
    background: 'white',
  },
  dateInput: {
    flex: 1,
    border: 'none',
    outline: 'none',
    fontSize: '14px',
    color: '#333',
    background: 'transparent',
    fontFamily: "'Segoe UI', sans-serif",
  },
  optionalBadge: {
    fontSize: '11px',
    color: '#aaa',
    background: '#f5f5f5',
    padding: '2px 8px',
    borderRadius: '6px',
  },

  // Form actions
  formActions: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: '4px',
  },
  cancelBtn: {
    padding: '10px 24px',
    borderRadius: '10px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    fontSize: '14px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
  },
  submitBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 28px',
    borderRadius: '10px',
    border: 'none',
    background: '#C8601A',
    color: 'white',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  submitBtnDisabled: {
    background: '#d4956a',
    cursor: 'not-allowed',
  },
  btnSpinner: {
    width: '14px',
    height: '14px',
    border: '2px solid rgba(255,255,255,0.4)',
    borderTop: '2px solid white',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
  },

  // Right column
  rightCol: {
    width: '480px',
    flexShrink: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  sideCard: {
    background: 'white',
    borderRadius: '16px',
    padding: '20px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.05)',
  },
  sideCardTitle: {
    fontSize: '15px',
    fontWeight: '700',
    color: '#1a1a1a',
  },

  // Preview
  previewHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '14px',
  },
  previewBadge: {
    fontSize: '11px',
    color: '#C8601A',
    background: '#FFF3E0',
    padding: '3px 9px',
    borderRadius: '20px',
    fontWeight: '500',
  },
  previewCard: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '10px',
    padding: '14px',
    border: '1.5px solid #f0ece6',
    borderRadius: '12px',
    marginBottom: '12px',
    background: '#FDFBF8',
  },
  previewIconBox: {
    width: '36px',
    height: '36px',
    borderRadius: '10px',
    background: '#FFF3E0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  previewContent: { flex: 1, minWidth: 0 },
  previewTopRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '6px',
    marginBottom: '2px',
  },
  previewTitle: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#1a1a1a',
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  previewStatusBadge: {
    fontSize: '10px',
    fontWeight: '700',
    background: '#FFF3E0',
    color: '#E65100',
    padding: '2px 7px',
    borderRadius: '20px',
    flexShrink: 0,
  },
  previewDesc: {
    fontSize: '12px',
    color: '#888',
    margin: '2px 0 6px 0',
    lineHeight: '1.4',
  },
  previewMeta: {
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  },
  previewAvatar: {
    width: '18px',
    height: '18px',
    borderRadius: '50%',
    background: '#C8601A',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'white',
    fontSize: '8px',
    fontWeight: '700',
    flexShrink: 0,
  },
  previewMetaText: { fontSize: '11px', color: '#aaa' },
  previewMetaDot:  { fontSize: '11px', color: '#ddd' },
  previewCatTag: {
    fontSize: '10px',
    fontWeight: '600',
    padding: '2px 8px',
    borderRadius: '20px',
    flexShrink: 0,
    alignSelf: 'flex-start',
  },

  // Progress bar
  progressBarBg: {
    height: '4px',
    background: '#f0ece6',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    background: '#C8601A',
    borderRadius: '4px',
    transition: 'width 0.2s',
  },
  progressLabel: {
    fontSize: '11px',
    color: '#bbb',
    display: 'block',
    textAlign: 'right',
    marginTop: '4px',
  },

  // Tips
  tipsTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 12px 0',
  },
  tipsList: { display: 'flex', flexDirection: 'column', gap: '10px' },
  tipItem: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '10px',
  },
  tipIcon: { fontSize: '15px', flexShrink: 0, marginTop: '1px' },
  tipText: { fontSize: '13px', color: '#555', lineHeight: '1.4' },

  // Barangay
  barangayHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    marginBottom: '6px',
  },
  barangayLabel: {
    fontSize: '12px',
    color: '#888',
    fontWeight: '500',
  },
  barangayName: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1a1a1a',
    margin: '0 0 4px 0',
  },
  barangayInfo: {
    fontSize: '12px',
    color: '#aaa',
    margin: '0 0 8px 0',
    lineHeight: '1.4',
  },
  barangayNeighbors: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  barangayNeighborsText: {
    fontSize: '12px',
    color: '#aaa',
  },
};

export default CreateFavorPage;