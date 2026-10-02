import type {
  BreakfastItem, BreakfastMode, Category, Dashboard, Forecast, GuestState, LoginResult, Report, Slot, Wifi
} from './types';

/** An error the backend explained with a code, e.g. "slotFull". "offline" means no answer at all. */
export class ApiError extends Error {
  constructor(public status: number, public code: string) {
    super(code);
  }
}

const TOKEN_KEY = 'hildur.session';

export interface StoredSession {
  token: string;
  role: LoginResult['role'];
}

/** The login token is kept in sessionStorage: it survives a page reload but not closing the tab. */
export const session = {
  load(): StoredSession | null {
    try {
      const raw = sessionStorage.getItem(TOKEN_KEY);
      return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
      return null;
    }
  },
  save(value: StoredSession | null) {
    try {
      if (value) sessionStorage.setItem(TOKEN_KEY, JSON.stringify(value));
      else sessionStorage.removeItem(TOKEN_KEY);
    } catch {
      // private mode etc. – the app still works until reload
    }
  }
};

let onUnauthorized: () => void = () => {};
/** Called when the server says our token is no longer valid (e.g. after a server restart). */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const current = session.load();
  if (current) headers.Authorization = `Bearer ${current.token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response: Response;
  try {
    response = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, 'offline');
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    const code = typeof data.error === 'string' ? data.error : 'genericErr';
    if (response.status === 401 && !path.startsWith('/auth/')) onUnauthorized();
    throw new ApiError(response.status, code);
  }
  if (response.status === 202 || response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  loginGuest: (bookingNumber: string, lastName: string) =>
    request<LoginResult>('POST', '/auth/guest', { bookingNumber, lastName }),
  loginStaff: (username: string, pin: string) => request<LoginResult>('POST', '/auth/staff', { username, pin }),
  logout: () => request<void>('POST', '/auth/logout'),

  me: () => request<GuestState>('GET', '/guest/me'),
  checkIn: () => request<GuestState>('POST', '/guest/check-in'),
  slots: () => request<Slot[]>('GET', '/guest/sauna/slots'),
  bookSauna: (slotId: number) => request<GuestState>('POST', '/guest/sauna/booking', { slotId }),
  cancelSauna: () => request<GuestState>('DELETE', '/guest/sauna/booking'),
  orderBreakfast: (items: BreakfastItem[], mode: BreakfastMode, time: string) =>
    request<GuestState>('PUT', '/guest/breakfast', { items, mode, time }),
  report: (category: Category, text: string, hasPhoto: boolean) =>
    request<GuestState>('POST', '/guest/reports', { category, text, hasPhoto }),
  forecast: () => request<Forecast>('GET', '/guest/aurora'),
  setAurora: (enabled: boolean) => request<GuestState>('PUT', '/guest/aurora', { enabled }),
  wifi: () => request<Wifi>('GET', '/guest/wifi'),
  exportData: () => request<void>('POST', '/guest/data/export'),
  deleteData: () => request<void>('POST', '/guest/data/deletion'),
  checkOut: (rating: number | null) => request<GuestState>('POST', '/guest/check-out', { rating }),
  restart: () => request<GuestState>('POST', '/guest/restart'),

  dashboard: () => request<Dashboard>('GET', '/staff/dashboard'),
  advanceReport: (id: number) => request<Report>('POST', `/staff/reports/${id}/advance`)
};
