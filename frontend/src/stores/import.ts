import { defineStore } from 'pinia';

type ParsedLine = { id: string; label: string; values: number[] };
type ParsedYear = { year: number; lines: ParsedLine[] };
type ParsedWealth = { date: string; assets: { support: string; value: number }[]; liabilities: number };

export type ImportStatus = 'idle' | 'parsing' | 'ready' | 'success' | 'error';

type ImportState = {
  status: ImportStatus;
  errorMessage: string;
  parsedBudget: ParsedYear[];
  parsedWealth: ParsedWealth[];
  fileName: string;
};

export const useImportStore = defineStore('import', {
  state: (): ImportState => ({
    status: 'idle',
    errorMessage: '',
    parsedBudget: [],
    parsedWealth: [],
    fileName: '',
  }),
  actions: {
    startParsing(fileName: string) {
      this.status = 'parsing';
      this.errorMessage = '';
      this.parsedBudget = [];
      this.parsedWealth = [];
      this.fileName = fileName;
    },
    setReady(budget: ParsedYear[], wealth: ParsedWealth[]) {
      this.parsedBudget = budget;
      this.parsedWealth = wealth;
      this.status = 'ready';
    },
    setError(message: string) {
      this.status = 'error';
      this.errorMessage = message;
    },
    setSuccess() {
      this.status = 'success';
    },
    dismiss() {
      this.status = 'idle';
      this.errorMessage = '';
    },
    reset() {
      this.status = 'idle';
      this.errorMessage = '';
      this.parsedBudget = [];
      this.parsedWealth = [];
      this.fileName = '';
    },
  },
});
