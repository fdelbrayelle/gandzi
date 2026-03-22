<script setup lang="ts">
import { computed, ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { useBudgetStore, GROUP_ORDER, GROUP_LABEL_KEYS } from '../stores/budget';
import type { ExpenseGroup } from '../stores/budget';
import { messages } from '../i18n/messages';

const prefStore = usePreferencesStore();
const budgetStore = useBudgetStore();
const t = computed(() => messages[prefStore.locale]);

const selectedYear = ref(new Date().getFullYear());
const currentYear = new Date().getFullYear();
const currentMonth = new Date().getMonth(); // 0-based
const isCurrentYear = computed(() => selectedYear.value === currentYear);

function prevYear() { selectedYear.value--; }
function nextYear() { selectedYear.value++; }
function goToCurrentYear() { selectedYear.value = currentYear; }

type BudgetLine = {
  id: string;
  labelKey: string;
  group: ExpenseGroup;
  values: number[];
  dynamic?: boolean;
};

const monthKeys = [
  'monthJan', 'monthFeb', 'monthMar', 'monthApr', 'monthMay', 'monthJun',
  'monthJul', 'monthAug', 'monthSep', 'monthOct', 'monthNov', 'monthDec',
] as const;

function hasNonZero(values: number[]): boolean {
  return values.some((v) => v !== 0);
}

function lineLabel(labelKey: string): string {
  // Dynamic investment lines use __invest__SupportName as labelKey
  if (labelKey.startsWith('__invest__')) {
    return `${t.value.budgetInvestment ?? 'Investment'} — ${labelKey.slice(9)}`;
  }
  return t.value[labelKey] ?? labelKey;
}

const lines = computed<BudgetLine[]>(() => {
  const data = budgetStore.getYearData(selectedYear.value);
  return budgetStore.allLineDefs
    .filter((def) => {
      const values = data[def.id];
      if (!def.dynamic) return def.group === 'income' || hasNonZero(values ?? []);
      return values && hasNonZero(values);
    })
    .map((def) => ({
      id: def.id,
      labelKey: def.labelKey,
      group: def.group,
      values: data[def.id] ?? [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      dynamic: def.dynamic,
    }));
});

type GroupedSection = { group: ExpenseGroup; labelKey: string; lines: BudgetLine[] };

const groupedLines = computed<GroupedSection[]>(() => {
  const sections: GroupedSection[] = [];
  for (const group of GROUP_ORDER) {
    const groupLines = lines.value.filter((l) => l.group === group);
    // lines already filtered by hasNonZero (except income), so length > 0 means data exists
    if (groupLines.length > 0) {
      sections.push({ group, labelKey: GROUP_LABEL_KEYS[group], lines: groupLines });
    }
  }
  return sections;
});

function groupSubtotalMonth(section: GroupedSection, monthIdx: number): number {
  return section.lines.reduce((sum, l) => sum + (l.values[monthIdx] || 0), 0);
}

function groupSubtotalAnnual(section: GroupedSection): number {
  return section.lines.reduce((sum, l) => {
    const lineSum = l.values.reduce((a, v) => a + (Number.isFinite(v) ? v : 0), 0);
    return sum + lineSum;
  }, 0);
}

const annualIncome = computed(() => {
  const incomeRow = lines.value.find((line) => line.group === 'income');
  return (incomeRow?.values ?? []).reduce((sum, value) => sum + value, 0);
});

const monthlyNet = computed(() => {
  return monthKeys.map((_, monthIdx) => {
    const income = lines.value
      .filter((line) => line.group === 'income')
      .reduce((sum, line) => sum + (line.values[monthIdx] || 0), 0);
    const expenses = lines.value
      .filter((line) => line.group !== 'income')
      .reduce((sum, line) => sum + (line.values[monthIdx] || 0), 0);
    return income - expenses;
  });
});

const annualNet = computed(() => monthlyNet.value.reduce((sum, value) => sum + value, 0));

function annualTotal(line: BudgetLine): number {
  const sum = line.values.reduce((acc, value) => acc + (Number.isFinite(value) ? value : 0), 0);
  return line.group === 'income' ? sum : -sum;
}

function monthlyAverage(line: BudgetLine): number {
  return annualTotal(line) / 12;
}

function annualShare(line: BudgetLine): number {
  if (annualIncome.value <= 0 || line.group === 'income') return 100;
  return (Math.abs(annualTotal(line)) / annualIncome.value) * 100;
}

function euro(value: number): string {
  return `${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €`;
}

function onValueInput(lineId: string, monthIdx: number, raw: string): void {
  const parsed = Number(raw);
  budgetStore.setValue(selectedYear.value, lineId, monthIdx, Number.isFinite(parsed) ? parsed : 0);
}
</script>

<template>
  <div class="budget-matrix-card">
    <div class="year-nav">
      <button class="year-nav-btn" type="button" @click="prevYear">&larr;</button>
      <span class="year-nav-label" :class="{ 'current-year': isCurrentYear }">{{ selectedYear }}</span>
      <button class="year-nav-btn" type="button" @click="nextYear">&rarr;</button>
      <button v-if="!isCurrentYear" class="year-nav-today" type="button" @click="goToCurrentYear">&crarr; {{ currentYear }}</button>
    </div>
    <div class="matrix-scroll">
      <table class="budget-matrix">
        <thead>
          <tr>
            <th class="sticky-col left-header">{{ selectedYear }}</th>
            <th v-for="(key, idx) in monthKeys" :key="key" :class="{ 'current-month': isCurrentYear && idx === currentMonth }">{{ t[key] }}</th>
            <th>{{ t.budgetHeaderAnnual }}</th>
            <th>{{ t.budgetHeaderMonthly }}</th>
            <th>{{ t.budgetHeaderPctAnnual }}</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="section in groupedLines" :key="section.group">
            <!-- Group header -->
            <tr class="group-header-row" :class="`group-${section.group}`">
              <th class="sticky-col group-header" :colspan="16">{{ t[section.labelKey] }}</th>
            </tr>
            <!-- Lines in group -->
            <tr v-for="line in section.lines" :key="line.id" :class="`group-${section.group}-row`">
              <th class="sticky-col row-label">{{ lineLabel(line.labelKey) }}</th>
              <td v-for="(value, monthIdx) in line.values" :key="`${line.id}-${monthIdx}`">
                <input
                  class="cell-input"
                  :class="line.group === 'income' ? 'positive' : 'negative'"
                  type="number"
                  step="0.01"
                  :value="value"
                  @input="onValueInput(line.id, monthIdx, ($event.target as HTMLInputElement).value)"
                />
              </td>
              <td :class="annualTotal(line) >= 0 ? 'positive strong' : 'negative strong'">{{ euro(annualTotal(line)) }}</td>
              <td :class="monthlyAverage(line) >= 0 ? 'positive' : 'negative'">{{ euro(monthlyAverage(line)) }}</td>
              <td>{{ annualShare(line).toFixed(2) }}%</td>
            </tr>
            <!-- Group subtotal (skip for income, only 1 line) -->
            <tr v-if="section.group !== 'income' && section.lines.length > 1" class="group-subtotal-row" :class="`group-${section.group}-subtotal`">
              <th class="sticky-col row-label subtotal-label">{{ t[section.labelKey] }}</th>
              <td v-for="(_, monthIdx) in 12" :key="`sub-${section.group}-${monthIdx}`" class="negative strong">
                {{ euro(-groupSubtotalMonth(section, monthIdx)) }}
              </td>
              <td class="negative strong">{{ euro(-groupSubtotalAnnual(section)) }}</td>
              <td class="negative">{{ euro(-groupSubtotalAnnual(section) / 12) }}</td>
              <td>{{ annualIncome > 0 ? (groupSubtotalAnnual(section) / annualIncome * 100).toFixed(2) : '0.00' }}%</td>
            </tr>
          </template>

          <tr class="net-row">
            <th class="sticky-col row-label">{{ t.budgetFreeCashflow }}</th>
            <td v-for="(value, idx) in monthlyNet" :key="`net-${idx}`" :class="value >= 0 ? 'positive strong' : 'negative strong'">
              {{ euro(value) }}
            </td>
            <td :class="annualNet >= 0 ? 'positive strong' : 'negative strong'">{{ euro(annualNet) }}</td>
            <td :class="annualNet / 12 >= 0 ? 'positive' : 'negative'">{{ euro(annualNet / 12) }}</td>
            <td>{{ ((annualNet / annualIncome) * 100).toFixed(2) }}%</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.budget-matrix-card {
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(17, 20, 41, 0.72), rgba(24, 27, 57, 0.88));
  overflow: hidden;
}

.matrix-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.budget-matrix {
  width: 100%;
  min-width: 0;
  table-layout: auto;
  border-collapse: separate;
  border-spacing: 0;
  font-size: clamp(0.68rem, 0.8vw, 0.84rem);
}

.budget-matrix thead th {
  position: sticky;
  top: 0;
  z-index: 4;
  background: linear-gradient(140deg, rgba(47, 56, 103, 0.95), rgba(32, 41, 84, 0.95));
  color: #e8efff;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding: 0.52rem 0.35rem;
  border-bottom: 1px solid var(--line-strong);
  white-space: nowrap;
  font-size: clamp(0.65rem, 0.75vw, 0.82rem);
}

.left-header {
  text-align: left;
  min-width: 120px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sticky-col {
  position: sticky;
  left: 0;
  z-index: 3;
  background: rgba(16, 19, 41, 0.96);
}

.row-label {
  color: var(--text-1);
  font-weight: 600;
  text-align: left;
  padding: 0.52rem 0.45rem;
  border-right: 1px solid var(--line-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.budget-matrix td {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  text-align: right;
  padding: 0.22rem 0.2rem;
  white-space: nowrap;
}

.cell-input {
  width: 100%;
  min-width: 60px;
  max-width: 110px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  font-size: inherit;
  text-align: right;
  padding: 0.3rem 0.25rem;
  outline: none;
  color: inherit;
}

.cell-input:focus {
  border-color: var(--line-strong);
  background: rgba(124, 247, 255, 0.09);
}

.cell-input::-webkit-outer-spin-button,
.cell-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.cell-input[type='number'] {
  -moz-appearance: textfield;
}

.group-header-row th {
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 0.55rem 0.55rem 0.3rem;
  border-bottom: none;
}

.group-header {
  background: rgba(16, 19, 41, 0.96) !important;
}

.group-income th { color: #5af2b8; }
.group-investment th { color: var(--brand-violet); }
.group-house th { color: var(--brand-cyan); }
.group-fixed th { color: var(--brand-orange); }
.group-variable th { color: var(--brand-magenta); }

.group-income-row td { background: rgba(76, 255, 180, 0.03); }
.group-investment-row td { background: rgba(140, 123, 255, 0.04); }
.group-house-row td { background: rgba(124, 247, 255, 0.03); }
.group-fixed-row td { background: rgba(255, 154, 79, 0.03); }
.group-variable-row td { background: rgba(255, 79, 216, 0.03); }

.group-subtotal-row td,
.group-subtotal-row th {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  font-weight: 600;
  font-size: 0.8rem;
}

.subtotal-label {
  font-style: italic;
  opacity: 0.75;
}

.net-row td,
.net-row th {
  background: rgba(124, 247, 255, 0.08);
  border-top: 1px solid var(--line-strong);
  font-weight: 700;
}

.strong {
  font-weight: 700;
}

.positive {
  color: #5af2b8;
}

.negative {
  color: #ff6f98;
}

.year-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.7rem 1rem;
  background: linear-gradient(140deg, rgba(47, 56, 103, 0.95), rgba(32, 41, 84, 0.95));
  border-bottom: 1px solid var(--line-soft);
  position: relative;
}

.year-nav-label {
  font-size: 1.1rem;
  font-weight: 700;
  color: #e8efff;
  min-width: 4rem;
  text-align: center;
}

.year-nav-label.current-year {
  color: #ffd54f;
}

.year-nav-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  color: #e8efff;
  font-size: 1rem;
  padding: 0.3rem 0.7rem;
  cursor: pointer;
  transition: background 0.15s;
}

.year-nav-btn:hover {
  background: rgba(255, 255, 255, 0.16);
}

.year-nav-today {
  position: absolute;
  right: 1rem;
  background: rgba(255, 213, 79, 0.15);
  border: 1px solid rgba(255, 213, 79, 0.4);
  border-radius: 8px;
  color: #ffd54f;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.25rem 0.6rem;
  cursor: pointer;
  transition: background 0.15s;
}

.year-nav-today:hover {
  background: rgba(255, 213, 79, 0.25);
}

.current-month {
  color: #ffd54f !important;
  border-bottom: 2px solid #ffd54f;
}

@media (max-width: 1200px) {
  .budget-matrix { font-size: 0.75rem; }
  .cell-input { min-width: 52px; padding: 0.25rem 0.2rem; }
  .budget-matrix thead th { padding: 0.4rem 0.25rem; }
  .row-label { padding: 0.4rem 0.35rem; max-width: 150px; }
  .left-header { min-width: 100px; }
}

@media (max-width: 768px) {
  .budget-matrix { font-size: 0.7rem; }
  .cell-input { min-width: 44px; padding: 0.2rem 0.15rem; border-radius: 4px; }
  .budget-matrix thead th { padding: 0.3rem 0.2rem; font-size: 0.65rem; }
  .budget-matrix td { padding: 0.15rem 0.15rem; }
  .row-label { padding: 0.3rem 0.25rem; max-width: 110px; font-size: 0.68rem; }
  .left-header { min-width: 80px; }
  .year-nav { padding: 0.5rem 0.7rem; gap: 0.6rem; }
  .year-nav-label { font-size: 0.95rem; }
  .year-nav-btn { padding: 0.2rem 0.5rem; font-size: 0.85rem; }
  .group-header-row th { font-size: 0.68rem; padding: 0.4rem 0.3rem 0.2rem; }
}
</style>
