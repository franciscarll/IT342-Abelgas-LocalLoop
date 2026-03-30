import React from 'react';

const LeftPanel = () => {
  return (
    <div style={styles.panel}>
      {/* Background decorative circles */}
      <div style={styles.circleTopRight} />
      <div style={styles.circleBottomLeft} />

      {/* Wave shape at bottom */}
      <div style={styles.waveContainer}>
        <svg viewBox="0 0 640 120" preserveAspectRatio="none" style={styles.wave}>
          <path
            d="M0,60 C160,120 480,0 640,60 L640,120 L0,120 Z"
            fill="rgba(0,0,0,0.08)"
          />
          <path
            d="M0,80 C200,20 440,100 640,50 L640,120 L0,120 Z"
            fill="rgba(0,0,0,0.06)"
          />
        </svg>
      </div>

      {/* Center content */}
      <div style={styles.content}>
        {/* Logo */}
        <div style={styles.logoContainer}>
          <svg width="72" height="56" viewBox="0 0 72 56" fill="none">
            {/* Left pin */}
            <path
              d="M24 4C16.268 4 10 10.268 10 18C10 26.5 20 38 24 42C28 38 38 26.5 38 18C38 10.268 31.732 4 24 4Z"
              fill="white"
              fillOpacity="0.9"
            />
            <circle cx="24" cy="18" r="5" fill="#C8601A" />
            {/* Right pin with heart */}
            <path
              d="M48 8C41.373 8 36 13.373 36 20C36 27.5 44.5 38 48 42C51.5 38 60 27.5 60 20C60 13.373 54.627 8 48 8Z"
              fill="white"
              fillOpacity="0.7"
            />
            <path
              d="M48 16.5C48 16.5 45 14 43.5 16C42 18 44 20 48 23C52 20 54 18 52.5 16C51 14 48 16.5 48 16.5Z"
              fill="#C8601A"
            />
          </svg>
        </div>

        <h1 style={styles.appName}>LocalLoop</h1>
        <p style={styles.tagline}>Your barangay, connected.</p>

        <div style={styles.divider} />

        {/* Feature icons */}
        <div style={styles.iconsRow}>
          {[
            {
              label: 'Errands',
              icon: (
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                  <polyline points="9,22 9,12 15,12 15,22" />
                </svg>
              ),
            },
            {
              label: 'Favors',
              icon: (
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
                </svg>
              ),
            },
            {
              label: 'Plant Care',
              icon: (
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22V12" />
                  <path d="M12 12C12 12 7 10 5 6c4 0 7 2 7 6z" />
                  <path d="M12 12C12 12 17 10 19 6c-4 0-7 2-7 6z" />
                  <path d="M12 22c0 0-5-3-5-8" />
                </svg>
              ),
            },
            {
              label: 'Community',
              icon: (
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
                </svg>
              ),
            },
          ].map(({ label, icon }) => (
            <div key={label} style={styles.iconItem}>
              <div style={styles.iconBox}>{icon}</div>
              <span style={styles.iconLabel}>{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const styles = {
  panel: {
    width: '45%',
    minHeight: '100vh',
    background: 'linear-gradient(160deg, #C8742A 0%, #B5621E 40%, #A05518 100%)',
    position: 'relative',
    overflow: 'hidden',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  circleTopRight: {
    position: 'absolute',
    top: '-80px',
    right: '-80px',
    width: '280px',
    height: '280px',
    borderRadius: '50%',
    background: 'rgba(255,255,255,0.08)',
  },
  circleBottomLeft: {
    position: 'absolute',
    bottom: '60px',
    left: '-60px',
    width: '200px',
    height: '200px',
    borderRadius: '50%',
    background: 'rgba(255,255,255,0.06)',
  },
  waveContainer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '120px',
  },
  wave: {
    width: '100%',
    height: '100%',
  },
  content: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    zIndex: 1,
    padding: '40px 20px',
  },
  logoContainer: {
    marginBottom: '16px',
  },
  appName: {
    color: 'white',
    fontSize: '32px',
    fontWeight: '700',
    margin: '0 0 8px 0',
    fontFamily: "'Georgia', serif",
    letterSpacing: '-0.5px',
  },
  tagline: {
    color: 'rgba(255,255,255,0.85)',
    fontSize: '15px',
    margin: '0 0 24px 0',
    fontFamily: "'Georgia', serif",
    fontStyle: 'italic',
  },
  divider: {
    width: '40px',
    height: '2px',
    background: 'rgba(255,255,255,0.4)',
    marginBottom: '32px',
    borderRadius: '2px',
  },
  iconsRow: {
    display: 'flex',
    gap: '24px',
  },
  iconItem: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '8px',
  },
  iconBox: {
    width: '56px',
    height: '56px',
    borderRadius: '14px',
    background: 'rgba(255,255,255,0.15)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backdropFilter: 'blur(4px)',
    border: '1px solid rgba(255,255,255,0.2)',
  },
  iconLabel: {
    color: 'rgba(255,255,255,0.85)',
    fontSize: '11px',
    fontFamily: 'sans-serif',
  },
};

export default LeftPanel;