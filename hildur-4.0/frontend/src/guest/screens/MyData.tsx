import { useState } from 'react';
import { api } from '../../api/client';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import { Kjell } from '../../components/Kjell';
import type { GuestCtx } from '../context';

/** 7 · Hildur's safety promise, Kjell's (lack of) admin rights, and download / delete. */
export function MyData({ ctx }: { ctx: GuestCtx }) {
  const { d, t } = useI18n();
  const [sheet, setSheet] = useState(false);
  const [armed, setArmed] = useState(false);

  const promises = [
    { icon: 'public', text: d.p1 },
    { icon: 'group', text: d.p2 },
    { icon: 'history', text: d.p3 },
    { icon: 'volume_off', text: d.p4 }
  ];

  const close = () => { setSheet(false); setArmed(false); };

  const download = async () => {
    close();
    const ok = await ctx.run(api.exportData().then(() => true));
    if (ok) ctx.toast(d.toastDownload);
  };

  const remove = async () => {
    // Two taps: the first one asks "Sure?", the second one does it.
    if (!armed) return setArmed(true);
    close();
    const ok = await ctx.run(api.deleteData().then(() => true));
    if (ok) ctx.toast(d.toastDelete);
  };

  return (
    <section className="section" style={{ gap: 12 }} data-screen-label="07 My data">
      <h3 className="serif" style={{ fontSize: 22, margin: '0 2px 4px' }}>{d.promise}</h3>
      {promises.map((p) => (
        <div key={p.icon} className="promise">
          <span className="promise__icon"><Icon name={p.icon} /></span>
          <p>{p.text}</p>
        </div>
      ))}

      <div className="mascot">
        <Kjell width={64} />
        <div>
          <span className="serif">{d.kjellName}</span>
          <small>{d.kjellRole}</small>
        </div>
      </div>

      <button className="btn btn-primary" style={{ marginTop: 8, height: 56 }} onClick={() => setSheet(true)}>
        <Icon name="folder_managed" />{d.dataBtn}
      </button>

      {sheet && (
        <>
          <div className="scrim" onClick={close} />
          <div className="sheet" role="dialog" aria-modal="true" aria-label={d.yourData}>
            <span className="sheet__grip" />
            <span className="serif">{d.yourData}</span>
            <button className="sheet-row" onClick={download}>
              <Icon name="download" style={{ color: '#2F7D64' }} />
              <span><b>{d.download}</b><small>{t('downloadSub', { email: ctx.guest.email })}</small></span>
            </button>
            <button className={armed ? 'sheet-row armed' : 'sheet-row'} onClick={remove}>
              <Icon name="delete" style={{ color: '#9A3F2F' }} />
              <span><b style={{ color: '#9A3F2F' }}>{armed ? d.delConfirm : d.del}</b><small>{d.deleteSub}</small></span>
            </button>
            <button className="sheet__close" onClick={close}>{d.close}</button>
          </div>
        </>
      )}
    </section>
  );
}
