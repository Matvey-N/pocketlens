import { Box, Typography } from '@mui/material';

export function Logo({ size = 28, withText = false }: { size?: number; withText?: boolean }) {
  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 1 }}>
      <Box component="svg" viewBox="0 0 64 64" sx={{ width: size, height: size }} aria-hidden="true">
        <rect width="64" height="64" rx="14" fill="#1F4E79" />
        <circle cx="28" cy="28" r="14" fill="none" stroke="#fff" strokeWidth="6" />
        <path d="M38 38l12 12" stroke="#7FD1AE" strokeWidth="7" strokeLinecap="round" />
      </Box>
      {withText && (
        <Typography variant="h6" component="span" sx={{ fontWeight: 700, letterSpacing: '-0.02em' }}>
          PocketLens
        </Typography>
      )}
    </Box>
  );
}
