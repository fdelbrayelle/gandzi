import { describe, expect, it } from 'vitest';
import { applyFilter } from '../../composables/chartFilters';

describe('chart filters', () => {
  it('filters by year and month', () => {
    const data = [
      { year: 2026, month: 1, category: 'groceries' },
      { year: 2026, month: 2, category: 'transport' },
    ];

    const result = applyFilter(data, { year: 2026, month: 2 });
    expect(result).toHaveLength(1);
    expect(result[0].category).toBe('transport');
  });
});
