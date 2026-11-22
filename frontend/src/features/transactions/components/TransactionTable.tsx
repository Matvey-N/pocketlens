import {
  Box,
  Card,
  CardContent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { CategoryChip } from '@/features/transactions/components/CategoryChip';
import { formatDate } from '@/shared/lib/format';
import type { Page, Transaction, TransactionSort } from '@/shared/types/api';
import { Money } from '@/shared/ui/Money';

type SortKey = 'date' | 'merchant' | 'amount';

interface TransactionTableProps {
  page: Page<Transaction>;
  sort: TransactionSort;
  onSortChange: (sort: TransactionSort) => void;
  onPageChange: (page: number, size: number) => void;
  onEditCategory: (transaction: Transaction) => void;
}

/**
 * Sortable, paginated transaction list. On phones the table becomes a list of cards so nothing scrolls
 * horizontally (NFR-U2).
 */
export function TransactionTable({
  page,
  sort,
  onSortChange,
  onPageChange,
  onEditCategory,
}: TransactionTableProps) {
  const theme = useTheme();
  const compact = useMediaQuery(theme.breakpoints.down('md'));
  const [sortKey, direction] = sort.split(',') as [SortKey, 'asc' | 'desc'];

  const toggleSort = (key: SortKey) => {
    const nextDirection = sortKey === key && direction === 'desc' ? 'asc' : 'desc';
    onSortChange(`${key},${nextDirection}` as TransactionSort);
  };

  const pagination = (
    <TablePagination
      component="div"
      count={page.totalItems}
      page={page.page}
      rowsPerPage={page.size}
      rowsPerPageOptions={[20, 50, 100]}
      onPageChange={(_, next) => onPageChange(next, page.size)}
      onRowsPerPageChange={(e) => onPageChange(0, Number(e.target.value))}
    />
  );

  if (compact) {
    return (
      <Box>
        <Stack spacing={1}>
          {page.items.map((t) => (
            <Card key={t.id}>
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" sx={{ justifyContent: 'space-between', gap: 1 }}>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 600 }} noWrap>
                      {t.merchant}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {formatDate(t.date)}
                    </Typography>
                  </Box>
                  <Money amount={t.amount} currency={t.currency} signed />
                </Stack>
                <Box sx={{ mt: 1 }}>
                  <CategoryChip transaction={t} onClick={() => onEditCategory(t)} />
                </Box>
              </CardContent>
            </Card>
          ))}
        </Stack>
        {pagination}
      </Box>
    );
  }

  return (
    <Card>
      <TableContainer>
        <Table size="small" aria-label="Transactions">
          <TableHead>
            <TableRow>
              <TableCell sortDirection={sortKey === 'date' ? direction : false}>
                <TableSortLabel
                  active={sortKey === 'date'}
                  direction={direction}
                  onClick={() => toggleSort('date')}
                >
                  Date
                </TableSortLabel>
              </TableCell>
              <TableCell sortDirection={sortKey === 'merchant' ? direction : false}>
                <TableSortLabel
                  active={sortKey === 'merchant'}
                  direction={direction}
                  onClick={() => toggleSort('merchant')}
                >
                  Merchant
                </TableSortLabel>
              </TableCell>
              <TableCell>Category</TableCell>
              <TableCell align="right" sortDirection={sortKey === 'amount' ? direction : false}>
                <TableSortLabel
                  active={sortKey === 'amount'}
                  direction={direction}
                  onClick={() => toggleSort('amount')}
                >
                  Amount
                </TableSortLabel>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {page.items.map((t) => (
              <TableRow key={t.id} hover>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDate(t.date)}</TableCell>
                <TableCell>
                  <Typography sx={{ fontWeight: 600 }}>{t.merchant}</Typography>
                  <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: 360 }}>
                    {t.description}
                  </Typography>
                </TableCell>
                <TableCell>
                  <CategoryChip transaction={t} onClick={() => onEditCategory(t)} />
                </TableCell>
                <TableCell align="right">
                  <Money amount={t.amount} currency={t.currency} signed />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {pagination}
    </Card>
  );
}
