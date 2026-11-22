import { createTheme, type Theme } from '@mui/material/styles';
import type { ThemeMode } from '@/app/uiSlice';

/**
 * Design tokens of PocketLens: deep "ink" blue for structure, mint for positive figures, coral for warnings.
 * Contrast of text colours against backgrounds is at least 4.5:1 in both modes (WCAG 2.1 AA, NFR-U2).
 */
export function buildTheme(mode: ThemeMode): Theme {
  const dark = mode === 'dark';
  return createTheme({
    palette: {
      mode,
      primary: { main: dark ? '#8AB4F8' : '#1F4E79', contrastText: dark ? '#0B1A2B' : '#FFFFFF' },
      secondary: { main: dark ? '#7FD1AE' : '#13795B' },
      error: { main: dark ? '#FF8A80' : '#B3261E' },
      warning: { main: dark ? '#FFB74D' : '#9A4D00' },
      success: { main: dark ? '#81C995' : '#1E7B34' },
      background: dark ? { default: '#0E141B', paper: '#151D27' } : { default: '#F5F7FA', paper: '#FFFFFF' },
      text: dark
        ? { primary: '#E8EDF2', secondary: '#AAB6C3' }
        : { primary: '#16212E', secondary: '#4A5868' },
    },
    shape: { borderRadius: 12 },
    typography: {
      fontFamily: '"Inter", "Segoe UI", system-ui, -apple-system, Roboto, Arial, sans-serif',
      h4: { fontWeight: 700, letterSpacing: '-0.02em' },
      h5: { fontWeight: 700, letterSpacing: '-0.01em' },
      h6: { fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiCard: {
        defaultProps: { variant: 'outlined' },
        styleOverrides: { root: { borderRadius: 16 } },
      },
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
      MuiTableCell: { styleOverrides: { head: { fontWeight: 600 } } },
    },
  });
}
