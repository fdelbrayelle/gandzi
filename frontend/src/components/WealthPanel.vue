<script setup lang="ts">
import { computed, ref, reactive } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { useWealthStore, financialWealth, grossWealth, netWealth, DEFAULT_SUPPORTS } from '../stores/wealth';
import type { WealthSnapshot, AssetEntry } from '../stores/wealth';
import { messages } from '../i18n/messages';
import { Line, Doughnut } from 'vue-chartjs';
import { Chart, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend, Filler, ArcElement } from 'chart.js';

Chart.register(LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend, Filler, ArcElement);

const prefStore = usePreferencesStore();
const wealthStore = useWealthStore();
const t = computed(() => messages[prefStore.locale]);

const monthKeys = [
  'monthJan', 'monthFeb', 'monthMar', 'monthApr', 'monthMay', 'monthJun',
  'monthJul', 'monthAug', 'monthSep', 'monthOct', 'monthNov', 'monthDec',
] as const;

// --- Period navigator for pie chart ---
type ViewMode = 'month' | 'year';
const viewMode = ref<ViewMode>('month');
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth()); // 0-based

function prevPeriod() {
  if (viewMode.value === 'month') {
    if (selectedMonth.value === 0) {
      selectedMonth.value = 11;
      selectedYear.value--;
    } else {
      selectedMonth.value--;
    }
  } else {
    selectedYear.value--;
  }
}

function nextPeriod() {
  if (viewMode.value === 'month') {
    if (selectedMonth.value === 11) {
      selectedMonth.value = 0;
      selectedYear.value++;
    } else {
      selectedMonth.value++;
    }
  } else {
    selectedYear.value++;
  }
}

function toggleViewMode() {
  viewMode.value = viewMode.value === 'month' ? 'year' : 'month';
}

const periodLabel = computed(() => {
  if (viewMode.value === 'year') return String(selectedYear.value);
  return `${t.value[monthKeys[selectedMonth.value]]} ${selectedYear.value}`;
});

const isCurrentPeriod = computed(() => {
  const now = new Date();
  if (viewMode.value === 'year') return selectedYear.value === now.getFullYear();
  return selectedYear.value === now.getFullYear() && selectedMonth.value === now.getMonth();
});

// Find the closest snapshot for the selected period
const periodSnapshot = computed<WealthSnapshot | null>(() => {
  if (wealthStore.snapshots.length === 0) return null;

  if (viewMode.value === 'year') {
    // Find latest snapshot within the selected year
    const yearSnaps = wealthStore.snapshots.filter((s) => s.date.startsWith(String(selectedYear.value)));
    return yearSnaps.length > 0 ? yearSnaps[yearSnaps.length - 1] : null;
  }

  // Month mode: find latest snapshot within the selected month
  const prefix = `${selectedYear.value}-${String(selectedMonth.value + 1).padStart(2, '0')}`;
  const monthSnaps = wealthStore.snapshots.filter((s) => s.date.startsWith(prefix));
  return monthSnaps.length > 0 ? monthSnaps[monthSnaps.length - 1] : null;
});

// --- Support summary table (from latest snapshot) ---
const latestSnap = computed(() => {
  if (wealthStore.snapshots.length === 0) return null;
  return wealthStore.snapshots[wealthStore.snapshots.length - 1];
});

const latestFinancial = computed(() => latestSnap.value ? financialWealth(latestSnap.value) : 0);
const latestGross = computed(() => latestSnap.value ? grossWealth(latestSnap.value) : 0);
const latestNet = computed(() => latestSnap.value ? netWealth(latestSnap.value) : 0);

const supportSummary = computed(() => {
  if (!latestSnap.value) return [];
  const total = grossWealth(latestSnap.value);
  return latestSnap.value.assets
    .filter((a) => a.value > 0)
    .map((a, idx) => ({
      support: a.support,
      value: a.value,
      pct: total > 0 ? (a.value / total) * 100 : 0,
      color: getColor(a.support, idx),
    }))
    .sort((a, b) => b.value - a.value);
});

// --- Pie chart data for selected period ---
const supportColors: Record<string, string> = {
  PEA: '#7cf7ff',
  CTO: '#8c7bff',
  'Livret A': '#5af2b8',
  Crypto: '#ff9a4f',
  Gold: '#ffd54f',
  'Real Estate': '#ff4fd8',
  Savings: '#72f7c7',
  Other: '#9aa3cf',
};

function getColor(support: string, index: number): string {
  return supportColors[support] || `hsl(${(index * 47) % 360}, 70%, 60%)`;
}

const pieChartData = computed(() => {
  const snap = periodSnapshot.value;
  if (!snap || snap.assets.length === 0) {
    return { labels: [], datasets: [{ data: [], backgroundColor: [] }] };
  }
  const filtered = snap.assets.filter((a) => a.value > 0).sort((a, b) => b.value - a.value);
  return {
    labels: filtered.map((a) => a.support),
    datasets: [{
      data: filtered.map((a) => a.value),
      backgroundColor: filtered.map((a, i) => getColor(a.support, i)),
      borderColor: 'rgba(6, 6, 14, 0.8)',
      borderWidth: 2,
    }],
  };
});

const pieChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: { color: '#cfd5f7', padding: 12, usePointStyle: true },
    },
    tooltip: {
      callbacks: {
        label: (ctx: { label: string; parsed: number; dataset: { data: number[] } }) => {
          const total = ctx.dataset.data.reduce((s: number, v: number) => s + v, 0);
          const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0';
          return `${ctx.label}: ${fmt(ctx.parsed)} (${pct}%)`;
        },
      },
    },
  },
};

// --- All used supports ---
const allUsedSupports = computed(() => {
  const set = new Set<string>();
  for (const snap of wealthStore.snapshots) {
    for (const a of snap.assets) set.add(a.support);
  }
  return Array.from(set);
});

// --- Line chart data ---
const chartLabels = computed(() => wealthStore.snapshots.map((s) => s.date));

const wealthEvolutionData = computed(() => ({
  labels: chartLabels.value,
  datasets: [
    {
      label: t.value.wealthFinancial,
      data: wealthStore.snapshots.map(financialWealth),
      borderColor: '#7cf7ff',
      backgroundColor: 'rgba(124, 247, 255, 0.08)',
      fill: true,
      tension: 0.3,
    },
    {
      label: t.value.wealthGross,
      data: wealthStore.snapshots.map(grossWealth),
      borderColor: '#8c7bff',
      backgroundColor: 'rgba(140, 123, 255, 0.08)',
      fill: true,
      tension: 0.3,
    },
    {
      label: t.value.wealthNet,
      data: wealthStore.snapshots.map(netWealth),
      borderColor: '#5af2b8',
      backgroundColor: 'rgba(90, 242, 184, 0.08)',
      fill: true,
      tension: 0.3,
    },
  ],
}));

const supportBreakdownData = computed(() => ({
  labels: chartLabels.value,
  datasets: allUsedSupports.value.map((support, idx) => ({
    label: support,
    data: wealthStore.snapshots.map((snap) => {
      const asset = snap.assets.find((a) => a.support === support);
      return asset?.value ?? 0;
    }),
    borderColor: getColor(support, idx),
    backgroundColor: 'transparent',
    tension: 0.3,
  })),
}));

const lineChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: '#cfd5f7' } },
    tooltip: { mode: 'index' as const, intersect: false },
  },
  scales: {
    x: { ticks: { color: '#9aa3cf' }, grid: { color: 'rgba(255,255,255,0.06)' } },
    y: { ticks: { color: '#9aa3cf' }, grid: { color: 'rgba(255,255,255,0.06)' } },
  },
};

// --- New snapshot form ---
const newDate = ref(new Date().toISOString().slice(0, 10));
const newLiabilities = ref(0);
const newAssets = reactive<Record<string, number>>(
  Object.fromEntries(DEFAULT_SUPPORTS.map((s) => [s, 0]))
);
const newCustomSupport = ref('');

function addAssetSupport() {
  const name = newCustomSupport.value.trim();
  if (name && !(name in newAssets)) {
    wealthStore.addCustomSupport(name);
    newAssets[name] = 0;
    newCustomSupport.value = '';
  }
}

function addSnapshot() {
  if (!newDate.value) return;
  const assets: AssetEntry[] = [];
  for (const support of wealthStore.allSupports) {
    const val = newAssets[support] ?? 0;
    if (val !== 0) assets.push({ support, value: val });
  }
  wealthStore.addSnapshot({
    date: newDate.value,
    assets,
    liabilities: newLiabilities.value,
  });
  for (const key of Object.keys(newAssets)) newAssets[key] = 0;
  newLiabilities.value = 0;
}

function removeSnapshot(index: number) {
  wealthStore.removeSnapshot(index);
}

// --- Formatting ---
function fmt(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + ' ' + prefStore.currency;
}

function assetValue(snap: WealthSnapshot, support: string): number {
  return snap.assets.find((a) => a.support === support)?.value ?? 0;
}
</script>

<template>
  <div class="wealth-panel">
    <!-- Summary cards -->
    <div class="wealth-summary">
      <div class="wealth-card financial">
        <h4 class="wealth-card-title">{{ t.wealthFinancial }}</h4>
        <p class="wealth-card-value">{{ latestSnap ? fmt(latestFinancial) : '—' }}</p>
      </div>
      <div class="wealth-card gross">
        <h4 class="wealth-card-title">{{ t.wealthGross }}</h4>
        <p class="wealth-card-value">{{ latestSnap ? fmt(latestGross) : '—' }}</p>
      </div>
      <div class="wealth-card net">
        <h4 class="wealth-card-title">{{ t.wealthNet }}</h4>
        <p class="wealth-card-value">{{ latestSnap ? fmt(latestNet) : '—' }}</p>
      </div>
    </div>

    <!-- Support summary table + Pie chart side by side -->
    <div v-if="latestSnap" class="wealth-overview">
      <!-- Left: support summary table -->
      <div class="overview-card">
        <h4 class="chart-title">{{ t.wealthSupportSummary }}</h4>
        <table class="support-summary-table">
          <thead>
            <tr>
              <th class="support-col">Support</th>
              <th>{{ t.wealthValue }}</th>
              <th>%</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in supportSummary" :key="row.support">
              <td class="support-col">
                <span class="support-dot" :style="{ background: row.color }"></span>
                {{ row.support }}
              </td>
              <td class="value-col positive">{{ fmt(row.value) }}</td>
              <td class="pct-col">{{ row.pct.toFixed(1) }}%</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td class="support-col"><strong>Total</strong></td>
              <td class="value-col positive"><strong>{{ fmt(latestGross) }}</strong></td>
              <td class="pct-col"><strong>100%</strong></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- Right: pie chart with period navigator -->
      <div class="overview-card">
        <div class="pie-header">
          <h4 class="chart-title">{{ t.wealthAllocation }}</h4>
          <div class="period-nav">
            <button class="period-nav-btn" type="button" @click="prevPeriod">&larr;</button>
            <span class="period-nav-label" :class="{ 'current-period': isCurrentPeriod }">{{ periodLabel }}</span>
            <button class="period-nav-btn" type="button" @click="nextPeriod">&rarr;</button>
            <button class="period-zoom-btn" type="button" @click="toggleViewMode">
              {{ viewMode === 'month' ? '&#128197;' : '&#128198;' }}
            </button>
          </div>
        </div>
        <div v-if="periodSnapshot" class="pie-container">
          <Doughnut :data="pieChartData" :options="pieChartOptions" />
        </div>
        <p v-else class="no-data-msg">{{ t.wealthNoData }}</p>
      </div>
    </div>

    <!-- Line charts -->
    <div v-if="wealthStore.snapshots.length >= 2" class="wealth-charts">
      <div class="chart-card">
        <h4 class="chart-title">{{ t.wealthEvolution }}</h4>
        <div class="chart-container">
          <Line :data="wealthEvolutionData" :options="lineChartOptions" />
        </div>
      </div>
      <div class="chart-card">
        <h4 class="chart-title">{{ t.wealthBySupport }}</h4>
        <div class="chart-container">
          <Line :data="supportBreakdownData" :options="lineChartOptions" />
        </div>
      </div>
    </div>

    <!-- Add new snapshot -->
    <div class="snapshot-form">
      <h3 class="snapshot-form-title">{{ t.wealthAddSnapshot }}</h3>
      <div class="snapshot-form-grid">
        <div class="snapshot-field">
          <label class="settings-label">{{ t.wealthDate }}</label>
          <input v-model="newDate" type="date" class="settings-input" />
        </div>
        <div v-for="support in wealthStore.allSupports" :key="support" class="snapshot-field">
          <label class="settings-label">{{ support }}</label>
          <input v-model.number="newAssets[support]" type="number" step="0.01" class="settings-input" />
        </div>
        <div class="snapshot-field">
          <label class="settings-label">{{ t.wealthLiabilities }}</label>
          <input v-model.number="newLiabilities" type="number" step="0.01" class="settings-input" />
        </div>
      </div>
      <div class="snapshot-form-actions">
        <div class="add-support-row">
          <input v-model="newCustomSupport" type="text" class="settings-input" :placeholder="t.wealthNewSupport" @keyup.enter="addAssetSupport" />
          <button class="action-btn" type="button" @click="addAssetSupport">+</button>
        </div>
        <button class="login-btn snapshot-add-btn" type="button" @click="addSnapshot">{{ t.wealthAdd }}</button>
      </div>
    </div>

    <!-- Snapshot history table -->
    <div v-if="wealthStore.snapshots.length > 0" class="snapshot-history">
      <h3 class="snapshot-form-title">{{ t.wealthHistory }}</h3>
      <div class="snapshot-table-scroll">
        <table class="snapshot-table">
          <thead>
            <tr>
              <th>{{ t.wealthDate }}</th>
              <th v-for="support in allUsedSupports" :key="support">{{ support }}</th>
              <th>{{ t.wealthLiabilities }}</th>
              <th>{{ t.wealthFinancial }}</th>
              <th>{{ t.wealthGross }}</th>
              <th>{{ t.wealthNet }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(snap, idx) in wealthStore.snapshots" :key="snap.date + idx">
              <td>{{ snap.date }}</td>
              <td v-for="support in allUsedSupports" :key="support" class="positive">
                {{ fmt(assetValue(snap, support)) }}
              </td>
              <td class="negative">{{ fmt(snap.liabilities) }}</td>
              <td class="positive">{{ fmt(financialWealth(snap)) }}</td>
              <td class="positive">{{ fmt(grossWealth(snap)) }}</td>
              <td :class="netWealth(snap) >= 0 ? 'positive' : 'negative'">{{ fmt(netWealth(snap)) }}</td>
              <td class="action-cell">
                <button class="action-btn delete" type="button" @click="removeSnapshot(idx)">&#128465;</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wealth-panel {
  max-width: 100%;
}

.wealth-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.9rem;
  margin-bottom: 1.2rem;
}

.wealth-card {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 1.1rem;
  background: rgba(10, 13, 31, 0.55);
  text-align: center;
}

.wealth-card.financial { border-color: rgba(124, 247, 255, 0.3); }
.wealth-card.gross { border-color: rgba(140, 123, 255, 0.3); }
.wealth-card.net { border-color: rgba(90, 242, 184, 0.3); }

.wealth-card-title {
  margin: 0 0 0.4rem;
  font-size: 0.85rem;
  color: var(--text-2);
  font-weight: 600;
}

.wealth-card-value {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
}

.wealth-card.financial .wealth-card-value { color: var(--brand-cyan); }
.wealth-card.gross .wealth-card-value { color: var(--brand-violet); }
.wealth-card.net .wealth-card-value { color: #5af2b8; }

/* --- Overview: table + pie side by side --- */
.wealth-overview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.9rem;
  margin-bottom: 1.2rem;
}

.overview-card {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 1rem;
  background: rgba(10, 13, 31, 0.55);
}

.support-summary-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.support-summary-table thead th {
  color: var(--text-2);
  font-weight: 600;
  font-size: 0.8rem;
  padding: 0.4rem 0.5rem;
  border-bottom: 1px solid var(--line-soft);
  text-align: right;
}

.support-summary-table thead th.support-col {
  text-align: left;
}

.support-summary-table td {
  padding: 0.45rem 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.support-summary-table tfoot td {
  border-top: 1px solid var(--line-soft);
  border-bottom: none;
  padding-top: 0.55rem;
}

.support-col {
  text-align: left;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.support-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.value-col {
  text-align: right;
  white-space: nowrap;
}

.pct-col {
  text-align: right;
  color: var(--text-2);
  white-space: nowrap;
}

.pie-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.6rem;
}

.period-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.period-nav-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  color: #e8efff;
  font-size: 0.85rem;
  padding: 0.2rem 0.5rem;
  cursor: pointer;
  transition: background 0.15s;
}

.period-nav-btn:hover { background: rgba(255, 255, 255, 0.16); }

.period-nav-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #e8efff;
  min-width: 5rem;
  text-align: center;
}

.period-nav-label.current-period {
  color: #ffd54f;
}

.period-zoom-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  color: #e8efff;
  font-size: 0.85rem;
  padding: 0.2rem 0.45rem;
  cursor: pointer;
  transition: background 0.15s;
}

.period-zoom-btn:hover { background: rgba(255, 255, 255, 0.16); }

.pie-container {
  height: 280px;
  position: relative;
}

.no-data-msg {
  color: var(--text-2);
  text-align: center;
  padding: 3rem 0;
  font-size: 0.9rem;
}

/* --- Line charts --- */
.wealth-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.9rem;
  margin-bottom: 1.2rem;
}

.chart-card {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 1rem;
  background: rgba(10, 13, 31, 0.55);
}

.chart-title {
  margin: 0 0 0.6rem;
  font-size: 0.95rem;
  color: var(--text-1);
}

.chart-container {
  height: 260px;
  position: relative;
}

/* --- Snapshot form --- */
.snapshot-form {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 1rem;
  background: rgba(10, 13, 31, 0.55);
  margin-bottom: 1.2rem;
}

.snapshot-form-title {
  margin: 0 0 0.7rem;
  font-size: 1rem;
}

.snapshot-form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 0.6rem;
}

.snapshot-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.snapshot-form-actions {
  display: flex;
  gap: 0.7rem;
  align-items: center;
  margin-top: 0.8rem;
  flex-wrap: wrap;
}

.add-support-row {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.add-support-row .settings-input {
  width: 160px;
}

.snapshot-add-btn {
  white-space: nowrap;
  padding: 0.55rem 1.2rem;
  margin-left: auto;
}

/* --- Snapshot history --- */
.snapshot-history {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 1rem;
  background: rgba(10, 13, 31, 0.55);
}

.snapshot-table-scroll {
  overflow-x: auto;
}

.snapshot-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.84rem;
}

.snapshot-table thead th {
  background: rgba(47, 56, 103, 0.6);
  color: #e8efff;
  padding: 0.5rem 0.5rem;
  text-align: right;
  font-weight: 600;
  white-space: nowrap;
}

.snapshot-table thead th:first-child { text-align: left; }

.snapshot-table td {
  padding: 0.4rem 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
  text-align: right;
}

.snapshot-table td:first-child {
  text-align: left;
  color: var(--text-1);
}

.action-cell {
  display: flex;
  gap: 0.3rem;
  justify-content: flex-end;
}

.action-btn {
  background: transparent;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  color: var(--text-1);
  cursor: pointer;
  padding: 0.25rem 0.45rem;
  font-size: 0.85rem;
  transition: border-color 0.15s;
}

.action-btn:hover { border-color: var(--line-strong); }
.action-btn.delete:hover { border-color: #ff6f98; color: #ff6f98; }

.positive { color: #5af2b8; }
.negative { color: #ff6f98; }

@media (max-width: 900px) {
  .wealth-summary { grid-template-columns: 1fr; }
  .wealth-overview { grid-template-columns: 1fr; }
  .wealth-charts { grid-template-columns: 1fr; }
  .snapshot-form-grid { grid-template-columns: 1fr 1fr; }
}
</style>
