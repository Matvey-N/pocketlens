import { z } from 'zod';

/** Validation of a new budget (mirrors the API: 1 – 1 000 000, two decimals, ISO currency). */
export const budgetSchema = z.object({
  categoryId: z.string().min(1, 'Choose a category'),
  monthlyLimit: z
    .string()
    .trim()
    .regex(/^\d{1,7}([.,]\d{1,2})?$/, 'Enter an amount with at most two decimals')
    .transform((value) => Number(value.replace(',', '.')))
    .refine((value) => value >= 1 && value <= 1_000_000, 'Between 1 and 1 000 000'),
  currency: z
    .string()
    .trim()
    .regex(/^[A-Za-z]{3}$/, '3-letter code, e.g. EUR'),
});
