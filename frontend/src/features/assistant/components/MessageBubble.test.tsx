import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MessageBubble } from '@/features/assistant/components/MessageBubble';
import { renderWithProviders } from '@/test/render';
import type { Message } from '@/shared/types/api';

const answer: Message = {
  id: 'm1',
  role: 'ASSISTANT',
  content: 'In October you spent 284.10 EUR on groceries.',
  interpretation: ['Spending 2026-10-01 – 2026-10-31, category: Groceries'],
  toolCalls: [],
  sourceTransactionCount: 17,
  createdAt: '2026-11-22T09:31:06Z',
};

describe('MessageBubble', () => {
  it('shows the interpretation and lets the user open the source transactions', async () => {
    const user = userEvent.setup();
    const onShowSources = vi.fn();
    renderWithProviders(<MessageBubble message={answer} onShowSources={onShowSources} />);

    await user.click(screen.getByText('How I understood your question'));
    expect(await screen.findByText(/category: Groceries/)).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Show 17 transactions' }));
    expect(onShowSources).toHaveBeenCalledWith('m1');
  });

  it('does not show answer details on the user question', () => {
    renderWithProviders(
      <MessageBubble message={{ ...answer, role: 'USER', content: 'How much?' }} onShowSources={vi.fn()} />,
    );
    expect(screen.getByText('How much?')).toBeInTheDocument();
    expect(screen.queryByText('How I understood your question')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Show/ })).not.toBeInTheDocument();
  });
});
