/**
 * Kjell, the hotel cat – a ginger blob with two ears.
 * mood "sleepy" has closed eyes (loading screens), "awake" has open eyes (empty states, My data).
 * Measurements scale from the design's 72×64 version.
 */
export function Kjell({ width = 72, mood = 'awake' }: { width?: number; mood?: 'awake' | 'sleepy' }) {
  const k = width / 72;
  const n = (v: number) => Math.round(v * k * 2) / 2;
  const ear = { borderLeftWidth: n(12), borderRightWidth: n(12), borderBottomWidth: n(20) };
  return (
    <div className="kjell" style={{ width, height: n(64) }} aria-hidden="true">
      <div className="ear" style={{ ...ear, left: n(7), transform: 'rotate(-14deg)' }} />
      <div className="ear" style={{ ...ear, right: n(7), transform: 'rotate(14deg)' }} />
      <div className="head" style={{ height: n(52) }}>
        {mood === 'sleepy' ? (
          <>
            <span className="eye-shut" style={{ left: n(19), top: n(18), width: n(7), height: n(4) }} />
            <span className="eye-shut" style={{ right: n(19), top: n(18), width: n(7), height: n(4) }} />
          </>
        ) : (
          <>
            <span className="eye-open" style={{ left: n(18), top: n(16), width: n(7), height: n(9) }} />
            <span className="eye-open" style={{ right: n(18), top: n(16), width: n(7), height: n(9) }} />
          </>
        )}
        <span className="nose" style={{ top: n(mood === 'sleepy' ? 28 : 29), width: n(8), height: n(5), marginLeft: -n(4) }} />
      </div>
    </div>
  );
}
