import axios from 'axios'

// URL normalization helper function
// Ensures all URLs are relative paths, stripping any absolute URLs
const normalizeUrl = (url: string | undefined): string => {
  if (!url) return ''
  
  // If it's already a relative path, return as-is
  if (url.startsWith('/')) {
    return url
  }
  
  // If it's an absolute URL, extract just the path
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const urlObj = new URL(url)
      return urlObj.pathname + (urlObj.search || '')
    } catch (e) {
      // If URL parsing fails, strip protocol and domain
      return url.replace(/^https?:\/\/[^/]+/, '')
    }
  }
  
  // If it doesn't start with /, make it relative
  return '/' + url
}

// Validate and fix URLs to prevent Docker hostnames
const validateAndFixUrl = (url: string | undefined, baseURL: string | undefined): { url: string; baseURL: string } => {
  // CRITICAL: Check for Docker hostnames BEFORE normalization
  // Check original values first to catch cases like "https://api:8000/api/tracks"
  const dockerHostnamePattern = /(?:https?:\/\/)?api:\d+/
  const originalUrl = url || ''
  const originalBaseURL = baseURL || ''
  const originalFullUrl = originalBaseURL + originalUrl
  
  // Detect Docker hostname in original values
  if (dockerHostnamePattern.test(originalUrl) || dockerHostnamePattern.test(originalBaseURL) || dockerHostnamePattern.test(originalFullUrl)) {
    console.error('[API] CRITICAL: Detected Docker hostname in URL!', {
      originalUrl,
      originalBaseURL,
      originalFullUrl,
      message: 'Docker hostnames like "api:8000" cannot be resolved by browsers. Using relative path instead.'
    })
    // Force relative paths - normalize will strip the Docker hostname
    return { url: normalizeUrl(url), baseURL: '/api' }
  }
  
  // Normalize URLs
  let normalizedUrl = normalizeUrl(url || '')
  let normalizedBaseURL = normalizeUrl(baseURL || '/api')
  
  // Double-check normalized values (shouldn't have Docker hostnames after normalization, but be safe)
  const normalizedFullUrl = normalizedBaseURL + normalizedUrl
  if (dockerHostnamePattern.test(normalizedFullUrl)) {
    console.error('[API] CRITICAL: Docker hostname detected after normalization!', {
      normalizedUrl,
      normalizedBaseURL,
      normalizedFullUrl,
      action: 'Forcing relative paths'
    })
    normalizedBaseURL = '/api'
    normalizedUrl = normalizeUrl(url || '')
  }
  
  // Ensure baseURL is always relative in browser
  if (typeof window !== 'undefined' && normalizedBaseURL && (normalizedBaseURL.startsWith('http://') || normalizedBaseURL.startsWith('https://'))) {
    console.warn('[API] Detected absolute baseURL in browser context, forcing relative:', normalizedBaseURL)
    normalizedBaseURL = '/api'
  }
  
  return { url: normalizedUrl, baseURL: normalizedBaseURL }
}

// Use relative path for API calls (works with Traefik proxy and Vite dev server)
// In browser, ALWAYS use relative path to match current page protocol
// This prevents mixed content errors and Docker hostname resolution issues
// CRITICAL: Lock baseURL to '/api' in browser - never allow absolute URLs
// CRITICAL: Force baseURL to be relative in browser context
// Never use Docker hostnames or absolute URLs in browser
const baseURL = typeof window !== 'undefined' ? '/api' : (import.meta.env.VITE_API_URL || '/api')

// CRITICAL: Ensure baseURL is never a Docker hostname
if (baseURL && (baseURL.includes('api:8000') || baseURL.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/))) {
  console.error('[API] CRITICAL: baseURL contains Docker hostname, forcing /api:', baseURL)
  // This shouldn't happen, but if it does, force it to /api
}

const api = axios.create({
  baseURL: baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
})

// CRITICAL: Override axios's internal URL construction to prevent absolute URLs
if (typeof window !== 'undefined') {
  // 1. Override getUri method - this is called by axios to construct the final URL
  const originalGetUri = api.getUri.bind(api)
  api.getUri = (config?: any) => {
    if (!config) {
      return originalGetUri(config)
    }
    
    // Force relative URLs before getUri processes them
    const originalBaseURL = config.baseURL
    const originalUrl = config.url
    
    // Force baseURL to '/api'
    config.baseURL = '/api'
    
    // Normalize url if needed
    if (config.url && (config.url.startsWith('http://') || config.url.startsWith('https://'))) {
      config.url = normalizeUrl(config.url)
    }
    
    // Get the URI from axios
    let uri = originalGetUri(config)
    
    // CRITICAL: If getUri returned an absolute URL with Docker hostname, strip it
    if (uri.includes('api:8000') || uri.match(/https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
      console.error('[API] getUri: CRITICAL - Docker hostname detected in URI, forcing relative:', uri)
      try {
        const urlObj = new URL(uri)
        uri = urlObj.pathname + urlObj.search
      } catch (e) {
        // If URL parsing fails, strip protocol and domain manually
        uri = uri.replace(/^https?:\/\/[^/]+/, '')
      }
    }
    
    // Restore original values (don't mutate the config permanently)
    config.baseURL = originalBaseURL
    config.url = originalUrl
    
    return uri
  }
  
  // 2. Add transformRequest to intercept URL construction
  api.defaults.transformRequest = [
    (data: any, headers: any) => {
      // This runs before axios constructs the final URL
      // We can't modify the URL here directly, but we ensure headers are correct
      return data
    },
    ...(Array.isArray(axios.defaults.transformRequest) ? axios.defaults.transformRequest : [axios.defaults.transformRequest].filter(Boolean))
  ]
  
  // 3. Custom adapter that wraps the default adapter
  // Store reference to original adapter (lazy initialization)
  let defaultAdapter: any = null
  
  const relativeUrlAdapter = (config: any) => {
    // Get default adapter on first use
    if (!defaultAdapter) {
      try {
        // Get the default adapter for the environment
        defaultAdapter = axios.getAdapter(['xhr', 'http'])
        if (!defaultAdapter) {
          defaultAdapter = axios.defaults.adapter
        }
      } catch (e) {
        console.error('[API] Failed to get default adapter:', e)
        // Fallback: let axios handle it
        defaultAdapter = null
      }
    }
    
    // CRITICAL: Force relative URLs before the adapter processes the request
    // Create a copy to avoid mutating the original config
    const correctedConfig = { ...config }
    
    // CRITICAL: Check for Docker hostnames in baseURL or url BEFORE processing
    if (correctedConfig.baseURL && (correctedConfig.baseURL.includes('api:8000') || correctedConfig.baseURL.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/))) {
      console.error('[API] Adapter: CRITICAL - Docker hostname in baseURL, forcing /api:', correctedConfig.baseURL)
      correctedConfig.baseURL = '/api'
    }
    
    if (correctedConfig.url && (correctedConfig.url.includes('api:8000') || correctedConfig.url.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/) || correctedConfig.url.match(/^[a-zA-Z0-9_-]+:\d+/))) {
      console.error('[API] Adapter: CRITICAL - Docker hostname in url, normalizing:', correctedConfig.url)
      // Extract path from Docker hostname URL
      if (correctedConfig.url.includes('api:8000')) {
        const pathMatch = correctedConfig.url.match(/api:8000(\/.*)$/)
        correctedConfig.url = pathMatch ? pathMatch[1] : normalizeUrl(correctedConfig.url)
      } else {
        correctedConfig.url = normalizeUrl(correctedConfig.url)
      }
    }
    
    // Force baseURL to '/api'
    correctedConfig.baseURL = '/api'
    
    // Normalize url if needed (absolute URLs)
    if (correctedConfig.url && (correctedConfig.url.startsWith('http://') || correctedConfig.url.startsWith('https://'))) {
      console.warn('[API] Adapter: Normalizing absolute URL to relative:', correctedConfig.url)
      correctedConfig.url = normalizeUrl(correctedConfig.url)
    }
    
    // Get the full URL that axios would construct
    const fullUrl = (correctedConfig.baseURL || '') + (correctedConfig.url || '')
    
    // CRITICAL: Final check - if the URL contains Docker hostname, force correction
    if (fullUrl.includes('api:8000') || fullUrl.match(/https?:\/\/[a-zA-Z0-9_-]+:\d+/) || fullUrl.match(/^[a-zA-Z0-9_-]+:\d+/)) {
      console.error('[API] Adapter: CRITICAL - Docker hostname detected in fullUrl, forcing correction:', fullUrl)
      correctedConfig.baseURL = '/api'
      // Extract path from fullUrl if it contains Docker hostname
      if (fullUrl.includes('api:8000')) {
        const pathMatch = fullUrl.match(/api:8000(\/.*)$/)
        correctedConfig.url = pathMatch ? pathMatch[1] : (correctedConfig.url || '').replace(/^.*api:8000/, '')
      } else {
        correctedConfig.url = normalizeUrl(correctedConfig.url || '')
      }
    }
    
    // Ensure baseURL is exactly '/api'
    if (correctedConfig.baseURL !== '/api') {
      correctedConfig.baseURL = '/api'
    }
    
    // Use a Proxy to intercept any attempts to read the URL as absolute
    const configProxy = new Proxy(correctedConfig, {
      get(target, prop) {
        if (prop === 'url' && target.url) {
          // If url is absolute, return normalized version
          if (target.url.startsWith('http://') || target.url.startsWith('https://')) {
            return normalizeUrl(target.url)
          }
        }
        if (prop === 'baseURL') {
          // Always return '/api' for baseURL
          return '/api'
        }
        return target[prop as keyof typeof target]
      },
      set(target, prop, value) {
        // Prevent setting absolute URLs
        if (prop === 'baseURL' && value && (value.startsWith('http://') || value.startsWith('https://') || value.includes('api:8000'))) {
          console.warn('[API] Adapter Proxy: Blocked attempt to set absolute baseURL:', value)
          target[prop as keyof typeof target] = '/api' as any
          return true
        }
        if (prop === 'url' && value && (value.startsWith('http://') || value.startsWith('https://'))) {
          console.warn('[API] Adapter Proxy: Blocked attempt to set absolute URL:', value)
          target[prop as keyof typeof target] = normalizeUrl(value) as any
          return true
        }
        target[prop as keyof typeof target] = value as any
        return true
      }
    })
    
    // CRITICAL: Final validation before calling adapter
  // Axios may construct URLs internally, so we need to ensure they're relative
  const finalBaseURL = configProxy.baseURL || '/api'
  const finalUrl = configProxy.url || ''
  const combinedUrl = finalBaseURL + finalUrl
  
  // If combined URL contains Docker hostname, force correction
  if (combinedUrl.includes('api:8000') || combinedUrl.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
    console.error('[API] Adapter: FINAL CHECK - Docker hostname in combined URL:', combinedUrl)
    configProxy.baseURL = '/api'
    // Extract just the path
    if (combinedUrl.includes('api:8000')) {
      const pathMatch = combinedUrl.match(/api:8000(\/.*)$/)
      configProxy.url = pathMatch ? pathMatch[1] : '/tracks'
    } else {
      try {
        const urlObj = new URL(combinedUrl)
        configProxy.url = urlObj.pathname + urlObj.search
      } catch {
        configProxy.url = combinedUrl.replace(/^https?:\/\/[^/]+/, '')
      }
    }
  }
  
  // Call the default adapter with our corrected config
    if (defaultAdapter) {
      try {
        return defaultAdapter(configProxy)
      } catch (e) {
        console.error('[API] Adapter execution error:', e)
        throw e
      }
    } else {
      // Fallback: use axios's default behavior
      return axios.getAdapter(['xhr', 'http'])(configProxy)
    }
  }
  
  // Override the adapter
  api.defaults.adapter = relativeUrlAdapter as any
  
  // 4. CRITICAL: Override XMLHttpRequest.open to intercept at the lowest level
  // This ensures that even if axios constructs an absolute URL, we can fix it
  if (typeof XMLHttpRequest !== 'undefined') {
    const OriginalXHROpen = XMLHttpRequest.prototype.open
    XMLHttpRequest.prototype.open = function(method: string, url: string | URL, ...args: any[]) {
      // CRITICAL: Always check and fix URLs before they reach the browser
      if (typeof url === 'string') {
        const originalUrl = url
        
        // Log problematic URLs for debugging
        if (url.includes('api:8000') || (url.startsWith('http://') && url.includes(':8000'))) {
          console.error('[API] XHR.open INTERCEPTED PROBLEMATIC URL:', { method, url, stack: new Error().stack })
        }
        
        // CRITICAL: Check for Docker hostnames (with or without protocol)
        // Pattern: api:8000 or https://api:8000 or http://api:8000 or api:8000/api/tracks
        // Also check for any hostname:port pattern that's not a valid domain
        const hasDockerHostname = url.includes('api:8000') || 
                                  url.match(/^[a-zA-Z0-9_-]+:\d+/) || 
                                  url.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/) ||
                                  (url.startsWith('http://') && url.includes(':8000'))
        
        if (hasDockerHostname) {
          console.error('[API] XHR.open: CRITICAL - Docker hostname detected, forcing relative:', url)
          
          // Extract just the path part - handle various formats
          let path = url
          
          // If it starts with http:// or https://, parse as URL
          if (url.startsWith('http://') || url.startsWith('https://')) {
            try {
              const urlObj = new URL(url)
              path = urlObj.pathname + urlObj.search
            } catch (e) {
              // If URL parsing fails, extract path manually
              // Remove protocol and hostname:port, keep path and query
              path = url.replace(/^https?:\/\/[^/]+/, '')
            }
          } else {
            // No protocol - extract path after hostname:port
            // Pattern: api:8000/api/tracks -> /api/tracks
            const pathMatch = url.match(/:[0-9]+(\/.*)$/)
            if (pathMatch) {
              path = pathMatch[1]
            } else {
              // If no path found, try to extract anything after the hostname:port
              path = url.replace(/^[^/]+/, '') || '/api'
            }
          }
          
          // Ensure path starts with /
          if (!path.startsWith('/')) {
            path = '/' + path
          }
          
          url = path
          console.warn('[API] XHR.open: Corrected Docker hostname URL:', { original: originalUrl, corrected: url })
        }
        
        // If it's an absolute URL but should be relative, make it relative
        if (url.startsWith('http://') || url.startsWith('https://')) {
          // CRITICAL: Always convert Docker hostnames to relative URLs
          if (url.includes('api:8000') || url.match(/https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
            console.error('[API] XHR.open: CRITICAL - Absolute URL with Docker hostname, forcing relative:', url)
            try {
              const urlObj = new URL(url)
              url = urlObj.pathname + urlObj.search
            } catch (e) {
              // If parsing fails, try to extract path
              url = url.replace(/^https?:\/\/[^/]+/, '')
            }
            // Ensure it starts with /api if it doesn't already
            if (!url.startsWith('/api') && !url.startsWith('/')) {
              url = '/api' + url
            }
          } else {
            // Only fix if it's not the current origin (to avoid breaking legitimate absolute URLs)
            try {
              const urlObj = new URL(url)
              const currentOrigin = window.location.origin
              if (urlObj.origin !== currentOrigin) {
                console.warn('[API] XHR.open: Converting absolute URL to relative:', url)
                url = urlObj.pathname + urlObj.search
              }
            } catch (e) {
              // If parsing fails, try to extract path
              url = url.replace(/^https?:\/\/[^/]+/, '')
            }
          }
        }
        
        // Log if we changed the URL
        if (originalUrl !== url) {
          console.warn('[API] XHR.open: URL corrected:', { original: originalUrl, corrected: url })
        }
      }
      // Call original open with potentially corrected URL
      return OriginalXHROpen.call(this, method, url, ...args)
    }
  }
  
  // 5. CRITICAL: Also override fetch() in case axios uses it
  if (typeof window !== 'undefined' && window.fetch) {
    const OriginalFetch = window.fetch
    window.fetch = function(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
      let url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url
      
      // Check for Docker hostnames
      if (typeof url === 'string' && (url.includes('api:8000') || (url.startsWith('http://') && url.includes(':8000')))) {
        console.error('[API] fetch() INTERCEPTED PROBLEMATIC URL:', url)
        
        // Extract path from URL
        if (url.includes('api:8000')) {
          const pathMatch = url.match(/api:8000(\/.*)$/)
          url = pathMatch ? pathMatch[1] : '/api/tracks'
        } else if (url.startsWith('http://') || url.startsWith('https://')) {
          try {
            const urlObj = new URL(url)
            url = urlObj.pathname + urlObj.search
          } catch {
            url = url.replace(/^https?:\/\/[^/]+/, '')
          }
        }
        
        // Ensure it starts with /
        if (!url.startsWith('/')) {
          url = '/' + url
        }
        
        console.warn('[API] fetch() corrected URL:', url)
        
        // Update input
        if (typeof input === 'string') {
          input = url
        } else if (input instanceof URL) {
          input = new URL(url, window.location.origin)
        } else {
          input = { ...input, url }
        }
      }
      
      return OriginalFetch.call(this, input as any, init)
    }
  }
}

// CRITICAL: Lock baseURL in browser context - prevent any modifications
if (typeof window !== 'undefined') {
  // Override baseURL setter to always force '/api'
  Object.defineProperty(api.defaults, 'baseURL', {
    get: () => '/api',
    set: (value: string) => {
      // Silently ignore attempts to set absolute URLs or Docker hostnames
      if (value && (value.startsWith('http://') || value.startsWith('https://') || value.includes('api:8000'))) {
        console.warn('[API] Attempted to set absolute baseURL blocked:', value, '-> forced to /api')
        return
      }
      // Only allow relative paths starting with /
      if (value && !value.startsWith('/')) {
        console.warn('[API] Attempted to set invalid baseURL blocked:', value, '-> forced to /api')
        return
      }
    },
    configurable: false,
    enumerable: true
  })
}

// Runtime URL validation - check actual request URLs before they're sent
// This catches cases where axios might construct absolute URLs internally
const validateRequestUrl = (config: any): void => {
  if (typeof window === 'undefined') return
  
  // Check the actual request URL that axios will use
  // Axios combines baseURL + url, so we need to check both
  const baseURL = config.baseURL || api.defaults.baseURL || ''
  const url = config.url || ''
  const fullUrl = baseURL + url
  
  // Check for Docker hostnames or absolute URLs
  if (fullUrl.includes('api:8000') || fullUrl.match(/https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
    console.error('[API] RUNTIME VALIDATION: Detected invalid URL before request!', {
      fullUrl,
      baseURL,
      url,
      action: 'Blocking request - URL contains Docker hostname or absolute URL'
    })
    // Force correction
    config.baseURL = '/api'
    config.url = normalizeUrl(url)
  }
  
  // Ensure baseURL is relative
  if (config.baseURL && (config.baseURL.startsWith('http://') || config.baseURL.startsWith('https://'))) {
    console.error('[API] RUNTIME VALIDATION: Absolute baseURL detected, forcing to /api:', config.baseURL)
    config.baseURL = '/api'
  }
}

// Request interceptor to add auth token and ensure relative URLs
api.interceptors.request.use(
  (config) => {
    // CRITICAL: In browser context, ALWAYS use relative URLs
    // This prevents Docker hostname resolution issues and ensures proper proxy routing
    if (typeof window !== 'undefined') {
      const originalBaseURL = config.baseURL
      const originalUrl = config.url
      
      // FORCE baseURL to '/api' in browser - never allow absolute URLs
      config.baseURL = '/api'
      
      // Validate and fix URLs to prevent Docker hostnames and absolute URLs
      const { url: normalizedUrl, baseURL: normalizedBaseURL } = validateAndFixUrl(config.url, config.baseURL)
      
      // Apply normalized URLs (baseURL should already be '/api' but ensure it)
      config.baseURL = '/api' // Force again after validation
      config.url = normalizedUrl
      
      // Additional safety check: if axios somehow constructed an absolute URL, fix it
      // This can happen if axios combines baseURL and url into an absolute URL
      if (config.url && (config.url.startsWith('http://') || config.url.startsWith('https://'))) {
        console.warn('[API] Interceptor caught absolute URL, normalizing:', {
          original: config.url,
          normalized: normalizeUrl(config.url)
        })
        config.url = normalizeUrl(config.url)
      }
      
      // CRITICAL: Check if baseURL contains any absolute URL patterns
      if (config.baseURL && (config.baseURL.startsWith('http://') || config.baseURL.startsWith('https://'))) {
        console.error('[API] CRITICAL: Absolute baseURL detected, forcing to /api:', config.baseURL)
        config.baseURL = '/api'
      }
      
      // Final validation: ensure the constructed URL doesn't contain Docker hostnames
      const constructedUrl = (config.baseURL || '') + (config.url || '')
      if (constructedUrl.includes('api:8000') || constructedUrl.match(/https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
        console.error('[API] CRITICAL: Docker hostname detected in constructed URL!', {
          constructedUrl,
          baseURL: config.baseURL,
          url: config.url,
          originalBaseURL,
          originalUrl,
          action: 'Forcing relative paths'
        })
        config.baseURL = '/api'
        config.url = normalizeUrl(config.url)
      }
      
      // Final safety check: ensure baseURL is exactly '/api'
      if (config.baseURL !== '/api') {
        console.error('[API] CRITICAL: baseURL is not /api, forcing correction:', config.baseURL, '-> /api')
        config.baseURL = '/api'
      }
      
      // CRITICAL: Final check for any Docker hostnames in the constructed URL
      const finalUrlCheck = (config.baseURL || '') + (config.url || '')
      if (finalUrlCheck.includes('api:8000') || finalUrlCheck.match(/^https?:\/\/[a-zA-Z0-9_-]+:\d+/)) {
        console.error('[API] CRITICAL: Docker hostname detected in final URL, forcing correction:', finalUrlCheck)
        config.baseURL = '/api'
        // Extract path from the URL if it contains Docker hostname
        if (config.url && config.url.includes('api:8000')) {
          const pathMatch = config.url.match(/api:8000(\/.*)$/)
          if (pathMatch) {
            config.url = pathMatch[1]
          } else {
            config.url = config.url.replace(/.*api:8000/, '')
          }
        }
      }
      
      // RUNTIME VALIDATION: Final check before request is sent
      validateRequestUrl(config)
      
      // Development mode logging
      if (process.env.NODE_ENV === 'development' && (originalBaseURL !== config.baseURL || originalUrl !== config.url)) {
        console.log('[API] URL normalization applied:', {
          original: { baseURL: originalBaseURL, url: originalUrl },
          normalized: { baseURL: config.baseURL, url: config.url }
        })
      }
    }
    
    // Add authentication token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // Log requests for debugging (final URL after normalization)
    const finalUrl = (config.baseURL || '') + (config.url || '')
    console.log('[API] Request:', config.method?.toUpperCase(), finalUrl, {
      hasToken: !!token,
      timeout: config.timeout,
      params: config.params
    })
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => {
    // Log successful responses in dev mode for debugging
    if (process.env.NODE_ENV === 'development') {
      console.log('[API] Success:', response.config.method?.toUpperCase(), response.config.url, response.status)
    }
    return response
  },
  (error) => {
    // Enhanced error logging with Docker hostname detection
    const url = error.config?.baseURL + (error.config?.url || '')
    const errorUrl = error.request?.responseURL || error.config?.url || url
    const hasDockerHostname = /(?:https?:\/\/)?api:\d+/.test(errorUrl) || /(?:https?:\/\/)?api:\d+/.test(url)
    
    // RUNTIME VALIDATION: If we detect a Docker hostname in the error, it means
    // the request was made with an absolute URL - this should never happen
    if (hasDockerHostname && typeof window !== 'undefined') {
      console.error('[API] RUNTIME VALIDATION FAILED: Request was made with Docker hostname!', {
        errorUrl,
        configUrl: url,
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        message: 'This indicates the interceptor failed to catch an absolute URL. The request should have been blocked.',
        recommendation: 'Check axios configuration and ensure baseURL is always /api in browser context'
      })
    }
    
    // CRITICAL: Detect Docker hostname in error URLs
    if (hasDockerHostname) {
      console.error('[API] CRITICAL ERROR: Docker hostname detected in failed request!', {
        errorUrl,
        configUrl: url,
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        errorCode: error.code,
        errorMessage: error.message,
        diagnosis: 'The browser tried to connect to "api:8000" which is a Docker internal hostname. Browsers cannot resolve Docker hostnames. This indicates a configuration error where absolute URLs with Docker hostnames are being constructed.',
        solution: 'All API requests must use relative URLs (/api/...) that work with the proxy (Vite dev server or Traefik). Check that baseURL is always "/api" in browser context.',
        currentLocation: typeof window !== 'undefined' ? window.location.href : 'N/A'
      })
    }
    
    if (error.code === 'ECONNABORTED') {
      console.error('[API] Request timeout:', error.config?.method?.toUpperCase(), url, 'after', error.config?.timeout, 'ms')
      console.error('[API] Timeout diagnostic:', {
        fullUrl: url,
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        timeout: error.config?.timeout,
        currentLocation: typeof window !== 'undefined' ? window.location.href : 'N/A',
        hasDockerHostname,
        message: hasDockerHostname 
          ? 'CRITICAL: Request contains Docker hostname. This will always fail in browser. Use relative URLs.'
          : 'Backend may not be running or not reachable. Check backend container status and network connectivity.'
      })
    } else if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
      // Enhanced diagnostics for network errors
      const diagnostic: any = {
        fullUrl: url,
        errorUrl,
        errorCode: error.code,
        errorMessage: error.message,
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        requestMade: !!error.request,
        responseReceived: !!error.response,
        currentLocation: typeof window !== 'undefined' ? window.location.href : 'N/A',
        hasDockerHostname
      }
      
      if (hasDockerHostname) {
        diagnostic.criticalIssue = 'Docker hostname detected in URL'
        diagnostic.solution = 'The URL contains "api:8000" which browsers cannot resolve. Ensure all API calls use relative URLs (/api/...).'
        diagnostic.checklist = [
          'Verify baseURL is "/api" (relative) not "https://api:8000/api"',
          'Check that request interceptor is normalizing URLs correctly',
          'Ensure no code is setting absolute URLs with Docker hostnames',
          'In development: Vite proxy should handle /api -> http://api:8000',
          'In production: Traefik should route /api -> backend service'
        ]
      } else {
        diagnostic.message = 'Request was sent but no response received. Backend may be down or unreachable.'
      }
      
      console.error('[API] No response received:', error.config?.method?.toUpperCase(), url, error.message)
      console.error('[API] Network error diagnostic:', diagnostic)
    } else if (error.response) {
      console.error('[API] Error response:', error.config?.method?.toUpperCase(), url, error.response.status)
      console.error('[API] Error response data:', error.response.data)
      // Log validation errors in a more readable format
      if (error.response.data?.detail) {
        if (Array.isArray(error.response.data.detail)) {
          const validationErrors = error.response.data.detail.map((err: any) => ({
            field: err.loc?.join('.') || 'unknown',
            message: err.msg || err.message || 'Validation error',
            type: err.type || 'unknown',
            input: err.input
          }))
          console.error('[API] Validation errors:', validationErrors)
          // Also log the raw error for debugging - expand the object
          console.error('[API] Raw validation errors:', JSON.stringify(error.response.data.detail, null, 2))
          // Log each error individually for better visibility
          error.response.data.detail.forEach((err: any, index: number) => {
            console.error(`[API] Validation error ${index + 1}:`, {
              location: err.loc,
              message: err.msg,
              type: err.type,
              input: err.input,
              ctx: err.ctx,
              fullError: JSON.stringify(err, null, 2)
            })
          })
        } else {
          console.error('[API] Error detail:', error.response.data.detail)
        }
      } else {
        // Log full response if no detail field
        console.error('[API] Full error response:', JSON.stringify(error.response.data, null, 2))
      }
    } else {
      console.error('[API] Request setup error:', error.message, {
        url,
        hasDockerHostname,
        baseURL: error.config?.baseURL,
        configUrl: error.config?.url
      })
    }
    
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Health check - test if API is reachable
export const checkApiHealth = async () => {
  try {
    // Health endpoint is at /api/health (full path)
    const response = await api.get('/health', {
      timeout: 5000,
      // Don't require auth for health check
    })
    return response.data
  } catch (error: any) {
    // Enhanced error logging for health check
    if (error.code === 'ECONNABORTED') {
      console.error('[API] Health check timeout - backend not responding')
    } else if (error.request && !error.response) {
      console.error('[API] Health check - no response from backend. Backend may be down.')
    } else {
      console.error('[API] Health check failed:', error.response?.status, error.message)
    }
    throw error
  }
}

// Sync API functions
export const syncTracks = async (limit = 1000, offset = 0) => {
  const response = await api.post('/sync/tracks', null, {
    params: { limit, offset },
    timeout: 30000, // 30 second timeout for sync (can take longer)
  })
  return response.data
}

export const getTracksCount = async (track_type?: string) => {
  const response = await api.get('/tracks/count', {
    params: track_type ? { track_type } : {},
    timeout: 5000, // 5 second timeout
  })
  return response.data
}

// Tracks API functions
export const getTrack = async (track_id: number) => {
  const response = await api.get(`/tracks/${track_id}`)
  return response.data
}

export const updateTrack = async (track_id: number, updates: {
  title?: string
  artist?: string
  genre?: string
  type?: string
  duration?: number
}) => {
  const response = await api.put(`/tracks/${track_id}`, updates)
  return response.data
}

export const syncPlaybackHistory = async (startDate: string, endDate: string) => {
  const response = await api.post('/sync/playback-history', {
    start_date: startDate,
    end_date: endDate
  })
  return response.data
}

export const getSyncStatus = async () => {
  const response = await api.get('/sync/status')
  return response.data
}

// Activity API functions
export const getRecentActivity = async (limit = 20) => {
  try {
    const response = await api.get('/activity/recent', {
      params: { limit },
      timeout: 3000, // 3 second timeout - fail fast
    })
    return response.data
  } catch (error: any) {
    // If endpoint doesn't exist or fails, return empty activities silently
    // Don't log to console as this is expected behavior when endpoint is unavailable
    return { activities: [] }
  }
}

// Dashboard stats - get actual counts from count endpoints
export const getDashboardStats = async () => {
  const [tracksRes, campaignsRes, clocksRes, logsRes] = await Promise.allSettled([
    api.get('/tracks/count', { timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/campaigns/count', { params: { active_only: true }, timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/clocks/count', { timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/logs/count', { params: { published_only: false }, timeout: 5000 }).catch(() => ({ data: { count: 0 } }))
  ])
  
  // Extract counts from responses
  const tracksCount = tracksRes.status === 'fulfilled' ? (tracksRes.value.data?.count || 0) : 0
  const campaignsCount = campaignsRes.status === 'fulfilled' ? (campaignsRes.value.data?.count || 0) : 0
  const clocksCount = clocksRes.status === 'fulfilled' ? (clocksRes.value.data?.count || 0) : 0
  const logsCount = logsRes.status === 'fulfilled' ? (logsRes.value.data?.count || 0) : 0
  
  // Format numbers with commas
  const formatNumber = (num: number) => {
    return num.toLocaleString('en-US')
  }
  
  return {
    totalTracks: formatNumber(tracksCount),
    activeCampaigns: formatNumber(campaignsCount),
    clockTemplates: formatNumber(clocksCount),
    reportsGenerated: formatNumber(logsCount)
  }
}

// Spots API functions
export const getSpots = async (params?: {
  skip?: number
  limit?: number
  order_id?: number
  scheduled_date?: string
  status_filter?: string
}) => {
  const response = await api.get('/spots', { params })
  return response.data
}

export const createSpot = async (spot: {
  order_id: number
  campaign_id?: number
  scheduled_date: string
  scheduled_time: string
  spot_length: number
  break_position?: string
  daypart?: string
  status?: string
}) => {
  const response = await api.post('/spots', spot)
  return response.data
}

export const createSpotsBulk = async (order_id: number, spots: any[]) => {
  const response = await api.post('/spots/bulk', spots, {
    params: { order_id }
  })
  return response.data
}

export const updateSpot = async (spot_id: number, updates: {
  scheduled_date?: string
  scheduled_time?: string
  spot_length?: number
  break_position?: string
  daypart?: string
  status?: string
  conflict_resolved?: boolean
}) => {
  const response = await api.put(`/spots/${spot_id}`, updates)
  return response.data
}

export const deleteSpot = async (spot_id: number) => {
  await api.delete(`/spots/${spot_id}`)
}

export const resolveSpotConflict = async (spot_id: number) => {
  const response = await api.post(`/spots/${spot_id}/resolve-conflict`)
  return response.data
}

// Logs API functions
export const getLogs = async (params?: {
  skip?: number
  limit?: number
  published_only?: boolean
  date_filter?: string
}) => {
  const response = await api.get('/logs', { params })
  return response.data
}

export const getLog = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}`)
  return response.data
}

export const getLogTimeline = async (log_id: number, hour?: string) => {
  const response = await api.get(`/logs/${log_id}/timeline`, {
    params: { hour }
  })
  return response.data
}

export const lockLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/lock`)
  return response.data
}

export const unlockLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/unlock`)
  return response.data
}

export const getLogConflicts = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}/conflicts`)
  return response.data
}

export const getLogAvails = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}/avails`)
  return response.data
}

export const addSpotToLog = async (log_id: number, spot: any) => {
  const response = await api.post(`/logs/${log_id}/spots`, spot)
  return response.data
}

export const updateSpotInLog = async (log_id: number, spot_id: number, updates: any) => {
  const response = await api.put(`/logs/${log_id}/spots/${spot_id}`, updates)
  return response.data
}

export const removeSpotFromLog = async (log_id: number, spot_id: number) => {
  await api.delete(`/logs/${log_id}/spots/${spot_id}`)
}

export const generateLog = async (target_date: string, clock_template_id: number) => {
  const response = await api.post('/logs/generate', {
    target_date,
    clock_template_id,
  })
  return response.data
}

export const previewLog = async (target_date: string, clock_template_id: number, preview_hours?: string[]) => {
  const response = await api.post('/logs/preview', {
    target_date,
    clock_template_id,
    preview_hours,
  })
  return response.data
}

export const publishLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/publish`)
  return response.data
}

// Clock Templates API functions
export const getClocks = async (params?: {
  skip?: number
  limit?: number
  search?: string
}) => {
  const response = await api.get('/clocks', { params })
  return response.data
}

export const getClock = async (clock_id: number) => {
  const response = await api.get(`/clocks/${clock_id}`)
  return response.data
}

// Dayparts API functions
export const getDayparts = async () => {
  const response = await api.get('/dayparts')
  return response.data
}

export const createDaypart = async (daypart: any) => {
  const response = await api.post('/dayparts', daypart)
  return response.data
}

export const updateDaypart = async (daypart_id: number, updates: any) => {
  const response = await api.put(`/dayparts/${daypart_id}`, updates)
  return response.data
}

export const deleteDaypart = async (daypart_id: number) => {
  await api.delete(`/dayparts/${daypart_id}`)
}

// Advertisers API functions
export const getAdvertisers = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/advertisers', { params })
  return response.data
}

export const getAdvertiser = async (advertiser_id: number) => {
  const response = await api.get(`/advertisers/${advertiser_id}`)
  return response.data
}

export const createAdvertiser = async (advertiser: any) => {
  const response = await api.post('/advertisers', advertiser)
  return response.data
}

export const updateAdvertiser = async (advertiser_id: number, updates: any) => {
  const response = await api.put(`/advertisers/${advertiser_id}`, updates)
  return response.data
}

export const deleteAdvertiser = async (advertiser_id: number) => {
  await api.delete(`/advertisers/${advertiser_id}`)
}

// Agencies API functions
export const getAgencies = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/agencies', { params })
  return response.data
}

export const getAgency = async (agency_id: number) => {
  const response = await api.get(`/agencies/${agency_id}`)
  return response.data
}

export const createAgency = async (agency: any) => {
  const response = await api.post('/agencies', agency)
  return response.data
}

export const updateAgency = async (agency_id: number, updates: any) => {
  const response = await api.put(`/agencies/${agency_id}`, updates)
  return response.data
}

export const deleteAgency = async (agency_id: number) => {
  await api.delete(`/agencies/${agency_id}`)
}

// Campaigns API functions
export const getCampaigns = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  date_filter?: string
}) => {
  const response = await api.get('/campaigns', { params })
  return response.data
}

export const getCampaign = async (campaign_id: number) => {
  const response = await api.get(`/campaigns/${campaign_id}`)
  return response.data
}

export const createCampaign = async (campaign: any) => {
  const response = await api.post('/campaigns', campaign)
  return response.data
}

export const updateCampaign = async (campaign_id: number, updates: any) => {
  const response = await api.put(`/campaigns/${campaign_id}`, updates)
  return response.data
}

export const deleteCampaign = async (campaign_id: number) => {
  await api.delete(`/campaigns/${campaign_id}`)
}

// Orders API functions (for spot scheduling)
export const getOrders = async (params?: {
  skip?: number
  limit?: number
  status?: string
  advertiser_id?: number
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/orders', { params })
  return response.data
}

export const getOrder = async (order_id: number) => {
  const response = await api.get(`/orders/${order_id}`)
  return response.data
}

export const createOrder = async (order: any) => {
  const response = await api.post('/orders', order)
  return response.data
}

export const updateOrder = async (order_id: number, updates: any) => {
  const response = await api.put(`/orders/${order_id}`, updates)
  return response.data
}

export const deleteOrder = async (order_id: number) => {
  await api.delete(`/orders/${order_id}`)
}

// Copy API functions
export const getCopy = async (params?: {
  skip?: number
  limit?: number
  order_id?: number
  advertiser_id?: number
  search?: string
}) => {
  const response = await api.get('/copy', { params })
  return response.data
}

export const getCopyById = async (copy_id: number) => {
  const response = await api.get(`/copy/${copy_id}`)
  return response.data
}

export const createCopy = async (copy: {
  order_id?: number
  advertiser_id?: number
  title: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  expires_at?: string
}) => {
  const response = await api.post('/copy', copy)
  return response.data
}

export const uploadCopy = async (
  file: File,
  title: string,
  order_id?: number,
  advertiser_id?: number,
  onUploadProgress?: (progress: number) => void
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  if (order_id) formData.append('order_id', order_id.toString())
  if (advertiser_id) formData.append('advertiser_id', advertiser_id.toString())

  const response = await api.post('/copy/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onUploadProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onUploadProgress(progress)
      }
    },
  })
  return response.data
}

export const updateCopy = async (copy_id: number, updates: {
  title?: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  expires_at?: string
  active?: boolean
}) => {
  const response = await api.put(`/copy/${copy_id}`, updates)
  return response.data
}

export const deleteCopy = async (copy_id: number) => {
  await api.delete(`/copy/${copy_id}`)
}

export const getExpiringCopy = async (days_ahead: number = 30) => {
  const response = await api.get('/copy/expiring', {
    params: { days_ahead: days_ahead }
  })
  return response.data
}

export const assignCopyToSpot = async (copy_id: number, spot_id: number) => {
  const response = await api.post(`/copy/${copy_id}/assign`, null, {
    params: { spot_id }
  })
  return response.data
}

// Copy Assignments API functions
export const getCopyAssignments = async (params?: {
  skip?: number
  limit?: number
  spot_id?: number
  copy_id?: number
}) => {
  const response = await api.get('/copy-assignments', { params })
  return response.data
}

export const createCopyAssignment = async (assignment: {
  spot_id: number
  copy_id: number
  order_id?: number
}) => {
  const response = await api.post('/copy-assignments', null, {
    params: assignment
  })
  return response.data
}

export const deleteCopyAssignment = async (assignment_id: number) => {
  await api.delete(`/copy-assignments/${assignment_id}`)
}

// Invoices API functions
export const getInvoices = async (params?: {
  skip?: number
  limit?: number
  advertiser_id?: number
  status_filter?: string
}) => {
  const response = await api.get('/invoices', { params })
  return response.data
}

export const getInvoice = async (invoice_id: number) => {
  const response = await api.get(`/invoices/${invoice_id}`)
  return response.data
}

export const createInvoice = async (invoice: {
  invoice_number: string
  advertiser_id: number
  agency_id?: number
  order_id?: number
  campaign_id?: number
  invoice_date: string
  due_date: string
  payment_terms?: string
  notes?: string
  lines?: Array<{
    description: string
    quantity?: number
    unit_price: number | string
    spot_ids?: number[]
  }>
}) => {
  const response = await api.post('/invoices', invoice)
  return response.data
}

export const updateInvoice = async (invoice_id: number, updates: {
  invoice_number?: string
  invoice_date?: string
  due_date?: string
  status?: string
  payment_terms?: string
  notes?: string
}) => {
  const response = await api.put(`/invoices/${invoice_id}`, updates)
  return response.data
}

export const sendInvoice = async (invoice_id: number) => {
  const response = await api.post(`/invoices/${invoice_id}/send`)
  return response.data
}

export const markInvoicePaid = async (invoice_id: number, payment_data: {
  amount?: number | string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.post(`/invoices/${invoice_id}/mark-paid`, payment_data)
  return response.data
}

export const getAgingReport = async () => {
  const response = await api.get('/invoices/aging')
  return response.data
}

// Payments API functions
export const getPayments = async (params?: {
  skip?: number
  limit?: number
  invoice_id?: number
}) => {
  const response = await api.get('/payments', { params })
  return response.data
}

export const getPayment = async (payment_id: number) => {
  const response = await api.get(`/payments/${payment_id}`)
  return response.data
}

export const createPayment = async (payment: {
  invoice_id: number
  amount: number | string
  payment_date: string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.post('/payments', payment)
  return response.data
}

export const updatePayment = async (payment_id: number, updates: {
  amount?: number | string
  payment_date?: string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.put(`/payments/${payment_id}`, updates)
  return response.data
}

export const deletePayment = async (payment_id: number) => {
  await api.delete(`/payments/${payment_id}`)
}

// Makegoods API functions
export const getMakegoods = async (params?: {
  skip?: number
  limit?: number
  campaign_id?: number
}) => {
  const response = await api.get('/makegoods', { params })
  return response.data
}

export const createMakegood = async (makegood: {
  original_spot_id: number
  makegood_spot_id: number
  campaign_id?: number
  reason?: string
}) => {
  const response = await api.post('/makegoods', makegood)
  return response.data
}

export const approveMakegood = async (makegood_id: number) => {
  const response = await api.post(`/makegoods/${makegood_id}/approve`)
  return response.data
}

// Reports API functions
export const getReconciliationReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/reconciliation', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getComplianceReport = async (start_date: string, end_date: string, format: string = 'csv') => {
  const response = await api.get('/reports/compliance', {
    params: { start_date, end_date, format }
  })
  return response.data
}

export const getPlaybackHistory = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/playback', {
    params: { start_date, end_date }
  })
  return response.data
}

// Traffic Reports
export const getDailyLogReport = async (log_date: string) => {
  const response = await api.get('/reports/traffic/daily-log', {
    params: { log_date }
  })
  return response.data
}

export const getMissingCopyReport = async (start_date?: string, end_date?: string) => {
  const response = await api.get('/reports/traffic/missing-copy', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getAvailsReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/traffic/avails', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getConflictsReport = async (log_date: string) => {
  const response = await api.get('/reports/traffic/conflicts', {
    params: { log_date }
  })
  return response.data
}

export const getExpirationsReport = async (days_ahead: number = 30) => {
  const response = await api.get('/reports/traffic/expirations', {
    params: { days_ahead }
  })
  return response.data
}

// Billing Reports
export const getContractActualizationReport = async (
  order_id: number,
  start_date?: string,
  end_date?: string
) => {
  const response = await api.get('/reports/billing/contract-actualization', {
    params: { order_id, start_date, end_date }
  })
  return response.data
}

export const getRevenueSummaryReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/billing/revenue-summary', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getARAgingReport = async () => {
  const response = await api.get('/reports/billing/ar-aging')
  return response.data
}

export const getMakegoodsReport = async (start_date?: string, end_date?: string) => {
  const response = await api.get('/reports/billing/makegoods', {
    params: { start_date, end_date }
  })
  return response.data
}

// Sales Reports
export const getRevenueByRepReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/sales/revenue-by-rep', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenueByAdvertiserReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/sales/revenue-by-advertiser', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getPendingOrdersReport = async () => {
  const response = await api.get('/reports/sales/pending-orders')
  return response.data
}

export const getExpiringContractsReport = async (days_ahead: number = 30) => {
  const response = await api.get('/reports/sales/expiring-contracts', {
    params: { days_ahead }
  })
  return response.data
}

// Export Report
export const exportReport = async (
  report_type: string,
  format: 'pdf' | 'excel' | 'csv',
  report_params?: Record<string, any>
) => {
  const response = await api.post('/reports/export', report_params || {}, {
    params: { report_type, format },
    responseType: format === 'pdf' ? 'blob' : 'blob'
  })
  return response.data
}

// Audit Logs API functions
export const getAuditLogs = async (params?: {
  skip?: number
  limit?: number
  user_id?: number
  action?: string
  resource_type?: string
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/admin/audit-logs', { params })
  return response.data
}

export const getAuditLog = async (audit_log_id: number) => {
  const response = await api.get(`/admin/audit-logs/${audit_log_id}`)
  return response.data
}

// Log Revisions API functions
export const getLogRevisions = async (log_id: number) => {
  const response = await api.get(`/log-revisions/logs/${log_id}/revisions`)
  return response.data
}

export const getLogRevision = async (log_id: number, revision_id: number) => {
  const response = await api.get(`/log-revisions/logs/${log_id}/revisions/${revision_id}`)
  return response.data
}

export const revertToRevision = async (log_id: number, revision_id: number) => {
  const response = await api.post(`/log-revisions/logs/${log_id}/revert`, null, {
    params: { revision_id }
  })
  return response.data
}

// Inventory API functions
export const getInventory = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getInventoryHeatmap = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory/heatmap', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getSelloutPercentages = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory/sellout', {
    params: { start_date, end_date }
  })
  return response.data
}

// Revenue API functions
export const getRevenueSummary = async (start_date: string, end_date: string) => {
  const response = await api.get('/revenue/summary', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenuePacing = async (start_date: string, end_date: string) => {
  const response = await api.get('/revenue/pacing', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenueForecast = async (months_ahead: number = 3) => {
  const response = await api.get('/revenue/forecast', {
    params: { months_ahead }
  })
  return response.data
}

// Sales Reps API functions
export const getSalesReps = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/sales-reps', { params })
  return response.data
}

// Sales Goals API functions
export const getSalesGoals = async (params?: {
  skip?: number
  limit?: number
  sales_rep_id?: number
}) => {
  const response = await api.get('/sales-goals', { params })
  return response.data
}

export const createSalesGoal = async (goal: {
  sales_rep_id: number
  period: string
  target_date: string
  goal_amount: number | string
}) => {
  const response = await api.post('/sales-goals', goal)
  return response.data
}

export const getSalesGoalsProgress = async (sales_rep_id?: number) => {
  const response = await api.get('/sales-goals/progress', {
    params: sales_rep_id ? { sales_rep_id } : {}
  })
  return response.data
}

// User management API functions
export const getUsers = async (params?: { limit?: number; skip?: number; role_filter?: string }) => {
  const response = await api.get('/users', { params: params || {} })
  return response.data
}

export const getUser = async (userId: number) => {
  const response = await api.get(`/users/${userId}`)
  return response.data
}

export const createUser = async (userData: { username: string; password: string; role: string }) => {
  const response = await api.post('/users', userData)
  return response.data
}

export const updateUser = async (userId: number, userData: { username?: string; password?: string; role?: string }) => {
  const response = await api.put(`/users/${userId}`, userData)
  return response.data
}

export const deleteUser = async (userId: number) => {
  await api.delete(`/users/${userId}`)
}

// Webhooks API functions
export const getWebhooks = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/webhooks', { params })
  return response.data
}

export const getWebhook = async (webhook_id: number) => {
  const response = await api.get(`/webhooks/${webhook_id}`)
  return response.data
}

export const createWebhook = async (webhook: {
  name: string
  webhook_type: string
  url: string
  events: string[]
  secret?: string
  headers?: Record<string, string>
}) => {
  const response = await api.post('/webhooks', webhook)
  return response.data
}

export const updateWebhook = async (webhook_id: number, updates: {
  name?: string
  url?: string
  events?: string[]
  secret?: string
  active?: boolean
  headers?: Record<string, string>
}) => {
  const response = await api.put(`/webhooks/${webhook_id}`, updates)
  return response.data
}

export const deleteWebhook = async (webhook_id: number) => {
  await api.delete(`/webhooks/${webhook_id}`)
}

export const testWebhook = async (webhook_id: number) => {
  const response = await api.post(`/webhooks/${webhook_id}/test`)
  return response.data
}

// Notifications API functions
export const getNotifications = async (params?: {
  unread_only?: boolean
  skip?: number
  limit?: number
}) => {
  const response = await api.get('/notifications', { params })
  return response.data
}

export const getUnreadCount = async () => {
  const response = await api.get('/notifications/unread-count')
  return response.data
}

export const markNotificationRead = async (notification_id: number) => {
  const response = await api.post(`/notifications/${notification_id}/read`)
  return response.data
}

export const markAllNotificationsRead = async () => {
  const response = await api.post('/notifications/mark-all-read')
  return response.data
}

// Settings API functions
export const getAllSettings = async () => {
  const response = await api.get('/settings')
  return response.data
}

export const getCategorySettings = async (category: string) => {
  const response = await api.get(`/settings/${category}`)
  return response.data
}

export const updateCategorySettings = async (category: string, settings: Record<string, { value: string; encrypted?: boolean; description?: string }>) => {
  const response = await api.put(`/settings/${category}`, { settings })
  return response.data
}

export const testSMTPConnection = async (config: {
  host: string
  port: number
  username: string
  password: string
  use_tls?: boolean
}) => {
  const response = await api.post('/settings/test-smtp', config)
  return response.data
}

export const testS3Connection = async (config: {
  access_key_id: string
  secret_access_key: string
  bucket_name: string
  region?: string
}) => {
  const response = await api.post('/settings/test-s3', config)
  return response.data
}

export const testBackblazeConnection = async (config: {
  application_key_id: string
  application_key: string
  bucket_name: string
}) => {
  const response = await api.post('/settings/test-backblaze', config)
  return response.data
}

// Backups API functions
export const getBackups = async (params?: {
  skip?: number
  limit?: number
  status_filter?: string
}) => {
  const response = await api.get('/backups', { params })
  return response.data
}

export const getBackup = async (backup_id: number) => {
  const response = await api.get(`/backups/${backup_id}`)
  return response.data
}

export const createBackup = async (backup: {
  backup_type: 'FULL' | 'DATABASE' | 'FILES'
  storage_provider: 'LOCAL' | 'S3' | 'BACKBLAZE_B2'
  description?: string
}) => {
  const response = await api.post('/backups', backup)
  return response.data
}

export const restoreBackup = async (backup_id: number, restore: {
  restore_database?: boolean
  restore_files?: boolean
}) => {
  const response = await api.post(`/backups/${backup_id}/restore`, restore)
  return response.data
}

export const deleteBackup = async (backup_id: number) => {
  await api.delete(`/backups/${backup_id}`)
}

// Daypart Categories API functions
export const getDaypartCategories = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/daypart-categories', { params })
  return response.data
}

export const getDaypartCategory = async (category_id: number) => {
  const response = await api.get(`/daypart-categories/${category_id}`)
  return response.data
}

export const createDaypartCategory = async (category: {
  name: string
  description?: string
  color?: string
  icon?: string
  sort_order?: number
}) => {
  const response = await api.post('/daypart-categories', category)
  return response.data
}

export const updateDaypartCategory = async (category_id: number, updates: {
  name?: string
  description?: string
  color?: string
  icon?: string
  sort_order?: number
  active?: boolean
}) => {
  const response = await api.put(`/daypart-categories/${category_id}`, updates)
  return response.data
}

export const deleteDaypartCategory = async (category_id: number) => {
  await api.delete(`/daypart-categories/${category_id}`)
}

// Rotation Rules API functions
export const getRotationRules = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  daypart_id?: number
  campaign_id?: number
}) => {
  const response = await api.get('/rotation-rules', { params })
  return response.data
}

export const getRotationRule = async (rule_id: number) => {
  const response = await api.get(`/rotation-rules/${rule_id}`)
  return response.data
}

export const createRotationRule = async (rule: {
  name: string
  description?: string
  rotation_type?: string
  daypart_id?: number
  campaign_id?: number
  min_separation?: number
  max_per_hour?: number
  max_per_day?: number
  weights?: Record<string, any>
  exclude_days?: number[]
  exclude_times?: Array<{ start: string; end: string }>
  priority?: number
}) => {
  const response = await api.post('/rotation-rules', rule)
  return response.data
}

export const updateRotationRule = async (rule_id: number, updates: {
  name?: string
  description?: string
  rotation_type?: string
  daypart_id?: number
  campaign_id?: number
  min_separation?: number
  max_per_hour?: number
  max_per_day?: number
  weights?: Record<string, any>
  exclude_days?: number[]
  exclude_times?: Array<{ start: string; end: string }>
  priority?: number
  active?: boolean
}) => {
  const response = await api.put(`/rotation-rules/${rule_id}`, updates)
  return response.data
}

export const deleteRotationRule = async (rule_id: number) => {
  await api.delete(`/rotation-rules/${rule_id}`)
}

// Traffic Logs API functions
export const getTrafficLogs = async (params?: {
  skip?: number
  limit?: number
  log_type?: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  user_id?: number
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/traffic-logs', { params })
  return response.data
}

export const getTrafficLog = async (log_id: number) => {
  const response = await api.get(`/traffic-logs/${log_id}`)
  return response.data
}

export const createTrafficLog = async (log: {
  log_type: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  copy_id?: number
  message: string
  metadata?: Record<string, any>
}) => {
  const response = await api.post('/traffic-logs', log)
  return response.data
}

export const createTrafficLogsBulk = async (logs: Array<{
  log_type: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  copy_id?: number
  message: string
  metadata?: Record<string, any>
}>) => {
  const response = await api.post('/traffic-logs/bulk', { logs })
  return response.data
}

export const getTrafficLogStats = async (params?: {
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/traffic-logs/stats/summary', { params })
  return response.data
}

// Proxy API functions - Server-side aggregation endpoints
// These endpoints aggregate data on the backend, so the browser doesn't make multiple API calls
export const getDashboardDataProxy = async () => {
  const response = await api.get('/proxy/dashboard')
  return response.data
}

export const getTracksAggregated = async (params?: {
  track_type?: string
  limit?: number
  skip?: number
  search?: string
}) => {
  const response = await api.get('/proxy/tracks/aggregated', { params })
  return response.data
}

export const proxyLibreTime = async (path: string) => {
  const response = await api.get(`/proxy/libretime/${path}`)
  return response.data
}

// Server-side proxy endpoints for all data fetching
export const getAdvertisersProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/proxy/advertisers', { params })
  return response.data
}

export const getAgenciesProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/proxy/agencies', { params })
  return response.data
}

export const getUsersProxy = async (params?: {
  skip?: number
  limit?: number
  role?: string
  search?: string
}) => {
  const response = await api.get('/proxy/users', { params })
  return response.data
}

export const getSalesRepsProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/proxy/sales-reps', { params })
  return response.data
}

export const getOrdersProxy = async (params?: {
  skip?: number
  limit?: number
  status?: string
  advertiser_id?: number
  search?: string
}) => {
  const response = await api.get('/proxy/orders', { params })
  return response.data
}

// Additional proxy endpoints for all remaining API calls
export const getDaypartsProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  category_id?: number
}) => {
  const response = await api.get('/proxy/dayparts', { params })
  return response.data
}

export const getDaypartCategoriesProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/proxy/daypart-categories', { params })
  return response.data
}

export const getRotationRulesProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/proxy/rotation-rules', { params })
  return response.data
}

export const getTrafficLogsProxy = async (params?: {
  skip?: number
  limit?: number
  log_type?: string
}) => {
  const response = await api.get('/proxy/traffic-logs', { params })
  return response.data
}

export const getCopyProxy = async (params?: {
  skip?: number
  limit?: number
  advertiser_id?: number
  order_id?: number
  search?: string
}) => {
  const response = await api.get('/proxy/copy', { params })
  return response.data
}

export const getCampaignsProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/proxy/campaigns', { params })
  return response.data
}

export const getInvoicesProxy = async (params?: {
  skip?: number
  limit?: number
  status?: string
  advertiser_id?: number
}) => {
  const response = await api.get('/proxy/invoices', { params })
  return response.data
}

export const getPaymentsProxy = async (params?: {
  skip?: number
  limit?: number
  invoice_id?: number
}) => {
  const response = await api.get('/proxy/payments', { params })
  return response.data
}

export const getMakegoodsProxy = async (params?: {
  skip?: number
  limit?: number
  campaign_id?: number
}) => {
  const response = await api.get('/proxy/makegoods', { params })
  return response.data
}

export const getInventoryProxy = async (params: {
  start_date: string
  end_date: string
}) => {
  const response = await api.get('/proxy/inventory', { params })
  return response.data
}

export const getSalesGoalsProxy = async (params?: {
  skip?: number
  limit?: number
  sales_rep_id?: number
}) => {
  const response = await api.get('/proxy/sales-goals', { params })
  return response.data
}

export const getSettingsProxy = async () => {
  const response = await api.get('/proxy/settings')
  return response.data
}

export const getAuditLogsProxy = async (params?: {
  skip?: number
  limit?: number
  user_id?: number
  action?: string
  resource_type?: string
}) => {
  const response = await api.get('/proxy/admin/audit-logs', { params })
  return response.data
}

export const getWebhooksProxy = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/proxy/webhooks', { params })
  return response.data
}

export const getNotificationsProxy = async (params?: {
  unread_only?: boolean
  skip?: number
  limit?: number
}) => {
  const response = await api.get('/proxy/notifications', { params })
  return response.data
}

export const getBackupsProxy = async (params?: {
  skip?: number
  limit?: number
}) => {
  const response = await api.get('/proxy/backups', { params })
  return response.data
}

// Production Orders API
export const getProductionOrders = async (params?: {
  status?: string
  advertiser_id?: number
  assigned_to?: number
  deadline_before?: string
  limit?: number
  offset?: number
}) => {
  const response = await api.get('/production-orders', { params })
  return response.data
}

export const getProductionOrder = async (po_id: number) => {
  const response = await api.get(`/production-orders/${po_id}`)
  return response.data
}

export const createProductionOrder = async (data: {
  copy_id: number
  client_name?: string
  deadline?: string
  instructions?: string
  talent_needs?: any
  audio_references?: string[]
  stations?: string[]
}) => {
  const response = await api.post('/production-orders', data)
  return response.data
}

export const updateProductionOrder = async (po_id: number, updates: any) => {
  const response = await api.put(`/production-orders/${po_id}`, updates)
  return response.data
}

export const assignProductionOrder = async (
  po_id: number,
  user_id: number,
  assignment_type: string,
  notes?: string
) => {
  const response = await api.post(`/production-orders/${po_id}/assign`, null, {
    params: { user_id, assignment_type, notes }
  })
  return response.data
}

export const updateProductionOrderStatus = async (po_id: number, new_status: string) => {
  const response = await api.post(`/production-orders/${po_id}/update-status`, null, {
    params: { new_status }
  })
  return response.data
}

export const deliverProductionOrder = async (
  po_id: number,
  delivery_method: string = 'local',
  target_server?: string,
  target_path?: string
) => {
  const response = await api.post(`/production-orders/${po_id}/deliver`, null, {
    params: { delivery_method, target_server, target_path }
  })
  return response.data
}

// Voice Talent API
export const getVoiceTalentRequests = async (status?: string) => {
  const response = await api.get('/voice-talent/requests', { params: { status } })
  return response.data
}

export const uploadTake = async (request_id: number, file: File, take_number?: number) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post(`/voice-talent/requests/${request_id}/upload-take`, formData, {
    params: { take_number },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export const approveTake = async (request_id: number, take_number: number) => {
  const response = await api.post(`/voice-talent/requests/${request_id}/approve-take`, null, {
    params: { take_number }
  })
  return response.data
}

// Production Assignments API
export const getProductionAssignments = async (params?: {
  production_order_id?: number
  user_id?: number
  assignment_type?: string
  status?: string
}) => {
  const response = await api.get('/production-assignments', { params })
  return response.data
}

export const createProductionAssignment = async (data: {
  production_order_id: number
  user_id: number
  assignment_type: string
  notes?: string
}) => {
  const response = await api.post('/production-assignments', data)
  return response.data
}

export const updateProductionAssignment = async (assignment_id: number, updates: {
  status?: string
  notes?: string
}) => {
  const response = await api.put(`/production-assignments/${assignment_id}`, updates)
  return response.data
}

// Copy Production API
export const setNeedsProduction = async (copy_id: number, needs_production: boolean) => {
  const response = await api.post(`/copy/${copy_id}/set-needs-production`, null, {
    params: { needs_production }
  })
  return response.data
}

export const approveCopy = async (copy_id: number, auto_create_po: boolean = true) => {
  const response = await api.post(`/copy/${copy_id}/approve`, null, {
    params: { auto_create_po }
  })
  return response.data
}

export const rejectCopy = async (copy_id: number, reason?: string) => {
  const response = await api.post(`/copy/${copy_id}/reject`, null, {
    params: { reason }
  })
  return response.data
}

export const getCopyProductionStatus = async (copy_id: number) => {
  const response = await api.get(`/copy/${copy_id}/production-status`)
  return response.data
}

// Production Archive API
export const searchProductionByClient = async (client_name: string, limit?: number) => {
  const response = await api.get('/production-archive/search/client', {
    params: { client_name, limit }
  })
  return response.data
}

export const searchProductionByScript = async (keyword: string, limit?: number) => {
  const response = await api.get('/production-archive/search/script', {
    params: { keyword, limit }
  })
  return response.data
}

export const searchProductionBySpotLength = async (spot_length: number, limit?: number) => {
  const response = await api.get('/production-archive/search/spot-length', {
    params: { spot_length, limit }
  })
  return response.data
}

export const getProductionHistory = async (params?: {
  client_name?: string
  start_date?: string
  end_date?: string
  status?: string
  limit?: number
}) => {
  const response = await api.get('/production-archive/history', { params })
  return response.data
}

// Production Reports API
export const getProductionTurnaroundReport = async (start_date?: string, end_date?: string) => {
  const response = await api.get('/reports/production/turnaround', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getProductionWorkloadReport = async (user_id?: number) => {
  const response = await api.get('/reports/production/workload', {
    params: { user_id }
  })
  return response.data
}

export const getMissedDeadlinesReport = async (days_overdue?: number) => {
  const response = await api.get('/reports/production/missed-deadlines', {
    params: { days_overdue }
  })
  return response.data
}

export default api
