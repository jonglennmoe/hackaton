import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { GuestState } from '../api/types';
import { useI18n, type Lang } from '../i18n';
import { Icon } from '../components/Icon';
import { HildurFace } from '../components/Hildur';
import { Kjell } from '../components/Kjell';
import type { GuestCtx, Screen } from './context';
import { Welcome } from './screens/Welcome';
import { Sauna, SaunaDone } from './screens/Sauna';
import { Breakfast, BreakfastDone } from './screens/Breakfast';
import { Report, ReportDone } from './screens/Report';
import { Aurora } from './screens/Aurora';
import { Wifi } from './screens/Wifi';
import { MyData } from './screens/MyData';
import { Checkout } from './screens/Checkout';

const CHECK_IN_ANIMATION_MS = 1600;

/** The guest's phone app: sticky header, Hildur's speech bubble, then the current screen. */
export function GuestApp({ onLogout }: { onLogout: () => void }) {
  const { lang, setLang, d, t, err } = useI18n();
  const [guest, setGuest] = useState<GuestState | null>(null);
  const [screen, setScreen] = useState<Screen>('welcome');
  const [cancelled, setCancelled] = useState(false);
  const [checkingIn, setCheckingIn] = useState(false);
  const [langMenu, setLangMenu] = useState(false);
  const [toast, setToast] = useState<{ message: string; kind: 'ok' | 'error' } | null>(null);
  const [rhyme, setRhyme] = useState<'rhyme' | 'nope' | null>(null);
  const rhymed = useRef(false);
  const taps = useRef(0);
  const timers = useRef<Record<string, number>>({});

  const later = (name: string, ms: number, fn: () => void) => {
    window.clearTimeout(timers.current[name]);
    timers.current[name] = window.setTimeout(fn, ms);
  };
  useEffect(() => () => Object.values(timers.current).forEach((id) => window.clearTimeout(id)), []);

  const showToast = useCallback((message: string, kind: 'ok' | 'error' = 'ok') => {
    setToast({ message, kind });
    later('toast', 2600, () => setToast(null));
  }, []);

  const run = useCallback(async <T,>(call: Promise<T>): Promise<T | undefined> => {
    try {
      return await call;
    } catch (e) {
      if (!(e instanceof ApiError && e.code === 'notLoggedIn')) {
        showToast(err(e instanceof ApiError ? e.code : 'genericErr'), 'error');
      }
      return undefined;
    }
  }, [err, showToast]);

  useEffect(() => {
    run(api.me()).then((g) => g && setGuest(g));
  }, [run]);

  const go = (next: Screen, opts?: { cancelled?: boolean }) => {
    setScreen(next);
    setCancelled(!!opts?.cancelled);
    setRhyme(null);
    setLangMenu(false);
    window.scrollTo(0, 0);
  };

  if (!guest) {
    return (
      <div className="guest">
        <main style={{ paddingTop: 80 }}>
          <div className="loading"><Kjell mood="sleepy" /><small>{d.kjellNote}</small></div>
        </main>
      </div>
    );
  }

  const ctx: GuestCtx = { guest, setGuest, go, run, toast: showToast };

  const checkIn = async () => {
    setCheckingIn(true);
    setRhyme(null);
    const started = Date.now();
    const updated = await run(api.checkIn());
    // Let Kjell finish fetching the key – the animation is part of the fun.
    later('checkin', Math.max(0, CHECK_IN_ANIMATION_MS - (Date.now() - started)), () => {
      if (updated) setGuest(updated);
      setCheckingIn(false);
    });
  };

  const tapAvatar = () => {
    taps.current += 1;
    if (taps.current >= 3) {
      taps.current = 0;
      window.clearTimeout(timers.current.tap);
      const already = rhymed.current;
      rhymed.current = true;
      setRhyme(already ? 'nope' : 'rhyme');
      later('rhyme', already ? 3500 : 9000, () => setRhyme(null));
    } else {
      later('tap', 900, () => { taps.current = 0; });
    }
  };

  const lines: Record<Screen, string> = {
    welcome: guest.checkedIn ? t('helloIn') : t('hello', { name: guest.firstName }),
    sauna: cancelled ? t('saunaCancelled') : t('saunaLine'),
    saunaDone: t('saunaDoneLine'),
    breakfast: t('bfLine'), bfDone: t('bfDoneLine'),
    report: t('repLine'), reportDone: t('repDoneLine'),
    aurora: t('auroraLine'),
    wifi: guest.checkedIn ? t('wifiLine') : t('wifiLocked'),
    data: t('dataLine'),
    checkout: guest.checkedOut ? t('byeLine') : t('coLine', { name: guest.firstName })
  };
  const line = rhyme === 'rhyme' ? d.rhyme : rhyme === 'nope' ? d.nope : checkingIn ? d.loadingCheckIn : lines[screen];

  const titles: Record<Screen, string> = {
    welcome: 'Hildur', sauna: d.mSauna, saunaDone: d.mSauna, breakfast: d.mBreakfast, bfDone: d.mBreakfast,
    report: d.mReport, reportDone: d.mReport, aurora: d.mAurora, wifi: d.mWifi, data: d.mData, checkout: d.checkout
  };

  const pickLang = (code: Lang) => { setLang(code); setLangMenu(false); };
  const moreLangs: [string, Lang | null][] = [['Svenska', 'sv'], ['English', 'en'], ['Suomi', null], ['Norsk', null], ['Deutsch', null]];

  return (
    <div className="guest">
      <header className="topbar">
        <div className="topbar__left">
          {screen !== 'welcome' && (
            <button className="back" onClick={() => go('welcome')} aria-label={d.backHome}><Icon name="arrow_back" /></button>
          )}
          <div className="topbar__title">
            <span className="serif">{titles[screen]}</span>
            {screen === 'welcome' && <span className="version-pill">4.0</span>}
          </div>
        </div>
        <div className="lang">
          <div className="seg light" role="group" aria-label="Language">
            <button title="Svenska" className={lang === 'sv' ? 'active' : ''} aria-pressed={lang === 'sv'} onClick={() => pickLang('sv')}>SV</button>
            <button title="English" className={lang === 'en' ? 'active' : ''} aria-pressed={lang === 'en'} onClick={() => pickLang('en')}>EN</button>
            <button title={d.more} className={langMenu ? 'open' : ''} aria-expanded={langMenu} onClick={() => setLangMenu(!langMenu)}>···</button>
          </div>
          {langMenu && (
            <div className="lang__menu">
              {moreLangs.map(([label, code]) => (
                <button key={label} onClick={() => (code ? pickLang(code) : (setLangMenu(false), showToast(d.toastSoon)))}>
                  <b>{label}</b><small>{code ? (lang === code ? '✓' : '') : d.soon}</small>
                </button>
              ))}
            </div>
          )}
        </div>
      </header>

      <div className="speech">
        <button className="hildur" onClick={tapAvatar} aria-label="Hildur">
          <HildurFace size={54} />
        </button>
        <div className="speech__col">
          {rhyme && !checkingIn && <span className="rhyme-tag">{d.rhymeTag}</span>}
          <div className="bubble" aria-live="polite">{line}</div>
        </div>
      </div>

      <main>
        {checkingIn ? (
          <div className="loading">
            <Kjell mood="sleepy" />
            <b>{d.loadingCheckIn}</b>
            <small>{d.kjellNote}</small>
          </div>
        ) : (
          <>
            {screen === 'welcome' && <Welcome ctx={ctx} onCheckIn={checkIn} onLogout={onLogout} />}
            {screen === 'sauna' && <Sauna ctx={ctx} />}
            {screen === 'saunaDone' && <SaunaDone ctx={ctx} />}
            {screen === 'breakfast' && <Breakfast ctx={ctx} />}
            {screen === 'bfDone' && <BreakfastDone ctx={ctx} />}
            {screen === 'report' && <Report ctx={ctx} />}
            {screen === 'reportDone' && <ReportDone ctx={ctx} />}
            {screen === 'aurora' && <Aurora ctx={ctx} />}
            {screen === 'wifi' && <Wifi ctx={ctx} />}
            {screen === 'data' && <MyData ctx={ctx} />}
            {screen === 'checkout' && <Checkout ctx={ctx} />}
          </>
        )}
      </main>

      {toast && (
        <div className={toast.kind === 'error' ? 'toast error' : 'toast'} role="status">
          <Icon name={toast.kind === 'error' ? 'error' : 'check_circle'} />{toast.message}
        </div>
      )}
    </div>
  );
}
