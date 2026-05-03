import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import Navbar from '../components/Navbar';

// ── Helpers ───────────────────────────────────────────────────────────────────
function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });
}

const CATEGORIES = ['Event', 'Health', 'Reminder', 'General'];

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

const EMPTY_FORM = { title: '', content: '', category: 'General', isPinned: false };
const PAGE_SIZE  = 8;

// ══════════════════════════════════════════════════════════════════════════════
const AdminAnnouncementsPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // ── Guard: redirect non-admins immediately ─────────────────────────────────
  useEffect(() => {
    if (user && user.role !== 'ROLE_ADMIN') {
      navigate('/announcements', { replace: true });
    }
  }, [user, navigate]);

  // ── List state ─────────────────────────────────────────────────────────────
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState('');
  const [currentPage, setCurrentPage]     = useState(0);
  const [totalPages, setTotalPages]       = useState(1);

  // ── Modal state ────────────────────────────────────────────────────────────
  const [modalOpen, setModalOpen]     = useState(false);
  const [editTarget, setEditTarget]   = useState(null); // null = create, obj = edit
  const [form, setForm]               = useState(EMPTY_FORM);
  const [formErrors, setFormErrors]   = useState({});
  const [saving, setSaving]           = useState(false);
  const [saveError, setSaveError]     = useState('');

  // ── Delete confirm state ───────────────────────────────────────────────────
  const [deleteConfirm, setDeleteConfirm] = useState(null); // id being confirmed
  const [deleting, setDeleting]           = useState(false);

  // ── Fetch list ─────────────────────────────────────────────────────────────
  const fetchAnnouncements = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const res  = await api.get('/announcements', { params: { page, size: PAGE_SIZE } });
      const data = res.data?.data;
      setAnnouncements(data?.content || data || []);
      setTotalPages(data?.totalPages ?? 1);
    } catch {
      setError('Could not load announcements.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnnouncements(0);
  }, [fetchAnnouncements]);

  // ── Open modal ─────────────────────────────────────────────────────────────
  const openCreate = () => {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setFormErrors({});
    setSaveError('');
    setModalOpen(true);
  };

  const openEdit = (ann) => {
    setEditTarget(ann);
    setForm({
      title:    ann.title    || '',
      content:  ann.content  || '',
      category: ann.category || 'General',
      isPinned: ann.isPinned ?? false,
    });
    setFormErrors({});
    setSaveError('');
    setModalOpen(true);
  };

  const closeModal = () => {
    if (saving) return;
    setModalOpen(false);
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setFormErrors({});
    setSaveError('');
  };

  // ── Form change ────────────────────────────────────────────────────────────
  const handleFormChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (formErrors[field]) setFormErrors(prev => ({ ...prev, [field]: '' }));
    setSaveError('');
  };

  // ── Validate ───────────────────────────────────────────────────────────────
  const validate = () => {
    const errs = {};
    if (!form.title.trim())   errs.title   = 'Title is required.';
    if (!form.content.trim()) errs.content = 'Content is required.';
    if (!form.category)       errs.category = 'Category is required.';
    return errs;
  };

  // ── Save (create or update) ────────────────────────────────────────────────
  const handleSave = async () => {
    const errs = validate();
    if (Object.keys(errs).length > 0) { setFormErrors(errs); return; }

    setSaving(true);
    setSaveError('');
    try {
      const payload = {
        title:    form.title.trim(),
        content:  form.content.trim(),
        category: form.category,
        isPinned: form.isPinned,
      };
      if (editTarget) {
        const res = await api.put(`/announcements/${editTarget.id}`, payload);
        const updated = res.data?.data || res.data;
        setAnnouncements(prev =>
          prev.map(a => a.id === editTarget.id ? updated : a)
        );
      } else {
        const res = await api.post('/announcements', payload);
        const created = res.data?.data || res.data;
        // Prepend so newest is first
        setAnnouncements(prev => [created, ...prev]);
      }
      closeModal();
    } catch (err) {
      setSaveError(
        err.response?.data?.error?.message ||
        err.response?.data?.message ||
        'Could not save the announcement. Please try again.'
      );
    } finally {
      setSaving(false);
    }
  };

  // ── Delete ─────────────────────────────────────────────────────────────────
  const handleDelete = async (id) => {
    setDeleting(true);
    try {
      await api.delete(`/announcements/${id}`);
      setAnnouncements(prev => prev.filter(a => a.id !== id));
      setDeleteConfirm(null);
    } catch (err) {
      alert(
        err.response?.data?.error?.message ||
        'Could not delete this announcement.'
      );
    } finally {
      setDeleting(false);
    }
  };

  // ── Page change ────────────────────────────────────────────────────────────
  const handlePageChange = (page) => {
    if (page < 0 || page >= totalPages) return;
    setCurrentPage(page);
    fetchAnnouncements(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ── Don't render if not admin ──────────────────────────────────────────────
  if (!user || user.role !== 'ROLE_ADMIN') return null;

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div style={s.page}>
      <Navbar />

      <div style={s.content}>

        {/* Page heading */}
        <div style={s.pageHeading}>
          <div>
            <h1 style={s.pageTitle}>📢 Manage Announcements</h1>
            <p style={s.pageSubtitle}>
              Create and manage announcements for {user?.barangay || 'your barangay'}.
            </p>
          </div>
          <div style={s.headingRight}>
            <a href="/announcements" style={s.residentViewLink}>← Resident view</a>
            <button style={s.newBtn} onClick={openCreate}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                   stroke="white" strokeWidth="2.5"
                   strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              New Announcement
            </button>
          </div>
        </div>

        {/* Table card */}
        <div style={s.tableCard}>
          {loading ? (
            <LoadingBox />
          ) : error ? (
            <div style={s.errorBox}>{error}</div>
          ) : announcements.length === 0 ? (
            <EmptyBox onNew={openCreate} />
          ) : (
            <>
              <table style={s.table}>
                <thead>
                  <tr style={s.thead}>
                    <th style={{ ...s.th, width: '40%' }}>Title</th>
                    <th style={s.th}>Category</th>
                    <th style={s.th}>Date Posted</th>
                    <th style={s.th}>Status</th>
                    <th style={{ ...s.th, textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {announcements.map((ann, i) => {
                    const tagColor = CATEGORY_TAG_COLORS[ann.category] || CATEGORY_TAG_COLORS.General;
                    const isConfirmingDelete = deleteConfirm === ann.id;
                    return (
                      <tr
                        key={ann.id}
                        style={{
                          ...s.tr,
                          background: i % 2 === 0 ? 'white' : '#FDFBF8',
                        }}
                      >
                        {/* Title */}
                        <td style={s.td}>
                          <div style={s.titleCell}>
                            {ann.isPinned && (
                              <span style={s.pinnedBadge} title="Pinned">📌</span>
                            )}
                            <span style={s.titleText}>{ann.title}</span>
                          </div>
                        </td>

                        {/* Category */}
                        <td style={s.td}>
                          <span style={{
                            ...s.categoryTag,
                            background: tagColor.bg,
                            color: tagColor.text,
                          }}>
                            {ann.category}
                          </span>
                        </td>

                        {/* Date */}
                        <td style={s.td}>
                          <span style={s.dateText}>{formatDate(ann.createdAt)}</span>
                        </td>

                        {/* Status */}
                        <td style={s.td}>
                          <span style={ann.isPinned ? s.statusPinned : s.statusPublished}>
                            {ann.isPinned ? 'Pinned' : 'Published'}
                          </span>
                        </td>

                        {/* Actions */}
                        <td style={{ ...s.td, textAlign: 'right' }}>
                          {isConfirmingDelete ? (
                            <div style={s.deleteConfirmRow}>
                              <span style={s.deleteConfirmText}>Delete?</span>
                              <button
                                style={s.deleteConfirmBtn}
                                onClick={() => handleDelete(ann.id)}
                                disabled={deleting}
                              >
                                {deleting ? 'Deleting…' : 'Yes'}
                              </button>
                              <button
                                style={s.cancelBtn}
                                onClick={() => setDeleteConfirm(null)}
                                disabled={deleting}
                              >
                                Cancel
                              </button>
                            </div>
                          ) : (
                            <div style={s.actionRow}>
                              <button
                                style={s.editBtn}
                                onClick={() => openEdit(ann)}
                              >
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="2"
                                     strokeLinecap="round" strokeLinejoin="round">
                                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                                Edit
                              </button>
                              <button
                                style={s.deleteBtn}
                                onClick={() => setDeleteConfirm(ann.id)}
                              >
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="2"
                                     strokeLinecap="round" strokeLinejoin="round">
                                  <polyline points="3 6 5 6 21 6"/>
                                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                                  <path d="M10 11v6M14 11v6"/>
                                  <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
                                </svg>
                                Delete
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {/* Pagination */}
              {totalPages > 1 && (
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
            </>
          )}
        </div>
      </div>

      {/* ══ MODAL ═════════════════════════════════════════════════════════ */}
      {modalOpen && (
        <div style={s.modalOverlay} onClick={(e) => {
          if (e.target === e.currentTarget) closeModal();
        }}>
          <div style={s.modal}>

            {/* Modal header */}
            <div style={s.modalHeader}>
              <h2 style={s.modalTitle}>
                {editTarget ? 'Edit Announcement' : 'New Announcement'}
              </h2>
              <button style={s.closeBtn} onClick={closeModal} disabled={saving}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="#555" strokeWidth="2"
                     strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div style={s.modalBody}>

              {/* Save error */}
              {saveError && (
                <div style={s.inlineError}>{saveError}</div>
              )}

              {/* Title */}
              <div style={s.fieldGroup}>
                <label style={s.fieldLabel}>Title <span style={s.required}>*</span></label>
                <input
                  type="text"
                  value={form.title}
                  onChange={e => handleFormChange('title', e.target.value)}
                  placeholder="e.g. Barangay Clean-Up Drive this Saturday"
                  style={{ ...s.fieldInput, ...(formErrors.title ? s.fieldInputError : {}) }}
                  maxLength={200}
                />
                {formErrors.title && <span style={s.fieldError}>{formErrors.title}</span>}
                <span style={s.charCount}>{form.title.length} / 200</span>
              </div>

              {/* Category */}
              <div style={s.fieldGroup}>
                <label style={s.fieldLabel}>Category <span style={s.required}>*</span></label>
                <div style={s.categoryPills}>
                  {CATEGORIES.map(cat => (
                    <button
                      key={cat}
                      type="button"
                      style={{
                        ...s.catPill,
                        ...(form.category === cat ? s.catPillActive : {}),
                      }}
                      onClick={() => handleFormChange('category', cat)}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
                {formErrors.category && <span style={s.fieldError}>{formErrors.category}</span>}
              </div>

              {/* Content */}
              <div style={s.fieldGroup}>
                <label style={s.fieldLabel}>Content <span style={s.required}>*</span></label>
                <textarea
                  value={form.content}
                  onChange={e => handleFormChange('content', e.target.value)}
                  placeholder="Write the announcement details here..."
                  style={{ ...s.fieldTextarea, ...(formErrors.content ? s.fieldInputError : {}) }}
                  rows={6}
                  maxLength={2000}
                />
                {formErrors.content && <span style={s.fieldError}>{formErrors.content}</span>}
                <span style={s.charCount}>{form.content.length} / 2000</span>
              </div>

              {/* Pin toggle */}
              <div style={s.pinRow}>
                <div>
                  <div style={s.pinLabel}>📌 Pin this announcement</div>
                  <div style={s.pinHint}>
                    Pinned announcements appear highlighted in the resident sidebar.
                    Only one can be pinned at a time.
                  </div>
                </div>
                <div
                  style={{
                    ...s.toggleSwitch,
                    background: form.isPinned ? '#C8601A' : '#e0e0e0',
                  }}
                  onClick={() => handleFormChange('isPinned', !form.isPinned)}
                >
                  <div style={{
                    ...s.toggleThumb,
                    transform: form.isPinned ? 'translateX(18px)' : 'translateX(2px)',
                  }} />
                </div>
              </div>
            </div>

            {/* Modal footer */}
            <div style={s.modalFooter}>
              <button style={s.cancelFooterBtn} onClick={closeModal} disabled={saving}>
                Cancel
              </button>
              <button
                style={{ ...s.saveBtn, opacity: saving ? 0.7 : 1 }}
                onClick={handleSave}
                disabled={saving}
              >
                {saving ? (
                  <span style={s.saveBtnInner}>
                    <div style={s.btnSpinner} />
                    {editTarget ? 'Saving…' : 'Publishing…'}
                  </span>
                ) : (
                  <span style={s.saveBtnInner}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                         stroke="white" strokeWidth="2.5"
                         strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    {editTarget ? 'Save Changes' : 'Publish Announcement'}
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Sub-components ────────────────────────────────────────────────────────────
const LoadingBox = () => (
  <div style={{ display: 'flex', justifyContent: 'center', padding: '60px 0', gap: '10px', alignItems: 'center' }}>
    <div style={{ width: '24px', height: '24px', border: '3px solid #f0ece6', borderTop: '3px solid #C8601A', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
    <span style={{ fontSize: '13px', color: '#aaa' }}>Loading announcements…</span>
  </div>
);

const EmptyBox = ({ onNew }) => (
  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '60px 0', gap: '12px' }}>
    <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#ddd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
      <path d="M13.73 21a2 2 0 01-3.46 0"/>
    </svg>
    <p style={{ fontSize: '14px', color: '#aaa', margin: 0 }}>No announcements yet.</p>
    <button style={{ padding: '9px 20px', borderRadius: '10px', border: 'none', background: '#C8601A', color: 'white', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }} onClick={onNew}>
      Create your first announcement
    </button>
  </div>
);

// ── Styles ────────────────────────────────────────────────────────────────────
const s = {
  page: { minHeight: '100vh', background: '#FAF7F2', fontFamily: "'Segoe UI', sans-serif" },
  content: { maxWidth: '1100px', margin: '0 auto', padding: '40px 32px' },

  // Heading
  pageHeading: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '28px',
    flexWrap: 'wrap',
    gap: '16px',
  },
  pageTitle: { fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: '0 0 4px 0' },
  pageSubtitle: { fontSize: '14px', color: '#888', margin: 0 },
  headingRight: { display: 'flex', alignItems: 'center', gap: '12px' },
  residentViewLink: {
    fontSize: '13px',
    color: '#888',
    textDecoration: 'none',
    fontWeight: '500',
  },
  newBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 20px',
    borderRadius: '10px',
    border: 'none',
    background: '#C8601A',
    color: 'white',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },

  // Table card
  tableCard: {
    background: 'white',
    borderRadius: '20px',
    boxShadow: '0 2px 16px rgba(0,0,0,0.05)',
    overflow: 'hidden',
  },
  table: { width: '100%', borderCollapse: 'collapse' },
  thead: { borderBottom: '2px solid #f0ece6' },
  th: {
    padding: '14px 18px',
    fontSize: '12px',
    fontWeight: '700',
    color: '#aaa',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    textAlign: 'left',
  },
  tr: { borderBottom: '1px solid #f5f5f5' },
  td: { padding: '16px 18px', verticalAlign: 'middle' },

  // Table cell content
  titleCell: { display: 'flex', alignItems: 'center', gap: '8px' },
  pinnedBadge: { fontSize: '14px', flexShrink: 0 },
  titleText: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a' },
  categoryTag: {
    display: 'inline-block',
    fontSize: '11px',
    fontWeight: '600',
    padding: '3px 10px',
    borderRadius: '20px',
  },
  dateText: { fontSize: '13px', color: '#666' },
  statusPublished: {
    display: 'inline-block',
    fontSize: '11px',
    fontWeight: '700',
    padding: '3px 10px',
    borderRadius: '20px',
    background: '#E8F5E9',
    color: '#2E7D32',
  },
  statusPinned: {
    display: 'inline-block',
    fontSize: '11px',
    fontWeight: '700',
    padding: '3px 10px',
    borderRadius: '20px',
    background: '#FFF3E0',
    color: '#C8601A',
  },

  // Actions
  actionRow: { display: 'flex', justifyContent: 'flex-end', gap: '8px' },
  editBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '5px',
    padding: '7px 14px',
    borderRadius: '8px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    color: '#555',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  deleteBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '5px',
    padding: '7px 14px',
    borderRadius: '8px',
    border: '1.5px solid #ffcdd2',
    background: 'white',
    color: '#e53935',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  deleteConfirmRow: { display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '8px' },
  deleteConfirmText: { fontSize: '13px', color: '#e53935', fontWeight: '500' },
  deleteConfirmBtn: {
    padding: '6px 14px',
    borderRadius: '8px',
    border: 'none',
    background: '#e53935',
    color: 'white',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  cancelBtn: {
    padding: '6px 14px',
    borderRadius: '8px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    color: '#555',
    fontSize: '13px',
    fontWeight: '500',
    cursor: 'pointer',
  },

  // Pagination
  pagination: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '6px',
    padding: '20px',
    borderTop: '1px solid #f5f5f5',
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

  // Error
  errorBox: {
    padding: '16px',
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '12px',
    color: '#e53935',
    fontSize: '14px',
    margin: '20px',
  },

  // ── Modal ──────────────────────────────────────────────────────────────────
  modalOverlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.40)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 500,
    padding: '20px',
  },
  modal: {
    background: 'white',
    borderRadius: '20px',
    width: '100%',
    maxWidth: '580px',
    maxHeight: '90vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 20px 60px rgba(0,0,0,0.20)',
    overflow: 'hidden',
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '22px 24px',
    borderBottom: '1px solid #f5f5f5',
  },
  modalTitle: { fontSize: '18px', fontWeight: '700', color: '#1a1a1a', margin: 0 },
  closeBtn: {
    width: '32px',
    height: '32px',
    borderRadius: '8px',
    border: 'none',
    background: '#f5f5f5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
    flexShrink: 0,
  },
  modalBody: {
    padding: '24px',
    overflowY: 'auto',
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  modalFooter: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '10px',
    padding: '18px 24px',
    borderTop: '1px solid #f5f5f5',
  },

  // Form fields
  fieldGroup: { display: 'flex', flexDirection: 'column', gap: '4px' },
  fieldLabel: { fontSize: '13px', fontWeight: '600', color: '#333' },
  required: { color: '#e53935' },
  fieldInput: {
    height: '44px',
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '0 14px',
    fontSize: '14px',
    color: '#333',
    outline: 'none',
    background: 'white',
    fontFamily: "'Segoe UI', sans-serif",
  },
  fieldInputError: { borderColor: '#ffcdd2', background: '#fff5f5' },
  fieldTextarea: {
    border: '1.5px solid #e8e8e8',
    borderRadius: '10px',
    padding: '12px 14px',
    fontSize: '14px',
    color: '#333',
    outline: 'none',
    background: 'white',
    resize: 'vertical',
    lineHeight: '1.6',
    fontFamily: "'Segoe UI', sans-serif",
  },
  fieldError: { fontSize: '12px', color: '#e53935' },
  charCount: { fontSize: '11px', color: '#bbb', textAlign: 'right' },
  inlineError: {
    background: '#fff5f5',
    border: '1px solid #ffcdd2',
    borderRadius: '10px',
    padding: '10px 14px',
    fontSize: '13px',
    color: '#e53935',
  },

  // Category pills
  categoryPills: { display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '4px' },
  catPill: {
    padding: '7px 16px',
    borderRadius: '20px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    fontSize: '13px',
    color: '#555',
    cursor: 'pointer',
    fontWeight: '500',
  },
  catPillActive: {
    border: '1.5px solid #C8601A',
    background: '#FFF3E0',
    color: '#C8601A',
    fontWeight: '600',
  },

  // Pin toggle
  pinRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '16px',
    padding: '16px',
    background: '#FAF7F2',
    borderRadius: '12px',
  },
  pinLabel: { fontSize: '14px', fontWeight: '600', color: '#1a1a1a', marginBottom: '4px' },
  pinHint: { fontSize: '12px', color: '#888', lineHeight: '1.4' },
  toggleSwitch: {
    width: '42px',
    height: '24px',
    borderRadius: '12px',
    cursor: 'pointer',
    position: 'relative',
    transition: 'background 0.2s',
    flexShrink: 0,
  },
  toggleThumb: {
    position: 'absolute',
    top: '3px',
    width: '18px',
    height: '18px',
    borderRadius: '50%',
    background: 'white',
    transition: 'transform 0.2s',
    boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
  },

  // Modal footer buttons
  cancelFooterBtn: {
    padding: '10px 20px',
    borderRadius: '10px',
    border: '1.5px solid #e8e8e8',
    background: 'white',
    color: '#555',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  saveBtn: {
    padding: '10px 24px',
    borderRadius: '10px',
    border: 'none',
    background: '#C8601A',
    color: 'white',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveBtnInner: { display: 'flex', alignItems: 'center', gap: '8px' },
  btnSpinner: {
    width: '14px',
    height: '14px',
    border: '2px solid rgba(255,255,255,0.4)',
    borderTop: '2px solid white',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
    flexShrink: 0,
  },
};

export default AdminAnnouncementsPage;