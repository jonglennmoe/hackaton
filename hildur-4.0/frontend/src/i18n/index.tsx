import { createContext, useContext, useState, type ReactNode } from 'react';
import { sv, type Dict } from './sv';
import { en } from './en';

export type Lang = 'sv' | 'en';
type StringKey = { [K in keyof Dict]: Dict[K] extends string ? K : never }[keyof Dict];

const DICTS: Record<Lang, Dict> = { sv, en };
const STORAGE_KEY = 'hildur.lang';

/** Replaces {name}-style placeholders: fmt('Hej {name}', { name: 'Anna' }) → 'Hej Anna'. */
export function fmt(text: string, vars: Record<string, string | number> = {}): string {
  return text.replace(/\{(\w+)\}/g, (match, key) => (key in vars ? String(vars[key]) : match));
}

interface I18n {
  lang: Lang;
  setLang: (lang: Lang) => void;
  d: Dict;
  /** Look up a text and fill in its placeholders. */
  t: (key: StringKey, vars?: Record<string, string | number>) => string;
  /** Translate a backend error code, falling back to a generic message. */
  err: (code: string) => string;
}

const I18nContext = createContext<I18n | null>(null);

function initialLang(): Lang {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'sv' || saved === 'en') return saved;
  } catch {
    // storage blocked – fall through to the default
  }
  return 'sv';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(initialLang);
  const d = DICTS[lang];
  const setLang = (next: Lang) => {
    setLangState(next);
    document.documentElement.lang = next;
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // not important
    }
  };
  const t: I18n['t'] = (key, vars) => fmt(d[key], vars);
  const err: I18n['err'] = (code) => {
    const mapped = code === 'checkInFirst' ? 'checkInFirstErr' : code;
    const text = (d as unknown as Record<string, unknown>)[mapped];
    return typeof text === 'string' ? text : d.genericErr;
  };
  return <I18nContext.Provider value={{ lang, setLang, d, t, err }}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18n {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used inside <I18nProvider>');
  return ctx;
}
