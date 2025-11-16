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
  
  // Traffic Management
  traffic: '#388e3c',         // Green - All traffic-related sections
  
  // Log Management
  logs: '#00897b',            // Teal - Log generator and voice tracking
  
  // Billing
  billing: '#f9a825',         // Amber - Invoices, payments, makegoods
  
  // Reports
  reports: '#5e35b1',         // Indigo - Reports hub
  
  // Analytics
  analytics: '#00acc1',       // Cyan - Inventory, revenue, sales goals
  
  // Admin
  admin: '#d32f2f',           // Red - Users, settings, audit logs, webhooks, notifications, backups
  
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
  if (group === 'billing') return moduleColors.billing
  if (group === 'analytics') return moduleColors.analytics
  if (group === 'admin') return moduleColors.admin
  return moduleColors.dashboard // default
}

const menuItems: MenuItem[] = [
  // Core Operations
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard', color: moduleColors.dashboard },
  { text: 'Audio Library', icon: <LibraryIcon />, path: '/library', color: moduleColors.library },
  { text: 'Clock Templates', icon: <ClockIcon />, path: '/clocks', color: moduleColors.clocks },
  
  // Traffic Management
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
  
  // Log Management
  { text: 'Log Generator', icon: <LogGeneratorIcon />, path: '/logs', group: 'logs', color: moduleColors.logs },
  { text: 'Voice Tracking', icon: <VoiceIcon />, path: '/voice', group: 'logs', color: moduleColors.logs },
  
  // Billing
  { text: 'Invoices', icon: <InvoiceIcon />, path: '/billing/invoices', group: 'billing', color: moduleColors.billing },
  { text: 'Payments', icon: <PaymentIcon />, path: '/billing/payments', group: 'billing', color: moduleColors.billing },
  { text: 'Makegoods', icon: <MakegoodIcon />, path: '/billing/makegoods', group: 'billing', color: moduleColors.billing },
  
  // Reports
  { text: 'Reports', icon: <ReportIcon />, path: '/reports', color: moduleColors.reports },
  
  // Help
  { text: 'Help', icon: <HelpIcon />, path: '/help', color: moduleColors.help },
  
  // Analytics
  { text: 'Inventory', icon: <InventoryIcon />, path: '/analytics/inventory', group: 'analytics', color: moduleColors.analytics },
  { text: 'Revenue', icon: <RevenueIcon />, path: '/analytics/revenue', group: 'analytics', color: moduleColors.analytics },
  { text: 'Sales Goals', icon: <GoalsIcon />, path: '/analytics/sales-goals', group: 'analytics', color: moduleColors.analytics },
  
  // Admin
  { text: 'Users', icon: <AccountIcon />, path: '/admin/users', group: 'admin', color: moduleColors.admin },
  { text: 'Settings', icon: <SettingsIcon />, path: '/admin/settings', group: 'admin', color: moduleColors.admin },
  { text: 'Audit Logs', icon: <SecurityIcon />, path: '/admin/audit-logs', group: 'admin', color: moduleColors.admin },
  { text: 'Webhooks', icon: <WebhookIcon />, path: '/admin/webhooks', group: 'admin', color: moduleColors.admin },
  { text: 'Notifications', icon: <NotificationsIcon />, path: '/admin/notifications', group: 'admin', color: moduleColors.admin },
  { text: 'Backups', icon: <BackupIcon />, path: '/admin/backups', group: 'admin', color: moduleColors.admin },
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
                  borderLeft: groupIsActive ? `3px solid ${groupColor}` : '3px solid transparent',
                  backgroundColor: groupIsActive ? alpha(groupColor, 0.1) : 'transparent',
                  '&:hover': {
                    backgroundColor: alpha(groupColor, 0.06),
                  },
                }}
              >
                <ListItemIcon>
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
              </Collapse>
            </React.Fragment>
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
