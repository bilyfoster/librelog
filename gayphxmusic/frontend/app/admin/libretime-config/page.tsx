'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Save, TestTube, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

interface LibreTimeConfig {
  configured: boolean;
  libretime_url: string;
  sync_interval_minutes: number;
  auto_sync_enabled: boolean;
  sync_status: string;
  last_sync_at: string;
  error_count: number;
  last_error: string;
}

export default function LibreTimeConfigPage() {
  const router = useRouter();
  const [config, setConfig] = useState<LibreTimeConfig | null>(null);
  const [formData, setFormData] = useState({
    libretime_url: '',
    api_key: '',
    sync_interval_minutes: 15,
    auto_sync_enabled: true
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<{valid: boolean, message: string} | null>(null);

  useEffect(() => {
    fetchConfig();
  }, []);


  const fetchConfig = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('admin_token');
      if (!token) {
        router.push('/admin/login');
        return;
      }

      const response = await fetch('/api/plays/libretime-config', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setConfig(data);
        
        if (data.configured) {
          setFormData({
            libretime_url: data.libretime_url || '',
            api_key: data.api_key || '', // Show the actual API key
            sync_interval_minutes: data.sync_interval_minutes || 15,
            auto_sync_enabled: data.auto_sync_enabled || true
          });
        }
      } else if (response.status === 401) {
        localStorage.removeItem('admin_token');
        localStorage.removeItem('admin_data');
        router.push('/admin/login');
      } else {
        toast.error('Failed to load configuration');
      }
    } catch (error) {
      console.error('Error fetching LibreTime config:', error);
      toast.error('Failed to load configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.libretime_url || !formData.api_key) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      setSaving(true);
      const token = localStorage.getItem('admin_token');
      if (!token) {
        router.push('/admin/login');
        return;
      }

      const response = await fetch('/api/plays/libretime-config', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        toast.success('LibreTime configuration saved successfully!');
        await fetchConfig();
      } else {
        const error = await response.json();
        toast.error(error.detail || 'Failed to save configuration');
      }
    } catch (error) {
      console.error('Error saving config:', error);
      toast.error('Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const validateApiKey = async () => {
    if (!formData.libretime_url || !formData.api_key) {
      toast.error('Please fill in URL and API key first');
      return;
    }

    try {
      setValidating(true);
      setValidationResult(null);
      
      const token = localStorage.getItem('admin_token');
      if (!token) {
        router.push('/admin/login');
        return;
      }
      
      console.log('Validating API key with:', {
        libretime_url: formData.libretime_url,
        api_key: formData.api_key
      });
      
      const response = await fetch('/api/plays/libretime-validate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          libretime_url: formData.libretime_url,
          api_key: formData.api_key
        })
      });

      console.log('Response status:', response.status);
      console.log('Response headers:', Object.fromEntries(response.headers.entries()));

      const result = await response.json();
      console.log('Response data:', result);
      
      setValidationResult(result);
      
      if (result.valid) {
        toast.success('API key is valid!');
      } else {
        toast.error(`Validation failed: ${result.message}`);
      }
    } catch (error) {
      console.error('Error validating API key:', error);
      toast.error('Validation failed');
      setValidationResult({valid: false, message: 'Network error during validation'});
    } finally {
      setValidating(false);
    }
  };

  const testConnection = async () => {
    if (!formData.libretime_url || !formData.api_key) {
      toast.error('Please fill in URL and API key first');
      return;
    }

    try {
      setTesting(true);
      const response = await fetch('/api/plays/sync-libretime?hours_back=1', {
        method: 'POST'
      });

      if (response.ok) {
        toast.success('Connection test started! Check the play tracking page for results.');
      } else {
        toast.error('Connection test failed');
      }
    } catch (error) {
      console.error('Error testing connection:', error);
      toast.error('Connection test failed');
    } finally {
      setTesting(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'active':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'error':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'paused':
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return 'bg-green-100 text-green-800';
      case 'error':
        return 'bg-red-100 text-red-800';
      case 'paused':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <div className="text-xl text-gray-600">Loading configuration...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-lime-50">
      {/* Play Syncing Area Banner */}
      <div className="bg-lime-400 border-b-4 border-lime-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2">
          <div className="flex items-center justify-center">
            <AlertCircle className="h-5 w-5 text-lime-800 mr-2" />
            <span className="text-lime-800 font-semibold">PLAY SYNCING AREA - LibreTime Integration</span>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center mb-4">
            <Link
              href="/admin/play-tracking"
              className="flex items-center text-gray-600 hover:text-gray-900 mr-4"
            >
              <ArrowLeft className="h-5 w-5 mr-2" />
              Back to Play Tracking
            </Link>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">LibreTime Integration</h1>
          <p className="mt-2 text-gray-600">
            Configure automatic sync with your LibreTime radio automation system
          </p>
        </div>

        {/* Current Status */}
        {config && (
          <div className="card mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Current Status</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <div className="flex items-center mb-2">
                  {getStatusIcon(config.sync_status)}
                  <span className="ml-2 font-medium text-gray-900">Integration Status</span>
                </div>
                <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(config.sync_status)}`}>
                  {config.sync_status}
                </span>
              </div>
              
              <div>
                <div className="text-sm font-medium text-gray-900 mb-1">Last Sync</div>
                <div className="text-sm text-gray-600">
                  {config.last_sync_at ? (
                    <span className="text-green-600">
                      {new Date(config.last_sync_at).toLocaleString()}
                    </span>
                  ) : (
                    <span className="text-orange-600 font-medium">
                      Not configured yet
                    </span>
                  )}
                </div>
              </div>
              
              <div>
                <div className="text-sm font-medium text-gray-900 mb-1">Error Count</div>
                <div className="text-sm text-gray-600">{config.error_count}</div>
              </div>
              
              <div>
                <div className="text-sm font-medium text-gray-900 mb-1">Auto Sync</div>
                <div className="text-sm text-gray-600">
                  {config.auto_sync_enabled ? 'Enabled' : 'Disabled'}
                </div>
              </div>
            </div>

            {config.last_error && (
              <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                <div className="text-sm font-medium text-red-800 mb-1">Last Error</div>
                <div className="text-sm text-red-700">{config.last_error}</div>
              </div>
            )}
          </div>
        )}

        {config && !config.configured && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
            <div className="flex items-start">
              <AlertCircle className="h-5 w-5 text-blue-600 mr-3 mt-0.5" />
              <div>
                <h3 className="text-lg font-semibold text-blue-900 mb-2">LibreTime Integration Not Configured</h3>
                <p className="text-blue-800 mb-4">
                  To sync play data from LibreTime, you need to configure the connection first. 
                  This will allow the system to automatically track when your music is played on GayPHX Radio.
                </p>
                <div className="text-sm text-blue-700">
                  <p className="font-medium mb-2">What you'll need:</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>LibreTime URL (e.g., https://studio.gayphx.com)</li>
                    <li>LibreTime API key (found in LibreTime settings)</li>
                    <li>Sync interval preference (default: 15 minutes)</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Configuration Form */}
        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Configuration</h2>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="libretime_url" className="block text-sm font-medium text-gray-700 mb-2">
                LibreTime URL *
              </label>
              <input
                type="url"
                id="libretime_url"
                value={formData.libretime_url}
                onChange={(e) => {
                  setFormData({ ...formData, libretime_url: e.target.value });
                  setValidationResult(null);
                }}
                placeholder="https://studio.gayphx.com"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
              <p className="mt-1 text-sm text-gray-500">
                The URL of your LibreTime installation (e.g., https://studio.gayphx.com)
              </p>
            </div>

            <div>
              <label htmlFor="api_key" className="block text-sm font-medium text-gray-700 mb-2">
                API Key *
                {config?.configured && (
                  <span className="ml-2 inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    ✓ Configured
                  </span>
                )}
              </label>
              <input
                type="text"
                id="api_key"
                value={formData.api_key}
                onChange={(e) => {
                  setFormData({ ...formData, api_key: e.target.value });
                  setValidationResult(null);
                }}
                placeholder="Enter your LibreTime API key"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                required
              />
              <p className="mt-1 text-sm text-gray-500">
                {config?.configured 
                  ? "API key is configured and displayed above. You can edit it to update or test validation."
                  : "Get this from your LibreTime admin panel under \"API Keys\""
                }
              </p>
              
              {/* Validation Result */}
              {validationResult && (
                <div className={`mt-2 p-3 rounded-lg ${
                  validationResult.valid 
                    ? 'bg-green-50 border border-green-200' 
                    : 'bg-red-50 border border-red-200'
                }`}>
                  <div className={`text-sm font-medium ${
                    validationResult.valid ? 'text-green-800' : 'text-red-800'
                  }`}>
                    {validationResult.valid ? '✓ Valid' : '✗ Invalid'}
                  </div>
                  <div className={`text-sm ${
                    validationResult.valid ? 'text-green-700' : 'text-red-700'
                  }`}>
                    {validationResult.message}
                  </div>
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="sync_interval" className="block text-sm font-medium text-gray-700 mb-2">
                  Sync Interval (minutes)
                </label>
                <input
                  type="number"
                  id="sync_interval"
                  value={formData.sync_interval_minutes}
                  onChange={(e) => setFormData({ ...formData, sync_interval_minutes: parseInt(e.target.value) })}
                  min="5"
                  max="1440"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="mt-1 text-sm text-gray-500">
                  How often to sync play data (5-1440 minutes)
                </p>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="auto_sync"
                  checked={formData.auto_sync_enabled}
                  onChange={(e) => setFormData({ ...formData, auto_sync_enabled: e.target.checked })}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="auto_sync" className="ml-2 block text-sm text-gray-700">
                  Enable automatic sync
                </label>
              </div>
            </div>

            <div className="flex space-x-4">
              <button
                type="button"
                onClick={validateApiKey}
                disabled={validating || !formData.libretime_url || !formData.api_key}
                className="flex items-center px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50"
              >
                <CheckCircle className="h-4 w-4 mr-2" />
                {validating ? 'Validating...' : 'Validate API Key'}
              </button>

              <button
                type="submit"
                disabled={saving || (validationResult ? !validationResult.valid : false)}
                className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                <Save className="h-4 w-4 mr-2" />
                {saving ? 'Saving...' : 'Save Configuration'}
              </button>

              <button
                type="button"
                onClick={testConnection}
                disabled={testing || !formData.libretime_url || !formData.api_key}
                className="flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
              >
                <TestTube className="h-4 w-4 mr-2" />
                {testing ? 'Testing...' : 'Test Connection'}
              </button>
            </div>
          </form>
        </div>

        {/* Help Section */}
        <div className="bg-blue-50 rounded-lg p-6 mt-8">
          <h3 className="text-lg font-semibold text-blue-900 mb-4">Setup Instructions</h3>
          <div className="space-y-3 text-sm text-blue-800">
            <p>1. <strong>Get your LibreTime API key:</strong> Log into your LibreTime admin panel and go to "API Keys" to generate a new key.</p>
            <p>2. <strong>Enter the URL:</strong> This should be the full URL to your LibreTime installation (e.g., https://studio.gayphx.com).</p>
            <p>3. <strong>Validate the API key:</strong> Use the "Validate API Key" button to test your credentials before saving.</p>
            <p>4. <strong>Save configuration:</strong> Once validated, save your configuration to enable LibreTime integration.</p>
            <p>5. <strong>Test the connection:</strong> Use the "Test Connection" button to verify everything is working with actual data sync.</p>
            <p>6. <strong>Set sync interval:</strong> Choose how often to sync play data (recommended: 15-30 minutes).</p>
            <p>7. <strong>Enable auto-sync:</strong> Check this box to automatically sync play data in the background.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

