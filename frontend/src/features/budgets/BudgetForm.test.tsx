import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BudgetForm } from '@/features/budgets/BudgetForm';
import { renderWithProviders } from '@/test/render';
import type { Category } from '@/shared/types/api';

const categories: Category[] = [
  { id: 'c1', name: 'Groceries', color: '#4CAF50', kind: 'EXPENSE', isDefault: true, systemKey: 'groceries' },
  { id: 'c2', name: 'Income', color: '#2E7D32', kind: 'INCOME', isDefault: true, systemKey: 'income' },
];

describe('BudgetForm', () => {
  it('shows validation errors and does not submit invalid input', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    renderWithProviders(<BudgetForm categories={categories} saving={false} onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Monthly limit'), '12.345');
    await user.click(screen.getByRole('button', { name: 'Add' }));

    expect(await screen.findByText('Choose a category')).toBeInTheDocument();
    expect(screen.getByText('Enter an amount with at most two decimals')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('offers only expense categories and submits parsed values', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(true);
    renderWithProviders(<BudgetForm categories={categories} saving={false} onSubmit={onSubmit} />);

    await user.click(screen.getByLabelText('Category'));
    expect(screen.queryByRole('option', { name: 'Income' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('option', { name: 'Groceries' }));
    await user.type(screen.getByLabelText('Monthly limit'), '300,50');
    await user.clear(screen.getByLabelText('Currency'));
    await user.type(screen.getByLabelText('Currency'), 'eur');
    await user.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ categoryId: 'c1', monthlyLimit: 300.5, currency: 'EUR' }),
    );
  });
});
