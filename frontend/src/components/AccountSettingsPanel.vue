<script setup lang="ts">
import { ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';

const store = usePreferencesStore();

const supportedLocales = ['en', 'fr', 'es', 'de', 'it', 'pt', 'zh', 'ja', 'hi', 'ar', 'ru'];
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
  saved.value = true;
  window.setTimeout(() => {
    saved.value = false;
  }, 1800);
}
</script>

<template>
  <div class="settings-grid">
    <section class="settings-card">
      <h3 class="settings-title">Profile</h3>
      <label class="settings-label" for="displayName">Display name</label>
      <input id="displayName" v-model="form.displayName" class="settings-input" type="text" />

      <label class="settings-label" for="locale">Language</label>
      <select id="locale" v-model="form.locale" class="settings-input">
        <option v-for="locale in supportedLocales" :key="locale" :value="locale">{{ locale.toUpperCase() }}</option>
      </select>

      <label class="settings-label" for="timezone">Timezone</label>
      <select id="timezone" v-model="form.timezone" class="settings-input">
        <option v-for="tz in supportedTimezones" :key="tz" :value="tz">{{ tz }}</option>
      </select>

      <label class="settings-label" for="dateFormat">Date format</label>
      <select id="dateFormat" v-model="form.dateFormat" class="settings-input">
        <option value="DD/MM/YYYY">DD/MM/YYYY</option>
        <option value="MM/DD/YYYY">MM/DD/YYYY</option>
        <option value="YYYY-MM-DD">YYYY-MM-DD</option>
      </select>
    </section>

    <section class="settings-card">
      <h3 class="settings-title">Finance Defaults</h3>
      <label class="settings-label" for="currency">Primary currency</label>
      <select id="currency" v-model="form.currency" class="settings-input">
        <option v-for="currency in supportedCurrencies" :key="currency" :value="currency">{{ currency }}</option>
      </select>

      <label class="settings-label" for="secondaryCurrencies">Secondary currencies (comma separated)</label>
      <input id="secondaryCurrencies" v-model="form.secondaryCurrencies" class="settings-input" type="text" placeholder="USD, CHF" />

      <label class="settings-label" for="snapshotFrequency">Wealth snapshot frequency</label>
      <select id="snapshotFrequency" v-model="form.snapshotFrequency" class="settings-input">
        <option value="DAILY">Daily</option>
        <option value="MONTHLY">Monthly</option>
        <option value="YEARLY">Yearly</option>
        <option value="ON_DEMAND">On demand</option>
      </select>

      <label class="settings-label" for="simulationHorizonYears">Default simulation horizon (years)</label>
      <input id="simulationHorizonYears" v-model.number="form.simulationHorizonYears" class="settings-input" type="number" min="1" max="100" />

      <label class="settings-label" for="exportFormat">Default export format</label>
      <select id="exportFormat" v-model="form.exportFormat" class="settings-input">
        <option value="JSON">JSON</option>
        <option value="CSV">CSV</option>
      </select>
    </section>

    <section class="settings-card">
      <h3 class="settings-title">Alerts & Notifications</h3>
      <label class="settings-label" for="budgetAlertThreshold">Budget alert threshold (%)</label>
      <input id="budgetAlertThreshold" v-model.number="form.budgetAlertThreshold" class="settings-input" type="number" min="1" max="100" />

      <label class="settings-toggle">
        <input v-model="form.emailNotificationsEnabled" type="checkbox" />
        <span>Email notifications enabled</span>
      </label>

      <label class="settings-label" for="emailAddress">Notification email</label>
      <input id="emailAddress" v-model="form.emailAddress" class="settings-input" type="email" placeholder="you@example.com" :disabled="!form.emailNotificationsEnabled" />

      <div class="settings-actions">
        <button class="login-btn" type="button" @click="save">Save settings</button>
        <span v-if="saved" class="saved-pill">Saved</span>
      </div>
    </section>
  </div>
</template>
