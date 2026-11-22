import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/app/store';
import { useLoginMutation } from '@/features/auth/authApi';
import { AuthCard } from '@/features/auth/AuthCard';
import { sessionStarted } from '@/features/auth/authSlice';
import { loginSchema, type LoginForm } from '@/features/auth/schemas';
import type { Problem } from '@/shared/types/api';

export default function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [login, { isLoading, error }] = useLoginMutation();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });

  const onSubmit = handleSubmit(async (values) => {
    const result = await login(values);
    if ('data' in result && result.data) {
      dispatch(sessionStarted(result.data));
      const target = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(target, { replace: true });
    }
  });

  const problem = error as Problem | undefined;
  return (
    <AuthCard title="Welcome back" subtitle="Log in to see where your money goes">
      <form onSubmit={onSubmit} noValidate>
        <Stack spacing={2}>
          {problem && (
            <Alert severity="error" role="alert">
              {problem.code === 'INVALID_CREDENTIALS' ? 'E-mail or password is incorrect.' : problem.detail}
            </Alert>
          )}
          <TextField
            label="E-mail"
            type="email"
            autoComplete="email"
            autoFocus
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            {...register('password')}
            error={Boolean(errors.password)}
            helperText={errors.password?.message}
          />
          <Button type="submit" variant="contained" size="large" loading={isLoading}>
            Log in
          </Button>
          <Typography variant="body2" sx={{ textAlign: 'center' }}>
            New to PocketLens?{' '}
            <Link component={RouterLink} to="/register">
              Create an account
            </Link>
          </Typography>
        </Stack>
      </form>
    </AuthCard>
  );
}
