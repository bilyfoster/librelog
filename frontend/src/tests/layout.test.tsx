import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import Layout from '../components/Layout'
import api from '../utils/api'

// Mock API
vi.mock('../utils/api', () => ({
  post: vi.fn(),
  get: vi.fn(),
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() }
  }
}))

// Mock AuthContext
const mockLogout = vi.fn()
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'testuser', role: 'admin' },
    login: vi.fn(),
    logout: mockLogout,
    isLoading: false
  })
}))

// Mock Outlet
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    Outlet: () => <div data-testid="outlet">Outlet Content</div>
  }
})

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

describe('Layout Component', () => {
  it('renders layout with navigation', () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    expect(screen.getByText('LibreLog')).toBeInTheDocument()
    expect(screen.getByText('GayPHX Radio Traffic System')).toBeInTheDocument()
  })

  it('renders navigation menu items', () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Music Library')).toBeInTheDocument()
    expect(screen.getByText('Clock Templates')).toBeInTheDocument()
    expect(screen.getByText('Traffic Manager')).toBeInTheDocument()
    expect(screen.getByText('Log Generator')).toBeInTheDocument()
    expect(screen.getByText('Voice Tracking')).toBeInTheDocument()
    expect(screen.getByText('Reports')).toBeInTheDocument()
  })

  it('renders user avatar', () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    expect(screen.getByText('T')).toBeInTheDocument() // First letter of username
  })

  it('handles logout', async () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    // Click on user avatar to open menu
    const avatar = screen.getByText('T')
    fireEvent.click(avatar)
    
    // Click logout
    const logoutButton = screen.getByText('Logout')
    fireEvent.click(logoutButton)
    
    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalled()
    })
  })

  it('renders outlet content', () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    expect(screen.getByTestId('outlet')).toBeInTheDocument()
  })

  it('shows mobile menu toggle on small screens', () => {
    render(
      <TestWrapper>
        <Layout />
      </TestWrapper>
    )
    
    // The menu button should be present but hidden on larger screens
    const menuButton = screen.getByLabelText('open drawer')
    expect(menuButton).toBeInTheDocument()
  })
})
