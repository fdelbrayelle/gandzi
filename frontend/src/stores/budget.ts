import { defineStore } from 'pinia';

type RowKind = 'income' | 'expense';

export type LineDef = {
  id: string;
  labelKey: string;
  kind: RowKind;
  defaultValues: number[];
};

export const LINE_DEFS: LineDef[] = [
  { id: 'income', labelKey: 'budgetIncome', kind: 'income', defaultValues: [4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400, 4400] },
  { id: 'mortgage', labelKey: 'budgetMortgage', kind: 'expense', defaultValues: [1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1400] },
  { id: 'groceries', labelKey: 'budgetGroceries', kind: 'expense', defaultValues: [500, 485, 470, 490, 510, 500, 520, 505, 495, 500, 515, 510] },
  { id: 'transport', labelKey: 'budgetTransport', kind: 'expense', defaultValues: [90, 80, 75, 88, 95, 92, 96, 94, 87, 90, 91, 93] },
  { id: 'utilities', labelKey: 'budgetUtilities', kind: 'expense', defaultValues: [194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194, 194] },
  { id: 'water', labelKey: 'budgetWater', kind: 'expense', defaultValues: [35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35] },
  { id: 'insurance', labelKey: 'budgetInsurance', kind: 'expense', defaultValues: [174, 174, 174, 174, 174, 174, 174, 174, 174, 174, 174, 174] },
  { id: 'subscriptions', labelKey: 'budgetSubscriptions', kind: 'expense', defaultValues: [130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130, 130] },
  { id: 'vacation', labelKey: 'budgetVacation', kind: 'expense', defaultValues: [0, 0, 0, 300, 0, 0, 1100, 0, 0, 0, 0, 300] },
];

export const BUDGET_LINE_IDS = LINE_DEFS.map((d) => d.id);

type BudgetState = {
  yearData: Record<number, Record<string, number[]>>;
};

export const useBudgetStore = defineStore('budget', {
  state: (): BudgetState => ({
    yearData: {},
  }),
  actions: {
    getYearData(year: number): Record<string, number[]> {
      if (!this.yearData[year]) {
        const data: Record<string, number[]> = {};
        for (const def of LINE_DEFS) {
          data[def.id] = [...def.defaultValues];
        }
        this.yearData[year] = data;
      }
      return this.yearData[year];
    },
    setValue(year: number, lineId: string, monthIdx: number, value: number): void {
      this.getYearData(year)[lineId][monthIdx] = value;
    },
    mergeImportData(data: Record<number, Record<string, number[]>>): void {
      for (const [yearStr, lineMap] of Object.entries(data)) {
        const year = Number(yearStr);
        const target = this.getYearData(year);
        for (const [lineId, values] of Object.entries(lineMap)) {
          if (target[lineId] && Array.isArray(values) && values.length === 12) {
            target[lineId] = values.map((v) => Math.abs(Number(v) || 0));
          }
        }
      }
    },
  },
});
