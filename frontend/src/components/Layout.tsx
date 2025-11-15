import React from 'react'
import {
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Box,
  IconButton,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  Collapse,
  ListSubheader,
} from '@mui/material'
import {
  Dashboard as DashboardIcon,
  LibraryMusic as LibraryIcon,
  Schedule as ClockIcon,
  Campaign as CampaignIcon,
  Assignment as LogIcon,
  Mic as VoiceIcon,
  Assessment as ReportIcon,
  AccountCircle as AccountIcon,
  Menu as MenuIcon,
  Description as CopyIcon,
  Receipt as InvoiceIcon,
  Payment as PaymentIcon,
  SwapHoriz as MakegoodIcon,
  Security as SecurityIcon,
  Analytics as AnalyticsIcon,
  Inventory as InventoryIcon,
  TrendingUp as RevenueIcon,
  Flag as GoalsIcon,
  Webhook as WebhookIcon,
  Notifications as NotificationsIcon,
  ExpandLess,
  ExpandMore,
  Settings as SettingsIcon,
  Backup as BackupIcon,
} from '@mui/icons-material'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import { useQuery } from '@tanstack/react-query'
import { checkApiHealth } from '../utils/api'
import { Chip } from '@mui/material'

const drawerWidth = 240

interface MenuItem {
  text: string
  icon: React.ReactNode
  path: string
  group?: string
}

const menuItems: MenuItem[] = [
  // Core Operations
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Audio Library', icon: <LibraryIcon />, path: '/library' },
  { text: 'Clock Templates', icon: <ClockIcon />, path: '/clocks' },
  
  // Traffic Management
  { text: 'Traffic Manager', icon: <CampaignIcon />, path: '/traffic', group: 'traffic' },
  { text: 'Advertisers', icon: <CampaignIcon />, path: '/traffic/advertisers', group: 'traffic' },
  { text: 'Agencies', icon: <CampaignIcon />, path: '/traffic/agencies', group: 'traffic' },
  { text: 'Orders', icon: <CampaignIcon />, path: '/traffic/orders', group: 'traffic' },
  { text: 'Sales Reps', icon: <CampaignIcon />, path: '/traffic/sales-reps', group: 'traffic' },
  { text: 'Spot Scheduler', icon: <CampaignIcon />, path: '/traffic/spot-scheduler', group: 'traffic' },
  { text: 'Dayparts', icon: <ClockIcon />, path: '/traffic/dayparts', group: 'traffic' },
  { text: 'Daypart Categories', icon: <ClockIcon />, path: '/traffic/daypart-categories', group: 'traffic' },
  { text: 'Rotation Rules', icon: <ClockIcon />, path: '/traffic/rotation-rules', group: 'traffic' },
  { text: 'Traffic Logs', icon: <LogIcon />, path: '/traffic/traffic-logs', group: 'traffic' },
  { text: 'Copy Library', icon: <CopyIcon />, path: '/traffic/copy', group: 'traffic' },
  
  // Log Management
  { text: 'Log Generator', icon: <LogIcon />, path: '/logs', group: 'logs' },
  { text: 'Voice Tracking', icon: <VoiceIcon />, path: '/voice', group: 'logs' },
  
  // Billing
  { text: 'Invoices', icon: <InvoiceIcon />, path: '/billing/invoices', group: 'billing' },
  { text: 'Payments', icon: <PaymentIcon />, path: '/billing/payments', group: 'billing' },
  { text: 'Makegoods', icon: <MakegoodIcon />, path: '/billing/makegoods', group: 'billing' },
  
  // Reports
  { text: 'Reports', icon: <ReportIcon />, path: '/reports' },
  
  // Analytics
  { text: 'Inventory', icon: <InventoryIcon />, path: '/analytics/inventory', group: 'analytics' },
  { text: 'Revenue', icon: <RevenueIcon />, path: '/analytics/revenue', group: 'analytics' },
  { text: 'Sales Goals', icon: <GoalsIcon />, path: '/analytics/sales-goals', group: 'analytics' },
  
  // Admin
  { text: 'Users', icon: <AccountIcon />, path: '/admin/users', group: 'admin' },
  { text: 'Settings', icon: <SettingsIcon />, path: '/admin/settings', group: 'admin' },
  { text: 'Audit Logs', icon: <SecurityIcon />, path: '/admin/audit-logs', group: 'admin' },
  { text: 'Webhooks', icon: <WebhookIcon />, path: '/admin/webhooks', group: 'admin' },
  { text: 'Notifications', icon: <NotificationsIcon />, path: '/admin/notifications', group: 'admin' },
  { text: 'Backups', icon: <BackupIcon />, path: '/admin/backups', group: 'admin' },
]

const menuGroups = {
  traffic: 'Traffic Management',
  logs: 'Log Management',
  billing: 'Billing',
  analytics: 'Analytics',
  admin: 'Admin',
}

const Layout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const [mobileOpen, setMobileOpen] = React.useState(false)
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
  
  // Check API health status
  const { data: healthData, error: healthError } = useQuery({
    queryKey: ['api-health-global'],
    queryFn: () => checkApiHealth(),
    retry: false,
    refetchInterval: 60000, // Check every minute
    staleTime: 30000,
  })
  
  // Collapsible menu state - load from localStorage or default to all expanded
  const [expandedGroups, setExpandedGroups] = React.useState<Record<string, boolean>>(() => {
    const saved = localStorage.getItem('menuExpandedGroups')
    if (saved) {
      try {
        return JSON.parse(saved)
      } catch {
        return { traffic: true, logs: true, billing: true, analytics: true, admin: true }
      }
    }
    return { traffic: true, logs: true, billing: true, analytics: true, admin: true }
  })

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleLogout = () => {
    logout()
    handleMenuClose()
  }

  const handleGroupToggle = (group: string) => {
    const newExpanded = { ...expandedGroups, [group]: !expandedGroups[group] }
    setExpandedGroups(newExpanded)
    localStorage.setItem('menuExpandedGroups', JSON.stringify(newExpanded))
  }

  const isPathActive = (path: string) => {
    if (location.pathname === path) return true
    // Check if path is a parent of current location
    if (location.pathname.startsWith(path) && path !== '/') return true
    return false
  }

  const isGroupActive = (group: string) => {
    return menuItems.some(item => item.group === group && isPathActive(item.path))
  }

  // Group menu items
  const coreItems = menuItems.filter(item => !item.group)
  const groupedItems = Object.keys(menuGroups).map(group => ({
    group,
    label: menuGroups[group as keyof typeof menuGroups],
    items: menuItems.filter(item => item.group === group),
  }))

  const drawer = (
    <Box>
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          LibreLog
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {/* Core Operations */}
        {coreItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={isPathActive(item.path)}
              onClick={() => navigate(item.path)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
        
        {/* Grouped Items */}
        {groupedItems.map(({ group, label, items }) => (
          <React.Fragment key={group}>
            <ListItemButton onClick={() => handleGroupToggle(group)}>
              <ListItemIcon>
                {expandedGroups[group] ? <ExpandLess /> : <ExpandMore />}
              </ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
            <Collapse in={expandedGroups[group]} timeout="auto" unmountOnExit>
              <List component="div" disablePadding>
                {items.map((item) => (
                  <ListItem key={item.text} disablePadding>
                    <ListItemButton
                      selected={isPathActive(item.path)}
                      onClick={() => navigate(item.path)}
                      sx={{ pl: 4 }}
                    >
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.text} />
                    </ListItemButton>
                  </ListItem>
                ))}
              </List>
            </Collapse>
          </React.Fragment>
        ))}
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            GayPHX Radio Traffic System
          </Typography>
          {healthError ? (
            <Chip 
              label="API Offline" 
              color="error" 
              size="small" 
              sx={{ mr: 1 }}
            />
          ) : healthData ? (
            <Chip 
              label="API Online" 
              color="success" 
              size="small" 
              sx={{ mr: 1 }}
            />
          ) : null}
          <NotificationBell />
          <IconButton
            size="large"
            aria-label="account of current user"
            aria-controls="menu-appbar"
            aria-haspopup="true"
            onClick={handleMenuOpen}
            color="inherit"
          >
            <Avatar sx={{ width: 32, height: 32 }}>
              {user?.username?.charAt(0).toUpperCase()}
            </Avatar>
          </IconButton>
          <Menu
            id="menu-appbar"
            anchorEl={anchorEl}
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            keepMounted
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
          >
            <MenuItem onClick={handleMenuClose}>
              <ListItemIcon>
                <AccountIcon fontSize="small" />
              </ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem onClick={handleLogout}>Logout</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  )
}

export default Layout
