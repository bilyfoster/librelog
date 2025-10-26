/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  env: {
    NEXT_PUBLIC_APP_URL: process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000',
  },
  // Disable host header validation for Docker
  experimental: {
    serverComponentsExternalPackages: [],
  },
  // Disable host header validation for Docker
  async rewrites() {
    // Only proxy health check to backend, let API routes handle the rest
    const apiUrl = process.env.NODE_ENV === 'production' 
      ? 'http://backend:8000' 
      : 'http://localhost:8000'
    
    return [
      {
        source: '/health',
        destination: `${apiUrl}/health`,
      },
    ]
  },
  // Allow all hosts to prevent "Invalid host header" errors
  async headers() {
    return [
      {
        source: '/api/:path*',
        headers: [
          {
            key: 'Access-Control-Allow-Origin',
            value: '*',
          },
          {
            key: 'Access-Control-Allow-Methods',
            value: 'GET, POST, PUT, DELETE, OPTIONS',
          },
          {
            key: 'Access-Control-Allow-Headers',
            value: 'Content-Type, Authorization',
          },
        ],
      },
    ]
  },
  images: {
    domains: ['localhost'],
  },
}

module.exports = nextConfig