<script setup lang="ts">
import { computed } from 'vue';
import { createPinia } from 'pinia';
import { usePreferencesStore } from '../stores/preferences';
import { messages } from '../i18n/messages';

const pinia = createPinia();
const store = usePreferencesStore(pinia);
const localeMessages = computed(() => messages[store.locale]);
</script>

<template>
  <main>
    <h1>{{ localeMessages.appTitle }}</h1>
    <p>{{ localeMessages.welcome }}</p>
  </main>
</template>
