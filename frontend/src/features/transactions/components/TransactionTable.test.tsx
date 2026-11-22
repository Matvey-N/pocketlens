import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransactionTable } from '@/features/transactions/components/TransactionTable';
import { renderWithProviders } from '@/test/render';
import type { Page, Transaction } from '@/shared/types/api';

const transactions: Transaction[] = [
  {
    id: 't1',
    date: '2026-11-02',
    amount: -17.9,
    currency: 'EUR',
    description: 'LIDL 1234 VILNIUS',
    merchant: 'LIDL VILNIUS',
    category: { id: 'c1', name: 'Groceries', color: '#4CAF50' },
    categorizationSource: 'RULE',
    confidence: 1,
    needsReview: false,
  },
  {
    id: 't2',
    date: '2026-11-04',
    amount: -4.5,
    currency: 'EUR',
    description: 'CAFE BLOOM 0425',
    merchant: 'CAFE BLOOM',
    category: null,
    categorizationSource: 'NONE',
    confidence: null,
    needsReview: true,
  },
];

const page: Page<Transaction> = { items: transactions, page: 0, size: 20, totalItems: 2, totalPages: 1 };

describe('TransactionTable', () => {
  beforeAll(() => {
    // desktop layout (table instead of cards)
    window.matchMedia = ((query: string) => ({
      matches: query.includes('min-width'),
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    })) as typeof window.matchMedia;
  });

  it('renders rows with category, review state and amounts', () => {
    renderWithProviders(
      <TransactionTable
        page={page}
        sort="date,desc"
        onSortChange={vi.fn()}
        onPageChange={vi.fn()}
        onEditCategory={vi.fn()}
      />,
    );
    const rows = within(screen.getByRole('table', { name: 'Transactions' })).getAllByRole('row');
    expect(rows).toHaveLength(3);
    expect(screen.getByText('LIDL VILNIUS')).toBeInTheDocument();
    expect(screen.getByText('Groceries')).toBeInTheDocument();
    expect(screen.getByText('Uncategorised · review')).toBeInTheDocument();
    expect(screen.getByText('-€17.90')).toBeInTheDocument();
  });

  it('changes sorting and opens the category editor', async () => {
    const user = userEvent.setup();
    const onSortChange = vi.fn();
    const onEditCategory = vi.fn();
    renderWithProviders(
      <TransactionTable
        page={page}
        sort="date,desc"
        onSortChange={onSortChange}
        onPageChange={vi.fn()}
        onEditCategory={onEditCategory}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Amount' }));
    expect(onSortChange).toHaveBeenCalledWith('amount,desc');
    await user.click(screen.getByRole('button', { name: 'Date' }));
    expect(onSortChange).toHaveBeenCalledWith('date,asc');

    await user.click(screen.getByLabelText(/Category Uncategorised, needs review/));
    expect(onEditCategory).toHaveBeenCalledWith(transactions[1]);
  });
});
