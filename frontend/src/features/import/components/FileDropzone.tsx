import UploadFileOutlined from '@mui/icons-material/UploadFileOutlined';
import { Box, Typography } from '@mui/material';
import { useRef, useState, type DragEvent, type KeyboardEvent } from 'react';

const MAX_SIZE_BYTES = 5 * 1024 * 1024;

interface FileDropzoneProps {
  file: File | null;
  onFile: (file: File) => void;
  onError: (message: string) => void;
}

/** Drag-and-drop or click/keyboard to choose a CSV file; checks type and size before upload. */
export function FileDropzone({ file, onFile, onError }: FileDropzoneProps) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const accept = (candidate: File | undefined) => {
    if (!candidate) return;
    if (!candidate.name.toLowerCase().endsWith('.csv')) {
      onError('Choose a CSV file (.csv).');
      return;
    }
    if (candidate.size > MAX_SIZE_BYTES) {
      onError('The file is larger than 5 MB.');
      return;
    }
    onFile(candidate);
  };

  const onDrop = (event: DragEvent) => {
    event.preventDefault();
    setDragging(false);
    accept(event.dataTransfer.files[0]);
  };

  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      input.current?.click();
    }
  };

  return (
    <Box
      role="button"
      tabIndex={0}
      aria-label="Choose a CSV statement file"
      onClick={() => input.current?.click()}
      onKeyDown={onKeyDown}
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      sx={{
        border: 2,
        borderStyle: 'dashed',
        borderColor: dragging ? 'primary.main' : 'divider',
        borderRadius: 3,
        p: 4,
        textAlign: 'center',
        cursor: 'pointer',
        bgcolor: dragging ? 'action.hover' : 'transparent',
        transition: 'all 0.15s',
        '&:focus-visible': { outline: 2, outlineColor: 'primary.main', outlineStyle: 'solid' },
      }}
    >
      <UploadFileOutlined sx={{ fontSize: 40, color: 'text.secondary' }} />
      <Typography sx={{ mt: 1, fontWeight: 600 }}>
        {file ? file.name : 'Drop your bank statement here or click to choose'}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {file ? `${(file.size / 1024).toFixed(1)} KB` : 'CSV, up to 5 MB'}
      </Typography>
      <input
        ref={input}
        type="file"
        accept=".csv,text/csv"
        hidden
        data-testid="file-input"
        onChange={(e) => accept(e.target.files?.[0])}
      />
    </Box>
  );
}
