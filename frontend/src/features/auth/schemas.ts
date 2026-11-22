import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your e-mail').email('Enter a valid e-mail'),
  password: z.string().min(1, 'Enter your password'),
});
export type LoginForm = z.infer<typeof loginSchema>;

/** Mirrors the backend rules (10–72 characters, at least one letter and one digit). */
export const registerSchema = z
  .object({
    displayName: z.string().trim().min(1, 'Enter your name').max(80, 'At most 80 characters'),
    email: z.string().trim().min(1, 'Enter your e-mail').email('Enter a valid e-mail').max(254),
    password: z
      .string()
      .min(10, 'At least 10 characters')
      .max(72, 'At most 72 characters')
      .regex(/[A-Za-z]/, 'Add at least one letter')
      .regex(/\d/, 'Add at least one digit'),
    confirmPassword: z.string(),
    acceptPrivacyNotice: z.boolean().refine((v) => v, 'You must accept the privacy notice'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });
export type RegisterForm = z.infer<typeof registerSchema>;
