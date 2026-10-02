import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import type { Forecast } from '../../api/types';
import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import type { GuestCtx } from '../context';

/** 5 · Northern lights alert: an opt-in toggle (off by default) and tonight's chance. */
export function Aurora({ ctx }: { ctx: GuestCtx }) {
  const { d, t } = useI18n();
  const [forecast, setForecast] = useState<Forecast | null>(null);
  const on = ctx.guest.notifyAurora;

  useEffect(() => {
    ctx.run(api.forecast()).then((f) => f && setForecast(f));
  }, []);

  const toggle = async () => {
    // Flip immediately so the switch feels instant; the server answer confirms it.
    ctx.setGuest({ ...ctx.guest, notifyAurora: !on });
    const updated = await ctx.run(api.setAurora(!on));
    ctx.setGuest(updated ?? ctx.guest);
  };

  const level = forecast ? d[forecast.level] : d.LOW;
  const sky = forecast ? d[forecast.sky] : d.CLOUDY;

  return (
    <section className="section" data-screen-label="05 Northern lights">
      <button className="toggle" role="switch" aria-checked={on} onClick={toggle}>
        <Icon name={on ? 'notifications_active' : 'notifications_off'} />
        <span className="toggle__text">
          <b>{d.notify}</b>
          <small>{on ? d.notifyOn : d.notifyOff}</small>
        </span>
        <span className={on ? 'switch on' : 'switch'}><i /></span>
      </button>

      <div className="sky">
        <div className="sky__glow" />
        <span className="eyebrow">{d.chanceTonight}</span>
        <div className="sky__level">
          <span className="serif">{level}</span>
          <small>{sky}</small>
        </div>
        <p>{t('chanceText', { level, sky })}</p>
        <div className="forecast">
          <div><Icon name="auto_awesome" /><small>{d.fKp}</small><b>{t('fKpV', { kp: forecast?.kp ?? 2 })}</b></div>
          <div><Icon name="cloud" /><small>{d.fCloud}</small><b>{forecast ? `${forecast.cloudCover} %` : '–'}</b></div>
          <div><Icon name="dark_mode" /><small>{d.fDark}</small><b>{forecast ? `${forecast.darkFrom}–${forecast.darkTo}` : '–'}</b></div>
        </div>
      </div>

      <div className="note"><Icon name="location_on" />{d.spot}</div>
    </section>
  );
}
