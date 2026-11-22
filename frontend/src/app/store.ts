import { combineReducers, configureStore } from '@reduxjs/toolkit';
import { useDispatch, useSelector } from 'react-redux';
import { errorMiddleware } from '@/app/errorMiddleware';
import { uiReducer } from '@/app/uiSlice';
import { authReducer, sessionEnded, sessionStarted } from '@/features/auth/authSlice';
import { notificationsReducer } from '@/features/notifications/notificationsSlice';
import { transactionFiltersReducer } from '@/features/transactions/transactionFiltersSlice';
import { api } from '@/shared/api/baseApi';
import { configureHttpClient } from '@/shared/api/httpClient';

const rootReducer = combineReducers({
  [api.reducerPath]: api.reducer,
  auth: authReducer,
  ui: uiReducer,
  notifications: notificationsReducer,
  transactionFilters: transactionFiltersReducer,
});

export type RootState = ReturnType<typeof rootReducer>;

export function createStore(preloadedState?: Partial<RootState>) {
  const store = configureStore({
    reducer: rootReducer,
    preloadedState,
    middleware: (getDefault) => getDefault().concat(api.middleware, errorMiddleware),
  });
  configureHttpClient({
    getAccessToken: () => store.getState().auth.accessToken,
    onRefreshed: (response) => store.dispatch(sessionStarted(response)),
    onExpired: () => {
      store.dispatch(sessionEnded());
      store.dispatch(api.util.resetApiState());
    },
  });
  return store;
}

export type AppStore = ReturnType<typeof createStore>;
export type AppDispatch = AppStore['dispatch'];

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();
