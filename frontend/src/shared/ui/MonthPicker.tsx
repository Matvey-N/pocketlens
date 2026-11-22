import ChevronLeft from '@mui/icons-material/ChevronLeft';
import ChevronRight from '@mui/icons-material/ChevronRight';
import { IconButton, Stack, Typography } from '@mui/material';
import { currentMonth, formatMonth, shiftMonth } from '@/shared/lib/format';

interface MonthPickerProps {
  value: string;
  onChange: (month: string) => void;
}

export function MonthPicker({ value, onChange }: MonthPickerProps) {
  const isCurrent = value >= currentMonth();
  return (
    <Stack direction="row" sx={{ alignItems: 'center' }} spacing={0.5}>
      <IconButton aria-label="Previous month" onClick={() => onChange(shiftMonth(value, -1))}>
        <ChevronLeft />
      </IconButton>
      <Typography sx={{ minWidth: 96, textAlign: 'center', fontWeight: 600 }} aria-live="polite">
        {formatMonth(value)}
      </Typography>
      <IconButton aria-label="Next month" disabled={isCurrent} onClick={() => onChange(shiftMonth(value, 1))}>
        <ChevronRight />
      </IconButton>
    </Stack>
  );
}
