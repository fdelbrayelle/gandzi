<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';

const store = usePreferencesStore();
const localeMessages = computed(() => messages[store.locale]);

const supportedLocales = ['en', 'fr', 'es', 'de', 'it', 'pt', 'zh', 'ja', 'hi', 'ar', 'ru', 'ka'];
const supportedCurrencies = ['EUR', 'USD', 'JPY', 'CNY', 'INR', 'GBP', 'CHF'];
const supportedTimezones = ['Europe/Paris', 'UTC', 'America/New_York', 'Asia/Tokyo', 'Asia/Shanghai', 'Asia/Kolkata'];

const form = ref({
  displayName: store.displayName,
  locale: store.locale,
  timezone: store.timezone,
  currency: store.currency,
  secondaryCurrencies: store.secondaryCurrencies.join(', '),
  budgetAlertThreshold: store.budgetAlertThreshold,
  emailNotificationsEnabled: store.emailNotificationsEnabled,
  emailAddress: store.emailAddress,
  snapshotFrequency: store.snapshotFrequency,
  simulationHorizonYears: store.simulationHorizonYears,
  dateFormat: store.dateFormat,
  exportFormat: store.exportFormat,
});

const saved = ref(false);
const showSaveToast = ref(false);

function notifySaved(): void {
  saved.value = true;
  showSaveToast.value = true;
  window.setTimeout(() => {
    saved.value = false;
    showSaveToast.value = false;
  }, 1800);
}

function save(): void {
  store.setDisplayName(form.value.displayName.trim() || 'Gandzi User');
  store.setLocale(form.value.locale);
  store.setTimezone(form.value.timezone);
  store.setCurrency(form.value.currency);
  store.setSecondaryCurrencies(
    form.value.secondaryCurrencies
      .split(',')
      .map((value) => value.trim().toUpperCase())
      .filter(Boolean),
  );
  store.setBudgetAlertThreshold(Math.min(100, Math.max(1, Number(form.value.budgetAlertThreshold) || 80)));
  store.setEmailNotificationsEnabled(form.value.emailNotificationsEnabled);
  store.setEmailAddress(form.value.emailAddress.trim());
  store.setSnapshotFrequency(form.value.snapshotFrequency);
  store.setSimulationHorizonYears(Math.min(100, Math.max(1, Number(form.value.simulationHorizonYears) || 30)));
  store.setDateFormat(form.value.dateFormat);
  store.setExportFormat(form.value.exportFormat);
  notifySaved();
}

function onKeydown(event: KeyboardEvent): void {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault();
    save();
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown);
});
</script>

<template>
  <div class="settings-grid">
    <div v-if="showSaveToast" class="settings-toast">{{ localeMessages.savedLabel }}</div>

    <section class="settings-card">
      <h3 class="settings-title">{{ localeMessages.profileTitle }}</h3>
      <label class="settings-label" for="displayName">{{ localeMessages.displayNameLabel }}</label>
      <input id="displayName" v-model="form.displayName" class="settings-input" type="text" />

      <label class="settings-label" for="locale">{{ localeMessages.languageLabel }}</label>
      <select id="locale" v-model="form.locale" class="settings-input settings-select">
        <option v-for="locale in supportedLocales" :key="locale" :value="locale">{{ locale.toUpperCase() }}</option>
      </select>

      <label class="settings-label" for="timezone">{{ localeMessages.timezoneLabel }}</label>
      <select id="timezone" v-model="form.timezone" class="settings-input settings-select">
        <option v-for="tz in supportedTimezones" :key="tz" :value="tz">{{ tz }}</option>
      </select>

      <label class="settings-label" for="dateFormat">{{ localeMessages.dateFormatLabel }}</label>
      <select id="dateFormat" v-model="form.dateFormat" class="settings-input settings-select">
        <option value="DD/MM/YYYY">DD/MM/YYYY</option>
        <option value="MM/DD/YYYY">MM/DD/YYYY</option>
        <option value="YYYY-MM-DD">YYYY-MM-DD</option>
      </select>
    </section>

    <section class="settings-card">
      <h3 class="settings-title">{{ localeMessages.financeDefaultsTitle }}</h3>
      <label class="settings-label" for="currency">{{ localeMessages.primaryCurrencyLabel }}</label>
      <select id="currency" v-model="form.currency" class="settings-input settings-select">
        <option v-for="currency in supportedCurrencies" :key="currency" :value="currency">{{ currency }}</option>
      </select>

      <label class="settings-label" for="secondaryCurrencies">{{ localeMessages.secondaryCurrenciesLabel }}</label>
      <input id="secondaryCurrencies" v-model="form.secondaryCurrencies" class="settings-input" type="text" placeholder="USD, CHF" />

      <label class="settings-label" for="snapshotFrequency">{{ localeMessages.wealthSnapshotFrequencyLabel }}</label>
      <select id="snapshotFrequency" v-model="form.snapshotFrequency" class="settings-input settings-select">
        <option value="DAILY">Daily</option>
        <option value="MONTHLY">Monthly</option>
        <option value="YEARLY">Yearly</option>
        <option value="ON_DEMAND">On demand</option>
      </select>

      <label class="settings-label" for="simulationHorizonYears">{{ localeMessages.simulationHorizonLabel }}</label>
      <input id="simulationHorizonYears" v-model.number="form.simulationHorizonYears" class="settings-input" type="number" min="1" max="100" />

      <label class="settings-label" for="exportFormat">{{ localeMessages.exportFormatLabel }}</label>
      <select id="exportFormat" v-model="form.exportFormat" class="settings-input settings-select">
        <option value="JSON">JSON</option>
        <option value="CSV">CSV</option>
      </select>
    </section>

    <section class="settings-card">
      <h3 class="settings-title">{{ localeMessages.alertsTitle }}</h3>
      <label class="settings-label" for="budgetAlertThreshold">{{ localeMessages.budgetAlertThresholdLabel }}</label>
      <input id="budgetAlertThreshold" v-model.number="form.budgetAlertThreshold" class="settings-input" type="number" min="1" max="100" />

      <label class="settings-toggle">
        <input v-model="form.emailNotificationsEnabled" type="checkbox" />
        <span>{{ localeMessages.emailNotificationsEnabledLabel }}</span>
      </label>

      <label class="settings-label" for="emailAddress">{{ localeMessages.notificationEmailLabel }}</label>
      <input id="emailAddress" v-model="form.emailAddress" class="settings-input" type="email" placeholder="you@example.com" :disabled="!form.emailNotificationsEnabled" />

      <div class="settings-actions">
        <button class="login-btn" type="button" @click="save">{{ localeMessages.saveSettingsCta }}</button>
        <span v-if="saved" class="saved-pill">{{ localeMessages.savedLabel }}</span>
      </div>
    </section>
  </div>
</template>
