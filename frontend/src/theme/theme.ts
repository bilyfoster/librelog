import { createTheme, ThemeOptions } from '@mui/material/styles'

/**
 * Module Color System
 * Preserved from original Layout.tsx
 * These colors are used for navigation accents and module-specific styling
 */
export const moduleColors = {
  // Core Operations - Rainbow colors
  dashboard: '#ff0000',       // Red - Main dashboard
  library: '#ff7f00',         // Orange - Audio library
  clocks: '#ffff00',          // Yellow - Clock templates
  
  // Workflow Sections - Rainbow order
  traffic: '#ff0000',         // Red - Traffic Management
  production: '#ff7f00',      // Orange - Production
  logs: '#ffff00',            // Yellow - Log Management
  billing: '#00ff00',         // Green - Billing
  analytics: '#0000ff',       // Blue - Analytics
  admin: '#4b0082',           // Indigo - Admin
  reports: '#9400d3',         // Violet - Reports hub
  help: '#616161',            // Grey - Help section
} as const

export type ModuleColorKey = keyof typeof moduleColors

/**
 * Function to get module color based on path or group
 * Preserved from Layout.tsx
 */
export const getModuleColor = (path: string, group?: string): string => {
  if (path === '/dashboard') return moduleColors.dashboard
  if (path === '/library' || path.startsWith('/library')) return moduleColors.library
  if (path === '/clocks') return moduleColors.clocks
  if (path === '/reports') return moduleColors.reports
  if (path === '/help') return moduleColors.help
  if (group === 'library') return moduleColors.library
  if (group === 'traffic') return moduleColors.traffic
  if (group === 'logs') return moduleColors.logs
  if (group === 'production') return moduleColors.production
  if (group === 'billing') return moduleColors.billing
  if (group === 'analytics') return moduleColors.analytics
  if (group === 'admin') return moduleColors.admin
  return moduleColors.dashboard // default
}

/**
 * Light Theme Configuration
 */
const lightThemeOptions: ThemeOptions = {
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#dc004e',
      light: '#ff5983',
      dark: '#9a0036',
    },
    background: {
      default: '#ffffff',
      paper: '#ffffff',
    },
    text: {
      primary: 'rgba(0, 0, 0, 0.87)',
      secondary: 'rgba(0, 0, 0, 0.6)',
    },
  },
  typography: {
    fontFamily: [
      'Roboto',
      'Helvetica',
      'Arial',
      'sans-serif',
    ].join(','),
    h1: {
      fontSize: '2.5rem',
      fontWeight: 500,
      lineHeight: 1.2,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 500,
      lineHeight: 1.3,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 500,
      lineHeight: 1.4,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 500,
      lineHeight: 1.4,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
      lineHeight: 1.5,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.6,
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.5,
    },
    body2: {
      fontSize: '0.875rem',
      lineHeight: 1.43,
    },
    button: {
      textTransform: 'none', // Disable uppercase transformation
      fontWeight: 500,
    },
  },
  spacing: 8, // Base spacing unit (8px), MUI default
  shape: {
    borderRadius: 4,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 4,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        },
      },
    },
  },
}

/**
 * Dark Theme Configuration
 */
const darkThemeOptions: ThemeOptions = {
  ...lightThemeOptions,
  palette: {
    mode: 'dark',
    primary: {
      main: '#90caf9',
      light: '#e3f2fd',
      dark: '#42a5f5',
    },
    secondary: {
      main: '#f48fb1',
      light: '#fce4ec',
      dark: '#ad1457',
    },
    background: {
      default: '#121212',
      paper: '#1e1e1e',
    },
    text: {
      primary: 'rgba(255, 255, 255, 0.87)',
      secondary: 'rgba(255, 255, 255, 0.6)',
    },
  },
}

/**
 * Create Material-UI themes
 */
export const lightTheme = createTheme(lightThemeOptions)
export const darkTheme = createTheme(darkThemeOptions)

/**
 * Default export - light theme
 */
export default lightTheme

