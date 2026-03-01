<script setup lang="ts">
import { computed } from 'vue';
import { createPinia } from 'pinia';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';
import GandziLogo from './branding/GandziLogo.vue';

const pinia = createPinia();
const store = usePreferencesStore(pinia);
const localeMessages = computed(() => messages[store.locale]);
</script>

<template>
  <div class="page-wrap">
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
        <button class="login-btn" type="button">{{ localeMessages.signInCta }}</button>
        <p class="login-hint">{{ localeMessages.signInHint }}</p>
      </aside>
    </section>
  </div>
</template>
