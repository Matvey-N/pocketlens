import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthResponse, User } from '@/shared/types/api';

export type SessionStatus = 'unknown' | 'authenticated' | 'anonymous';

export interface AuthState {
  status: SessionStatus;
  accessToken: string | null;
  user: User | null;
}

const initialState: AuthState = { status: 'unknown', accessToken: null, user: null };

/**
 * Session state. The access token lives only in memory; the refresh token is an HttpOnly cookie that
 * JavaScript cannot read, so a page reload restores the session through /auth/refresh.
 */
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    sessionStarted(state, action: PayloadAction<AuthResponse>) {
      state.status = 'authenticated';
      state.accessToken = action.payload.accessToken;
      state.user = action.payload.user;
    },
    sessionEnded(state) {
      state.status = 'anonymous';
      state.accessToken = null;
      state.user = null;
    },
  },
});

export const { sessionStarted, sessionEnded } = authSlice.actions;
export const authReducer = authSlice.reducer;

export const selectSessionStatus = (state: { auth: AuthState }) => state.auth.status;
export const selectCurrentUser = (state: { auth: AuthState }) => state.auth.user;
export const selectIsAdmin = (state: { auth: AuthState }) => state.auth.user?.role === 'ADMIN';
