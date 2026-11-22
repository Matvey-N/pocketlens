import { api } from '@/shared/api/baseApi';
import type {
  CsvPreview,
  ImportBatch,
  ImportProfile,
  ImportProfileRequest,
  ImportSummary,
  StatementFormat,
} from '@/shared/types/api';

function fileForm(file: File): FormData {
  const form = new FormData();
  form.append('file', file);
  return form;
}

export const importApi = api.injectEndpoints({
  endpoints: (build) => ({
    formats: build.query<StatementFormat[], void>({
      query: () => ({ url: '/imports/formats' }),
    }),
    importHistory: build.query<ImportBatch[], void>({
      query: () => ({ url: '/imports' }),
      providesTags: ['Imports'],
    }),
    importStatement: build.mutation<ImportSummary, { file: File; format?: string; profileId?: string }>({
      query: ({ file, format, profileId }) => ({
        url: '/imports',
        method: 'POST',
        data: fileForm(file),
        params: format ? { format } : { profileId },
      }),
      invalidatesTags: ['Imports', 'Transactions', 'Dashboard', 'Subscriptions'],
    }),
    previewStatement: build.mutation<CsvPreview, File>({
      query: (file) => ({ url: '/imports/preview', method: 'POST', data: fileForm(file) }),
    }),
    importProfiles: build.query<ImportProfile[], void>({
      query: () => ({ url: '/import-profiles' }),
      providesTags: ['Profiles'],
    }),
    createImportProfile: build.mutation<ImportProfile, ImportProfileRequest>({
      query: (body) => ({ url: '/import-profiles', method: 'POST', data: body }),
      invalidatesTags: ['Profiles'],
    }),
  }),
});

export const {
  useFormatsQuery,
  useImportHistoryQuery,
  useImportStatementMutation,
  usePreviewStatementMutation,
  useImportProfilesQuery,
  useCreateImportProfileMutation,
} = importApi;
