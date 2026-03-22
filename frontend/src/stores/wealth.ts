import { defineStore } from 'pinia';

export type AssetEntry = {
  support: string; // e.g. 'PEA', 'CTO', 'Gold', 'Crypto', 'Savings', 'Real Estate', 'Other'
  value: number;
};

export type WealthSnapshot = {
  date: string; // YYYY-MM-DD
  assets: AssetEntry[];
  liabilities: number; // debts, mortgages, etc.
};

export const DEFAULT_SUPPORTS = ['PEA', 'CTO', 'Livret A', 'Crypto', 'Gold', 'Real Estate', 'Savings', 'Other'];

type WealthState = {
  snapshots: WealthSnapshot[];
  customSupports: string[];
};

function financialWealth(snap: WealthSnapshot): number {
  const nonRealEstate = snap.assets.filter((a) => a.support !== 'Real Estate');
  return nonRealEstate.reduce((sum, a) => sum + a.value, 0);
}

function grossWealth(snap: WealthSnapshot): number {
  return snap.assets.reduce((sum, a) => sum + a.value, 0);
}

function netWealth(snap: WealthSnapshot): number {
  return grossWealth(snap) - snap.liabilities;
}

export { financialWealth, grossWealth, netWealth };

export const useWealthStore = defineStore('wealth', {
  state: (): WealthState => ({
    snapshots: [],
    customSupports: [],
  }),
  getters: {
    allSupports(state): string[] {
      const custom = state.customSupports.filter((s) => !DEFAULT_SUPPORTS.includes(s));
      return [...DEFAULT_SUPPORTS, ...custom];
    },
  },
  actions: {
    addSnapshot(snapshot: WealthSnapshot): void {
      this.snapshots.push(snapshot);
      this.snapshots.sort((a, b) => a.date.localeCompare(b.date));
      this.syncCustomSupports(snapshot);
    },
    updateSnapshot(index: number, snapshot: WealthSnapshot): void {
      if (index >= 0 && index < this.snapshots.length) {
        this.snapshots[index] = snapshot;
        this.syncCustomSupports(snapshot);
      }
    },
    removeSnapshot(index: number): void {
      if (index >= 0 && index < this.snapshots.length) {
        this.snapshots.splice(index, 1);
      }
    },
    addCustomSupport(support: string): void {
      if (support && !this.allSupports.includes(support)) {
        this.customSupports.push(support);
      }
    },
    syncCustomSupports(snapshot: WealthSnapshot): void {
      for (const asset of snapshot.assets) {
        if (!DEFAULT_SUPPORTS.includes(asset.support) && !this.customSupports.includes(asset.support)) {
          this.customSupports.push(asset.support);
        }
      }
    },
    mergeImportSnapshots(imported: WealthSnapshot[]): void {
      for (const snap of imported) {
        const existing = this.snapshots.findIndex((s) => s.date === snap.date);
        if (existing >= 0) {
          this.snapshots[existing] = snap;
        } else {
          this.snapshots.push(snap);
        }
        this.syncCustomSupports(snap);
      }
      this.snapshots.sort((a, b) => a.date.localeCompare(b.date));
    },
  },
});
