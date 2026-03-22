import { defineStore } from 'pinia';
import { apiGet, apiPut, apiPatch } from '../composables/useApi';

export type ExpenseGroup = 'income' | 'investment' | 'house' | 'fixed' | 'variable';

export type LineDef = {
  id: string;
  labelKey: string;
  group: ExpenseGroup;
  defaultValues: number[];
  dynamic?: boolean;
};

export const GROUP_ORDER: ExpenseGroup[] = ['income', 'investment', 'house', 'fixed', 'variable'];

export const GROUP_LABEL_KEYS: Record<ExpenseGroup, string> = {
  income: 'groupIncome',
  investment: 'groupInvestment',
  house: 'groupHouse',
  fixed: 'groupFixed',
  variable: 'groupVariable',
};

export const LINE_DEFS: LineDef[] = [
  // Income
  { id: 'income', labelKey: 'budgetIncome', group: 'income', defaultValues: [4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400] },
  // House
  { id: 'mortgage', labelKey: 'budgetMortgage', group: 'house', defaultValues: [1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400] },
  { id: 'rent', labelKey: 'budgetRent', group: 'house', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'utilities', labelKey: 'budgetUtilities', group: 'house', defaultValues: [194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194] },
  { id: 'water', labelKey: 'budgetWater', group: 'house', defaultValues: [35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35] },
  // Fixed expenses
  { id: 'insurance', labelKey: 'budgetInsurance', group: 'fixed', defaultValues: [174, 174, 174, 174, 174, 174, 174, 174, 174, 174, 174, 174] },
  { id: 'subscriptions', labelKey: 'budgetSubscriptions', group: 'fixed', defaultValues: [130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130] },
  // Variable expenses
  { id: 'groceries', labelKey: 'budgetGroceries', group: 'variable', defaultValues: [500, 485, 470, 490, 510, 500, 520, 505, 495, 500, 515, 510] },
  { id: 'transport', labelKey: 'budgetTransport', group: 'variable', defaultValues: [90, 80, 75, 88, 95, 92, 96, 94, 87, 90, 91, 93] },
  { id: 'vacation', labelKey: 'budgetVacation', group: 'variable', defaultValues: [0, 0, 0, 300, 0, 0, 1100, 0, 0, 0, 0, 300] },
  { id: 'gifts', labelKey: 'budgetGifts', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'leisure', labelKey: 'budgetLeisure', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'personal_care', labelKey: 'budgetPersonalCare', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'health', labelKey: 'budgetHealth', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'taxes', labelKey: 'budgetTaxes', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'property_tax', labelKey: 'budgetPropertyTax', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'bank_fees', labelKey: 'budgetBankFees', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'misc', labelKey: 'budgetMisc', group: 'variable', defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
];

export const BUDGET_LINE_IDS = LINE_DEFS.map((d) => d.id);

type BudgetState = {
  yearData: Record<number, Record<string, number[]>>;
  investmentLines: LineDef[];
};

const STORAGE_KEY = 'gandzi_budget_v1';

export const useBudgetStore = defineStore('budget', {
  state: (): BudgetState => ({
    yearData: {},
    investmentLines: [],
  }),
  getters: {
    allLineDefs(state): LineDef[] {
      return [...LINE_DEFS, ...state.investmentLines];
    },
    allLineIds(state): string[] {
      return [...BUDGET_LINE_IDS, ...state.investmentLines.map((l) => l.id)];
    },
  },
  actions: {
    async initFromStorage() {
      // Try API first, fall back to localStorage
      type ApiResponse = { yearData: Record<string, Record<string, number[]>>; investmentLines: { id: string; labelKey: string; defaultValues: number[] }[] };
      const apiData = await apiGet<ApiResponse>('/api/v1/budget');
      if (apiData && Object.keys(apiData.yearData).length > 0) {
        // Convert string keys to number keys
        const yearData: Record<number, Record<string, number[]>> = {};
        for (const [yearStr, lines] of Object.entries(apiData.yearData)) {
          yearData[Number(yearStr)] = lines;
        }
        this.yearData = yearData;
        this.investmentLines = (apiData.investmentLines || []).map((l) => ({
          id: l.id,
          labelKey: l.labelKey,
          group: 'investment' as const,
          defaultValues: l.defaultValues,
          dynamic: true,
        }));
        this.saveToStorage();
        return;
      }
      // Fall back to localStorage
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        const parsed = JSON.parse(raw) as Partial<BudgetState>;
        if (parsed.yearData) this.yearData = parsed.yearData;
        if (parsed.investmentLines) this.investmentLines = parsed.investmentLines;
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    },
    saveToStorage() {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
        yearData: this.yearData,
        investmentLines: this.investmentLines,
      }));
    },
    async saveToApi() {
      const investLines = this.investmentLines.map((l) => ({
        id: l.id,
        labelKey: l.labelKey,
        defaultValues: l.defaultValues,
      }));
      await apiPut('/api/v1/budget', {
        yearData: this.yearData,
        investmentLines: investLines,
      });
    },
    ensureInvestmentLine(support: string): void {
      const id = `invest_${support.toLowerCase().replace(/\s+/g, '_')}`;
      if (!this.investmentLines.some((l) => l.id === id)) {
        this.investmentLines.push({
          id,
          labelKey: `__invest__${support}`,
          group: 'investment',
          defaultValues: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
          dynamic: true,
        });
      }
    },
    getYearData(year: number): Record<string, number[]> {
      if (!this.yearData[year]) {
        const data: Record<string, number[]> = {};
        for (const def of this.allLineDefs) {
          data[def.id] = [...def.defaultValues];
        }
        this.yearData[year] = data;
      }
      const data = this.yearData[year];
      for (const def of this.investmentLines) {
        if (!data[def.id]) {
          data[def.id] = [...def.defaultValues];
        }
      }
      return data;
    },
    setValue(year: number, lineId: string, monthIdx: number, value: number): void {
      this.getYearData(year)[lineId][monthIdx] = value;
      this.saveToStorage();
      // Debounced API sync
      apiPatch('/api/v1/budget/cell', { year, lineId, monthIdx, value });
    },
    mergeImportData(data: Record<number, Record<string, number[]>>): void {
      for (const [yearStr, lineMap] of Object.entries(data)) {
        const year = Number(yearStr);
        const target = this.getYearData(year);
        for (const [lineId, values] of Object.entries(lineMap)) {
          if (lineId.startsWith('invest_') && !this.allLineIds.includes(lineId)) {
            const support = lineId.replace('invest_', '').replace(/_/g, ' ');
            this.ensureInvestmentLine(support);
            if (!target[lineId]) target[lineId] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
          }
          if (target[lineId] && Array.isArray(values) && values.length === 12) {
            target[lineId] = values.map((v) => Math.abs(Number(v) || 0));
          }
        }
      }
      this.saveToStorage();
      this.saveToApi();
    },
  },
});
