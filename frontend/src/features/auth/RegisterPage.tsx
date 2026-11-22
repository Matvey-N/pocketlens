import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  FormHelperText,
  Link,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/app/store';
import { usePrivacyNoticeQuery, useRegisterMutation } from '@/features/auth/authApi';
import { AuthCard } from '@/features/auth/AuthCard';
import { sessionStarted } from '@/features/auth/authSlice';
import { registerSchema, type RegisterForm } from '@/features/auth/schemas';
import type { Problem } from '@/shared/types/api';

export default function RegisterPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { data: notice, isLoading: noticeLoading } = usePrivacyNoticeQuery();
  const [registerUser, { isLoading, error }] = useRegisterMutation();
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      displayName: '',
      email: '',
      password: '',
      confirmPassword: '',
      acceptPrivacyNotice: false,
    },
  });

  const onSubmit = handleSubmit(async ({ displayName, email, password, acceptPrivacyNotice }) => {
    if (!notice) return;
    const result = await registerUser({
      displayName,
      email,
      password,
      acceptPrivacyNotice,
      privacyNoticeVersion: notice.version,
    });
    if ('data' in result && result.data) {
      dispatch(sessionStarted(result.data));
      navigate('/import', { replace: true });
    }
  });

  const problem = error as Problem | undefined;
  return (
    <AuthCard
      title="Create your account"
      subtitle="Import a statement and get your first overview in minutes"
    >
      <form onSubmit={onSubmit} noValidate>
        <Stack spacing={2}>
          {problem && (
            <Alert severity="error" role="alert">
              {problem.code === 'CONFLICT' ? 'An account with this e-mail already exists.' : problem.detail}
            </Alert>
          )}
          <TextField
            label="Name"
            autoComplete="given-name"
            {...register('displayName')}
            error={Boolean(errors.displayName)}
            helperText={errors.displayName?.message}
          />
          <TextField
            label="E-mail"
            type="email"
            autoComplete="email"
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="new-password"
            {...register('password')}
            error={Boolean(errors.password)}
            helperText={errors.password?.message ?? '10+ characters with letters and digits'}
          />
          <TextField
            label="Repeat password"
            type="password"
            autoComplete="new-password"
            {...register('confirmPassword')}
            error={Boolean(errors.confirmPassword)}
            helperText={errors.confirmPassword?.message}
          />
          <Box
            sx={{ maxHeight: 120, overflow: 'auto', p: 1.5, bgcolor: 'action.hover', borderRadius: 2 }}
            tabIndex={0}
            aria-label="Privacy notice"
          >
            {noticeLoading ? (
              <Skeleton height={60} />
            ) : (
              <Typography variant="body2" color="text.secondary">
                {notice?.text}
              </Typography>
            )}
          </Box>
          <Box>
            <Controller
              name="acceptPrivacyNotice"
              control={control}
              render={({ field }) => (
                <FormControlLabel
                  control={
                    <Checkbox checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />
                  }
                  label="I have read and accept the privacy notice"
                />
              )}
            />
            {errors.acceptPrivacyNotice && (
              <FormHelperText error>{errors.acceptPrivacyNotice.message}</FormHelperText>
            )}
          </Box>
          <Button type="submit" variant="contained" size="large" loading={isLoading} disabled={!notice}>
            Create account
          </Button>
          <Typography variant="body2" sx={{ textAlign: 'center' }}>
            Already registered?{' '}
            <Link component={RouterLink} to="/login">
              Log in
            </Link>
          </Typography>
        </Stack>
      </form>
    </AuthCard>
  );
}
