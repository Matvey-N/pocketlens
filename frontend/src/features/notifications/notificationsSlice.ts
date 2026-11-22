import { createSlice, nanoid, type PayloadAction } from '@reduxjs/toolkit';

export type Severity = 'success' | 'info' | 'warning' | 'error';

export interface Notification {
  id: string;
  severity: Severity;
  message: string;
  correlationId?: string;
}

interface NotificationsState {
  queue: Notification[];
}

const initialState: NotificationsState = { queue: [] };

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    notify: {
      reducer(state, action: PayloadAction<Notification>) {
        // Do not stack the same message twice (e.g. several failed requests at once).
        if (!state.queue.some((n) => n.message === action.payload.message)) {
          state.queue.push(action.payload);
        }
      },
      prepare(input: Omit<Notification, 'id'>) {
        return { payload: { ...input, id: nanoid() } };
      },
    },
    dismiss(state, action: PayloadAction<string>) {
      state.queue = state.queue.filter((n) => n.id !== action.payload);
    },
  },
});

export const { notify, dismiss } = notificationsSlice.actions;
export const notificationsReducer = notificationsSlice.reducer;
export const selectCurrentNotification = (state: { notifications: NotificationsState }) =>
  state.notifications.queue[0] ?? null;
