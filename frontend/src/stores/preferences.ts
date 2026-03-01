import { defineStore } from 'pinia';
import type { Locale } from '../i18n/messages';

export const usePreferencesStore = defineStore('preferences', {
  state: () => ({
    locale: 'en' as Locale,
    timezone: 'Europe/Paris',
    currency: 'EUR',
  }),
  actions: {
    setLocale(locale: Locale) {
      this.locale = locale;
    },
    setTimezone(timezone: string) {
      this.timezone = timezone;
    },
    setCurrency(currency: string) {
      this.currency = currency;
    },
  },
});
