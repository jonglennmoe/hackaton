import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Dashboard } from '../api/types';
import { useI18n } from '../i18n';
import { Icon } from '../components/Icon';
import { Kjell } from '../components/Kjell';
import { DarkLangSwitch } from '../components/LangSwitch';
import { CATEGORIES } from '../guest/screens/Report';

const REFRESH_MS = 4000;

/** Staff view: today's sauna capacity, problem reports with status, and the activity log. */
export function StaffDashboard({ onLogout }: { onLogout: () => void }) {
  const { lang, d, t } = useI18n();
  const [data, setData] = useState<Dashboard | null>(null);

  const refresh = useCallback(() => {
    api.dashboard().then(setData).catch(() => {});
  }, []);

  // Keep the screen live: guests book and report while staff watch.
  useEffect(() => {
    refresh();
    const id = window.setInterval(refresh, REFRESH_MS);
    return () => window.clearInterval(id);
  }, [refresh]);

  const advance = async (id: number) => {
    await api.advanceReport(id).catch(() => {});
    refresh();
  };

  const booked = data?.slots.reduce((sum, s) => sum + s.booked, 0) ?? 0;
  const capacity = data?.slots.reduce((sum, s) => sum + s.capacity, 0) ?? 0;

  return (
    <div className="staff" data-screen-label="09 Staff view">
      <header className="staff__header">
        <div className="staff__brand">
          <span className="brand__name">Hildur</span>
          <span className="version-pill on-dark">4.0</span>
          <small>{d.staffTitle}</small>
        </div>
        <div className="staff__actions">
          {data && <span>{t('signedIn', { staff: data.me.name, role: lang === 'sv' ? data.me.roleSv : data.me.roleEn })}</span>}
          <DarkLangSwitch />
          <button className="staff__logout" onClick={onLogout}><Icon name="logout" />{d.logout}</button>
        </div>
      </header>

      {!data ? (
        <main><div className="loading"><Kjell mood="sleepy" /><small>{d.kjellNote}</small></div></main>
      ) : (
        <main>
          <section className="panel">
            <div className="panel__head">
              <span className="serif">{d.staffSauna}</span>
              <small>{booked} / {capacity} {d.places}</small>
            </div>
            {data.slots.map((s) => {
              const full = s.booked >= s.capacity;
              return (
                <div key={s.id} className="cap-row">
                  <b>{s.time}</b>
                  <div>
                    <div className={full ? 'bar full' : 'bar'} role="meter" aria-valuemin={0} aria-valuemax={s.capacity}
                      aria-valuenow={s.booked} aria-label={s.time}>
                      <i style={{ width: `${(s.booked / s.capacity) * 100}%` }} />
                    </div>
                    {s.appGuests.length > 0 && <small>{t('guestNote', { guests: s.appGuests.join(', ') })}</small>}
                  </div>
                  <span className={full ? 'full' : ''}>{s.booked}/{s.capacity}</span>
                </div>
              );
            })}
          </section>

          <section className="panel reports">
            <div className="panel__head">
              <span className="serif">{d.staffReports}</span>
              <small className="hint">{d.tapStatus}</small>
            </div>
            {data.reports.map((r) => {
              const cat = CATEGORIES.find((c) => c.key === r.category) ?? CATEGORIES[3];
              return (
                <div key={r.id} className="rep">
                  <Icon name={cat.icon} />
                  <div>
                    <small>#{r.id} · {d[cat.label]} · {d.roomK} {r.room}</small>
                    <p>{lang === 'sv' ? r.textSv : r.textEn}</p>
                  </div>
                  <button className={`status ${r.status}`} onClick={() => advance(r.id)}>{d[r.status]}</button>
                </div>
              );
            })}
          </section>

          <section className="panel log">
            <span className="serif">{d.staffLog}</span>
            {data.log.map((e, i) => (
              <div key={`${e.time}-${i}`} className="log__row">
                <time>{e.time}</time>
                <b>{e.who}</b>
                <span>{lang === 'sv' ? e.sv : e.en}</span>
              </div>
            ))}
          </section>
        </main>
      )}
    </div>
  );
}
