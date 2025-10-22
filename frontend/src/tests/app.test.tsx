import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import App from '../App'

// Mock all page components
vi.mock('../pages/Dashboard', () => ({
  default: () => <div data-testid="dashboard">Dashboard</div>
}))

vi.mock('../pages/auth/Login', () => ({
  default: () => <div data-testid="login">Login</div>
}))

vi.mock('../pages/library/LibraryList', () => ({
  default: () => <div data-testid="library">Library</div>
}))

vi.mock('../pages/clocks/ClockBuilder', () => ({
  default: () => <div data-testid="clocks">Clock Builder</div>
}))

vi.mock('../pages/traffic/TrafficManager', () => ({
  default: () => <div data-testid="traffic">Traffic Manager</div>
}))

vi.mock('../pages/logs/LogGenerator', () => ({
  default: () => <div data-testid="logs">Log Generator</div>
}))

vi.mock('../pages/voice/VoiceRecorder', () => ({
  default: () => <div data-testid="voice">Voice Recorder</div>
}))

vi.mock('../pages/reports/ReportsHub', () => ({
  default: () => <div data-testid="reports">Reports</div>
}))

// Mock AuthContext
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'testuser', role: 'admin' },
    login: vi.fn(),
    logout: vi.fn(),
    isLoading: false
  })
}))

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  })
  
  const theme = createTheme()
  
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <BrowserRouter>
          {children}
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

describe('App Component', () => {
  it('renders without crashing', () => {
    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    )
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument()
  })

  it('redirects to dashboard from root', () => {
    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    )
    
    expect(screen.getByTestId('dashboard')).toBeInTheDocument()
  })

  it('renders login page at /login', () => {
    // Mock window.location for testing different routes
    Object.defineProperty(window, 'location', {
      value: { pathname: '/login' },
      writable: true
    })
    
    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    )
    
    expect(screen.getByTestId('login')).toBeInTheDocument()
  })
})
