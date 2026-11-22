/** Formatting helpers. Amounts are always shown with their own currency — never converted or summed across currencies. */

export function formatMoney(amount: number, currency: string, locale = 'en-GB'): string {
  try {
    return new Intl.NumberFormat(locale, { style: 'currency', currency, minimumFractionDigits: 2 }).format(
      amount,
    );
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function formatDate(iso: string, locale = 'en-GB'): string {
  const date = new Date(`${iso.length === 10 ? `${iso}T00:00:00` : iso}`);
  return Number.isNaN(date.getTime())
    ? iso
    : date.toLocaleDateString(locale, { day: '2-digit', month: 'short', year: 'numeric' });
}

/** `2026-11` → `Nov 2026`. */
export function formatMonth(month: string, locale = 'en-GB'): string {
  const [year, m] = month.split('-').map(Number);
  if (!year || !m) return month;
  return new Date(year, m - 1, 1).toLocaleDateString(locale, { month: 'short', year: 'numeric' });
}

export function currentMonth(today = new Date()): string {
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
}

export function shiftMonth(month: string, delta: number): string {
  const [year, m] = month.split('-').map(Number);
  const date = new Date(year ?? 2000, (m ?? 1) - 1 + delta, 1);
  return currentMonth(date);
}

/** First and last day of a `yyyy-MM` month as ISO dates. */
export function monthRange(month: string): { from: string; to: string } {
  const [year, m] = month.split('-').map(Number);
  const last = new Date(year ?? 2000, m ?? 1, 0).getDate();
  return { from: `${month}-01`, to: `${month}-${String(last).padStart(2, '0')}` };
}
