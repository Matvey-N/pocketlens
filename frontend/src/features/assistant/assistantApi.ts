import { api } from '@/shared/api/baseApi';
import type { AnswerSources, Conversation, ConversationDetail, Exchange } from '@/shared/types/api';

export const assistantApi = api.injectEndpoints({
  endpoints: (build) => ({
    conversations: build.query<Conversation[], void>({
      query: () => ({ url: '/conversations' }),
      providesTags: ['Conversations'],
    }),
    conversation: build.query<ConversationDetail, string>({
      query: (id) => ({ url: `/conversations/${id}` }),
      providesTags: (_result, _error, id) => [{ type: 'Conversations', id }],
    }),
    startConversation: build.mutation<Conversation, void>({
      query: () => ({ url: '/conversations', method: 'POST' }),
      invalidatesTags: ['Conversations'],
    }),
    deleteConversation: build.mutation<void, string>({
      query: (id) => ({ url: `/conversations/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Conversations'],
    }),
    ask: build.mutation<Exchange, { conversationId: string; content: string }>({
      query: ({ conversationId, content }) => ({
        url: `/conversations/${conversationId}/messages`,
        method: 'POST',
        data: { content },
      }),
      invalidatesTags: (_result, _error, { conversationId }) => [
        { type: 'Conversations', id: conversationId },
        'Conversations',
      ],
    }),
    answerSources: build.query<AnswerSources, { conversationId: string; messageId: string }>({
      query: ({ conversationId, messageId }) => ({
        url: `/conversations/${conversationId}/messages/${messageId}/transactions`,
      }),
    }),
  }),
});

export const {
  useConversationsQuery,
  useConversationQuery,
  useStartConversationMutation,
  useDeleteConversationMutation,
  useAskMutation,
  useAnswerSourcesQuery,
} = assistantApi;
