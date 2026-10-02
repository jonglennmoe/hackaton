import type { GuestState } from '../api/types';

export type Screen =
  | 'welcome' | 'sauna' | 'saunaDone' | 'breakfast' | 'bfDone'
  | 'report' | 'reportDone' | 'aurora' | 'wifi' | 'data' | 'checkout';

/** What every guest screen gets from the shell. */
export interface GuestCtx {
  guest: GuestState;
  setGuest: (g: GuestState) => void;
  go: (screen: Screen, opts?: { cancelled?: boolean }) => void;
  /** Runs an API call; on failure shows a friendly toast and resolves to undefined. */
  run: <T>(call: Promise<T>) => Promise<T | undefined>;
  toast: (message: string, kind?: 'ok' | 'error') => void;
}
