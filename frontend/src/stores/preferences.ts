import { defineStore } from 'pinia';
import type { Locale } from '../i18n/messages';

type SnapshotFrequency = 'DAILY' | 'MONTHLY' | 'YEARLY' | 'ON_DEMAND';
type DateFormat = 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD';
type ExportFormat = 'JSON' | 'CSV';

type PreferencesState = {
  displayName: string;
  locale: Locale;
  timezone: string;
  currency: string;
  secondaryCurrencies: string[];
  budgetAlertThreshold: number;
  emailNotificationsEnabled: boolean;
  emailAddress: string;
  snapshotFrequency: SnapshotFrequency;
  simulationHorizonYears: number;
  dateFormat: DateFormat;
  exportFormat: ExportFormat;
};

const STORAGE_KEY = 'gandzi_preferences_v1';

export const usePreferencesStore = defineStore('preferences', {
  state: (): PreferencesState => ({
    displayName: 'Gandzi User',
    locale: 'en' as Locale,
    timezone: 'Europe/Paris',
    currency: 'EUR',
    secondaryCurrencies: ['USD', 'CHF'],
    budgetAlertThreshold: 80,
    emailNotificationsEnabled: true,
    emailAddress: '',
    snapshotFrequency: 'MONTHLY',
    simulationHorizonYears: 30,
    dateFormat: 'DD/MM/YYYY',
    exportFormat: 'JSON',
  }),
  actions: {
    initFromStorage() {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        const parsed = JSON.parse(raw) as Partial<PreferencesState>;
        this.$patch({
          ...this.$state,
          ...parsed,
        });
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    },
    saveToStorage() {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.$state));
    },
    setDisplayName(displayName: string) {
      this.displayName = displayName;
      this.saveToStorage();
    },
    setLocale(locale: Locale) {
      this.locale = locale;
      this.saveToStorage();
    },
    setTimezone(timezone: string) {
      this.timezone = timezone;
      this.saveToStorage();
    },
    setCurrency(currency: string) {
      this.currency = currency;
      this.saveToStorage();
    },
    setSecondaryCurrencies(secondaryCurrencies: string[]) {
      this.secondaryCurrencies = secondaryCurrencies;
      this.saveToStorage();
    },
    setBudgetAlertThreshold(budgetAlertThreshold: number) {
      this.budgetAlertThreshold = budgetAlertThreshold;
      this.saveToStorage();
    },
    setEmailNotificationsEnabled(emailNotificationsEnabled: boolean) {
      this.emailNotificationsEnabled = emailNotificationsEnabled;
      this.saveToStorage();
    },
    setEmailAddress(emailAddress: string) {
      this.emailAddress = emailAddress;
      this.saveToStorage();
    },
    setSnapshotFrequency(snapshotFrequency: SnapshotFrequency) {
      this.snapshotFrequency = snapshotFrequency;
      this.saveToStorage();
    },
    setSimulationHorizonYears(simulationHorizonYears: number) {
      this.simulationHorizonYears = simulationHorizonYears;
      this.saveToStorage();
    },
    setDateFormat(dateFormat: DateFormat) {
      this.dateFormat = dateFormat;
      this.saveToStorage();
    },
    setExportFormat(exportFormat: ExportFormat) {
      this.exportFormat = exportFormat;
      this.saveToStorage();
    },
  },
});
