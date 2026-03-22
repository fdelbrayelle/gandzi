<script setup lang="ts">
import { computed, ref } from 'vue';
import { usePreferencesStore } from '../stores/preferences';
import { useBudgetStore, BUDGET_LINE_IDS } from '../stores/budget';
import { messages } from '../i18n/messages';
import * as XLSX from 'xlsx';

const store = usePreferencesStore();
const budgetStore = useBudgetStore();
const t = computed(() => messages[store.locale]);

type ParsedLine = { id: string; label: string; values: number[] };
type ParsedYear = { year: number; lines: ParsedLine[] };

const fileInput = ref<HTMLInputElement | null>(null);
const dragOver = ref(false);
const status = ref<'idle' | 'parsing' | 'preview' | 'success' | 'error'>('idle');
const errorMessage = ref('');
const parsedData = ref<ParsedYear[]>([]);


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
    status.value = 'error';
    errorMessage.value = t.value.importNoApiKey;
    return;
  }

  status.value = 'parsing';
  errorMessage.value = '';

  try {
    const text = await readFileContent(file);
    const result = await callLlm(text, file.name);
    parsedData.value = result;
    status.value = 'preview';
  } catch (err) {
    status.value = 'error';
    errorMessage.value = `${t.value.importError}: ${err instanceof Error ? err.message : String(err)}`;
  }
}

async function readFileContent(file: File): Promise<string> {
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (ext === 'csv') {
    return await file.text();
  }
  // Excel / ODS files
  const buffer = await file.arrayBuffer();
  const workbook = XLSX.read(buffer, { type: 'array' });
  const sheets: string[] = [];
  for (const name of workbook.SheetNames) {
    const csv = XLSX.utils.sheet_to_csv(workbook.Sheets[name]);
    sheets.push(`--- Sheet: ${name} ---\n${csv}`);
  }
  return sheets.join('\n\n');
}

function buildLlmPrompt(fileContent: string, fileName: string): string {
  return `You are a budget data parser. Given the following spreadsheet/CSV content from file "${fileName}", extract budget data and map it to these budget line IDs:

Budget line IDs: ${BUDGET_LINE_IDS.join(', ')}

For each year found in the data, output monthly values (Jan through Dec, 12 values) for each budget line.

Rules:
- Sheet names may represent years (e.g. "2024", "2025", "Budget 2026"). If a sheet name contains a year, use that year for all data in that sheet. If not, look for year information inside the data itself.
- For CSV files (single sheet), look for year information in headers, column names, or row labels.
- Match rows to the closest budget line ID based on meaning (e.g. "Salaire"/"Salary"/"Revenue" -> "income", "Loyer"/"Rent"/"Mortgage" -> "mortgage", "Courses"/"Food"/"Groceries" -> "groceries", etc.)
- If a category doesn't match any budget line, skip it
- Values should be positive numbers (no negatives)
- If a month has no data, use 0
- Support any language in the input

Respond with ONLY valid JSON, no markdown, no explanation. Use compact single-line format (no extra whitespace). Use this exact format:
[
  {
    "year": 2026,
    "lines": [
      { "id": "income", "label": "Original Label", "values": [v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12] }
    ]
  }
]

File content:
${fileContent}`;
}

async function callLlm(fileContent: string, fileName: string): Promise<ParsedYear[]> {
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
      max_tokens: 16384,
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
      max_tokens: 16384,
    });
  } else {
    // Google Gemini
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

  // Strip markdown code fences if present
  text = text.replace(/^```(?:json)?\s*\n?/i, '').replace(/\n?```\s*$/i, '').trim();

  let parsed: ParsedYear[];
  try {
    parsed = JSON.parse(text) as ParsedYear[];
  } catch {
    throw new Error('AI returned invalid JSON. Please try again or use a smaller file.');
  }
  // Validate structure
  for (const yearEntry of parsed) {
    if (typeof yearEntry.year !== 'number') throw new Error('Invalid year in response');
    for (const line of yearEntry.lines) {
      if (!BUDGET_LINE_IDS.includes(line.id)) throw new Error(`Unknown line ID: ${line.id}`);
      if (!Array.isArray(line.values) || line.values.length !== 12) throw new Error(`Line ${line.id} must have 12 values`);
    }
  }
  return parsed;
}

function applyImport() {
  const data: Record<number, Record<string, number[]>> = {};
  for (const yearEntry of parsedData.value) {
    data[yearEntry.year] = {};
    for (const line of yearEntry.lines) {
      data[yearEntry.year][line.id] = line.values.map((v) => Math.abs(Number(v) || 0));
    }
  }
  budgetStore.mergeImportData(data);
  status.value = 'success';
}

function cancel() {
  status.value = 'idle';
  parsedData.value = [];
  errorMessage.value = '';
}

const totalYears = computed(() => parsedData.value.length);
const totalLines = computed(() => parsedData.value.reduce((sum, y) => sum + y.lines.length, 0));
</script>

<template>
  <div class="import-panel">
    <!-- File drop zone -->
    <div
      v-if="status === 'idle' || status === 'error' || status === 'success'"
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

    <!-- Success message -->
    <div v-if="status === 'success'" class="import-message import-success-msg">
      {{ t.importSuccess }}
    </div>

    <!-- Error message -->
    <div v-if="status === 'error'" class="import-message import-error-msg">
      {{ errorMessage }}
    </div>

    <!-- Parsing spinner -->
    <div v-if="status === 'parsing'" class="parsing-state">
      <div class="parsing-spinner" />
      <p>{{ t.importParsing }}</p>
    </div>

    <!-- Preview -->
    <div v-if="status === 'preview'" class="preview-section">
      <h3 class="preview-title">{{ t.importPreview }}</h3>
      <p class="preview-stats">
        {{ totalYears }} {{ t.importYearsFound }} &mdash; {{ totalLines }} {{ t.importLinesFound }}
      </p>

      <div v-for="yearEntry in parsedData" :key="yearEntry.year" class="preview-year">
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

      <div class="preview-actions">
        <button class="login-btn" type="button" @click="applyImport">{{ t.importApply }}</button>
        <button class="cancel-btn" type="button" @click="cancel">{{ t.importCancel }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-panel {
  max-width: 100%;
}

.drop-zone {
  border: 2px dashed var(--line-soft);
  border-radius: 16px;
  padding: 2.5rem 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  background: rgba(255, 255, 255, 0.02);
}

.drop-zone:hover,
.drop-zone-active {
  border-color: var(--brand-cyan);
  background: rgba(124, 247, 255, 0.05);
}

.file-input-hidden {
  display: none;
}

.drop-zone-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.drop-zone-text {
  color: var(--text-1);
  font-size: 1rem;
  margin: 0;
}

.drop-zone-hint {
  color: var(--text-2);
  font-size: 0.82rem;
  margin: 0.4rem 0 0;
}

.import-message {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
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

.parsing-state {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
  justify-content: center;
  color: var(--text-1);
}

.parsing-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--line-soft);
  border-top-color: var(--brand-cyan);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.preview-section {
  margin-top: 1rem;
}

.preview-title {
  margin: 0 0 0.3rem;
  font-size: 1.1rem;
  color: var(--text-0);
}

.preview-stats {
  color: var(--text-2);
  font-size: 0.85rem;
  margin: 0 0 1rem;
}

.preview-year {
  margin-bottom: 1.2rem;
}

.preview-year-title {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  color: var(--brand-cyan);
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
  overflow-x: auto;
  display: block;
}

.preview-table thead th {
  background: rgba(47, 56, 103, 0.6);
  color: #e8efff;
  padding: 0.4rem 0.35rem;
  white-space: nowrap;
  font-weight: 600;
}

.preview-table td {
  padding: 0.35rem 0.35rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

.preview-id {
  color: var(--brand-cyan);
  font-family: monospace;
}

.preview-label {
  color: var(--text-2);
}

.preview-val {
  text-align: right;
  color: var(--text-1);
}

.preview-actions {
  display: flex;
  gap: 0.7rem;
  margin-top: 1rem;
}

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

.cancel-btn:hover {
  border-color: var(--line-strong);
}
</style>
