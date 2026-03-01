export type Locale = 'en' | 'fr' | 'es' | 'de' | 'it' | 'pt' | 'zh' | 'ja' | 'hi' | 'ar' | 'ru';

type MessageMap = Record<string, string>;

const base: MessageMap = {
  appTitle: 'Gandzi - Financial Treasure',
  welcome: 'A precise command center for budgets, wealth, and long-term financial projections.',
  signInTitle: 'Sign in to Gandzi',
  signInSubtitle: 'Use your secure OAuth2 account to access your financial treasure.',
  signInCta: 'Continue to secure sign-in',
};

export const messages: Record<Locale, MessageMap> = {
  en: base,
  fr: {
    ...base,
    appTitle: 'Gandzi - Tresor financier',
    welcome: 'Un centre de pilotage precis pour budget, patrimoine et projections financieres long terme.',
    signInTitle: 'Se connecter a Gandzi',
    signInSubtitle: 'Utilisez votre compte OAuth2 securise pour acceder a votre tresor financier.',
    signInCta: 'Continuer vers la connexion securisee',
  },
  es: { ...base },
  de: { ...base },
  it: { ...base },
  pt: { ...base },
  zh: { ...base },
  ja: { ...base },
  hi: { ...base },
  ar: { ...base },
  ru: { ...base },
};
