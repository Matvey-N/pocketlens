import { Box } from '@mui/material';
import { formatMoney } from '@/shared/lib/format';

interface MoneyProps {
  amount: number;
  currency: string;
  /** Colour positive amounts green and negative amounts in the normal text colour. */
  signed?: boolean;
}

export function Money({ amount, currency, signed = false }: MoneyProps) {
  return (
    <Box
      component="span"
      sx={{
        fontVariantNumeric: 'tabular-nums',
        whiteSpace: 'nowrap',
        color: signed && amount > 0 ? 'success.main' : 'inherit',
        fontWeight: signed && amount > 0 ? 600 : undefined,
      }}
    >
      {signed && amount > 0 ? '+' : ''}
      {formatMoney(amount, currency)}
    </Box>
  );
}
