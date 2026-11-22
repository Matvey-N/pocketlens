import Add from '@mui/icons-material/Add';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Grid,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useAskMutation,
  useConversationQuery,
  useConversationsQuery,
  useDeleteConversationMutation,
  useStartConversationMutation,
} from '@/features/assistant/assistantApi';
import { ChatInput } from '@/features/assistant/components/ChatInput';
import { MessageBubble } from '@/features/assistant/components/MessageBubble';
import { SourcesDialog } from '@/features/assistant/components/SourcesDialog';
import { formatDate } from '@/shared/lib/format';
import type { Problem } from '@/shared/types/api';
import { EmptyState } from '@/shared/ui/EmptyState';
import { PageHeader } from '@/shared/ui/PageHeader';
import { QueryError } from '@/shared/ui/QueryError';

const SUGGESTIONS = [
  'How much did I spend on groceries last month?',
  'What were my 5 largest expenses this month?',
  'Will I stay within my budgets this month?',
  'How much do my subscriptions cost per month?',
];

export default function AssistantPage() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { data: conversations, isLoading: listLoading } = useConversationsQuery();
  const {
    data: conversation,
    error,
    refetch,
  } = useConversationQuery(conversationId ?? '', {
    skip: !conversationId,
  });
  const [start, startState] = useStartConversationMutation();
  const [remove] = useDeleteConversationMutation();
  const [ask, askState] = useAskMutation();
  const [pendingQuestion, setPendingQuestion] = useState<string | null>(null);
  const [sourcesFor, setSourcesFor] = useState<string | null>(null);
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottom.current?.scrollIntoView?.({ behavior: 'smooth' });
  }, [conversation?.messages.length, pendingQuestion]);

  const newConversation = async () => {
    const result = await start();
    if ('data' in result && result.data) navigate(`/assistant/${result.data.id}`);
  };

  const send = async (question: string) => {
    let id = conversationId;
    if (!id) {
      const created = await start();
      if (!('data' in created) || !created.data) return;
      id = created.data.id;
      navigate(`/assistant/${id}`);
    }
    setPendingQuestion(question);
    await ask({ conversationId: id, content: question });
    setPendingQuestion(null);
  };

  const deleteConversation = async (id: string) => {
    await remove(id);
    if (id === conversationId) navigate('/assistant');
  };

  const askProblem = askState.error as Problem | undefined;
  const messages = conversation?.messages ?? [];
  const waiting = askState.isLoading || startState.isLoading;

  return (
    <>
      <PageHeader
        title="Assistant"
        subtitle="Ask questions about your own spending — every figure comes from your data"
        actions={
          <Button startIcon={<Add />} variant="outlined" onClick={newConversation}>
            New conversation
          </Button>
        }
      />
      <Grid container spacing={2}>
        <Grid
          size={{ xs: 12, md: 4, lg: 3 }}
          sx={{ display: { xs: conversationId ? 'none' : 'block', md: 'block' } }}
        >
          <Card>
            {listLoading && <Skeleton variant="rounded" height={200} />}
            {conversations && conversations.length === 0 && (
              <Typography color="text.secondary" sx={{ p: 2 }}>
                No conversations yet.
              </Typography>
            )}
            <List dense aria-label="Conversations">
              {conversations?.map((c) => (
                <ListItemButton
                  key={c.id}
                  selected={c.id === conversationId}
                  onClick={() => navigate(`/assistant/${c.id}`)}
                >
                  <ListItemText
                    primary={c.title ?? 'New conversation'}
                    secondary={formatDate(c.updatedAt)}
                    slotProps={{ primary: { noWrap: true } }}
                  />
                  <IconButton
                    edge="end"
                    size="small"
                    aria-label="Delete conversation"
                    onClick={(e) => {
                      e.stopPropagation();
                      void deleteConversation(c.id);
                    }}
                  >
                    <DeleteOutline fontSize="small" />
                  </IconButton>
                </ListItemButton>
              ))}
            </List>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 8, lg: 9 }}>
          <Card
            sx={{
              display: 'flex',
              flexDirection: 'column',
              height: { xs: 'calc(100vh - 220px)', md: 'calc(100vh - 200px)' },
              minHeight: 420,
            }}
          >
            <Box sx={{ flexGrow: 1, overflowY: 'auto', p: 2 }} aria-live="polite" aria-label="Messages">
              {error && <QueryError error={error} onRetry={refetch} />}
              {messages.length === 0 && !pendingQuestion && (
                <EmptyState
                  title="What would you like to know?"
                  description="The assistant answers from your imported transactions and shows the transactions behind every figure."
                  action={
                    <Stack
                      direction="row"
                      spacing={1}
                      sx={{ flexWrap: 'wrap', justifyContent: 'center', gap: 1 }}
                    >
                      {SUGGESTIONS.map((s) => (
                        <Chip key={s} label={s} onClick={() => void send(s)} disabled={waiting} />
                      ))}
                    </Stack>
                  }
                />
              )}
              <Stack spacing={1.5}>
                {messages.map((m) => (
                  <MessageBubble key={m.id} message={m} onShowSources={setSourcesFor} />
                ))}
                {pendingQuestion && (
                  <>
                    <MessageBubble
                      message={{
                        id: 'pending',
                        role: 'USER',
                        content: pendingQuestion,
                        interpretation: [],
                        toolCalls: [],
                        sourceTransactionCount: 0,
                        createdAt: new Date().toISOString(),
                      }}
                      onShowSources={() => undefined}
                    />
                    <Box sx={{ display: 'flex' }} aria-label="The assistant is typing">
                      <Skeleton variant="rounded" width={220} height={44} sx={{ borderRadius: 3 }} />
                    </Box>
                  </>
                )}
              </Stack>
              <div ref={bottom} />
            </Box>
            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
              {askProblem && !askState.isLoading && (
                <Alert severity={askProblem.code === 'PROMPT_REJECTED' ? 'warning' : 'error'} sx={{ mb: 1 }}>
                  {askProblem.code === 'AI_UNAVAILABLE'
                    ? 'The assistant is temporarily unavailable. Your question was saved — please try again later.'
                    : askProblem.detail}
                </Alert>
              )}
              <ChatInput disabled={waiting} onSend={(q) => void send(q)} />
            </Box>
          </Card>
        </Grid>
      </Grid>
      {conversationId && (
        <SourcesDialog
          conversationId={conversationId}
          messageId={sourcesFor}
          onClose={() => setSourcesFor(null)}
        />
      )}
    </>
  );
}
