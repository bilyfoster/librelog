import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    strictPort: false,
    allowedHosts: [
      'log-dev.gayphx.com',
      'localhost',
      '.gayphx.com',
    ],
    proxy: {
      '/api': {
        target: 'http://api:8000',
        changeOrigin: true,
        secure: false,
        ws: true, // Enable WebSocket proxying
        rewrite: (path) => path.replace(/^\/api/, ''), // Strip /api prefix before forwarding
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, res) => {
            console.error('Vite proxy error:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            // Log proxy requests for debugging
            console.log('Proxying:', req.url, '->', proxyReq.path);
          });
          // Ensure proxy responses don't include HTTP redirects
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // Remove any Location headers that might contain HTTP URLs
            if (proxyRes.headers.location) {
              const location = proxyRes.headers.location;
              // If location is HTTP, convert to relative path
              if (location.startsWith('http://')) {
                const url = new URL(location);
                proxyRes.headers.location = url.pathname + (url.search || '');
              }
            }
          });
        },
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/tests/setup.ts'],
  },
})
