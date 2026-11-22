import { createSlice } from '@reduxjs/toolkit';

export type ThemeMode = 'light' | 'dark';
const STORAGE_KEY = 'pocketlens.theme';

function readStoredMode(): ThemeMode {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') return stored;
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

const uiSlice = createSlice({
  name: 'ui',
  initialState: () => ({ themeMode: readStoredMode() }),
  reducers: {
    toggleTheme(state) {
      state.themeMode = state.themeMode === 'light' ? 'dark' : 'light';
      try {
        window.localStorage.setItem(STORAGE_KEY, state.themeMode);
      } catch {
        // storage can be unavailable (private mode); the choice then lasts for this visit only
      }
    },
  },
});

export const { toggleTheme } = uiSlice.actions;
export const uiReducer = uiSlice.reducer;
export const selectThemeMode = (state: { ui: { themeMode: ThemeMode } }) => state.ui.themeMode;
