import React from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import { useQuery } from '@tanstack/react-query'
import { checkApiHealth } from '../utils/api'
import { getModuleColor } from '../theme'
import {
  Drawer,
  AppBar,
  Toolbar,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  Chip,
  Box,
  Typography,
  Collapse,
  useTheme,
  useMediaQuery,
} from '@mui/material'
import {
  Menu as MenuIcon,
  ChevronRight,
  ExpandMore,
  Person,
  Logout,
  Dashboard,
  LibraryMusic,
  DiscFull,
  Schedule,
  Traffic,
  Store,
  Business,
  ShoppingCart,
  People,
  CalendarToday,
  History,
  LocalOffer,
  Shuffle,
  List as ListIcon,
  Description,
  Build,
  Speed,
  Mic,
  Archive,
  NoteAdd,
  Headphones,
  Receipt,
  CreditCard,
  RotateRight,
  Warehouse,
  TrendingUp,
  Flag,
  AdminPanelSettings,
  Groups,
  Public,
  Radio,
  Layers,
  Settings,
  Security,
  Code,
  Notifications,
  Storage,
  Assessment,
  Help,
} from '@mui/icons-material'

const drawerWidth = 240

interface MenuItemType {
  text: string
  icon: React.ReactNode
  path: string
  group?: string
  color?: string
}

// Icon mapping function
const getIcon = (iconName: string): React.ReactNode => {
  const iconMap: { [key: string]: React.ReactNode } = {
    'fa-solid fa-gauge': <Dashboard />,
    'fa-solid fa-music': <LibraryMusic />,
    'fa-solid fa-compact-disc': <DiscFull />,
    'fa-solid fa-clock': <Schedule />,
    'fa-solid fa-traffic-light': <Traffic />,
    'fa-solid fa-store': <Store />,
    'fa-solid fa-building': <Business />,
    'fa-solid fa-shopping-cart': <ShoppingCart />,
    'fa-solid fa-users': <People />,
    'fa-solid fa-calendar-check': <CalendarToday />,
    'fa-solid fa-clock-rotate-left': <History />,
    'fa-solid fa-tags': <LocalOffer />,
    'fa-solid fa-shuffle': <Shuffle />,
    'fa-solid fa-list': <ListIcon />,
    'fa-solid fa-file-lines': <Description />,
    'fa-solid fa-screwdriver-wrench': <Build />,
    'fa-solid fa-gauge-high': <Speed />,
    'fa-solid fa-microphone-lines': <Mic />,
    'fa-solid fa-box-archive': <Archive />,
    'fa-solid fa-file-circle-plus': <NoteAdd />,
    'fa-solid fa-microphone': <Mic />,
    'fa-solid fa-headphones': <Headphones />,
    'fa-solid fa-receipt': <Receipt />,
    'fa-solid fa-credit-card': <CreditCard />,
    'fa-solid fa-arrows-rotate': <RotateRight />,
    'fa-solid fa-warehouse': <Warehouse />,
    'fa-solid fa-chart-line': <TrendingUp />,
    'fa-solid fa-flag': <Flag />,
    'fa-solid fa-user': <Person />,
    'fa-solid fa-people-group': <Groups />,
    'fa-solid fa-globe': <Public />,
    'fa-solid fa-radio': <Radio />,
    'fa-solid fa-layer-group': <Layers />,
    'fa-solid fa-gear': <Settings />,
    'fa-solid fa-shield-halved': <Security />,
    'fa-solid fa-code-branch': <Code />,
    'fa-solid fa-bell': <Notifications />,
    'fa-solid fa-database': <Storage />,
    'fa-solid fa-chart-bar': <Assessment />,
    'fa-solid fa-circle-question': <Help />,
  }
  return iconMap[iconName] || <Box sx={{ width: 24, height: 24 }} />
}

const menuItems: MenuItemType[] = [
  // Core Operations
  { text: 'Dashboard', icon: getIcon('fa-solid fa-gauge'), path: '/dashboard' },
  
  // Library & Programming
  { text: 'Audio Library', icon: getIcon('fa-solid fa-music'), path: '/library', group: 'library' },
  { text: 'Music Manager', icon: getIcon('fa-solid fa-compact-disc'), path: '/library/music', group: 'library' },
  { text: 'Clock Templates', icon: getIcon('fa-solid fa-clock'), path: '/clocks', group: 'library' },
  
  // Traffic Management
  { text: 'Traffic Manager', icon: getIcon('fa-solid fa-traffic-light'), path: '/traffic', group: 'traffic' },
  { text: 'Advertisers', icon: getIcon('fa-solid fa-store'), path: '/traffic/advertisers', group: 'traffic' },
  { text: 'Agencies', icon: getIcon('fa-solid fa-building'), path: '/traffic/agencies', group: 'traffic' },
  { text: 'Orders', icon: getIcon('fa-solid fa-shopping-cart'), path: '/traffic/orders', group: 'traffic' },
  { text: 'Sales Reps', icon: getIcon('fa-solid fa-users'), path: '/traffic/sales-reps', group: 'traffic' },
  { text: 'Spot Scheduler', icon: getIcon('fa-solid fa-calendar-check'), path: '/traffic/spot-scheduler', group: 'traffic' },
  { text: 'Dayparts', icon: getIcon('fa-solid fa-clock-rotate-left'), path: '/traffic/dayparts', group: 'traffic' },
  { text: 'Daypart Categories', icon: getIcon('fa-solid fa-tags'), path: '/traffic/daypart-categories', group: 'traffic' },
  { text: 'Rotation Rules', icon: getIcon('fa-solid fa-shuffle'), path: '/traffic/rotation-rules', group: 'traffic' },
  { text: 'Traffic Logs', icon: getIcon('fa-solid fa-list'), path: '/traffic/traffic-logs', group: 'traffic' },
  { text: 'Copy Library', icon: getIcon('fa-solid fa-file-lines'), path: '/traffic/copy', group: 'traffic' },
  
  // Production
  { text: 'Production Orders', icon: getIcon('fa-solid fa-screwdriver-wrench'), path: '/production/orders', group: 'production' },
  { text: 'Producer Dashboard', icon: getIcon('fa-solid fa-gauge-high'), path: '/production/dashboard', group: 'production' },
  { text: 'Voice Talent Portal', icon: getIcon('fa-solid fa-microphone-lines'), path: '/production/voice-talent', group: 'production' },
  { text: 'Production Archive', icon: getIcon('fa-solid fa-box-archive'), path: '/production/archive', group: 'production' },
  
  // Log Management
  { text: 'Log Generator', icon: getIcon('fa-solid fa-file-circle-plus'), path: '/logs', group: 'logs' },
  { text: 'Voice Tracking', icon: getIcon('fa-solid fa-microphone'), path: '/voice', group: 'logs' },
  { text: 'Voice Tracks Manager', icon: getIcon('fa-solid fa-headphones'), path: '/voice/tracks', group: 'logs' },
  
  // Billing
  { text: 'Invoices', icon: getIcon('fa-solid fa-receipt'), path: '/billing/invoices', group: 'billing' },
  { text: 'Payments', icon: getIcon('fa-solid fa-credit-card'), path: '/billing/payments', group: 'billing' },
  { text: 'Makegoods', icon: getIcon('fa-solid fa-arrows-rotate'), path: '/billing/makegoods', group: 'billing' },
  
  // Analytics
  { text: 'Inventory', icon: getIcon('fa-solid fa-warehouse'), path: '/analytics/inventory', group: 'analytics' },
  { text: 'Revenue', icon: getIcon('fa-solid fa-chart-line'), path: '/analytics/revenue', group: 'analytics' },
  { text: 'Sales Goals', icon: getIcon('fa-solid fa-flag'), path: '/analytics/sales-goals', group: 'analytics' },
  
  // Admin
  { text: 'Users', icon: getIcon('fa-solid fa-user'), path: '/admin/users', group: 'admin' },
  { text: 'Sales Teams', icon: getIcon('fa-solid fa-people-group'), path: '/admin/sales-teams', group: 'admin' },
  { text: 'Sales Offices', icon: getIcon('fa-solid fa-building'), path: '/admin/sales-offices', group: 'admin' },
  { text: 'Sales Regions', icon: getIcon('fa-solid fa-globe'), path: '/admin/sales-regions', group: 'admin' },
  { text: 'Stations', icon: getIcon('fa-solid fa-radio'), path: '/admin/stations', group: 'admin' },
  { text: 'Clusters', icon: getIcon('fa-solid fa-layer-group'), path: '/admin/clusters', group: 'admin' },
  { text: 'Settings', icon: getIcon('fa-solid fa-gear'), path: '/admin/settings', group: 'admin' },
  { text: 'Audit Logs', icon: getIcon('fa-solid fa-shield-halved'), path: '/admin/audit-logs', group: 'admin' },
  { text: 'Webhooks', icon: getIcon('fa-solid fa-code-branch'), path: '/admin/webhooks', group: 'admin' },
  { text: 'Notifications', icon: getIcon('fa-solid fa-bell'), path: '/admin/notifications', group: 'admin' },
  { text: 'Backups', icon: getIcon('fa-solid fa-database'), path: '/admin/backups', group: 'admin' },
  
  // Standalone items
  { text: 'Reports', icon: getIcon('fa-solid fa-chart-bar'), path: '/reports' },
  { text: 'Help', icon: getIcon('fa-solid fa-circle-question'), path: '/help' },
]

const menuGroups = {
  library: 'Library & Programming',
  traffic: 'Traffic Management',
  production: 'Production',
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
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  
  // Fetch branding settings
  const { data: settingsData } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => {
      try {
        const { getAllSettings } = await import('../utils/api')
        return await getAllSettings()
      } catch {
        return null
      }
    },
    staleTime: 5 * 60 * 1000,
  })
  
  const systemName = settingsData?.branding?.system_name?.value || 'GayPHX Radio Traffic System'
  const headerColor = settingsData?.branding?.header_color?.value || '#424242'
  const logoUrl = settingsData?.branding?.logo_url?.value || ''
  
  // Check API health status
  const { data: healthData, error: healthError } = useQuery({
    queryKey: ['api-health-global'],
    queryFn: () => checkApiHealth(),
    retry: false,
    refetchInterval: 60000,
    staleTime: 30000,
  })
  
  // Collapsible menu state
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

  const handleLogout = () => {
    logout()
    setAnchorEl(null)
  }

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleGroupToggle = (group: string) => {
    const newExpanded = { ...expandedGroups, [group]: !expandedGroups[group] }
    setExpandedGroups(newExpanded)
    localStorage.setItem('menuExpandedGroups', JSON.stringify(newExpanded))
  }

  const isPathActive = (path: string) => {
    if (location.pathname === path) return true
    if (location.pathname.startsWith(path) && path !== '/') return true
    return false
  }

  // Group menu items
  const coreItems = menuItems.filter(item => !item.group && item.path === '/dashboard')
  const standaloneItems = menuItems.filter(item => !item.group && item.path !== '/dashboard')
  const groupedItems = Object.keys(menuGroups).map(group => ({
    group,
    label: menuGroups[group as keyof typeof menuGroups],
    items: menuItems.filter(item => item.group === group),
  }))

  const drawer = (
    <Box>
      <Toolbar
        sx={{
          backgroundColor: theme.palette.mode === 'dark' ? '#1e1e1e' : '#f5f5f5',
          borderBottom: `2px solid ${theme.palette.divider}`,
          minHeight: '64px !important',
        }}
      >
        <Typography variant="h6" sx={{ fontWeight: 'bold', fontSize: '1.25rem' }}>
          LibreLog
        </Typography>
      </Toolbar>
      
      <List sx={{ padding: '8px 0' }}>
        {/* Core Operations */}
        {coreItems.map((item) => {
          const isActive = isPathActive(item.path)
          const moduleColor = getModuleColor(item.path, item.group)
          return (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                onClick={() => navigate(item.path)}
                sx={{
                  borderLeft: isActive ? `3px solid ${moduleColor}` : '3px solid transparent',
                  backgroundColor: isActive 
                    ? theme.palette.mode === 'dark' 
                      ? 'rgba(25, 118, 210, 0.16)' 
                      : 'rgba(25, 118, 210, 0.08)'
                    : 'transparent',
                  '&:hover': {
                    backgroundColor: isActive
                      ? theme.palette.mode === 'dark'
                        ? 'rgba(25, 118, 210, 0.24)'
                        : 'rgba(25, 118, 210, 0.12)'
                      : theme.palette.mode === 'dark'
                        ? 'rgba(255, 255, 255, 0.05)'
                        : 'rgba(0, 0, 0, 0.04)',
                  },
                }}
              >
                <ListItemIcon sx={{ color: isActive ? moduleColor : 'inherit', minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          )
        })}
        
        {/* Grouped Items */}
        {groupedItems.map(({ group, label, items }) => {
          const isExpanded = expandedGroups[group]
          const groupIsActive = items.some(item => isPathActive(item.path))
          const moduleColor = getModuleColor('', group)
          return (
            <React.Fragment key={group}>
              <ListItem disablePadding>
                <ListItemButton
                  onClick={() => handleGroupToggle(group)}
                  sx={{
                    borderLeft: groupIsActive ? `3px solid ${moduleColor}` : '3px solid transparent',
                    backgroundColor: groupIsActive
                      ? theme.palette.mode === 'dark'
                        ? 'rgba(25, 118, 210, 0.16)'
                        : 'rgba(25, 118, 210, 0.08)'
                      : theme.palette.mode === 'dark'
                        ? 'rgba(255, 255, 255, 0.05)'
                        : '#f5f5f5',
                    fontWeight: 'bold',
                    '&:hover': {
                      backgroundColor: groupIsActive
                        ? theme.palette.mode === 'dark'
                          ? 'rgba(25, 118, 210, 0.24)'
                          : 'rgba(25, 118, 210, 0.12)'
                        : theme.palette.mode === 'dark'
                          ? 'rgba(255, 255, 255, 0.08)'
                          : '#eeeeee',
                    },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    {isExpanded ? <ExpandMore fontSize="small" /> : <ChevronRight fontSize="small" />}
                  </ListItemIcon>
                  <ListItemText primary={label} />
                  {!isExpanded && items.length > 0 && (
                    <Chip 
                      label={items.length} 
                      size="small" 
                      sx={{ 
                        height: 20, 
                        fontSize: '0.75rem',
                        ml: 1,
                      }} 
                    />
                  )}
                </ListItemButton>
              </ListItem>
              <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {items.map((item) => {
                    const isActive = isPathActive(item.path)
                    const moduleColor = getModuleColor(item.path, item.group)
                    return (
                      <ListItem key={item.text} disablePadding>
                        <ListItemButton
                          onClick={() => navigate(item.path)}
                          sx={{
                            pl: 6,
                            borderLeft: isActive ? `3px solid ${moduleColor}` : '1px solid transparent',
                            backgroundColor: isActive
                              ? theme.palette.mode === 'dark'
                                ? 'rgba(25, 118, 210, 0.16)'
                                : 'rgba(25, 118, 210, 0.08)'
                              : 'transparent',
                            '&:hover': {
                              backgroundColor: isActive
                                ? theme.palette.mode === 'dark'
                                  ? 'rgba(25, 118, 210, 0.24)'
                                  : 'rgba(25, 118, 210, 0.12)'
                                : theme.palette.mode === 'dark'
                                  ? 'rgba(255, 255, 255, 0.05)'
                                  : 'rgba(0, 0, 0, 0.04)',
                            },
                          }}
                        >
                          <ListItemIcon sx={{ color: isActive ? moduleColor : 'inherit', minWidth: 40, opacity: isActive ? 1 : 0.7 }}>
                            {item.icon}
                          </ListItemIcon>
                          <ListItemText 
                            primary={item.text}
                            primaryTypographyProps={{
                              fontSize: '0.95em',
                              fontWeight: isActive ? 500 : 400,
                            }}
                          />
                        </ListItemButton>
                      </ListItem>
                    )
                  })}
                </List>
              </Collapse>
            </React.Fragment>
          )
        })}
        
        {/* Standalone items */}
        {standaloneItems.map((item) => {
          const isActive = isPathActive(item.path)
          const moduleColor = getModuleColor(item.path, item.group)
          return (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                onClick={() => navigate(item.path)}
                sx={{
                  borderLeft: isActive ? `3px solid ${moduleColor}` : '3px solid transparent',
                  backgroundColor: isActive
                    ? theme.palette.mode === 'dark'
                      ? 'rgba(25, 118, 210, 0.16)'
                      : 'rgba(25, 118, 210, 0.08)'
                    : 'transparent',
                  '&:hover': {
                    backgroundColor: isActive
                      ? theme.palette.mode === 'dark'
                        ? 'rgba(25, 118, 210, 0.24)'
                        : 'rgba(25, 118, 210, 0.12)'
                      : theme.palette.mode === 'dark'
                        ? 'rgba(255, 255, 255, 0.05)'
                        : 'rgba(0, 0, 0, 0.04)',
                  },
                }}
              >
                <ListItemIcon sx={{ color: isActive ? moduleColor : 'inherit', minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          )
        })}
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* AppBar */}
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
          backgroundColor: headerColor,
          zIndex: theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexGrow: 1 }}>
            {logoUrl && (
              <Box
                component="img"
                src={logoUrl}
                alt="Logo"
                sx={{ maxHeight: 40, maxWidth: 200 }}
                onError={(e) => {
                  console.error('Failed to load logo:', logoUrl)
                  e.currentTarget.style.display = 'none'
                }}
              />
            )}
            <Typography variant="h6" component="h1" sx={{ fontWeight: 500 }}>
              {systemName}
            </Typography>
          </Box>
          
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
              size="small"
              sx={{ 
                mr: 1,
                bgcolor: 'success.main',
                color: 'success.contrastText',
              }}
            />
          ) : null}
          
          <NotificationBell />
          
          <IconButton
            onClick={handleMenuOpen}
            sx={{ ml: 1 }}
            size="small"
          >
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </Avatar>
          </IconButton>
          
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={() => { navigate('/profile'); handleMenuClose(); }}>
              <ListItemIcon>
                <Person fontSize="small" />
              </ListItemIcon>
              <ListItemText>Profile</ListItemText>
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <Logout fontSize="small" />
              </ListItemIcon>
              <ListItemText>Logout</ListItemText>
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Drawer */}
      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile.
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

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          mt: '64px',
          overflow: 'auto',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  )
}

export default Layout
