import { useEffect, useRef, useState } from 'react';
import { api } from '../../api/client';
import type { Wifi as WifiInfo } from '../../api/types';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import { Kjell } from '../../components/Kjell';
import type { GuestCtx } from '../context';

/** 6 · Wi-Fi. The password is fetched only here, only after check-in, and never stored. */
export function Wifi({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const [wifi, setWifi] = useState<WifiInfo | null>(null);
  const [copied, setCopied] = useState(false);
  const timer = useRef<number>(undefined);
  const checkedIn = ctx.guest.checkedIn;

  useEffect(() => {
    if (checkedIn) ctx.run(api.wifi()).then((w) => w && setWifi(w));
    return () => window.clearTimeout(timer.current);
  }, [checkedIn]);

  if (!checkedIn) {
    return (
      <section className="section" data-screen-label="06 Wi-Fi">
        <div className="card empty">
          <Kjell />
          <span className="serif">{d.checkInFirst}</span>
          <p>{d.kjellGuards}</p>
          <button className="btn btn-primary" onClick={() => ctx.go('welcome')}>{d.toCheckIn}</button>
        </div>
      </section>
    );
  }

  const copy = async () => {
    if (!wifi) return;
    try {
      await navigator.clipboard.writeText(wifi.password);
    } catch {
      // clipboard blocked (e.g. http) – the password is still on screen
    }
    setCopied(true);
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section className="section" data-screen-label="06 Wi-Fi">
      <div className="card wifi">
        <div className="wifi__group">
          <span className="eyebrow">{d.network}</span>
          <span className="wifi__net">{wifi?.network ?? '…'}</span>
        </div>
        <div className="wifi__group">
          <span className="eyebrow">{d.password}</span>
          <div className="wifi__pw">{wifi?.password ?? '…'}</div>
        </div>
        <button className={copied ? 'btn copied' : 'btn btn-primary'} onClick={copy} disabled={!wifi}>
          <Icon name={copied ? 'check' : 'content_copy'} />{copied ? d.copied : d.copy}
        </button>
        <div className="only-you"><Icon name="visibility_lock" />{d.onlyYou}</div>
      </div>
    </section>
  );
}
