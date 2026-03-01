export type ChartFilter = {
  year?: number;
  month?: number;
  accountId?: string;
  category?: string;
  wealthType?: 'financial' | 'gross' | 'net';
};

export function applyFilter<T extends Record<string, unknown>>(data: T[], filter: ChartFilter): T[] {
  return data.filter((row) => {
    if (filter.year && row.year !== filter.year) return false;
    if (typeof filter.month === 'number' && row.month !== filter.month) return false;
    if (filter.accountId && row.accountId !== filter.accountId) return false;
    if (filter.category && row.category !== filter.category) return false;
    if (filter.wealthType && row.wealthType !== filter.wealthType) return false;
    return true;
  });
}

export function exportChartAsPng(canvas: HTMLCanvasElement, filename: string): string {
  const url = canvas.toDataURL('image/png');
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  return url;
}
