import { Chip, Tooltip } from '@mui/material';
import type { Transaction } from '@/shared/types/api';

const SOURCE_LABEL = { RULE: 'Rule', AI: 'AI', MANUAL: 'You', NONE: '' } as const;

/** Category of a transaction with its colour and how it was assigned. */
export function CategoryChip({ transaction, onClick }: { transaction: Transaction; onClick?: () => void }) {
  const { category, categorizationSource, confidence, needsReview } = transaction;
  const label = category?.name ?? 'Uncategorised';
  const source = SOURCE_LABEL[categorizationSource];
  const tooltip =
    categorizationSource === 'AI' && confidence !== null
      ? `Suggested by AI (confidence ${Math.round(confidence * 100)}%)`
      : source
        ? `Assigned by: ${source}`
        : 'Not categorised yet';
  return (
    <Tooltip title={tooltip}>
      <Chip
        size="small"
        label={needsReview ? `${label} · review` : label}
        onClick={onClick}
        variant={needsReview ? 'outlined' : 'filled'}
        color={needsReview ? 'warning' : 'default'}
        sx={{
          maxWidth: 220,
          ...(category && !needsReview
            ? { bgcolor: `${category.color}22`, borderLeft: `4px solid ${category.color}` }
            : {}),
        }}
        aria-label={`Category ${label}${needsReview ? ', needs review' : ''}. Change category`}
      />
    </Tooltip>
  );
}
