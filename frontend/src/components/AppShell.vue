<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { createPinia } from 'pinia';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';
import GandziLogo from './branding/GandziLogo.vue';

const pinia = createPinia();
const store = usePreferencesStore(pinia);
const localeMessages = computed(() => messages[store.locale]);
const defaultLoginUrl =
  'http://localhost:8081/realms/gandzi/protocol/openid-connect/auth?client_id=gandzi-frontend&response_type=code&scope=openid&redirect_uri=http%3A%2F%2Flocalhost%3A4321%2F';
const loginBaseUrl = (import.meta.env.PUBLIC_AUTH_LOGIN_URL as string | undefined) ?? defaultLoginUrl;
const loginUrl = loginBaseUrl.includes('prompt=')
  ? loginBaseUrl
  : `${loginBaseUrl}&prompt=login`;
const authStorageKey = 'gandzi_auth';
const isAuthenticated = ref(false);
const currentSection = ref('budget');
const budgetRows = ref([
  { month: 'January', planned: 3200, actual: 2890, variance: 310 },
  { month: 'February', planned: 3200, actual: 3340, variance: -140 },
  { month: 'March', planned: 3200, actual: 3010, variance: 190 },
  { month: 'April', planned: 3200, actual: 3125, variance: 75 },
  { month: 'May', planned: 3200, actual: 2980, variance: 220 },
  { month: 'June', planned: 3200, actual: 2870, variance: 330 },
  { month: 'July', planned: 3200, actual: 0, variance: 3200 },
  { month: 'August', planned: 3200, actual: 0, variance: 3200 },
  { month: 'September', planned: 3200, actual: 0, variance: 3200 },
  { month: 'October', planned: 3200, actual: 0, variance: 3200 },
  { month: 'November', planned: 3200, actual: 0, variance: 3200 },
  { month: 'December', planned: 3200, actual: 0, variance: 3200 },
]);

const sections = [
  { id: 'budget', label: '📅 Annual Budget' },
  { id: 'wealth', label: '💼 Wealth' },
  { id: 'simulations', label: '🧮 Simulations' },
  { id: 'account', label: '⚙️ Account Settings' },
  { id: 'import-export', label: '💾 Import / Export' },
];

const totals = computed(() => {
  const planned = budgetRows.value.reduce((acc, row) => acc + row.planned, 0);
  const actual = budgetRows.value.reduce((acc, row) => acc + row.actual, 0);
  return { planned, actual, variance: planned - actual };
});

onMounted(() => {
  const params = new URLSearchParams(window.location.search);
  if (params.has('code') || params.has('session_state')) {
    isAuthenticated.value = true;
    window.localStorage.setItem(authStorageKey, 'true');
    window.history.replaceState({}, document.title, window.location.pathname);
    return;
  }

  isAuthenticated.value = window.localStorage.getItem(authStorageKey) === 'true';
});

function logout() {
  window.localStorage.removeItem(authStorageKey);
  isAuthenticated.value = false;
  currentSection.value = 'budget';
  window.history.replaceState({}, document.title, window.location.pathname);
}
</script>

<template>
  <div v-if="!isAuthenticated" class="page-wrap">
    <section class="hero">
      <article class="panel brand-panel">
        <GandziLogo />
        <p class="brand-copy">
          {{ localeMessages.welcome }}
        </p>
        <div class="chip-row">
          <span class="meta-chip">🔒 GDPR-first</span>
          <span class="meta-chip">📘 Financial Education</span>
        </div>
      </article>

      <aside class="panel login-panel">
        <h2 class="login-title">{{ localeMessages.signInTitle }}</h2>
        <p class="login-subtitle">{{ localeMessages.signInSubtitle }}</p>
        <a class="login-btn" :href="loginUrl">{{ localeMessages.signInCta }}</a>
      </aside>
    </section>
  </div>

  <div v-else class="dashboard-wrap">
    <aside class="dashboard-sidebar panel">
      <div class="sidebar-brand">
        <p class="sidebar-title">Gandzi</p>
        <p class="sidebar-subtitle">Financial Treasure</p>
      </div>
      <nav class="sidebar-menu">
        <button
          v-for="item in sections"
          :key="item.id"
          class="menu-item"
          :class="{ active: currentSection === item.id }"
          type="button"
          @click="currentSection = item.id"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <main class="dashboard-main panel">
      <header class="dashboard-header">
        <div>
          <h1 class="dashboard-title">Dashboard</h1>
          <p class="dashboard-subtitle">Authenticated workspace</p>
        </div>
        <div class="header-actions">
          <button class="logout-btn" type="button" @click="logout">Log out</button>
        </div>
      </header>

      <section v-if="currentSection === 'budget'" class="dashboard-section">
        <h2 class="section-title">Annual Budget 2026</h2>
        <p class="section-subtitle">Month-by-month planner and actuals.</p>

        <div class="budget-table-wrap">
          <table class="budget-table">
            <thead>
              <tr>
                <th>Month</th>
                <th>Planned (EUR)</th>
                <th>Actual (EUR)</th>
                <th>Variance (EUR)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in budgetRows" :key="row.month">
                <td>{{ row.month }}</td>
                <td>{{ row.planned }}</td>
                <td>{{ row.actual }}</td>
                <td :class="{ positive: row.variance >= 0, negative: row.variance < 0 }">{{ row.variance }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <th>Total</th>
                <th>{{ totals.planned }}</th>
                <th>{{ totals.actual }}</th>
                <th :class="{ positive: totals.variance >= 0, negative: totals.variance < 0 }">{{ totals.variance }}</th>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <section v-else class="dashboard-section">
        <h2 class="section-title">{{ sections.find((s) => s.id === currentSection)?.label }}</h2>
        <p class="section-subtitle">
          This feature area is available from the dashboard menu and will be expanded in the next iteration.
        </p>
      </section>
    </main>
  </div>
</template>
