import { useI18n } from '../../i18n';
import { Icon } from '../../components/Icon';
import type { GuestCtx, Screen } from '../context';

/** 1 · Welcome / check-in, with the home menu. */
export function Welcome({ ctx, onCheckIn, onLogout }: { ctx: GuestCtx; onCheckIn: () => void; onLogout: () => void }) {
  const { d, t } = useI18n();
  const g = ctx.guest;

  const menu: { icon: string; label: string; sub: string; tint: string; ink: string; to: Screen }[] = [
    { icon: 'hot_tub', label: d.mSauna, sub: g.saunaTime ? t('sSaunaBooked', { t: g.saunaTime }) : d.sSauna, tint: '#F1E4D3', ink: '#8A5432', to: g.saunaTime ? 'saunaDone' : 'sauna' },
    { icon: 'bakery_dining', label: d.mBreakfast, sub: g.breakfast ? t('sBreakfastDone', { t: g.breakfast.time }) : d.sBreakfast, tint: '#F6EBD6', ink: '#8A6420', to: g.breakfast ? 'bfDone' : 'breakfast' },
    { icon: 'build', label: d.mReport, sub: g.report ? t('sReportDone', { id: g.report.id }) : d.sReport, tint: '#F5E3DE', ink: '#9A3F2F', to: g.report ? 'reportDone' : 'report' },
    { icon: 'auto_awesome', label: d.mAurora, sub: g.notifyAurora ? d.sAuroraOn : d.sAurora, tint: '#E3F3EC', ink: '#2F7D64', to: 'aurora' },
    { icon: g.checkedIn ? 'wifi' : 'wifi_lock', label: d.mWifi, sub: g.checkedIn ? d.sWifi : d.sWifiLocked, tint: '#E3E8F3', ink: '#2C3D6E', to: 'wifi' },
    { icon: 'shield_person', label: d.mData, sub: d.sData, tint: '#ECE6F5', ink: '#6E4A9E', to: 'data' }
  ];

  return (
    <section className="section" style={{ gap: 16 }} data-screen-label="01 Welcome">
      <div className="stay">
        <div className="stay__grid">
          <div>
            <span className="eyebrow">{d.room}</span>
            <span className="stay__room">{g.room}</span>
            <span className="stay__sub">{d.roomType}</span>
          </div>
          <div>
            <span className="eyebrow">{d.stay}</span>
            <span className="stay__dates">{d.dates}</span>
            <span className="stay__sub">{d.nightsLabel}</span>
          </div>
        </div>
        {g.checkedIn ? (
          <div className="checked-in"><Icon name="check_circle" fill />{d.checkedInLabel}</div>
        ) : (
          <button className="btn btn-aurora" onClick={onCheckIn}><Icon name="key" />{d.checkIn}</button>
        )}
      </div>

      <h2 className="menu-title">{d.menuTitle}</h2>
      <div className="menu-grid">
        {menu.map((m) => (
          <button key={m.icon} className="tile" onClick={() => ctx.go(m.to)}>
            <span className="tile__icon" style={{ background: m.tint, color: m.ink }}><Icon name={m.icon} /></span>
            <span className="tile__text"><b>{m.label}</b><small>{m.sub}</small></span>
          </button>
        ))}
      </div>

      {g.checkedIn && (
        <button className="checkout-link" onClick={() => ctx.go('checkout')}>
          <Icon name="luggage" />
          <span><b>{d.checkout}</b><small>{d.checkoutSub}</small></span>
          <Icon name="chevron_right" />
        </button>
      )}

      <button className="btn-link" style={{ marginTop: 8 }} onClick={onLogout}><Icon name="logout" />{d.logout}</button>
    </section>
  );
}
