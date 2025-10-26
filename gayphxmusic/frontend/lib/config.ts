// Runtime configuration for API URL
// This allows the frontend to connect to the backend properly in Docker

const getApiUrl = () => {
  // Always use the backend URL directly to avoid Next.js proxy issues
  return 'http://localhost:8000'
}

export const API_BASE_URL = getApiUrl()
