export type Locale = 'en' | 'fr' | 'es' | 'de' | 'it' | 'pt' | 'zh' | 'ja' | 'hi' | 'ar' | 'ru';

type MessageMap = Record<string, string>;

const base: MessageMap = {
  appTitle: 'Gandzi - Financial Treasure',
  welcome: 'Your personal finance cockpit',
};

export const messages: Record<Locale, MessageMap> = {
  en: base,
  fr: { appTitle: 'Gandzi - Tresor financier', welcome: 'Votre cockpit financier personnel' },
  es: { appTitle: 'Gandzi - Tesoro financiero', welcome: 'Tu panel financiero personal' },
  de: { appTitle: 'Gandzi - Finanzschatz', welcome: 'Ihr persoenliches Finanz-Cockpit' },
  it: { appTitle: 'Gandzi - Tesoro finanziario', welcome: 'La tua cabina finanziaria personale' },
  pt: { appTitle: 'Gandzi - Tesouro financeiro', welcome: 'Seu cockpit financeiro pessoal' },
  zh: { appTitle: 'Gandzi - Financial Treasure', welcome: 'Your personal finance cockpit' },
  ja: { appTitle: 'Gandzi - Financial Treasure', welcome: 'Your personal finance cockpit' },
  hi: { appTitle: 'Gandzi - Financial Treasure', welcome: 'Your personal finance cockpit' },
  ar: { appTitle: 'Gandzi - Financial Treasure', welcome: 'Your personal finance cockpit' },
  ru: { appTitle: 'Gandzi - Financial Treasure', welcome: 'Your personal finance cockpit' },
};
