<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';
import GandziLogo from './branding/GandziLogo.vue';
import AnnualBudgetMatrix from './AnnualBudgetMatrix.vue';
import AccountSettingsPanel from './AccountSettingsPanel.vue';

const pinia = createPinia();
setActivePinia(pinia);
const store = usePreferencesStore(pinia);
const localeMessages = computed(() => messages[store.locale]);
const defaultLoginUrl =
  'http://localhost:8081/realms/gandzi/protocol/openid-connect/auth?client_id=gandzi-frontend&response_type=code&scope=openid&redirect_uri=http%3A%2F%2Flocalhost%3A4321%2F';
const loginBaseUrl = (import.meta.env.PUBLIC_AUTH_LOGIN_URL as string | undefined) ?? defaultLoginUrl;
const loginUrl = loginBaseUrl.includes('prompt=')
  ? loginBaseUrl
  : `${loginBaseUrl}&prompt=login`;
const authStorageKey = 'gandzi_auth';
const sectionStorageKey = 'gandzi_dashboard_section';
const isAuthenticated = ref(false);
const authResolved = ref(false);
type DashboardSection = 'budget' | 'wealth' | 'simulations' | 'account' | 'import-export';
const currentSection = ref<DashboardSection>('budget');

const sections: { id: DashboardSection; label: string }[] = [
  { id: 'budget', label: '📅 Annual Budget' },
  { id: 'wealth', label: '💼 Wealth' },
  { id: 'simulations', label: '🧮 Simulations' },
  { id: 'account', label: '⚙️ Account Settings' },
  { id: 'import-export', label: '💾 Import / Export' },
];

watch(currentSection, (section) => {
  window.localStorage.setItem(sectionStorageKey, section);
});

onMounted(() => {
  store.initFromStorage();
  const savedSection = window.localStorage.getItem(sectionStorageKey);
  if (savedSection && sections.some((section) => section.id === savedSection)) {
    currentSection.value = savedSection as DashboardSection;
  }
  const params = new URLSearchParams(window.location.search);
  if (params.has('code') || params.has('session_state')) {
    isAuthenticated.value = true;
    window.localStorage.setItem(authStorageKey, 'true');
    window.history.replaceState({}, document.title, window.location.pathname);
    authResolved.value = true;
    return;
  }

  isAuthenticated.value = window.localStorage.getItem(authStorageKey) === 'true';
  authResolved.value = true;
});

function logout() {
  window.localStorage.removeItem(authStorageKey);
  isAuthenticated.value = false;
  currentSection.value = 'budget';
  window.history.replaceState({}, document.title, window.location.pathname);
}

function setCurrentSection(section: DashboardSection) {
  currentSection.value = section;
}
</script>

<template>
  <div v-if="!authResolved" class="auth-loading">
    <div class="auth-loading-dot" />
  </div>

  <div v-else-if="!isAuthenticated" class="page-wrap">
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
          @click="setCurrentSection(item.id)"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <main class="dashboard-main panel">
      <header class="dashboard-header">
        <div>
          <h1 class="dashboard-title">Dashboard</h1>
        </div>
        <div class="header-actions">
          <button class="logout-btn" type="button" @click="logout">Log out</button>
        </div>
      </header>

      <section v-if="currentSection === 'budget'" class="dashboard-section">
        <h2 class="section-title">Annual Budget 2026</h2>
        <p class="section-subtitle">Editable month-by-month matrix with annual and monthly rollups.</p>
        <AnnualBudgetMatrix />
      </section>

      <section v-else-if="currentSection === 'account'" class="dashboard-section">
        <h2 class="section-title">Account Settings</h2>
        <p class="section-subtitle">Manage language, currency, timezone, alerts, and defaults.</p>
        <AccountSettingsPanel />
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
