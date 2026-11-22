/**
 * TypeScript types of the PocketLens REST API (mirror of docs/reference/openapi.yaml).
 */

export type Uuid = string;
export type IsoDate = string;
export type IsoDateTime = string;

export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'MALFORMED_REQUEST'
  | 'IMPORT_FILE_INVALID'
  | 'IMPORT_COLUMNS_MISSING'
  | 'IMPORT_ROWS_INVALID'
  | 'PROMPT_REJECTED'
  | 'UNAUTHORIZED'
  | 'INVALID_CREDENTIALS'
  | 'INVALID_REFRESH_TOKEN'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'CONFLICT'
  | 'FILE_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR'
  | 'AI_UNAVAILABLE'
  | 'NETWORK_ERROR';

/** RFC 9457 problem details returned by the API for every error. */
export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code: ErrorCode;
  correlationId?: string;
  errors?: { field: string; message: string }[];
  missingColumns?: string[];
  foundColumns?: string[];
  rowErrors?: { row: number; message: string }[];
  retryAfterSeconds?: number;
}

export type Role = 'USER' | 'ADMIN';

export interface User {
  id: Uuid;
  email: string;
  displayName: string;
  role: Role;
  privacyNoticeVersion: string;
  createdAt: IsoDateTime;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: User;
}

export interface PrivacyNotice {
  version: string;
  text: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  acceptPrivacyNotice: boolean;
  privacyNoticeVersion: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export type CategoryKind = 'EXPENSE' | 'INCOME' | 'TRANSFER';

export interface Category {
  id: Uuid;
  name: string;
  color: string;
  kind: CategoryKind;
  isDefault: boolean;
  systemKey: string | null;
}

export interface CategoryRequest {
  name: string;
  color: string;
  kind: CategoryKind;
}

export type CategorizationSource = 'NONE' | 'RULE' | 'AI' | 'MANUAL';

export interface Transaction {
  id: Uuid;
  date: IsoDate;
  amount: number;
  currency: string;
  description: string;
  merchant: string;
  category: { id: Uuid; name: string; color: string } | null;
  categorizationSource: CategorizationSource;
  confidence: number | null;
  needsReview: boolean;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type TransactionSort =
  'date,desc' | 'date,asc' | 'amount,asc' | 'amount,desc' | 'merchant,asc' | 'merchant,desc';

export interface TransactionQuery {
  from?: IsoDate;
  to?: IsoDate;
  categoryId?: Uuid;
  uncategorized?: boolean;
  needsReview?: boolean;
  q?: string;
  currency?: string;
  page: number;
  size: number;
  sort: TransactionSort;
}

export interface UpdateTransactionRequest {
  categoryId: Uuid;
  applyToMerchant: boolean;
}

export interface UpdateTransactionResponse {
  transaction: Transaction;
  ruleCreated: boolean;
  recategorizedCount: number;
}

export interface Rule {
  id: Uuid;
  pattern: string;
  categoryId: Uuid;
  categoryName: string;
  global: boolean;
  createdAt: IsoDateTime;
}

export interface StatementFormat {
  id: string;
  name: string;
  delimiter: string;
  requiredColumns: string[];
}

export interface ImportSummary {
  batchId: Uuid;
  fileName: string;
  format: string;
  rowsRead: number;
  imported: number;
  duplicates: number;
  skipped: number;
  categorizedByRules: number;
  categorizedByAi: number;
  needsReview: number;
  uncategorized: number;
  aiAvailable: boolean;
}

export interface ImportBatch {
  id: Uuid;
  fileName: string;
  format: string;
  rowsRead: number;
  imported: number;
  duplicates: number;
  skipped: number;
  needsReview: number;
  createdAt: IsoDateTime;
}

export interface CsvPreview {
  delimiter: string;
  headers: string[];
  rows: string[][];
  matchingFormats: string[];
}

export interface ImportProfileRequest {
  name: string;
  delimiter: string;
  dateColumn: string;
  dateFormat: string;
  descriptionColumn: string;
  amountColumn?: string | null;
  debitColumn?: string | null;
  creditColumn?: string | null;
  currencyColumn?: string | null;
  defaultCurrency?: string | null;
  decimalSeparator: string;
}

export interface ImportProfile extends ImportProfileRequest {
  id: Uuid;
  createdAt: IsoDateTime;
}

export type BudgetHealth = 'ON_TRACK' | 'AT_RISK' | 'EXCEEDED';

export interface BudgetStatus {
  budgetId: Uuid;
  categoryId: Uuid;
  categoryName: string;
  categoryColor: string;
  currency: string;
  limit: number;
  spent: number;
  remaining: number;
  projectedSpending: number;
  percentUsed: number;
  health: BudgetHealth;
  projectedOverrunDate: IsoDate | null;
}

export interface BudgetRequest {
  categoryId: Uuid;
  monthlyLimit: number;
  currency: string;
}

export interface Budget {
  id: Uuid;
  categoryId: Uuid;
  categoryName: string;
  monthlyLimit: number;
  currency: string;
}

export interface CategorySlice {
  categoryId: Uuid | null;
  name: string;
  color: string;
  amount: number;
  count: number;
}

export interface MonthPoint {
  month: string;
  expenses: number;
  income: number;
}

export interface CurrencySummary {
  currency: string;
  expenses: number;
  income: number;
  net: number;
  byCategory: CategorySlice[];
  trend: MonthPoint[];
}

export interface Dashboard {
  month: string;
  currencies: CurrencySummary[];
  budgets: BudgetStatus[];
  needsReviewCount: number;
}

export interface Subscription {
  merchant: string;
  currency: string;
  cadence: 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  lastAmount: number;
  previousAmount: number;
  priceIncreased: boolean;
  monthlyCost: number;
  lastPaymentDate: IsoDate;
  nextExpectedDate: IsoDate;
  occurrences: number;
  transactionIds: Uuid[];
}

export interface Conversation {
  id: Uuid;
  title: string | null;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface ToolCall {
  tool: string;
  arguments: Record<string, unknown>;
  interpretation: string | null;
  failed: boolean;
}

export interface Message {
  id: Uuid;
  role: 'USER' | 'ASSISTANT';
  content: string;
  interpretation: string[];
  toolCalls: ToolCall[];
  sourceTransactionCount: number;
  createdAt: IsoDateTime;
}

export interface ConversationDetail extends Conversation {
  messages: Message[];
}

export interface Exchange {
  question: Message;
  answer: Message;
}

export interface AnswerSources {
  transactions: Transaction[];
  totals: { currency: string; amount: number; count: number }[];
}

export interface AdminStats {
  users: number;
  transactions: number;
  assistantAnswersLast30Days: number;
  aiCallsLast30Days: number;
  inputTokensLast30Days: number;
  outputTokensLast30Days: number;
  aiEnabled: boolean;
}
