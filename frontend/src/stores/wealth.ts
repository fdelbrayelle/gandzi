import { defineStore } from 'pinia';

export type AssetEntry = {
  support: string;
  value: number;
};

export type WealthSnapshot = {
  date: string; // YYYY-MM-DD
  assets: AssetEntry[];
  liabilities: number;
};

export const DEFAULT_SUPPORTS = ['PEA', 'CTO', 'Livret A', 'Crypto', 'Gold', 'Real Estate', 'Savings', 'Other'];

const STORAGE_KEY = 'gandzi_wealth_v1';

type WealthState = {
  snapshots: WealthSnapshot[];
  customSupports: string[];
};

function financialWealth(snap: WealthSnapshot, _coOwnershipPct = 100): number {
  const nonRealEstate = snap.assets.filter((a) => a.support !== 'Real Estate');
  return nonRealEstate.reduce((sum, a) => sum + a.value, 0);
}

function grossWealth(snap: WealthSnapshot, coOwnershipPct = 100): number {
  const share = coOwnershipPct / 100;
  return snap.assets.reduce((sum, a) => {
    if (a.support === 'Real Estate') return sum + a.value * share;
    return sum + a.value;
  }, 0);
}

function netWealth(snap: WealthSnapshot, coOwnershipPct = 100): number {
  const share = coOwnershipPct / 100;
  // Liabilities (mortgage) are also shared proportionally
  return grossWealth(snap, coOwnershipPct) - snap.liabilities * share;
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
    initFromStorage() {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        const parsed = JSON.parse(raw) as Partial<WealthState>;
        if (parsed.snapshots) this.snapshots = parsed.snapshots;
        if (parsed.customSupports) this.customSupports = parsed.customSupports;
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    },
    saveToStorage() {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
        snapshots: this.snapshots,
        customSupports: this.customSupports,
      }));
    },
    addSnapshot(snapshot: WealthSnapshot): void {
      this.snapshots.push(snapshot);
      this.snapshots.sort((a, b) => a.date.localeCompare(b.date));
      this.syncCustomSupports(snapshot);
      this.saveToStorage();
    },
    updateSnapshot(index: number, snapshot: WealthSnapshot): void {
      if (index >= 0 && index < this.snapshots.length) {
        this.snapshots[index] = snapshot;
        this.syncCustomSupports(snapshot);
        this.saveToStorage();
      }
    },
    removeSnapshot(index: number): void {
      if (index >= 0 && index < this.snapshots.length) {
        this.snapshots.splice(index, 1);
        this.saveToStorage();
      }
    },
    addCustomSupport(support: string): void {
      if (support && !this.allSupports.includes(support)) {
        this.customSupports.push(support);
        this.saveToStorage();
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
      this.saveToStorage();
    },
  },
});
