import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';
import api from '../api/axios';

const NotificationContext = createContext(null);

const STORAGE_KEY = 'll_badge_count';

export const NotificationProvider = ({ children }) => {
  const { user } = useAuth();

  const [badgeCount, setBadgeCount] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 0;
  });

  const persistCount = (count) => {
    setBadgeCount(count);
    localStorage.setItem(STORAGE_KEY, String(count));
  };

  const refreshBadge = useCallback(async () => {
    // Only fetch for logged-in, non-admin residents
    if (!user || user.role === 'ROLE_ADMIN') {
      persistCount(0);
      return;
    }
    try {
      const [postedRes, claimedRes] = await Promise.all([
        api.get('/favors/my-posted', { params: { page: 0, size: 50 } }),
        api.get('/favors/my-claimed', { params: { page: 0, size: 50 } }),
      ]);

      const postedData  = postedRes.data?.data;
      const claimedData = claimedRes.data?.data;

      const postedList  = postedData?.content  || postedData  || [];
      const claimedList = claimedData?.content || claimedData || [];

      const postedClaimedCount  = postedList.filter(f => f.status === 'CLAIMED').length;
      const claimedActiveCount  = claimedList.filter(f => f.status === 'CLAIMED').length;

      persistCount(postedClaimedCount + claimedActiveCount);
    } catch {
      // Silently fail — stale localStorage value remains
    }
  }, [user]);

  // Fetch on mount and whenever the user changes
  useEffect(() => {
    if (!user) {
      persistCount(0);
      return;
    }
    refreshBadge();
  }, [user, refreshBadge]);

  // Clear badge on logout
  useEffect(() => {
    if (!user) {
      persistCount(0);
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [user]);

  return (
    <NotificationContext.Provider value={{ badgeCount, refreshBadge }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);