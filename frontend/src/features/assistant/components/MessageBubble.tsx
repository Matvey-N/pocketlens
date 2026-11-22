import ExpandMore from '@mui/icons-material/ExpandMore';
import ListAlt from '@mui/icons-material/ListAlt';
import { Accordion, AccordionDetails, AccordionSummary, Box, Button, Paper, Typography } from '@mui/material';
import type { Message } from '@/shared/types/api';

interface MessageBubbleProps {
  message: Message;
  onShowSources: (messageId: string) => void;
}

/**
 * One chat message. Assistant answers show how the question was understood (US-4.2) and a button to list the
 * transactions behind the figures (US-4.3).
 */
export function MessageBubble({ message, onShowSources }: MessageBubbleProps) {
  const mine = message.role === 'USER';
  return (
    <Box sx={{ display: 'flex', justifyContent: mine ? 'flex-end' : 'flex-start' }}>
      <Paper
        elevation={0}
        sx={{
          px: 2,
          py: 1.5,
          maxWidth: { xs: '92%', md: '75%' },
          bgcolor: mine ? 'primary.main' : 'background.paper',
          color: mine ? 'primary.contrastText' : 'text.primary',
          border: mine ? 0 : 1,
          borderColor: 'divider',
          borderRadius: 3,
          borderBottomRightRadius: mine ? 4 : undefined,
          borderBottomLeftRadius: mine ? undefined : 4,
        }}
      >
        <Typography sx={{ whiteSpace: 'pre-wrap' }}>{message.content}</Typography>
        {!mine && message.interpretation.length > 0 && (
          <Accordion
            disableGutters
            elevation={0}
            sx={{ mt: 1, bgcolor: 'transparent', '&:before': { display: 'none' } }}
          >
            <AccordionSummary expandIcon={<ExpandMore />} sx={{ px: 0, minHeight: 0 }}>
              <Typography variant="body2" color="text.secondary">
                How I understood your question
              </Typography>
            </AccordionSummary>
            <AccordionDetails sx={{ px: 0, pt: 0 }}>
              {message.interpretation.map((line) => (
                <Typography key={line} variant="body2" color="text.secondary">
                  • {line}
                </Typography>
              ))}
            </AccordionDetails>
          </Accordion>
        )}
        {!mine && message.sourceTransactionCount > 0 && (
          <Button
            size="small"
            startIcon={<ListAlt />}
            onClick={() => onShowSources(message.id)}
            sx={{ mt: 0.5, px: 0 }}
          >
            Show {message.sourceTransactionCount} transaction{message.sourceTransactionCount === 1 ? '' : 's'}
          </Button>
        )}
      </Paper>
    </Box>
  );
}
