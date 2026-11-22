import AccountBalanceWalletOutlined from '@mui/icons-material/AccountBalanceWalletOutlined';
import AdminPanelSettingsOutlined from '@mui/icons-material/AdminPanelSettingsOutlined';
import ChatOutlined from '@mui/icons-material/ChatOutlined';
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined';
import DashboardOutlined from '@mui/icons-material/DashboardOutlined';
import LightModeOutlined from '@mui/icons-material/LightModeOutlined';
import Logout from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined';
import SettingsOutlined from '@mui/icons-material/SettingsOutlined';
import UploadFileOutlined from '@mui/icons-material/UploadFileOutlined';
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useState, type ReactNode } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/store';
import { selectThemeMode, toggleTheme } from '@/app/uiSlice';
import { useLogoutMutation } from '@/features/auth/authApi';
import { selectCurrentUser, selectIsAdmin, sessionEnded } from '@/features/auth/authSlice';
import { Logo } from '@/layout/Logo';
import { api } from '@/shared/api/baseApi';
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary';

const DRAWER_WIDTH = 240;

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: <DashboardOutlined /> },
  { to: '/transactions', label: 'Transactions', icon: <ReceiptLongOutlined /> },
  { to: '/import', label: 'Import', icon: <UploadFileOutlined /> },
  { to: '/budgets', label: 'Budgets', icon: <AccountBalanceWalletOutlined /> },
  { to: '/assistant', label: 'Assistant', icon: <ChatOutlined /> },
  { to: '/settings', label: 'Settings', icon: <SettingsOutlined /> },
];

/** Application shell: responsive navigation (permanent drawer on desktop, temporary on mobile) and top bar. */
export function AppLayout() {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAppSelector(selectCurrentUser);
  const isAdmin = useAppSelector(selectIsAdmin);
  const mode = useAppSelector(selectThemeMode);
  const [logout] = useLogoutMutation();

  const items = isAdmin
    ? [...NAV_ITEMS, { to: '/admin', label: 'Admin', icon: <AdminPanelSettingsOutlined /> }]
    : NAV_ITEMS;

  const handleLogout = async () => {
    await logout();
    dispatch(sessionEnded());
    dispatch(api.util.resetApiState());
    navigate('/login', { replace: true });
  };

  const drawer = (
    <Box
      component="nav"
      aria-label="Main navigation"
      sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
    >
      <Toolbar>
        <Logo withText />
      </Toolbar>
      <List sx={{ px: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            end={item.to === '/'}
            onClick={() => setMobileOpen(false)}
            sx={{
              borderRadius: 2,
              mb: 0.5,
              '&.active': {
                bgcolor: 'action.selected',
                color: 'primary.main',
                '& svg': { color: 'primary.main' },
              },
            }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Toolbar>
          {!desktop && (
            <IconButton
              edge="start"
              onClick={() => setMobileOpen(true)}
              aria-label="Open navigation"
              sx={{ mr: 1 }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Box sx={{ flexGrow: 1 }} />
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mr: 1, display: { xs: 'none', sm: 'block' } }}
          >
            {user?.displayName}
          </Typography>
          <Tooltip title={mode === 'light' ? 'Dark theme' : 'Light theme'}>
            <IconButton onClick={() => dispatch(toggleTheme())} aria-label="Toggle colour theme">
              {mode === 'light' ? <DarkModeOutlined /> : <LightModeOutlined />}
            </IconButton>
          </Tooltip>
          <Tooltip title="Log out">
            <IconButton onClick={handleLogout} aria-label="Log out">
              <Logout />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        {desktop ? (
          <Drawer variant="permanent" open sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}>
            {drawer}
          </Drawer>
        ) : (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }}
            sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
          >
            {drawer}
          </Drawer>
        )}
      </Box>
      <Box component="main" sx={{ flexGrow: 1, minWidth: 0, p: { xs: 2, sm: 3 }, mt: 8 }}>
        <ErrorBoundary resetKey={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </Box>
    </Box>
  );
}
