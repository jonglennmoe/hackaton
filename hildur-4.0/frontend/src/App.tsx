import { useEffect, useState } from 'react';
import { api, session, setUnauthorizedHandler, type StoredSession } from './api/client';
import type { LoginResult } from './api/types';
import { LoginPage } from './LoginPage';
import { GuestApp } from './guest/GuestApp';
import { StaffDashboard } from './staff/StaffDashboard';

/**
 * Top-level switch between the three "worlds": login, guest app and staff view.
 * Which one you see depends only on the role of the token you hold.
 */
export function App() {
  const [auth, setAuth] = useState<StoredSession | null>(() => session.load());
  const [loginTab, setLoginTab] = useState<'guest' | 'staff'>('guest');

  useEffect(() => {
    // If the server forgets our token (e.g. it restarted), go back to login.
    setUnauthorizedHandler(() => {
      session.save(null);
      setAuth(null);
    });
  }, []);

  const onLogin = (result: LoginResult) => {
    const next = { token: result.token, role: result.role };
    session.save(next);
    setAuth(next);
    window.scrollTo(0, 0);
  };

  const onLogout = () => {
    const wasStaff = auth?.role === 'STAFF';
    api.logout().catch(() => {});
    session.save(null);
    setAuth(null);
    setLoginTab(wasStaff ? 'staff' : 'guest');
    window.scrollTo(0, 0);
  };

  if (auth?.role === 'GUEST') return <GuestApp onLogout={onLogout} />;
  if (auth?.role === 'STAFF') return <StaffDashboard onLogout={onLogout} />;
  return <LoginPage initialTab={loginTab} onLogin={onLogin} />;
}
