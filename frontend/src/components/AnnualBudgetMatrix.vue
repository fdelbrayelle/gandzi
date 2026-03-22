<script setup lang="ts">
import { computed, ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { useBudgetStore, LINE_DEFS } from '../stores/budget';
import { messages } from '../i18n/messages';

const prefStore = usePreferencesStore();
const budgetStore = useBudgetStore();
const t = computed(() => messages[prefStore.locale]);

const selectedYear = ref(new Date().getFullYear());

function prevYear() { selectedYear.value--; }
function nextYear() { selectedYear.value++; }

type BudgetLine = {
  id: string;
  labelKey: string;
  kind: 'income' | 'expense';
  values: number[];
};

const monthKeys = [
  'monthJan', 'monthFeb', 'monthMar', 'monthApr', 'monthMay', 'monthJun',
  'monthJul', 'monthAug', 'monthSep', 'monthOct', 'monthNov', 'monthDec',
] as const;

const lines = computed<BudgetLine[]>(() => {
  const data = budgetStore.getYearData(selectedYear.value);
  return LINE_DEFS.map((def) => ({
    id: def.id,
    labelKey: def.labelKey,
    kind: def.kind,
    values: data[def.id],
  }));
});

const annualIncome = computed(() => {
  const incomeRow = lines.value.find((line) => line.kind === 'income');
  return (incomeRow?.values ?? []).reduce((sum, value) => sum + value, 0);
});

const monthlyNet = computed(() => {
  return monthKeys.map((_, monthIdx) => {
    const income = lines.value
      .filter((line) => line.kind === 'income')
      .reduce((sum, line) => sum + (line.values[monthIdx] || 0), 0);
    const expenses = lines.value
      .filter((line) => line.kind === 'expense')
      .reduce((sum, line) => sum + (line.values[monthIdx] || 0), 0);
    return income - expenses;
  });
});

const annualNet = computed(() => monthlyNet.value.reduce((sum, value) => sum + value, 0));

function annualTotal(line: BudgetLine): number {
  const sum = line.values.reduce((acc, value) => acc + (Number.isFinite(value) ? value : 0), 0);
  return line.kind === 'expense' ? -sum : sum;
}

function monthlyAverage(line: BudgetLine): number {
  return annualTotal(line) / 12;
}

function annualShare(line: BudgetLine): number {
  if (annualIncome.value <= 0 || line.kind === 'income') return 100;
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
      <span class="year-nav-label" :class="{ 'current-year': selectedYear === new Date().getFullYear() }">{{ selectedYear }}</span>
      <button class="year-nav-btn" type="button" @click="nextYear">&rarr;</button>
    </div>
    <div class="matrix-scroll">
      <table class="budget-matrix">
        <thead>
          <tr>
            <th class="sticky-col left-header">{{ selectedYear }}</th>
            <th v-for="key in monthKeys" :key="key">{{ t[key] }}</th>
            <th>{{ t.budgetHeaderAnnual }}</th>
            <th>{{ t.budgetHeaderMonthly }}</th>
            <th>{{ t.budgetHeaderPctAnnual }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="line in lines" :key="line.id" :class="line.kind === 'income' ? 'income-row' : 'expense-row'">
            <th class="sticky-col row-label">{{ t[line.labelKey] }}</th>
            <td v-for="(value, monthIdx) in line.values" :key="`${line.id}-${monthIdx}`">
              <input
                class="cell-input"
                :class="line.kind === 'income' ? 'positive' : 'negative'"
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
}

.budget-matrix {
  min-width: 1320px;
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 0.84rem;
}

.budget-matrix thead th {
  position: sticky;
  top: 0;
  z-index: 4;
  background: linear-gradient(140deg, rgba(47, 56, 103, 0.95), rgba(32, 41, 84, 0.95));
  color: #e8efff;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding: 0.52rem 0.45rem;
  border-bottom: 1px solid var(--line-strong);
  white-space: nowrap;
}

.left-header {
  text-align: left;
  min-width: 170px;
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
  padding: 0.52rem 0.55rem;
  border-right: 1px solid var(--line-soft);
}

.budget-matrix td {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  text-align: right;
  padding: 0.22rem 0.3rem;
  white-space: nowrap;
}

.cell-input {
  width: 100%;
  min-width: 82px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.82rem;
  text-align: right;
  padding: 0.34rem 0.38rem;
  outline: none;
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

.income-row td {
  background: rgba(76, 255, 180, 0.03);
}

.expense-row td {
  background: rgba(255, 98, 141, 0.03);
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
  padding: 0.7rem 0;
  background: linear-gradient(140deg, rgba(47, 56, 103, 0.95), rgba(32, 41, 84, 0.95));
  border-bottom: 1px solid var(--line-soft);
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
</style>
