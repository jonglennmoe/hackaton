import { useEffect, useState } from 'react';
import { api, ApiError } from '../../api/client';
import type { Slot } from '../../api/types';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import { Kjell } from '../../components/Kjell';
import type { GuestCtx } from '../context';

/** 2 · Sauna booking: one card per time slot with seats left. */
export function Sauna({ ctx }: { ctx: GuestCtx }) {
  const { d, t, err } = useI18n();
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [busy, setBusy] = useState(false);
  const g = ctx.guest;

  useEffect(() => {
    ctx.run(api.slots()).then((s) => s && setSlots(s));
  }, []);

  const pick = async (slot: Slot) => {
    if (busy) return;
    if (slot.id === g.saunaSlotId) return ctx.go('saunaDone');
    setBusy(true);
    try {
      ctx.setGuest(await api.bookSauna(slot.id));
      ctx.go('saunaDone');
    } catch (e) {
      // Someone may have taken the last seat a moment ago – show the fresh numbers.
      ctx.toast(err(e instanceof ApiError ? e.code : 'genericErr'), 'error');
      ctx.run(api.slots()).then((s) => s && setSlots(s));
      setBusy(false);
    }
  };

  return (
    <section className="section" data-screen-label="02 Sauna">
      {g.saunaTime && (
        <button className="banner" onClick={() => ctx.go('saunaDone')}>
          <Icon name="event_available" fill />
          <span>{t('saunaBanner', { t: g.saunaTime })}</span>
          <u>{d.view}</u>
        </button>
      )}
      <div className="day-head">
        <span className="serif">{d.today}</span>
        <small>{d.saunaPlace}</small>
      </div>
      {!slots ? (
        <div className="loading"><Kjell mood="sleepy" /><small>{d.kjellNote}</small></div>
      ) : (
        <div className="slots">
          {slots.map((s) => {
            const mine = s.id === g.saunaSlotId;
            const left = s.capacity - s.booked;
            const full = left <= 0 && !mine;
            const label = mine ? d.yours : full ? t('full', { cap: s.capacity }) : t('left', { n: left, cap: s.capacity });
            return (
              <button key={s.id} className={`slot${mine ? ' mine' : ''}${full ? ' full' : ''}`} disabled={full || busy}
                onClick={() => pick(s)} aria-label={`${s.time} – ${label}`}>
                <span className="slot__time">{s.time}</span>
                <span className="slot__body">
                  <span className="slot__label">{label}</span>
                  <span className="slot__dots">
                    {Array.from({ length: s.capacity }, (_, k) => <span key={k} className={k < s.booked ? 'taken' : ''} />)}
                  </span>
                </span>
                <Icon name={mine ? 'check_circle' : full ? 'block' : 'chevron_right'} />
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}

/** 2b · Confirmation with a cancel button. */
export function SaunaDone({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const [busy, setBusy] = useState(false);

  const cancel = async () => {
    setBusy(true);
    const updated = await ctx.run(api.cancelSauna());
    if (updated) {
      ctx.setGuest(updated);
      ctx.go('sauna', { cancelled: true });
    } else {
      setBusy(false);
    }
  };

  if (!ctx.guest.saunaTime) {
    return (
      <section className="section">
        <button className="btn btn-primary" onClick={() => ctx.go('sauna')}>{d.sSauna}</button>
      </section>
    );
  }

  return (
    <section className="section" data-screen-label="02b Sauna confirmed">
      <div className="card confirm">
        <span className="confirm__check"><Icon name="check" /></span>
        <span className="eyebrow">{d.booked}</span>
        <span className="confirm__time">{ctx.guest.saunaTime}</span>
        <span className="confirm__day">{d.today}</span>
        <hr />
        <div className="facts">
          <div><Icon name="cabin" />{d.saunaPlace}</div>
          <div><Icon name="dry_cleaning" />{d.bring}</div>
        </div>
      </div>
      <button className="btn btn-danger" onClick={cancel} disabled={busy}>{d.cancelBooking}</button>
      <button className="btn btn-primary" onClick={() => ctx.go('welcome')}>{d.backHome}</button>
    </section>
  );
}
