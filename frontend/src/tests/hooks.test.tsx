import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useAuth } from '../contexts/AuthContext'
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

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

// Mock useNavigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue(null)
  })

  it('provides user state', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={new QueryClient()}>
        {children}
      </QueryClientProvider>
    )

    const { result } = renderHook(() => useAuth(), { wrapper })
    
    expect(result.current.user).toBeNull()
    expect(result.current.isLoading).toBe(true)
    expect(typeof result.current.login).toBe('function')
    expect(typeof result.current.logout).toBe('function')
  })

  it('handles login with valid credentials', async () => {
    const mockResponse = { data: { access_token: 'test-token' } }
    ;(api.post as any).mockResolvedValue(mockResponse)
    
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={new QueryClient()}>
        {children}
      </QueryClientProvider>
    )

    const { result } = renderHook(() => useAuth(), { wrapper })
    
    await act(async () => {
      await result.current.login('testuser', 'password')
    })
    
    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      username: 'testuser',
      password: 'password'
    })
    expect(localStorageMock.setItem).toHaveBeenCalledWith('token', 'test-token')
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  it('handles login with invalid credentials', async () => {
    ;(api.post as any).mockRejectedValue(new Error('Invalid credentials'))
    
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={new QueryClient()}>
        {children}
      </QueryClientProvider>
    )

    const { result } = renderHook(() => useAuth(), { wrapper })
    
    await expect(async () => {
      await act(async () => {
        await result.current.login('testuser', 'wrongpassword')
      })
    }).rejects.toThrow('Invalid credentials')
  })

  it('handles logout', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={new QueryClient()}>
        {children}
      </QueryClientProvider>
    )

    const { result } = renderHook(() => useAuth(), { wrapper })
    
    act(() => {
      result.current.logout()
    })
    
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('token')
    expect(mockNavigate).toHaveBeenCalledWith('/login')
  })
})

describe('API Utils', () => {
  it('creates API instance with correct configuration', () => {
    expect(api.defaults.baseURL).toBe('http://localhost:8000/api')
    expect(api.defaults.headers['Content-Type']).toBe('application/json')
  })

  it('has request interceptor configured', () => {
    expect(api.interceptors.request.use).toHaveBeenCalled()
  })

  it('has response interceptor configured', () => {
    expect(api.interceptors.response.use).toHaveBeenCalled()
  })
})
