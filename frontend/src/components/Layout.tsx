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
  Traffic as TrafficIcon,
  Store as AdvertisersIcon,
  BusinessCenter as AgenciesIcon,
  ShoppingCart as OrdersIcon,
  People as SalesRepsIcon,
  EventNote as SpotSchedulerIcon,
  AccessTime as DaypartsIcon,
  Category as DaypartCategoriesIcon,
  Shuffle as RotationRulesIcon,
  ListAlt as TrafficLogsIcon,
  Assignment as LogGeneratorIcon,
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
  Help as HelpIcon,
  Build as ProductionIcon,
  Dashboard as ProducerDashboardIcon,
  Archive as ArchiveIcon,
  RecordVoiceOver as VoiceTalentIcon,
} from '@mui/icons-material'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import { useQuery } from '@tanstack/react-query'
import { checkApiHealth } from '../utils/api'
import { Chip, alpha } from '@mui/material'

const drawerWidth = 240

/**
 * Navigation Color Scheme Configuration
 * 
 * Customize the colors for each module/section by modifying the values below.
 * Colors are applied as:
 * - 3px left border accent on active items (full opacity)
 * - 10-15% opacity background tint on hover/active states
 * - 15% opacity icon color tinting on active items
 * 
 * To change a color, simply update the hex value for the desired module.
 * Example: To change Admin color to dark blue, change admin: '#d32f2f' to admin: '#1565c0'
 */
const moduleColors = {
  // Core Operations
  dashboard: '#1976d2',      // Blue - Main dashboard
  library: '#7b1fa2',         // Purple - Audio library
  clocks: '#f57c00',          // Orange - Clock templates
  
  // Workflow Sections (ROYGBP order)
  // 1. Traffic Management - Red
  traffic: '#d32f2f',         // Red - Traffic Management (orders, campaigns, scheduling)
  
  // 2. Production - Orange
  production: '#f57c00',      // Orange - Production (orders, voice talent)
  
  // 3. Log Management - Yellow
  logs: '#f57f17',           // Yellow/Amber - Log Management (log generator, voice tracking)
  
  // 4. Billing - Green
  billing: '#388e3c',         // Green - Billing (invoices, payments, makegoods)
  
  // 5. Analytics - Blue
  analytics: '#1976d2',       // Blue - Analytics (inventory, revenue, sales goals)
  
  // 6. Admin - Purple (stays at end)
  admin: '#7b1fa2',          // Purple - Admin (users, settings, audit logs, webhooks, notifications, backups)
  
  // Reports (standalone)
  reports: '#5e35b1',         // Indigo - Reports hub
  
  // Help
  help: '#616161',            // Grey - Help section
} as const

// Type for module color keys (for type safety)
type ModuleColorKey = keyof typeof moduleColors

interface MenuItem {
  text: string
  icon: React.ReactNode
  path: string
  group?: string
  color?: string
}

// Function to get module color based on path or group
const getModuleColor = (path: string, group?: string): string => {
  if (path === '/dashboard') return moduleColors.dashboard
  if (path === '/library') return moduleColors.library
  if (path === '/clocks') return moduleColors.clocks
  if (path === '/reports') return moduleColors.reports
  if (path === '/help') return moduleColors.help
  if (group === 'traffic') return moduleColors.traffic
  if (group === 'logs') return moduleColors.logs
  if (group === 'production') return moduleColors.production
  if (group === 'billing') return moduleColors.billing
  if (group === 'analytics') return moduleColors.analytics
  if (group === 'admin') return moduleColors.admin
  return moduleColors.dashboard // default
}

const menuItems: MenuItem[] = [
  // Core Operations
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard', color: moduleColors.dashboard },
  { text: 'Audio Library', icon: <LibraryIcon />, path: '/library', color: moduleColors.library },
  
  // 1. Traffic Management (Red) - Workflow: Orders, campaigns, scheduling
  { text: 'Traffic Manager', icon: <TrafficIcon />, path: '/traffic', group: 'traffic', color: moduleColors.traffic },
  { text: 'Advertisers', icon: <AdvertisersIcon />, path: '/traffic/advertisers', group: 'traffic', color: moduleColors.traffic },
  { text: 'Agencies', icon: <AgenciesIcon />, path: '/traffic/agencies', group: 'traffic', color: moduleColors.traffic },
  { text: 'Orders', icon: <OrdersIcon />, path: '/traffic/orders', group: 'traffic', color: moduleColors.traffic },
  { text: 'Sales Reps', icon: <SalesRepsIcon />, path: '/traffic/sales-reps', group: 'traffic', color: moduleColors.traffic },
  { text: 'Spot Scheduler', icon: <SpotSchedulerIcon />, path: '/traffic/spot-scheduler', group: 'traffic', color: moduleColors.traffic },
  { text: 'Dayparts', icon: <DaypartsIcon />, path: '/traffic/dayparts', group: 'traffic', color: moduleColors.traffic },
  { text: 'Daypart Categories', icon: <DaypartCategoriesIcon />, path: '/traffic/daypart-categories', group: 'traffic', color: moduleColors.traffic },
  { text: 'Rotation Rules', icon: <RotationRulesIcon />, path: '/traffic/rotation-rules', group: 'traffic', color: moduleColors.traffic },
  { text: 'Traffic Logs', icon: <TrafficLogsIcon />, path: '/traffic/traffic-logs', group: 'traffic', color: moduleColors.traffic },
  { text: 'Copy Library', icon: <CopyIcon />, path: '/traffic/copy', group: 'traffic', color: moduleColors.traffic },
  
  // 2. Production (Orange) - Workflow: Create production orders from copy
  { text: 'Production Orders', icon: <ProductionIcon />, path: '/production/orders', group: 'production', color: moduleColors.production },
  { text: 'Producer Dashboard', icon: <ProducerDashboardIcon />, path: '/production/dashboard', group: 'production', color: moduleColors.production },
  { text: 'Voice Talent Portal', icon: <VoiceTalentIcon />, path: '/production/voice-talent', group: 'production', color: moduleColors.production },
  { text: 'Production Archive', icon: <ArchiveIcon />, path: '/production/archive', group: 'production', color: moduleColors.production },
  
  // 3. Log Management (Yellow) - Workflow: Generate logs, voice tracking
  { text: 'Log Generator', icon: <LogGeneratorIcon />, path: '/logs', group: 'logs', color: moduleColors.logs },
  { text: 'Voice Tracking', icon: <VoiceIcon />, path: '/voice', group: 'logs', color: moduleColors.logs },
  { text: 'Voice Tracks Manager', icon: <VoiceIcon />, path: '/voice/tracks', group: 'logs', color: moduleColors.logs },
  
  // 4. Billing (Green) - Workflow: Invoices, payments, makegoods
  { text: 'Invoices', icon: <InvoiceIcon />, path: '/billing/invoices', group: 'billing', color: moduleColors.billing },
  { text: 'Payments', icon: <PaymentIcon />, path: '/billing/payments', group: 'billing', color: moduleColors.billing },
  { text: 'Makegoods', icon: <MakegoodIcon />, path: '/billing/makegoods', group: 'billing', color: moduleColors.billing },
  
  // 5. Analytics (Blue) - Workflow: Reports, revenue, sales goals
  { text: 'Inventory', icon: <InventoryIcon />, path: '/analytics/inventory', group: 'analytics', color: moduleColors.analytics },
  { text: 'Revenue', icon: <RevenueIcon />, path: '/analytics/revenue', group: 'analytics', color: moduleColors.analytics },
  { text: 'Sales Goals', icon: <GoalsIcon />, path: '/analytics/sales-goals', group: 'analytics', color: moduleColors.analytics },
  
  // 6. Admin (Purple) - Workflow: Settings, users, audit logs, webhooks, notifications, backups
  { text: 'Users', icon: <AccountIcon />, path: '/admin/users', group: 'admin', color: moduleColors.admin },
  { text: 'Settings', icon: <SettingsIcon />, path: '/admin/settings', group: 'admin', color: moduleColors.admin },
  { text: 'Audit Logs', icon: <SecurityIcon />, path: '/admin/audit-logs', group: 'admin', color: moduleColors.admin },
  { text: 'Webhooks', icon: <WebhookIcon />, path: '/admin/webhooks', group: 'admin', color: moduleColors.admin },
  { text: 'Notifications', icon: <NotificationsIcon />, path: '/admin/notifications', group: 'admin', color: moduleColors.admin },
  { text: 'Backups', icon: <BackupIcon />, path: '/admin/backups', group: 'admin', color: moduleColors.admin },
  
  // Standalone items (below collapsible sections)
  { text: 'Clock Templates', icon: <ClockIcon />, path: '/clocks', color: moduleColors.clocks },
  { text: 'Reports', icon: <ReportIcon />, path: '/reports', color: moduleColors.reports },
  { text: 'Help', icon: <HelpIcon />, path: '/help', color: moduleColors.help },
]

const menuGroups = {
  // Workflow order (ROYGBP)
  traffic: 'Traffic Management',      // Red - 1. Orders, campaigns, scheduling
  production: 'Production',            // Orange - 2. Create production orders
  logs: 'Log Management',             // Yellow - 3. Generate logs, voice tracking
  billing: 'Billing',                 // Green - 4. Invoices, payments, makegoods
  analytics: 'Analytics',             // Blue - 5. Reports, revenue, sales goals
  admin: 'Admin',                     // Purple - 6. Settings, users, audit logs (stays at end)
}

const Layout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const [mobileOpen, setMobileOpen] = React.useState(false)
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
  
  // Fetch branding settings
  const { data: settingsData } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => {
      const response = await fetch('/api/settings/', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (!response.ok) return null
      return response.json()
    },
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
  })
  
  const systemName = settingsData?.branding?.system_name?.value || 'GayPHX Radio Traffic System'
  const headerColor = settingsData?.branding?.header_color?.value || '#424242'
  const logoUrl = settingsData?.branding?.logo_url?.value || ''
  
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
        return { traffic: true, logs: true, production: true, billing: true, analytics: true, admin: true }
      }
    }
    return { traffic: true, logs: true, production: true, billing: true, analytics: true, admin: true }
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
  // Core items are Dashboard and Audio Library only (at the top)
  const coreItems = menuItems.filter(item => !item.group && (item.path === '/dashboard' || item.path === '/library'))
  // Standalone items (Clock Templates, Reports, Help) go at the bottom
  const standaloneItems = menuItems.filter(item => !item.group && item.path !== '/dashboard' && item.path !== '/library')
  const groupedItems = Object.keys(menuGroups).map(group => ({
    group,
    label: menuGroups[group as keyof typeof menuGroups],
    items: menuItems.filter(item => item.group === group),
  }))

  const drawer = (
    <Box
      sx={{
        backgroundColor: '#fafafa', // Neutral light background
        height: '100%',
      }}
    >
      <Toolbar
        sx={{
          backgroundColor: '#f5f5f5', // Slightly darker header to stand out
          borderBottom: '2px solid #e0e0e0',
        }}
      >
        <Typography variant="h6" noWrap component="div">
          LibreLog
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {/* Core Operations */}
        {coreItems.map((item) => {
          const isActive = isPathActive(item.path)
          const itemColor = item.color || getModuleColor(item.path, item.group)
          return (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                selected={isActive}
                onClick={() => navigate(item.path)}
                sx={{
                  borderLeft: isActive ? `3px solid ${itemColor}` : '3px solid transparent',
                  backgroundColor: isActive ? alpha(itemColor, 0.12) : 'transparent',
                  '&:hover': {
                    backgroundColor: alpha(itemColor, 0.08),
                  },
                  '& .MuiListItemIcon-root': {
                    color: isActive ? alpha(itemColor, 0.15) : 'inherit',
                  },
                }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          )
        })}
        
        {/* Grouped Items */}
        {groupedItems.map(({ group, label, items }) => {
          const groupColor = getModuleColor('', group)
          const groupIsActive = isGroupActive(group)
          return (
            <React.Fragment key={group}>
              <ListItemButton 
                onClick={() => handleGroupToggle(group)}
                sx={{
                  borderLeft: `3px solid ${groupColor}`, // Always show section color
                  backgroundColor: alpha(groupColor, 0.15), // Darker for header
                  '&:hover': {
                    backgroundColor: alpha(groupColor, 0.2),
                  },
                  '& .MuiListItemText-primary': {
                    fontWeight: 'bold',
                    color: groupColor,
                  },
                  // For light colors (yellow), use darker text for better contrast
                  ...(group === 'logs' ? {
                    '& .MuiListItemText-primary': {
                      color: '#424242', // Dark text for light backgrounds
                    },
                    '& .MuiListItemIcon-root': {
                      color: '#424242',
                    },
                  } : {}),
                }}
              >
                <ListItemIcon sx={{ 
                  color: group === 'logs' ? '#424242' : groupColor 
                }}>
                  {expandedGroups[group] ? <ExpandLess /> : <ExpandMore />}
                </ListItemIcon>
                <ListItemText primary={label} />
              </ListItemButton>
              <Collapse in={expandedGroups[group]} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {items.map((item) => {
                    const isActive = isPathActive(item.path)
                    const itemColor = item.color || groupColor
                    return (
                      <ListItem key={item.text} disablePadding>
                        <ListItemButton
                          selected={isActive}
                          onClick={() => navigate(item.path)}
                          sx={{
                            pl: 4,
                            borderLeft: isActive ? `3px solid ${itemColor}` : `1px solid ${alpha(itemColor, 0.2)}`,
                            backgroundColor: isActive ? alpha(itemColor, 0.08) : alpha(itemColor, 0.03), // Lighter for sub-items
                            '&:hover': {
                              backgroundColor: alpha(itemColor, 0.1),
                            },
                            '& .MuiListItemIcon-root': {
                              color: isActive ? itemColor : alpha(itemColor, 0.6),
                            },
                            // For light colors (yellow), use darker text for better contrast
                            ...(group === 'logs' ? {
                              '& .MuiListItemText-primary': {
                                color: isActive ? '#424242' : '#616161',
                              },
                              '& .MuiListItemIcon-root': {
                                color: isActive ? '#424242' : alpha('#424242', 0.7),
                              },
                            } : {}),
                          }}
                        >
                          <ListItemIcon>{item.icon}</ListItemIcon>
                          <ListItemText primary={item.text} />
                        </ListItemButton>
                      </ListItem>
                    )
                  })}
                </List>
              </Collapse>
            </React.Fragment>
          )
        })}
        
        {/* Standalone items (Clock Templates, Reports, Help) - after all collapsible sections */}
        {standaloneItems.map((item) => {
          const isActive = isPathActive(item.path)
          const itemColor = item.color || getModuleColor(item.path, item.group)
          return (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                selected={isActive}
                onClick={() => navigate(item.path)}
                sx={{
                  borderLeft: isActive ? `3px solid ${itemColor}` : '3px solid transparent',
                  backgroundColor: isActive ? alpha(itemColor, 0.12) : 'transparent',
                  '&:hover': {
                    backgroundColor: alpha(itemColor, 0.08),
                  },
                  '& .MuiListItemIcon-root': {
                    color: isActive ? alpha(itemColor, 0.15) : 'inherit',
                  },
                }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          )
        })}
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
          backgroundColor: headerColor,
          boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
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
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexGrow: 1 }}>
            {logoUrl && (
              <img 
                src={logoUrl} 
                alt="Logo" 
                style={{ maxHeight: 40, maxWidth: 200 }}
                onError={(e) => {
                  console.error('Failed to load logo:', logoUrl)
                  e.currentTarget.style.display = 'none'
                }}
              />
            )}
            <Typography variant="h6" noWrap component="div">
              {systemName}
            </Typography>
          </Box>
          {healthError ? (
            <Chip 
              label="API Offline" 
              size="small" 
              sx={{ 
                mr: 1,
                backgroundColor: '#d32f2f !important',
                color: '#fff !important',
                fontWeight: 'bold',
              }}
            />
          ) : healthData ? (
            <Chip 
              label="API Online" 
              size="small" 
              sx={{ 
                mr: 1,
                backgroundColor: '#2e7d32 !important',
                color: '#fff !important',
                fontWeight: 'bold',
              }}
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
            '& .MuiDrawer-paper': { 
              boxSizing: 'border-box', 
              width: drawerWidth,
              backgroundColor: '#fafafa', // Neutral light background
              borderRight: '1px solid #e0e0e0',
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { 
              boxSizing: 'border-box', 
              width: drawerWidth,
              backgroundColor: '#fafafa', // Neutral light background
              borderRight: '1px solid #e0e0e0',
            },
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
