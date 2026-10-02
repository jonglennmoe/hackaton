import { useI18n, type Lang } from '../i18n';

/** SV / EN pill on dark backgrounds (login and staff header). */
export function DarkLangSwitch() {
  const { lang, setLang } = useI18n();
  return (
    <div className="seg dark" role="group" aria-label="Language">
      {(['sv', 'en'] as Lang[]).map((code) => (
        <button key={code} className={lang === code ? 'active' : ''} aria-pressed={lang === code} onClick={() => setLang(code)}>
          {code.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
