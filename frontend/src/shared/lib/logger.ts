/**
 * Minimal client-side logger. Errors are written to the console with context; in production they could be
 * forwarded to a log collector by replacing `sink`.
 */
type Level = 'info' | 'warn' | 'error';

interface LogEntry {
  level: Level;
  message: string;
  context?: Record<string, unknown>;
  timestamp: string;
}

let sink = (entry: LogEntry): void => {
  const line = `[PocketLens] ${entry.message}`;
  if (entry.level === 'error') console.error(line, entry.context ?? {});
  else if (entry.level === 'warn') console.warn(line, entry.context ?? {});
  else console.info(line, entry.context ?? {});
};

function log(level: Level, message: string, context?: Record<string, unknown>): void {
  sink({ level, message, context, timestamp: new Date().toISOString() });
}

export const logger = {
  info: (message: string, context?: Record<string, unknown>) => log('info', message, context),
  warn: (message: string, context?: Record<string, unknown>) => log('warn', message, context),
  error: (message: string, context?: Record<string, unknown>) => log('error', message, context),
  /** Replaces the output (used in tests). */
  setSink: (next: (entry: LogEntry) => void) => {
    sink = next;
  },
};
