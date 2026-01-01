/**
 * LibreTime Integration Settings JavaScript
 * Handles form interactions, API calls, and dynamic UI updates
 */

// API Base URL
const API_BASE = '/api/libretime';

// Global state
let currentConfig = null;
let authToken = null;
let userPermissions = [];
let selectedStationId = null;
let stations = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    init();
});

function init() {
    // Get auth token
    authToken = localStorage.getItem('authToken');
    if (!authToken) {
        showNotification('Please log in to access integration settings', 'error');
        // Redirect to login after a delay
        setTimeout(() => {
            window.location.href = '/index.html';
        }, 2000);
        return;
    }

    // Load user permissions
    loadUserPermissions().then(() => {
        // Setup event listeners
        setupEventListeners();
        
        // Apply permission-based UI visibility
        applyPermissions();
        
        // Load stations first, then load configuration when station is selected
        loadStations().then(() => {
            // Setup smooth scrolling for navigation
            setupNavigation();
        });
    });
}

function setupEventListeners() {
    // Station selector
    const stationSelector = document.getElementById('station-selector');
    if (stationSelector) {
        stationSelector.addEventListener('change', handleStationChange);
    }
    
    // Connection form
    const connectionForm = document.getElementById('connection-form');
    if (connectionForm) {
        connectionForm.addEventListener('submit', handleConnectionSubmit);
    }
    
    const testConnectionBtn = document.getElementById('test-connection-btn');
    if (testConnectionBtn) {
        testConnectionBtn.addEventListener('click', testConnection);
    }
    
    // Toggle JWT visibility
    const toggleJwtBtn = document.getElementById('toggle-jwt-visibility');
    if (toggleJwtBtn) {
        toggleJwtBtn.addEventListener('click', () => {
            togglePasswordVisibility('jwt-token', toggleJwtBtn);
        });
    }
    
    // Sync form
    const syncForm = document.getElementById('sync-form');
    if (syncForm) {
        syncForm.addEventListener('submit', handleSyncSubmit);
    }
    
    const syncNowBtn = document.getElementById('sync-now-btn');
    if (syncNowBtn) {
        syncNowBtn.addEventListener('click', triggerManualSync);
    }
    
    // File form
    const fileForm = document.getElementById('file-form');
    if (fileForm) {
        fileForm.addEventListener('submit', handleFileSubmit);
    }
    
    // Log form
    const logForm = document.getElementById('log-form');
    if (logForm) {
        logForm.addEventListener('submit', handleLogSubmit);
    }
    
    // Webhook form
    const webhookForm = document.getElementById('webhook-form');
    if (webhookForm) {
        webhookForm.addEventListener('submit', handleWebhookSubmit);
    }
    
    const toggleWebhookBtn = document.getElementById('toggle-webhook-visibility');
    if (toggleWebhookBtn) {
        toggleWebhookBtn.addEventListener('click', () => {
            togglePasswordVisibility('webhook-secret', toggleWebhookBtn);
        });
    }
    
    const testWebhookBtn = document.getElementById('test-webhook-btn');
    if (testWebhookBtn) {
        testWebhookBtn.addEventListener('click', testWebhook);
    }
    
    // API Testing
    const discoverEndpointsBtn = document.getElementById('discover-endpoints-btn');
    if (discoverEndpointsBtn) {
        discoverEndpointsBtn.addEventListener('click', discoverEndpoints);
    }
    
    const runTestsBtn = document.getElementById('run-tests-btn');
    if (runTestsBtn) {
        runTestsBtn.addEventListener('click', runAllTests);
    }
    
    const exportDocsBtn = document.getElementById('export-docs-btn');
    if (exportDocsBtn) {
        exportDocsBtn.addEventListener('click', exportDocumentation);
    }
    
    // Form validation
    setupFormValidation();
}

function setupNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = link.getAttribute('href').substring(1);
            const targetSection = document.getElementById(targetId);
            if (targetSection) {
                targetSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
                // Update active nav link
                navLinks.forEach(l => l.removeAttribute('aria-current'));
                link.setAttribute('aria-current', 'page');
            }
        });
    });
}

function setupFormValidation() {
    // URL validation
    const apiUrlInput = document.getElementById('api-base-url');
    if (apiUrlInput) {
        apiUrlInput.addEventListener('blur', () => {
            validateUrl(apiUrlInput);
        });
    }
    
    // File size validation
    const maxFileSizeInput = document.getElementById('max-file-size');
    if (maxFileSizeInput) {
        maxFileSizeInput.addEventListener('blur', () => {
            validateFileSize(maxFileSizeInput);
        });
    }
}

function validateUrl(input) {
    const url = input.value.trim();
    const errorElement = document.getElementById('api-url-error');
    
    if (!url) {
        setFieldError(input, errorElement, 'API Base URL is required');
        return false;
    }
    
    try {
        new URL(url);
        clearFieldError(input, errorElement);
        return true;
    } catch (e) {
        setFieldError(input, errorElement, 'Please enter a valid URL (must start with http:// or https://)');
        return false;
    }
}

function validateFileSize(input) {
    const value = parseInt(input.value);
    const errorElement = document.getElementById('max-file-size-error');
    
    if (isNaN(value) || value < 1 || value > 5000) {
        setFieldError(input, errorElement, 'File size must be between 1 and 5000 MB');
        return false;
    }
    
    clearFieldError(input, errorElement);
    return true;
}

function setFieldError(input, errorElement, message) {
    input.setAttribute('aria-invalid', 'true');
    if (errorElement) {
        errorElement.textContent = message;
    }
}

function clearFieldError(input, errorElement) {
    input.removeAttribute('aria-invalid');
    if (errorElement) {
        errorElement.textContent = '';
    }
}

function togglePasswordVisibility(inputId, button) {
    const input = document.getElementById(inputId);
    if (!input) return;
    
    const isPassword = input.type === 'password';
    input.type = isPassword ? 'text' : 'password';
    button.setAttribute('aria-label', isPassword ? 'Hide' : 'Show');
}

// API Functions
async function apiCall(endpoint, options = {}) {
    // Add stationId to query parameters for LibreTime API calls
    let url = `${API_BASE}${endpoint}`;
    if (selectedStationId && endpoint.startsWith('/integration')) {
        const separator = endpoint.includes('?') ? '&' : '?';
        url = `${url}${separator}stationId=${selectedStationId}`;
    }
    
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
        }
    };
    
    const mergedOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...(options.headers || {})
        }
    };
    
    try {
        showLoading(true);
        const response = await fetch(url, mergedOptions);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
        }
        
        return data;
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    } finally {
        showLoading(false);
    }
}

// Load Stations
async function loadStations() {
    try {
        const response = await fetch('/api/stations', {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (!response.ok) {
            throw new Error(`Failed to load stations: ${response.statusText}`);
        }
        
        stations = await response.json();
        populateStationSelector();
    } catch (error) {
        console.error('Failed to load stations:', error);
        showNotification('Failed to load stations: ' + error.message, 'error');
    }
}

function populateStationSelector() {
    const selector = document.getElementById('station-selector');
    if (!selector) return;
    
    // Clear existing options except the first one
    selector.innerHTML = '<option value="">-- Select a Station --</option>';
    
    // Add stations
    stations.forEach(station => {
        const option = document.createElement('option');
        option.value = station.id;
        option.textContent = `${station.callSign} - ${station.name || station.callSign}`;
        selector.appendChild(option);
    });
    
    // If there's only one station, auto-select it
    if (stations.length === 1) {
        selector.value = stations[0].id;
        handleStationChange({ target: selector });
    }
}

function handleStationChange(event) {
    const stationId = event.target.value;
    selectedStationId = stationId || null;
    
    // Clear current config
    currentConfig = null;
    clearForms();
    
    // Enable/disable forms based on station selection
    const forms = document.querySelectorAll('.settings-form');
    forms.forEach(form => {
        const inputs = form.querySelectorAll('input, select, textarea, button');
        inputs.forEach(input => {
            input.disabled = !selectedStationId;
        });
    });
    
    // Load configuration for selected station
    if (selectedStationId) {
        loadConfiguration();
        loadSyncStatistics();
        loadSyncHistory();
    } else {
        // Hide sections or show message
        showNotification('Please select a station to configure LibreTime integration', 'info');
    }
}

function clearForms() {
    // Clear all form inputs
    document.querySelectorAll('.settings-form input, .settings-form select, .settings-form textarea').forEach(input => {
        if (input.type === 'checkbox') {
            input.checked = false;
        } else {
            input.value = '';
        }
    });
}

// Load Configuration
async function loadConfiguration() {
    if (!selectedStationId) {
        console.log('No station selected, cannot load configuration');
        return;
    }
    
    try {
        const config = await apiCall('/integration/config');
        currentConfig = config;
        populateForms(config);
    } catch (error) {
        if (error.message.includes('404')) {
            // Configuration doesn't exist yet, that's okay
            console.log('No configuration found for this station, user can create one');
            currentConfig = null;
        } else {
            showNotification('Failed to load configuration: ' + error.message, 'error');
        }
    }
}

function populateForms(config) {
    // Connection settings
    const apiUrlInput = document.getElementById('api-base-url');
    if (apiUrlInput && config.apiBaseUrl) {
        apiUrlInput.value = config.apiBaseUrl;
    }
    
    // Sync settings
    const syncEnabled = document.getElementById('sync-enabled');
    if (syncEnabled) {
        syncEnabled.checked = config.syncEnabled || false;
    }
    
    const syncFrequency = document.getElementById('sync-frequency');
    if (syncFrequency && config.syncFrequency) {
        syncFrequency.value = config.syncFrequency;
    }
    
    const syncDirection = document.querySelector(`input[name="syncDirection"][value="${config.syncDirection}"]`);
    if (syncDirection) {
        syncDirection.checked = true;
    }
    
    const conflictResolution = document.getElementById('conflict-resolution');
    if (conflictResolution && config.conflictResolution) {
        conflictResolution.value = config.conflictResolution;
    }
    
    // File settings
    const maxFileSize = document.getElementById('max-file-size');
    if (maxFileSize && config.maxFileSizeMb) {
        maxFileSize.value = config.maxFileSizeMb;
    }
    
    const supportedFormats = config.supportedFormats || [];
    supportedFormats.forEach(format => {
        const checkbox = document.querySelector(`input[name="supportedFormats"][value="${format}"]`);
        if (checkbox) {
            checkbox.checked = true;
        }
    });
    
    // Webhook settings
    const webhookUrl = document.getElementById('webhook-url');
    if (webhookUrl && config.webhookUrl) {
        webhookUrl.value = config.webhookUrl;
    }
    
    const webhookEnabled = document.getElementById('webhook-enabled');
    if (webhookEnabled) {
        webhookEnabled.checked = config.webhookEnabled || false;
    }
}

// Handle Form Submissions
async function handleConnectionSubmit(e) {
    e.preventDefault();
    
    if (!selectedStationId) {
        showNotification('Please select a station first', 'error');
        document.getElementById('station-selector')?.focus();
        return;
    }
    
    if (!validateUrl(document.getElementById('api-base-url'))) {
        return;
    }
    
    const formData = {
        apiBaseUrl: document.getElementById('api-base-url').value.trim(),
        jwtToken: document.getElementById('jwt-token').value,
        syncEnabled: document.getElementById('sync-enabled')?.checked || false,
        syncFrequency: document.getElementById('sync-frequency')?.value || 'MANUAL',
        syncDirection: document.querySelector('input[name="syncDirection"]:checked')?.value || 'BIDIRECTIONAL',
        conflictResolution: document.getElementById('conflict-resolution')?.value || 'LAST_WRITE_WINS',
        maxFileSizeMb: parseInt(document.getElementById('max-file-size')?.value || '500'),
        supportedFormats: Array.from(document.querySelectorAll('input[name="supportedFormats"]:checked')).map(cb => cb.value),
        webhookUrl: document.getElementById('webhook-url')?.value.trim() || null,
        webhookSecret: document.getElementById('webhook-secret')?.value || null,
        webhookEnabled: document.getElementById('webhook-enabled')?.checked || false
    };
    
    try {
        let config;
        if (currentConfig) {
            config = await apiCall('/integration/config', {
                method: 'PUT',
                body: JSON.stringify(formData)
            });
        } else {
            config = await apiCall('/integration/config', {
                method: 'POST',
                body: JSON.stringify(formData)
            });
        }
        
        currentConfig = config;
        showNotification('Configuration saved successfully', 'success');
    } catch (error) {
        showNotification('Failed to save configuration: ' + error.message, 'error');
    }
}

async function handleSyncSubmit(e) {
    e.preventDefault();
    // Sync settings are part of the main config, so we'll update the full config
    await handleConnectionSubmit(e);
}

async function handleFileSubmit(e) {
    e.preventDefault();
    // File settings are part of the main config
    await handleConnectionSubmit(e);
}

async function handleLogSubmit(e) {
    e.preventDefault();
    showNotification('Log generation settings saved', 'success');
    // Note: Log generation settings might be stored separately in the future
}

async function handleWebhookSubmit(e) {
    e.preventDefault();
    // Webhook settings are part of the main config
    await handleConnectionSubmit(e);
}

// Test Connection
async function testConnection() {
    if (!selectedStationId) {
        showNotification('Please select a station first', 'error');
        document.getElementById('station-selector')?.focus();
        return;
    }
    
    const btn = document.getElementById('test-connection-btn');
    const statusIndicator = document.getElementById('status-indicator');
    const statusText = document.getElementById('status-text');
    
    if (!btn || !statusIndicator || !statusText) return;
    
    btn.disabled = true;
    statusIndicator.className = 'status-indicator testing';
    statusText.textContent = 'Testing connection...';
    
    try {
        const result = await apiCall('/integration/test-connection', {
            method: 'POST'
        });
        
        if (result.success) {
            statusIndicator.className = 'status-indicator connected';
            statusText.textContent = `Connected (${result.responseTimeMs}ms)`;
            showNotification('Connection test successful', 'success');
        } else {
            statusIndicator.className = 'status-indicator disconnected';
            statusText.textContent = 'Connection failed';
            showNotification('Connection test failed: ' + (result.errorDetails || result.message), 'error');
        }
    } catch (error) {
        statusIndicator.className = 'status-indicator disconnected';
        statusText.textContent = 'Connection error';
        showNotification('Connection test error: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

// Trigger Manual Sync
async function triggerManualSync() {
    const btn = document.getElementById('sync-now-btn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.textContent = 'Syncing...';
    
    try {
        // TODO: Implement manual sync endpoint
        showNotification('Manual sync triggered', 'info');
        // Refresh sync statistics after a delay
        setTimeout(() => {
            loadSyncStatistics();
            loadSyncHistory();
        }, 2000);
    } catch (error) {
        showNotification('Failed to trigger sync: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sync Now';
    }
}

// Test Webhook
async function testWebhook() {
    const btn = document.getElementById('test-webhook-btn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.textContent = 'Testing...';
    
    try {
        // TODO: Implement webhook test endpoint
        showNotification('Webhook test functionality coming soon', 'info');
    } catch (error) {
        showNotification('Webhook test failed: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Test Webhook';
    }
}

// API Testing Functions
async function discoverEndpoints() {
    const btn = document.getElementById('discover-endpoints-btn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.textContent = 'Discovering...';
    
    try {
        const endpoints = await apiCall('/testing/discover', {
            method: 'POST'
        });
        
        showNotification(`Discovered ${endpoints.length} endpoints`, 'success');
        displayEndpoints(endpoints);
    } catch (error) {
        showNotification('Failed to discover endpoints: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Discover Endpoints';
    }
}

async function runAllTests() {
    const btn = document.getElementById('run-tests-btn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.textContent = 'Running tests...';
    
    try {
        const summary = await apiCall('/testing/run-all-tests', {
            method: 'POST'
        });
        
        displayTestSummary(summary);
        displayTestResults(summary.testResults || []);
        
        showNotification(`Tests completed: ${summary.passedTests} passed, ${summary.failedTests} failed`, 
            summary.failedTests > 0 ? 'error' : 'success');
    } catch (error) {
        showNotification('Failed to run tests: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Run All Tests';
    }
}

async function exportDocumentation() {
    const btn = document.getElementById('export-docs-btn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.textContent = 'Exporting...';
    
    try {
        const format = 'MARKDOWN'; // Could make this configurable
        const response = await fetch(`${API_BASE}/testing/export-documentation?format=${format}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to export documentation');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `libretime-api-documentation-${new Date().toISOString().split('T')[0]}.md`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        showNotification('Documentation exported successfully', 'success');
    } catch (error) {
        showNotification('Failed to export documentation: ' + error.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Export Documentation';
    }
}

// Display Functions
function displayEndpoints(endpoints) {
    // Could display in a table or list
    console.log('Discovered endpoints:', endpoints);
}

function displayTestSummary(summary) {
    const summaryElement = document.getElementById('test-summary');
    if (!summaryElement) return;
    
    summaryElement.innerHTML = `
        <p><strong>Total Tests:</strong> ${summary.totalTests}</p>
        <p><strong>Passed:</strong> ${summary.passedTests}</p>
        <p><strong>Failed:</strong> ${summary.failedTests}</p>
        <p><strong>Skipped:</strong> ${summary.skippedTests}</p>
        <p><strong>Test Run:</strong> ${new Date(summary.testRunTimestamp).toLocaleString()}</p>
    `;
}

function displayTestResults(results) {
    const tbody = document.getElementById('test-results-tbody');
    if (!tbody) return;
    
    if (!results || results.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="no-results">No test results available</td></tr>';
        return;
    }
    
    tbody.innerHTML = results.map(result => {
        const statusClass = result.status === 'PASSED' ? 'working' : 
                           result.status === 'FAILED' ? 'broken' : 'unknown';
        return `
            <tr>
                <td>${result.endpointPath || 'N/A'}</td>
                <td>${result.httpMethod || 'N/A'}</td>
                <td><span class="status-badge ${statusClass}">${result.status}</span></td>
                <td>${result.responseTimeMs ? result.responseTimeMs + 'ms' : 'N/A'}</td>
                <td>
                    ${result.errorMessage ? `<button class="btn btn-secondary" onclick="showErrorDetails('${result.errorMessage}')">View Error</button>` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

function showErrorDetails(message) {
    alert('Error Details:\n\n' + message);
}

// Load Sync Statistics
async function loadSyncStatistics() {
    if (!selectedStationId) {
        return;
    }
    
    try {
        const stats = await apiCall('/sync-history/statistics');
        
        document.getElementById('stat-total-files').textContent = stats.totalFiles || 0;
        document.getElementById('stat-synced-files').textContent = stats.syncedFiles || 0;
        document.getElementById('stat-failed-files').textContent = stats.failedFiles || 0;
        document.getElementById('stat-pending-files').textContent = stats.pendingFiles || 0;
        document.getElementById('stat-success-rate').textContent = 
            stats.successRate ? stats.successRate.toFixed(1) + '%' : '0%';
    } catch (error) {
        console.error('Failed to load sync statistics:', error);
    }
}

// Load Sync History
async function loadSyncHistory() {
    if (!selectedStationId) {
        return;
    }
    
    try {
        const history = await apiCall('/sync-history');
        displaySyncHistory(history);
    } catch (error) {
        console.error('Failed to load sync history:', error);
    }
}

function displaySyncHistory(history) {
    const tbody = document.getElementById('sync-history-tbody');
    if (!tbody) return;
    
    if (!history || history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="no-results">No sync history available</td></tr>';
        return;
    }
    
    tbody.innerHTML = history.map(item => {
        const statusClass = item.status === 'completed' ? 'working' : 
                          item.status === 'failed' ? 'broken' : 'unknown';
        return `
            <tr>
                <td>${item.syncType || 'N/A'}</td>
                <td><span class="status-badge ${statusClass}">${item.status}</span></td>
                <td>${item.itemsSucceeded || 0}/${item.itemsTotal || 0}</td>
                <td>${item.startedAt ? new Date(item.startedAt).toLocaleString() : 'N/A'}</td>
                <td>${item.completedAt ? new Date(item.completedAt).toLocaleString() : 'In progress'}</td>
                <td>
                    ${item.errorSummary ? `<button class="btn btn-secondary" onclick="showErrorDetails('${item.errorSummary}')">View Error</button>` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

// Permission Functions
async function loadUserPermissions() {
    try {
        // Get user info which should include permissions
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const userInfo = await response.json();
            // Extract permissions from user info (adjust based on your API structure)
            userPermissions = userInfo.permissions || [];
        } else {
            // If permissions endpoint doesn't exist, check role-based access
            const userRole = localStorage.getItem('userRole');
            if (userRole === 'ADMIN') {
                // Admin has all permissions
                userPermissions = [
                    'LIBRETIME_INTEGRATION_VIEW',
                    'LIBRETIME_INTEGRATION_CONFIGURE',
                    'LIBRETIME_INTEGRATION_TEST',
                    'LIBRETIME_INTEGRATION_SYNC',
                    'LIBRETIME_INTEGRATION_EXPORT_LOGS'
                ];
            } else if (userRole === 'OPERATIONS') {
                // Operations has VIEW and SYNC
                userPermissions = [
                    'LIBRETIME_INTEGRATION_VIEW',
                    'LIBRETIME_INTEGRATION_SYNC'
                ];
            } else {
                // Default: VIEW only
                userPermissions = ['LIBRETIME_INTEGRATION_VIEW'];
            }
        }
    } catch (error) {
        console.error('Failed to load user permissions:', error);
        // Default to VIEW only on error
        userPermissions = ['LIBRETIME_INTEGRATION_VIEW'];
    }
}

function hasPermission(permission) {
    return userPermissions.includes(permission);
}

function applyPermissions() {
    // Hide/disable features based on permissions
    
    // Connection settings - requires VIEW
    if (!hasPermission('LIBRETIME_INTEGRATION_VIEW')) {
        document.getElementById('connection-settings')?.setAttribute('hidden', 'true');
    }
    
    // Configure button - requires CONFIGURE
    const saveConnectionBtn = document.getElementById('save-connection-btn');
    if (saveConnectionBtn && !hasPermission('LIBRETIME_INTEGRATION_CONFIGURE')) {
        saveConnectionBtn.disabled = true;
        saveConnectionBtn.title = 'You do not have permission to configure integration';
    }
    
    // Sync settings - requires VIEW
    if (!hasPermission('LIBRETIME_INTEGRATION_VIEW')) {
        document.getElementById('sync-settings')?.setAttribute('hidden', 'true');
    }
    
    // Sync Now button - requires SYNC
    const syncNowBtn = document.getElementById('sync-now-btn');
    if (syncNowBtn && !hasPermission('LIBRETIME_INTEGRATION_SYNC')) {
        syncNowBtn.disabled = true;
        syncNowBtn.title = 'You do not have permission to trigger synchronization';
    }
    
    // File upload - requires SYNC
    const uploadSection = document.querySelector('#file-settings .form-actions');
    if (uploadSection && !hasPermission('LIBRETIME_INTEGRATION_SYNC')) {
        uploadSection.querySelector('button')?.setAttribute('disabled', 'true');
    }
    
    // API Testing - requires TEST
    if (!hasPermission('LIBRETIME_INTEGRATION_TEST')) {
        document.getElementById('api-testing')?.setAttribute('hidden', 'true');
    } else {
        const discoverBtn = document.getElementById('discover-endpoints-btn');
        const runTestsBtn = document.getElementById('run-tests-btn');
        if (discoverBtn && !hasPermission('LIBRETIME_INTEGRATION_TEST')) {
            discoverBtn.disabled = true;
        }
        if (runTestsBtn && !hasPermission('LIBRETIME_INTEGRATION_TEST')) {
            runTestsBtn.disabled = true;
        }
    }
    
    // Export Documentation - requires EXPORT_LOGS
    const exportDocsBtn = document.getElementById('export-docs-btn');
    if (exportDocsBtn && !hasPermission('LIBRETIME_INTEGRATION_EXPORT_LOGS')) {
        exportDocsBtn.disabled = true;
        exportDocsBtn.title = 'You do not have permission to export documentation';
    }
}

// Utility Functions
function showLoading(show) {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) {
        overlay.hidden = !show;
    }
}

function showNotification(message, type = 'info') {
    const notification = document.getElementById('notification');
    if (!notification) return;
    
    notification.textContent = message;
    notification.className = `notification ${type}`;
    notification.hidden = false;
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
        notification.hidden = true;
    }, 5000);
}

// Make functions available globally for onclick handlers
window.showErrorDetails = showErrorDetails;

