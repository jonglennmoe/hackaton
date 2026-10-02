import type { CSSProperties } from 'react';

/**
 * Hildur's avatar: a round night-blue face with a sauna-wood knitted hat,
 * drawn with CSS shapes. Measurements scale from the design's 66px face (inside a 72px ring).
 */
export function HildurFace({ size }: { size: number }) {
  const k = size / 66;
  const px = (n: number) => `${Math.round(n * k * 2) / 2}px`;
  const at = (s: CSSProperties) => s;
  return (
    <div className="hildur__face">
      <span className="hat" style={at({ left: px(11), right: px(11), top: px(-11), height: px(24), borderRadius: `${px(24)} ${px(24)} ${px(7)} ${px(7)}` })} />
      <span className="pom" style={at({ top: px(-19), width: px(13), height: px(13), marginLeft: px(-6.5) })} />
      <span className="eye" style={at({ left: px(18), top: px(28), width: px(8), height: px(9) })} />
      <span className="eye" style={at({ right: px(18), top: px(28), width: px(8), height: px(9) })} />
      <span className="cheek" style={at({ left: px(12), top: px(40), width: px(9), height: px(6) })} />
      <span className="cheek" style={at({ right: px(12), top: px(40), width: px(9), height: px(6) })} />
      <span className="smile" style={at({ top: px(40), width: px(16), height: px(8), marginLeft: px(-8), borderRadius: `0 0 ${px(16)} ${px(16)}` })} />
    </div>
  );
}

/** Static avatar (login screen). */
export function HildurAvatar({ size = 72 }: { size?: number }) {
  return (
    <div className="hildur" style={{ width: size, height: size, marginTop: 14 }} aria-hidden="true">
      <HildurFace size={size - 6} />
    </div>
  );
}
