<script setup lang="ts">
import { computed, ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { useBudgetStore, BUDGET_LINE_IDS } from '../stores/budget';
import { useImportStore } from '../stores/import';
import { useWealthStore } from '../stores/wealth';
import type { WealthSnapshot } from '../stores/wealth';
import { messages } from '../i18n/messages';
import * as XLSX from 'xlsx';

type ParsedLine = { id: string; label: string; values: number[] };
type ParsedYear = { year: number; lines: ParsedLine[] };
type ParsedWealth = { date: string; assets: { support: string; value: number }[]; liabilities: number };

const store = usePreferencesStore();
const budgetStore = useBudgetStore();
const importStore = useImportStore();
const wealthStore = useWealthStore();
const t = computed(() => messages[store.locale]);

const fileInput = ref<HTMLInputElement | null>(null);
const dragOver = ref(false);

function onDropZoneClick() {
  fileInput.value?.click();
}

function onDragOver(e: DragEvent) {
  e.preventDefault();
  dragOver.value = true;
}

function onDragLeave() {
  dragOver.value = false;
}

function onDrop(e: DragEvent) {
  e.preventDefault();
  dragOver.value = false;
  const file = e.dataTransfer?.files[0];
  if (file) processFile(file);
}

function onFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (file) processFile(file);
}

async function processFile(file: File) {
  if (!store.llmApiKey) {
    importStore.setError(t.value.importNoApiKey);
    return;
  }

  importStore.startParsing(file.name);

  try {
    const text = await readFileContent(file);
    const result = await callLlm(text, file.name);
    importStore.setReady(result.budget, result.wealth);
  } catch (err) {
    importStore.setError(`${t.value.importError}: ${err instanceof Error ? err.message : String(err)}`);
  }
}

async function readFileContent(file: File): Promise<string> {
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (ext === 'csv') {
    return await file.text();
  }
  const buffer = await file.arrayBuffer();
  const workbook = XLSX.read(buffer, { type: 'array' });
  const sheets: string[] = [];
  for (const name of workbook.SheetNames) {
    const csv = XLSX.utils.sheet_to_csv(workbook.Sheets[name]);
    sheets.push(`--- Sheet: ${name} ---\n${csv}`);
  }
  return sheets.join('\n\n');
}

const MAX_CONTENT_CHARS = 80000; // ~20k tokens, leaves room for output

function buildLlmPrompt(fileContent: string, fileName: string): string {
  // Truncate if too large to avoid blowing the context window
  const content = fileContent.length > MAX_CONTENT_CHARS
    ? fileContent.slice(0, MAX_CONTENT_CHARS) + '\n\n[... file truncated for size ...]'
    : fileContent;
  return `You are a financial data parser. Given the following spreadsheet/CSV content from file "${fileName}", extract TWO types of data:

1) BUDGET DATA — map rows to these budget line IDs: ${BUDGET_LINE_IDS.join(', ')}
   For investments by support, use IDs like invest_<support>. Map these keywords to investments:
   - PEA, Plan d'Épargne en Actions -> invest_pea
   - CTO, Compte-Titres Ordinaire -> invest_cto
   - Livret A -> invest_livret_a
   - LDD, LDDS, Livret de Développement Durable -> invest_ldd
   - LEP, Livret d'Épargne Populaire -> invest_lep
   - PER, Plan d'Épargne Retraite -> invest_per
   - AV, Assurance Vie, Life Insurance -> invest_assurance_vie
   - SCPI, REIT, Real Estate Investment Trust -> invest_scpi
   - Crypto, Bitcoin, BTC, Ethereum, ETH -> invest_crypto
   - Or, Gold, Oro -> invest_gold
   - Savings Account, Épargne, Sparkonto -> invest_savings
   For each year, output 12 monthly values (Jan-Dec) per line.

2) WEALTH SNAPSHOTS — if the file contains asset valuations, portfolio balances, or patrimoine data, extract snapshots with date, assets by support, and liabilities.

Rules:
- Sheet names may represent years (e.g. "2024", "Budget 2026"). Use that year for data in that sheet.
- Match rows to the closest budget line ID:
  income, mortgage, rent, utilities, water (Housing)
  insurance, subscriptions (Fixed)
  groceries, transport, vacation, gifts, leisure, personal_care, health, taxes, property_tax, bank_fees, misc (Variable)
  Examples: "Salaire"/"Revenue" -> "income", "Loyer"/"Rent" -> "rent", "Prêt immobilier"/"Mortgage" -> "mortgage", "Courses"/"Groceries" -> "groceries", "Cadeaux" -> "gifts", "Loisirs" -> "leisure", "Santé" -> "health", "Impôts" -> "taxes", "Taxe foncière" -> "property_tax", "Frais bancaires" -> "bank_fees"
- If a category doesn't match any budget line, skip it
- Values should be positive numbers
- If a month has no data, use 0
- Support any language in the input
- For wealth: detect asset holdings like PEA, CTO, Livret A, Crypto, Gold, Real Estate, etc.
- Sheet names like "Patrimoine", "Wealth", "Assets", "Portfolio", "Vermögen", "Patrimonio" etc. indicate wealth data.
- Wealth data may also appear as rows with dates and asset values in any sheet.

Respond with ONLY valid JSON, no markdown. Use compact format:
{
  "budget": [
    {"year": 2026, "lines": [{"id": "income", "label": "Original Label", "values": [v1,...,v12]}]}
  ],
  "wealth": [
    {"date": "2026-01-31", "assets": [{"support": "PEA", "value": 15000}], "liabilities": 120000}
  ]
}

If no wealth data found, set "wealth" to []. If no budget data found, set "budget" to [].

File content:
${content}`;
}

type LlmResult = { budget: ParsedYear[]; wealth: ParsedWealth[] };

async function callLlm(fileContent: string, fileName: string): Promise<LlmResult> {
  const prompt = buildLlmPrompt(fileContent, fileName);

  let url: string;
  let headers: Record<string, string>;
  let body: string;

  if (store.llmProvider === 'anthropic') {
    url = 'https://api.anthropic.com/v1/messages';
    headers = {
      'Content-Type': 'application/json',
      'x-api-key': store.llmApiKey,
      'anthropic-version': '2023-06-01',
      'anthropic-dangerous-direct-browser-access': 'true',
    };
    body = JSON.stringify({
      model: store.llmModel,
      max_tokens: 32768,
      messages: [{ role: 'user', content: prompt }],
    });
  } else if (store.llmProvider === 'openai') {
    url = 'https://api.openai.com/v1/chat/completions';
    headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${store.llmApiKey}`,
    };
    body = JSON.stringify({
      model: store.llmModel,
      messages: [{ role: 'user', content: prompt }],
      max_tokens: 32768,
    });
  } else {
    url = `https://generativelanguage.googleapis.com/v1beta/models/${store.llmModel}:generateContent?key=${store.llmApiKey}`;
    headers = { 'Content-Type': 'application/json' };
    body = JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
    });
  }

  const response = await fetch(url, { method: 'POST', headers, body });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`API ${response.status}: ${errorText.slice(0, 200)}`);
  }

  const json = await response.json();
  let text: string;

  let truncated = false;
  if (store.llmProvider === 'anthropic') {
    text = json.content?.[0]?.text ?? '';
    truncated = json.stop_reason === 'max_tokens';
  } else if (store.llmProvider === 'openai') {
    text = json.choices?.[0]?.message?.content ?? '';
    truncated = json.choices?.[0]?.finish_reason === 'length';
  } else {
    text = json.candidates?.[0]?.content?.parts?.[0]?.text ?? '';
    truncated = json.candidates?.[0]?.finishReason === 'MAX_TOKENS';
  }

  if (truncated) {
    throw new Error('AI response was truncated — the file may be too large. Try importing fewer sheets or a smaller file.');
  }

  // Strip markdown code fences
  text = text.replace(/^```(?:json)?\s*\n?/i, '').replace(/\n?```\s*$/i, '').trim();

  // Extract JSON: find the first { or [ and the last } or ]
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    // Try to extract JSON from surrounding text
    const jsonStart = text.search(/[\[{]/);
    const jsonEndBrace = text.lastIndexOf('}');
    const jsonEndBracket = text.lastIndexOf(']');
    const jsonEnd = Math.max(jsonEndBrace, jsonEndBracket);
    if (jsonStart >= 0 && jsonEnd > jsonStart) {
      const extracted = text.slice(jsonStart, jsonEnd + 1);
      try {
        raw = JSON.parse(extracted);
      } catch {
        throw new Error(`AI returned invalid JSON. First 200 chars: ${text.slice(0, 200)}`);
      }
    } else {
      throw new Error(`AI returned invalid JSON. First 200 chars: ${text.slice(0, 200)}`);
    }
  }

  // Handle both old array format and new object format
  let budget: ParsedYear[];
  let wealth: ParsedWealth[] = [];

  if (Array.isArray(raw)) {
    budget = raw as ParsedYear[];
  } else {
    const obj = raw as { budget?: ParsedYear[]; wealth?: ParsedWealth[] };
    budget = obj.budget ?? [];
    wealth = obj.wealth ?? [];
  }

  for (const yearEntry of budget) {
    if (typeof yearEntry.year !== 'number') throw new Error('Invalid year in response');
    for (const line of yearEntry.lines) {
      if (!Array.isArray(line.values)) throw new Error(`Line ${line.id} must have values array`);
      // Pad with 0s if fewer than 12 months, trim if more
      while (line.values.length < 12) line.values.push(0);
      if (line.values.length > 12) line.values = line.values.slice(0, 12);
    }
  }

  return { budget, wealth };
}

function applyImport() {
  // Apply budget data
  const data: Record<number, Record<string, number[]>> = {};
  for (const yearEntry of importStore.parsedBudget) {
    data[yearEntry.year] = {};
    for (const line of yearEntry.lines) {
      // Ensure investment lines exist in the budget store
      if (line.id.startsWith('invest_')) {
        const support = line.id.replace('invest_', '').replace(/_/g, ' ');
        budgetStore.ensureInvestmentLine(support);
      }
      data[yearEntry.year][line.id] = line.values.map((v) => Math.abs(Number(v) || 0));
    }
  }
  budgetStore.mergeImportData(data);

  // Apply wealth data
  if (importStore.parsedWealth.length > 0) {
    const snapshots: WealthSnapshot[] = importStore.parsedWealth.map((w) => ({
      date: w.date,
      assets: (w.assets ?? []).map((a) => ({ support: a.support, value: Math.abs(Number(a.value) || 0) })),
      liabilities: Math.abs(Number(w.liabilities) || 0),
    }));
    wealthStore.mergeImportSnapshots(snapshots);
  }

  importStore.setSuccess();
}

function cancel() {
  importStore.reset();
}

const totalYears = computed(() => importStore.parsedBudget.length);
const totalLines = computed(() => importStore.parsedBudget.reduce((sum, y) => sum + y.lines.length, 0));
const totalSnapshots = computed(() => importStore.parsedWealth.length);
</script>

<template>
  <div class="import-panel">
    <!-- File drop zone -->
    <div
      v-if="importStore.status === 'idle' || importStore.status === 'success' || importStore.status === 'parsing'"
      class="drop-zone"
      :class="{ 'drop-zone-active': dragOver }"
      @click="onDropZoneClick"
      @dragover="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
    >
      <input
        ref="fileInput"
        type="file"
        accept=".csv,.xlsx,.xls,.ods"
        class="file-input-hidden"
        @change="onFileSelect"
      />
      <div class="drop-zone-icon">&#128196;</div>
      <p class="drop-zone-text">{{ t.importFileDrop }}</p>
      <p class="drop-zone-hint">{{ t.importFileHint }}</p>
    </div>

    <!-- Success message (inline) -->
    <div v-if="importStore.status === 'success'" class="import-message import-success-msg">
      {{ t.importSuccess }}
    </div>

    <!-- Error message (inline, for context when on this page) -->
    <div v-if="importStore.status === 'error'" class="import-message import-error-msg">
      {{ importStore.errorMessage }}
      <button class="dismiss-link" type="button" @click="importStore.dismiss()">&#10005;</button>
    </div>

    <!-- Parsing state handled by global toast -->

    <!-- Preview -->
    <div v-if="importStore.status === 'ready'" class="preview-section">
      <h3 class="preview-title">{{ t.importPreview }}</h3>
      <p class="preview-stats">
        {{ totalYears }} {{ t.importYearsFound }} &mdash; {{ totalLines }} {{ t.importLinesFound }}
        <template v-if="totalSnapshots > 0"> &mdash; {{ totalSnapshots }} {{ t.importSnapshotsFound }}</template>
      </p>

      <div v-for="yearEntry in importStore.parsedBudget" :key="yearEntry.year" class="preview-year">
        <h4 class="preview-year-title">{{ yearEntry.year }}</h4>
        <table class="preview-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>{{ t.importPreview }}</th>
              <th v-for="(_, i) in 12" :key="i">{{ t[`month${['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][i]}`] }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="line in yearEntry.lines" :key="line.id">
              <td class="preview-id">{{ line.id }}</td>
              <td class="preview-label">{{ line.label }}</td>
              <td v-for="(val, i) in line.values" :key="i" class="preview-val">{{ val }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Wealth preview -->
      <div v-if="importStore.parsedWealth.length > 0" class="preview-year">
        <h4 class="preview-year-title">{{ t.wealthTitle }}</h4>
        <table class="preview-table">
          <thead>
            <tr>
              <th>{{ t.wealthDate }}</th>
              <th>{{ t.wealthLiabilities }}</th>
              <th v-for="snap in [importStore.parsedWealth[0]]" :key="'hdr'">
                <template v-for="asset in snap.assets" :key="asset.support">
                  <!-- headers from first snapshot -->
                </template>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="snap in importStore.parsedWealth" :key="snap.date">
              <td>{{ snap.date }}</td>
              <td class="preview-val">{{ snap.liabilities }}</td>
              <td v-for="asset in snap.assets" :key="asset.support" class="preview-val">
                {{ asset.support }}: {{ asset.value }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="preview-actions">
        <button class="login-btn" type="button" @click="applyImport">{{ t.importApply }}</button>
        <button class="cancel-btn" type="button" @click="cancel">{{ t.importCancel }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-panel { max-width: 100%; }

.drop-zone {
  border: 2px dashed var(--line-soft);
  border-radius: 16px;
  padding: 2.5rem 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  background: rgba(255, 255, 255, 0.02);
}

.drop-zone:hover, .drop-zone-active {
  border-color: var(--brand-cyan);
  background: rgba(124, 247, 255, 0.05);
}

.file-input-hidden { display: none; }

.drop-zone-icon { font-size: 2.5rem; margin-bottom: 0.5rem; }
.drop-zone-text { color: var(--text-1); font-size: 1rem; margin: 0; }
.drop-zone-hint { color: var(--text-2); font-size: 0.82rem; margin: 0.4rem 0 0; }

.import-message {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.import-success-msg {
  background: rgba(90, 242, 184, 0.1);
  border: 1px solid rgba(90, 242, 184, 0.35);
  color: #5af2b8;
}

.import-error-msg {
  background: rgba(255, 111, 152, 0.1);
  border: 1px solid rgba(255, 111, 152, 0.35);
  color: #ff6f98;
}

.dismiss-link {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 1rem;
  padding: 0 0.3rem;
  opacity: 0.7;
}

.dismiss-link:hover { opacity: 1; }

.parsing-state {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
  justify-content: center;
  color: var(--text-1);
}

.parsing-spinner {
  width: 24px; height: 24px;
  border: 3px solid var(--line-soft);
  border-top-color: var(--brand-cyan);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.preview-section { margin-top: 1rem; }
.preview-title { margin: 0 0 0.3rem; font-size: 1.1rem; color: var(--text-0); }
.preview-stats { color: var(--text-2); font-size: 0.85rem; margin: 0 0 1rem; }
.preview-year { margin-bottom: 1.2rem; }
.preview-year-title { margin: 0 0 0.5rem; font-size: 1rem; color: var(--brand-cyan); }

.preview-table {
  width: 100%; border-collapse: collapse; font-size: 0.8rem; overflow-x: auto; display: block;
}

.preview-table thead th {
  background: rgba(47, 56, 103, 0.6); color: #e8efff; padding: 0.4rem 0.35rem; white-space: nowrap; font-weight: 600;
}

.preview-table td {
  padding: 0.35rem 0.35rem; border-bottom: 1px solid rgba(255, 255, 255, 0.06); white-space: nowrap;
}

.preview-id { color: var(--brand-cyan); font-family: monospace; }
.preview-label { color: var(--text-2); }
.preview-val { text-align: right; color: var(--text-1); }

.preview-actions { display: flex; gap: 0.7rem; margin-top: 1rem; }

.cancel-btn {
  border: 1px solid var(--line-soft);
  background: transparent;
  color: var(--text-1);
  border-radius: var(--radius-md);
  padding: 0.7rem 1.2rem;
  cursor: pointer;
  font-size: 0.9rem;
  transition: border-color 0.15s;
}

.cancel-btn:hover { border-color: var(--line-strong); }
</style>
