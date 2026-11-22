import {
  currentMonth,
  formatDate,
  formatMoney,
  formatMonth,
  monthRange,
  shiftMonth,
} from '@/shared/lib/format';

describe('format', () => {
  it('formats money in its own currency', () => {
    expect(formatMoney(-17.9, 'EUR')).toBe('-€17.90');
    expect(formatMoney(5, 'XXX1')).toBe('5.00 XXX1');
  });

  it('works with months', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
    expect(monthRange('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(formatMonth('2026-11')).toBe('Nov 2026');
    expect(currentMonth(new Date(2026, 10, 22))).toBe('2026-11');
    expect(formatDate('2026-11-02')).toBe('02 Nov 2026');
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });
});
