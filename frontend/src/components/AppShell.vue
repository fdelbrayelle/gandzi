<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';
import GandziLogo from './branding/GandziLogo.vue';
import AnnualBudgetMatrix from './AnnualBudgetMatrix.vue';
import AccountSettingsPanel from './AccountSettingsPanel.vue';
import ImportExportPanel from './ImportExportPanel.vue';
import WealthPanel from './WealthPanel.vue';
import { useImportStore } from '../stores/import';

const pinia = createPinia();
setActivePinia(pinia);
const store = usePreferencesStore(pinia);
const importStore = useImportStore(pinia);
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

const sections = computed((): { id: DashboardSection; label: string }[] => [
  { id: 'budget', label: `📅 ${localeMessages.value.menuBudget}` },
  { id: 'wealth', label: `💼 ${localeMessages.value.menuWealth}` },
  { id: 'simulations', label: `🧮 ${localeMessages.value.menuSimulations}` },
  { id: 'import-export', label: `💾 ${localeMessages.value.menuImportExport}` },
  { id: 'account', label: `⚙️ ${localeMessages.value.menuAccount}` },
]);

watch(currentSection, (section) => {
  window.localStorage.setItem(sectionStorageKey, section);
});

onMounted(() => {
  store.initFromStorage();
  const savedSection = window.localStorage.getItem(sectionStorageKey);
  if (savedSection && sections.value.some((section) => section.id === savedSection)) {
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

function goToImportReport() {
  currentSection.value = 'import-export';
  importStore.status = 'ready'; // ensure preview is shown
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
          <span class="meta-chip">📘 Financial Education</span>
          <span class="meta-chip">🔐 Privacy-by-Design</span>
          <span class="meta-chip">📅 Annual Budget</span>
          <span class="meta-chip">💼 Wealth Tracking</span>
          <span class="meta-chip">🧮 Simulations</span>
          <span class="meta-chip">💾 Import / Export</span>
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
          <h1 class="dashboard-title">{{ localeMessages.dashboardTitle }}</h1>
        </div>
        <div class="header-actions">
          <button class="logout-btn" type="button" @click="logout">{{ localeMessages.logoutCta }}</button>
        </div>
      </header>

      <section v-if="currentSection === 'budget'" class="dashboard-section">
        <h2 class="section-title">{{ localeMessages.annualBudgetTitle }}</h2>
        <p class="section-subtitle">{{ localeMessages.annualBudgetSubtitle }}</p>
        <AnnualBudgetMatrix />
      </section>

      <section v-else-if="currentSection === 'wealth'" class="dashboard-section">
        <h2 class="section-title">{{ localeMessages.wealthTitle }}</h2>
        <p class="section-subtitle">{{ localeMessages.wealthSubtitle }}</p>
        <WealthPanel />
      </section>

      <section v-else-if="currentSection === 'account'" class="dashboard-section">
        <h2 class="section-title">{{ localeMessages.accountSettingsTitle }}</h2>
        <p class="section-subtitle">{{ localeMessages.accountSettingsSubtitle }}</p>
        <AccountSettingsPanel />
      </section>

      <section v-else-if="currentSection === 'import-export'" class="dashboard-section">
        <h2 class="section-title">{{ localeMessages.importExportTitle }}</h2>
        <p class="section-subtitle">{{ localeMessages.importExportSubtitle }}</p>
        <ImportExportPanel />
      </section>

      <section v-else class="dashboard-section">
        <h2 class="section-title">{{ localeMessages.placeholderSectionTitle }}</h2>
        <p class="section-subtitle">{{ localeMessages.placeholderSectionSubtitle }}</p>
      </section>
    </main>

    <!-- Global import toast -->
    <Transition name="toast">
      <div v-if="importStore.status === 'parsing'" class="import-toast import-toast-parsing">
        <div class="toast-spinner" />
        <span>{{ localeMessages.importParsing }}</span>
        <span class="toast-file">{{ importStore.fileName }}</span>
      </div>
      <div v-else-if="importStore.status === 'ready' && currentSection !== 'import-export'" class="import-toast import-toast-ready" key="ready">
        <span>{{ localeMessages.importReady }}</span>
        <button class="toast-link" type="button" @click="goToImportReport">{{ localeMessages.importViewReport }}</button>
        <button class="toast-dismiss" type="button" @click="importStore.dismiss()">&#10005;</button>
      </div>
      <div v-else-if="importStore.status === 'success' && currentSection !== 'import-export'" class="import-toast import-toast-success" key="success">
        <span>{{ localeMessages.importSuccess }}</span>
        <button class="toast-dismiss" type="button" @click="importStore.dismiss()">&#10005;</button>
      </div>
      <div v-else-if="importStore.status === 'error' && currentSection !== 'import-export'" class="import-toast import-toast-error" key="error">
        <span>{{ importStore.errorMessage }}</span>
        <button class="toast-dismiss" type="button" @click="importStore.dismiss()">&#10005;</button>
      </div>
    </Transition>
  </div>
</template>
