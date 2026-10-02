// These types mirror the Java records the backend sends (GuestDtos, ReportService.ReportDto, ...).

export type Role = 'GUEST' | 'STAFF';
export type BreakfastItem = 'RYE' | 'SALMON' | 'FIL' | 'EGG' | 'WAFFLE' | 'COFFEE' | 'TEA' | 'JUICE';
export type BreakfastMode = 'ROOM' | 'DINING';
export type Category = 'ROOM' | 'WIFI' | 'SAUNA' | 'OTHER';
export type ReportStatus = 'RECEIVED' | 'IN_PROGRESS' | 'FIXED';

export interface LoginResult {
  token: string;
  role: Role;
  name: string;
}

export interface Report {
  id: number;
  category: Category;
  room: string;
  textSv: string;
  textEn: string;
  hasPhoto: boolean;
  status: ReportStatus;
  receivedAt: string | null;
  inProgressAt: string | null;
  fixedAt: string | null;
}

export interface GuestState {
  bookingNumber: string;
  firstName: string;
  lastName: string;
  room: string;
  email: string;
  checkedIn: boolean;
  checkedOut: boolean;
  rating: number | null;
  notifyAurora: boolean;
  saunaSlotId: number | null;
  saunaTime: string | null;
  breakfast: { items: BreakfastItem[]; mode: BreakfastMode; time: string } | null;
  report: Report | null;
  onDutyStaff: string;
}

export interface Slot {
  id: number;
  time: string;
  capacity: number;
  booked: number;
}

export interface Forecast {
  level: 'LOW' | 'MEDIUM' | 'HIGH';
  sky: 'CLOUDY' | 'CLEAR';
  kp: number;
  cloudCover: number;
  darkFrom: string;
  darkTo: string;
}

export interface Wifi {
  network: string;
  password: string;
}

export interface LogEntry {
  time: string;
  who: string;
  sv: string;
  en: string;
}

export interface Dashboard {
  me: { name: string; roleSv: string; roleEn: string };
  slots: (Slot & { appGuests: string[] })[];
  reports: Report[];
  log: LogEntry[];
}
