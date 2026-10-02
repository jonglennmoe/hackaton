import { useState } from 'react';
import { api } from '../../api/client';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import type { GuestCtx } from '../context';

/** 8 · Check-out: stay summary, a 1-tap rating and a goodbye. */
export function Checkout({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const g = ctx.guest;
  const [rating, setRating] = useState<number>(g.rating ?? 0);
  const [busy, setBusy] = useState(false);

  const ratingTexts = ['', d.r1, d.r2, d.r3, d.r4, d.r5];

  const checkOut = async () => {
    setBusy(true);
    const updated = await ctx.run(api.checkOut(rating || null));
    if (updated) {
      ctx.setGuest(updated);
      window.scrollTo(0, 0);
    }
    setBusy(false);
  };

  const restart = async () => {
    const updated = await ctx.run(api.restart());
    if (updated) {
      ctx.setGuest(updated);
      ctx.go('welcome');
    }
  };

  if (g.checkedOut) {
    return (
      <section className="section" data-screen-label="08 Check-out">
        <div className="bye">
          <Icon name="nights_stay" />
          <span className="serif">{d.checkedOutTitle}</span>
          <p>{d.checkedOutText}</p>
        </div>
        <button className="btn btn-outline" onClick={restart}>{d.restart}</button>
      </section>
    );
  }

  const summary = [
    { k: d.nights, v: '3' },
    { k: d.roomK, v: g.room },
    { k: d.saunaK, v: g.saunaTime ?? '–' },
    { k: d.bfK, v: g.breakfast ? `${g.breakfast.items.length} ${d.itemsWord} · ${g.breakfast.time}` : '–' },
    { k: d.repK, v: g.report ? `#${g.report.id} · ${d[g.report.status]}` : '–' }
  ];

  return (
    <section className="section" data-screen-label="08 Check-out">
      <div className="card summary">
        <span className="serif">{d.yourStay}</span>
        {summary.map((r) => (
          <div key={r.k} className="summary__row"><span>{r.k}</span><b>{r.v}</b></div>
        ))}
      </div>

      <div className="card rating">
        <b>{d.howWas}</b>
        <div className="stars" role="radiogroup" aria-label={d.howWas}>
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} role="radio" aria-checked={rating === n} aria-label={String(n)}
              className={n <= rating ? 'on' : ''} onClick={() => setRating(n)}>
              <Icon name="star" fill />
            </button>
          ))}
        </div>
        <small>{ratingTexts[rating]}</small>
      </div>

      <button className="btn btn-primary lg" onClick={checkOut} disabled={busy}>
        <Icon name="logout" />{d.checkoutBtn}
      </button>
    </section>
  );
}
