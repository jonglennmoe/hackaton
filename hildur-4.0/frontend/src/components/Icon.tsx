import type { CSSProperties } from 'react';

/** A Material Symbols Rounded icon, e.g. <Icon name="hot_tub" />. */
export function Icon({ name, fill, size, style }: { name: string; fill?: boolean; size?: number; style?: CSSProperties }) {
  return (
    <span className={fill ? 'sym fill' : 'sym'} style={size ? { fontSize: size, ...style } : style} aria-hidden="true">
      {name}
    </span>
  );
}
