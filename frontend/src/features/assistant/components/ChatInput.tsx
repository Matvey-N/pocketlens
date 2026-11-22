import Send from '@mui/icons-material/Send';
import { IconButton, InputAdornment, TextField } from '@mui/material';
import { useState, type FormEvent, type KeyboardEvent } from 'react';

export const MAX_QUESTION_LENGTH = 1000;

interface ChatInputProps {
  disabled: boolean;
  onSend: (question: string) => void;
}

/** Question input: Enter sends, Shift+Enter adds a line; limited to 1 000 characters (NFR-S4). */
export function ChatInput({ disabled, onSend }: ChatInputProps) {
  const [value, setValue] = useState('');
  const trimmed = value.trim();
  const tooLong = value.length > MAX_QUESTION_LENGTH;

  const send = (event?: FormEvent) => {
    event?.preventDefault();
    if (!trimmed || tooLong || disabled) return;
    onSend(trimmed);
    setValue('');
  };

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send();
    }
  };

  return (
    <form onSubmit={send}>
      <TextField
        multiline
        maxRows={5}
        placeholder="Ask about your spending, e.g. “How much did I spend on eating out last month?”"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={onKeyDown}
        error={tooLong}
        helperText={
          tooLong ? `At most ${MAX_QUESTION_LENGTH} characters` : `${value.length}/${MAX_QUESTION_LENGTH}`
        }
        slotProps={{
          htmlInput: { 'aria-label': 'Your question' },
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  type="submit"
                  aria-label="Send question"
                  disabled={!trimmed || tooLong || disabled}
                >
                  <Send />
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />
    </form>
  );
}
