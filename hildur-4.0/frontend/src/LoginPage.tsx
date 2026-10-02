import { useState, type FormEvent } from 'react';
import { api, ApiError } from './api/client';
import type { LoginResult } from './api/types';
import { useI18n } from './i18n';
import { Icon } from './components/Icon';
import { HildurAvatar } from './components/Hildur';
import { DarkLangSwitch } from './components/LangSwitch';

type Tab = 'guest' | 'staff';

/** Demo bookings for the "fill in test details" link, cycled one per tap. */
const DEMO_GUESTS = [['HX-4821', 'Lind'], ['HX-5530', 'Berg'], ['HX-6102', 'Smith']] as const;

/** Separate login for guests (booking number + last name) and staff (username + PIN). */
export function LoginPage({ initialTab, onLogin }: { initialTab: Tab; onLogin: (r: LoginResult) => void }) {
  const { t, err } = useI18n();
  const [tab, setTab] = useState<Tab>(initialTab);
  const [values, setValues] = useState({ booking: '', last: '', user: '', pin: '' });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [demoIdx, setDemoIdx] = useState(0);

  const set = (key: keyof typeof values, value: string) => {
    setValues((v) => ({ ...v, [key]: value }));
    setError(null);
  };

  const fields = tab === 'guest'
    ? [
        { key: 'booking' as const, label: t('fBooking'), type: 'text', placeholder: 'HX-4821', mode: 'text' as const, auto: 'off' },
        { key: 'last' as const, label: t('fLast'), type: 'text', placeholder: 'Lind', mode: 'text' as const, auto: 'family-name' }
      ]
    : [
        { key: 'user' as const, label: t('fStaffId'), type: 'text', placeholder: 'lisa.b', mode: 'text' as const, auto: 'username' },
        { key: 'pin' as const, label: t('fPin'), type: 'password', placeholder: '••••', mode: 'numeric' as const, auto: 'current-password' }
      ];

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    try {
      const result = tab === 'guest'
        ? await api.loginGuest(values.booking, values.last)
        : await api.loginStaff(values.user, values.pin);
      onLogin(result);
    } catch (ex) {
      const code = ex instanceof ApiError ? ex.code : 'genericErr';
      setError(code === 'invalidRequest' ? (tab === 'guest' ? 'errGuest' : 'errStaff') : code);
      setBusy(false);
    }
  };

  const fillDemo = () => {
    setError(null);
    if (tab === 'guest') {
      const [booking, last] = DEMO_GUESTS[demoIdx];
      setValues((v) => ({ ...v, booking, last }));
      setDemoIdx((demoIdx + 1) % DEMO_GUESTS.length);
    } else {
      setValues((v) => ({ ...v, user: 'lisa.b', pin: '2026' }));
    }
  };

  const tabs: [Tab, string, string][] = [['guest', t('tabGuest'), 'luggage'], ['staff', t('tabStaff'), 'badge']];

  return (
    <div className="login" data-screen-label="00 Login">
      <div className="login__glow" />
      <header className="login__header">
        <div className="brand">
          <span className="brand__name">Hildur</span>
          <span className="version-pill on-dark">4.0</span>
        </div>
        <DarkLangSwitch />
      </header>

      <div className="login__intro">
        <HildurAvatar />
        <h1>{tab === 'guest' ? t('gTitle') : t('sTitle')}</h1>
        <p>{tab === 'guest' ? t('gSub') : t('sSub')}</p>
      </div>

      <form className="login__sheet" onSubmit={submit} noValidate>
        <div className="tabs" role="tablist">
          {tabs.map(([key, label, icon]) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} className={tab === key ? 'active' : ''}
              onClick={() => { setTab(key); setError(null); }}>
              <Icon name={icon} />{label}
            </button>
          ))}
        </div>

        {fields.map((f) => (
          <label key={f.key} className="field">
            <span>{f.label}</span>
            <input value={values[f.key]} onChange={(e) => set(f.key, e.target.value)} type={f.type}
              placeholder={f.placeholder} inputMode={f.mode} autoComplete={f.auto} autoCapitalize="off" spellCheck={false} />
          </label>
        ))}

        {error && (
          <div className="alert" role="alert"><Icon name="error" />{err(error)}</div>
        )}

        <button type="submit" className="btn btn-primary lg" disabled={busy}>
          <Icon name="login" />{tab === 'guest' ? t('gBtn') : t('sBtn')}
        </button>
        <button type="button" className="demo-link" onClick={fillDemo}>{t('demo')}</button>
        <div className="login__note"><Icon name="lock" />{t('loginNote')}</div>
      </form>
    </div>
  );
}
