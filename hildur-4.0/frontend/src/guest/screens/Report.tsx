import { useEffect, useRef, useState } from 'react';
import { api } from '../../api/client';
import type { Category } from '../../api/types';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import type { GuestCtx } from '../context';

export const CATEGORIES: { key: Category; label: 'cRoom' | 'cWifi' | 'cSauna' | 'cOther'; icon: string }[] = [
  { key: 'ROOM', label: 'cRoom', icon: 'bed' },
  { key: 'WIFI', label: 'cWifi', icon: 'wifi_off' },
  { key: 'SAUNA', label: 'cSauna', icon: 'hot_tub' },
  { key: 'OTHER', label: 'cOther', icon: 'help' }
];

/** 4 · Report a problem: category, short text, optional photo. */
export function Report({ ctx }: { ctx: GuestCtx }) {
  const { d } = useI18n();
  const [category, setCategory] = useState<Category>('ROOM');
  const [text, setText] = useState('');
  const [photo, setPhoto] = useState<{ name: string; url: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  // Free the preview image's memory when it's replaced or the screen closes.
  useEffect(() => () => { if (photo) URL.revokeObjectURL(photo.url); }, [photo]);

  const onFile = (file: File | undefined) => {
    if (file) setPhoto({ name: file.name, url: URL.createObjectURL(file) });
  };

  const empty = !text.trim();
  const send = async () => {
    if (empty || busy) return;
    setBusy(true);
    const updated = await ctx.run(api.report(category, text.trim(), !!photo));
    if (updated) {
      ctx.setGuest(updated);
      ctx.go('reportDone');
    } else {
      setBusy(false);
    }
  };

  return (
    <section className="section" data-screen-label="04 Report a problem">
      <h3 className="h3">{d.category}</h3>
      <div className="choice-grid" role="radiogroup" aria-label={d.category}>
        {CATEGORIES.map((c) => (
          <button key={c.key} role="radio" aria-checked={category === c.key}
            className={category === c.key ? 'choice row on' : 'choice row'} onClick={() => setCategory(c.key)}>
            <Icon name={c.icon} />{d[c.label]}
          </button>
        ))}
      </div>

      <h3 className="h3">{d.whatHappened}</h3>
      <textarea className="textarea" value={text} onChange={(e) => setText(e.target.value)} placeholder={d.placeholder}
        rows={4} maxLength={280} aria-label={d.whatHappened} />

      <input ref={fileInput} type="file" accept="image/*" capture="environment" hidden
        onChange={(e) => { onFile(e.target.files?.[0]); e.target.value = ''; }} />
      {photo ? (
        <div className="photo">
          <img src={photo.url} alt="" />
          <span>{photo.name}</span>
          <button onClick={() => setPhoto(null)} aria-label={d.close}><Icon name="close" /></button>
        </div>
      ) : (
        <button className="add-photo" onClick={() => fileInput.current?.click()}><Icon name="add_a_photo" />{d.addPhoto}</button>
      )}

      <button className="btn btn-primary lg" style={{ marginTop: 6 }} disabled={empty || busy} onClick={send}>
        <Icon name="send" />{d.send}
      </button>
    </section>
  );
}

/** 4b · Report status. Polls the server so the guest sees staff progress live. */
export function ReportDone({ ctx }: { ctx: GuestCtx }) {
  const { d, t } = useI18n();
  const r = ctx.guest.report;
  const { setGuest } = ctx;

  useEffect(() => {
    if (!r || r.status === 'FIXED') return;
    const id = window.setInterval(() => {
      api.me().then(setGuest).catch(() => {});
    }, 3000);
    return () => window.clearInterval(id);
  }, [r?.status, setGuest]);

  if (!r) return null;
  const order = ['RECEIVED', 'IN_PROGRESS', 'FIXED'];
  const reached = order.indexOf(r.status);
  const steps = [
    { label: d.st0, time: r.receivedAt },
    { label: t('st1', { staff: ctx.guest.onDutyStaff }), time: r.inProgressAt },
    { label: d.st2, time: r.fixedAt }
  ];
  const cat = CATEGORIES.find((c) => c.key === r.category)!;

  return (
    <section className="section" data-screen-label="04b Report status">
      <div className="card" style={{ gap: 18 }}>
        <div className="case-head">
          <span className="serif">{t('caseNo', { id: r.id })}</span>
          <small>{d[cat.label]}</small>
        </div>
        <p className="quote">“{r.textSv}”</p>
        <div className="steps">
          {steps.map((s, i) => (
            <div key={i} className={`step${i <= reached ? ' done' : ''}${i < reached ? ' linked' : ''}`}>
              <div className="step__rail">
                <span className="step__dot"><Icon name="check" /></span>
                <span className="step__line" />
              </div>
              <div className="step__text">
                <b>{s.label}</b>
                <small>{i <= reached ? s.time : d.pending}</small>
              </div>
            </div>
          ))}
        </div>
      </div>
      <button className="btn btn-outline" onClick={() => ctx.go('report')}>{d.reportAnother}</button>
      <button className="btn btn-primary" onClick={() => ctx.go('welcome')}>{d.backHome}</button>
    </section>
  );
}
