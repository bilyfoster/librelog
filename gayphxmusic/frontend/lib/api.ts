// API configuration - use direct backend URL to avoid proxy issues
const getApiUrl = () => {
  if (typeof window !== 'undefined') {
    return 'http://localhost:8000'
  }
  return 'http://backend:8000'
}

export const API_BASE_URL = getApiUrl()

export const api = {
  // Auth endpoints
  signup: `${API_BASE_URL}/api/auth/signup`,
  requestMagicLink: `${API_BASE_URL}/api/auth/request-magic-link`,
  verify: `${API_BASE_URL}/api/auth/verify`,
  
  // Admin endpoints
  adminUsers: `${API_BASE_URL}/api/admin/users`,
  adminProfile: `${API_BASE_URL}/api/admin/profile`,
  adminIsrcKey: `${API_BASE_URL}/api/admin/isrc-key`,
  adminLogin: `${API_BASE_URL}/api/admin/login`,
  
  // Submissions endpoints
  submissions: `${API_BASE_URL}/api/submissions`,
  
  // Health check
  health: `${API_BASE_URL}/health`,
}

// Legacy exports for backward compatibility
export const adminApi = api
export const submissionsApi = api