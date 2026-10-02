import { useState } from 'react';
import { api } from '../../api/client';
import type { BreakfastItem, BreakfastMode } from '../../api/types';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import type { GuestCtx } from '../context';

const ITEMS: BreakfastItem[] = ['RYE', 'SALMON', 'FIL', 'EGG', 'WAFFLE', 'COFFEE', 'TEA', 'JUICE'];
const TIMES = ['07:00', '07:30', '08:00', '08:30', '09:00', '09:30'];

/** 3 · Breakfast order: tick items, pick room or dining room, pick a time. */
export function Breakfast({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const existing = ctx.guest.breakfast;
  const [picked, setPicked] = useState<Set<BreakfastItem>>(() => new Set(existing?.items ?? ['COFFEE']));
  const [mode, setMode] = useState<BreakfastMode>(existing?.mode ?? 'ROOM');
  const [time, setTime] = useState(existing?.time ?? '08:00');
  const [busy, setBusy] = useState(false);

  const toggle = (item: BreakfastItem) => {
    const next = new Set(picked);
    if (next.has(item)) next.delete(item);
    else next.add(item);
    setPicked(next);
  };

  const count = picked.size;
  const order = async () => {
    if (!count || busy) return;
    setBusy(true);
    const items = ITEMS.filter((i) => picked.has(i));
    const updated = await ctx.run(api.orderBreakfast(items, mode, time));
    if (updated) {
      ctx.setGuest(updated);
      ctx.go('bfDone');
    } else {
      setBusy(false);
    }
  };

  const modes: [BreakfastMode, string, string][] = [['ROOM', d.room2, 'room_service'], ['DINING', d.dining, 'restaurant']];

  return (
    <section className="section" data-screen-label="03 Breakfast">
      <h3 className="h3">{d.menu}</h3>
      <div className="checklist" role="group" aria-label={d.menu}>
        {ITEMS.map((item) => (
          <button key={item} className="check-row" role="checkbox" aria-checked={picked.has(item)} onClick={() => toggle(item)}>
            <span className={picked.has(item) ? 'box on' : 'box'}><Icon name="check" /></span>
            <span>{d.items[item]}</span>
          </button>
        ))}
      </div>

      <h3 className="h3">{d.where}</h3>
      <div className="choice-grid" role="radiogroup" aria-label={d.where}>
        {modes.map(([key, label, icon]) => (
          <button key={key} role="radio" aria-checked={mode === key} className={mode === key ? 'choice on' : 'choice'} onClick={() => setMode(key)}>
            <Icon name={icon} />{label}
          </button>
        ))}
      </div>

      <h3 className="h3">{d.time}</h3>
      <div className="chips" role="radiogroup" aria-label={d.time}>
        {TIMES.map((x) => (
          <button key={x} role="radio" aria-checked={time === x} className={time === x ? 'chip on' : 'chip'} onClick={() => setTime(x)}>{x}</button>
        ))}
      </div>

      <button className="btn btn-primary lg" style={{ marginTop: 10 }} disabled={!count || busy} onClick={order}>
        {count ? `${d.orderBf} · ${count}` : d.pickOne}
      </button>
    </section>
  );
}

/** 3b · Order summary. */
export function BreakfastDone({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const o = ctx.guest.breakfast;
  if (!o) return null;
  return (
    <section className="section" data-screen-label="03b Breakfast confirmed">
      <div className="card" style={{ gap: 16 }}>
        <div className="order-head">
          <span className="round-check"><Icon name="check" /></span>
          <span className="serif">{d.yourOrder}</span>
        </div>
        <div className="kv">
          <div><small>{d.when}</small><b>{d.tomorrow} {o.time}</b></div>
          <div><small>{d.whereShort}</small><b>{o.mode === 'ROOM' ? d.room2 : d.dining}</b></div>
        </div>
        <div className="bullets">
          {o.items.map((i) => <div key={i}>{d.items[i]}</div>)}
        </div>
      </div>
      <button className="btn btn-outline" onClick={() => ctx.go('breakfast')}>{d.edit}</button>
      <button className="btn btn-primary" onClick={() => ctx.go('welcome')}>{d.backHome}</button>
    </section>
  );
}
