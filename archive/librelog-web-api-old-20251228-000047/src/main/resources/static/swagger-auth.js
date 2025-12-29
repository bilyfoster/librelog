// Swagger UI authentication helper
(function() {
    // Get token from URL or localStorage
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    const tokenFromStorage = localStorage.getItem('accessToken') || sessionStorage.getItem('swagger_token');
    const token = tokenFromUrl || tokenFromStorage;

    if (token) {
        // Configure Swagger UI to use the token
        window.ui = window.ui || {};
        
        // Intercept fetch requests to add Authorization header
        const originalFetch = window.fetch;
        window.fetch = function(...args) {
            const [url, options = {}] = args;
            if (url && typeof url === 'string' && url.startsWith('/api/')) {
                options.headers = options.headers || {};
                options.headers['Authorization'] = 'Bearer ' + token;
            }
            return originalFetch.apply(this, args);
        };

        // Also configure XMLHttpRequest
        const originalOpen = XMLHttpRequest.prototype.open;
        const originalSend = XMLHttpRequest.prototype.send;
        
        XMLHttpRequest.prototype.open = function(method, url, ...rest) {
            this._url = url;
            return originalOpen.apply(this, [method, url, ...rest]);
        };
        
        XMLHttpRequest.prototype.send = function(...args) {
            if (this._url && this._url.startsWith('/api/')) {
                this.setRequestHeader('Authorization', 'Bearer ' + token);
            }
            return originalSend.apply(this, args);
        };
    }
})();

