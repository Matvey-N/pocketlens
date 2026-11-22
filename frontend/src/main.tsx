import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { App } from '@/app/App';
import { createStore } from '@/app/store';
import { logger } from '@/shared/lib/logger';

const store = createStore();

window.addEventListener('unhandledrejection', (event) => {
  logger.error('Unhandled promise rejection', { reason: String(event.reason) });
});

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
);
