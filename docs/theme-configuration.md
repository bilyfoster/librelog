# Material-UI Theme Configuration

## Overview

LibreLog uses Material-UI (MUI) v5.16.7 for all UI components. The theme system is configured in `frontend/src/theme/theme.ts` and provides:

- Light and dark mode support
- Module-specific color system
- Consistent typography and spacing
- Custom component styling

## Theme Location

The theme configuration is located at:
- **File**: `frontend/src/theme/theme.ts`
- **Export**: `lightTheme`, `darkTheme`, `moduleColors`, `getModuleColor()`

## Module Color System

The module color system provides distinct colors for each major section of the application:

```typescript
export const moduleColors = {
  dashboard: '#ff0000',       // Red - Main dashboard
  library: '#ff7f00',         // Orange - Audio library
  clocks: '#ffff00',          // Yellow - Clock templates
  traffic: '#ff0000',         // Red - Traffic Management
  production: '#ff7f00',      // Orange - Production
  logs: '#ffff00',            // Yellow - Log Management
  billing: '#00ff00',         // Green - Billing
  analytics: '#0000ff',       // Blue - Analytics
  admin: '#4b0082',           // Indigo - Admin
  reports: '#9400d3',         // Violet - Reports hub
  help: '#616161',            // Grey - Help section
}
```

### Using Module Colors

Module colors are used in navigation and module-specific styling:

```typescript
import { getModuleColor } from '../theme/theme'

const color = getModuleColor('/traffic/orders', 'traffic')
// Returns: '#ff0000'
```

## Theme Structure

### Light Theme

The light theme is the default theme used throughout the application:

- **Primary Color**: `#1976d2` (Blue)
- **Secondary Color**: `#dc004e` (Pink/Red)
- **Background**: White (`#ffffff`)
- **Text**: Dark gray/black for contrast

### Dark Theme

The dark theme is available but not currently enabled by default:

- **Primary Color**: `#90caf9` (Light Blue)
- **Secondary Color**: `#f48fb1` (Light Pink)
- **Background**: Dark (`#121212`)
- **Text**: White/Light gray

## Typography

The theme uses Roboto as the primary font family with fallbacks:

```typescript
fontFamily: ['Roboto', 'Helvetica', 'Arial', 'sans-serif'].join(',')
```

Typography scales:
- **H1**: 2.5rem, 500 weight
- **H2**: 2rem, 500 weight
- **H3**: 1.75rem, 500 weight
- **H4**: 1.5rem, 500 weight
- **H5**: 1.25rem, 500 weight
- **H6**: 1rem, 500 weight
- **Body**: 1rem, 400 weight

## Spacing

Material-UI uses an 8px spacing unit by default. The theme preserves this standard spacing scale.

## Component Customization

### Buttons

Buttons are configured to not transform text to uppercase:

```typescript
MuiButton: {
  styleOverrides: {
    root: {
      textTransform: 'none',
    },
  },
}
```

### Chips

Chips have custom color variants for success and error states:

```typescript
MuiChip: {
  variants: [
    {
      props: { color: 'success' },
      style: {
        backgroundColor: '#4caf50',
        color: '#fff',
      },
    },
    {
      props: { color: 'error' },
      style: {
        backgroundColor: '#f44336',
        color: '#fff',
      },
    },
  ],
}
```

## Usage in Components

### Basic Usage

```typescript
import { ThemeProvider } from '@mui/material'
import { lightTheme } from './theme/theme'

function App() {
  return (
    <ThemeProvider theme={lightTheme}>
      {/* Your app content */}
    </ThemeProvider>
  )
}
```

### Using Theme in Components

```typescript
import { useTheme } from '@mui/material/styles'
import { Box } from '@mui/material'

function MyComponent() {
  const theme = useTheme()
  
  return (
    <Box sx={{ 
      color: theme.palette.primary.main,
      padding: theme.spacing(2)
    }}>
      Content
    </Box>
  )
}
```

### Using Module Colors

```typescript
import { getModuleColor } from '../theme/theme'

function NavigationItem({ path, group }) {
  const moduleColor = getModuleColor(path, group)
  
  return (
    <Box sx={{ 
      borderLeft: `3px solid ${moduleColor}`,
      padding: 2
    }}>
      Navigation Item
    </Box>
  )
}
```

## Migration from WebAwesome

The Material-UI theme was created during the migration from WebAwesome. Key preserved elements:

1. **Module Colors**: All module colors from the original Layout.tsx were preserved
2. **Typography**: Font family and sizes maintained
3. **Spacing**: Standard 8px spacing unit preserved
4. **Color Palette**: Primary and secondary colors maintained

## Future Enhancements

Potential improvements to the theme system:

1. **Dark Mode Toggle**: Add user preference for dark mode
2. **Custom Theme Builder**: Allow users to customize colors
3. **Accessibility**: Enhanced contrast ratios for accessibility
4. **Responsive Typography**: Better scaling for different screen sizes

## References

- [Material-UI Theme Documentation](https://mui.com/material-ui/customization/theming/)
- [Material-UI Theme API](https://mui.com/material-ui/customization/theming/#theme-configuration-variables)
- Theme file: `frontend/src/theme/theme.ts`

