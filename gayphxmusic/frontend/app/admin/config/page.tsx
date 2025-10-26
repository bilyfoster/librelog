'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Settings, Save, Palette, Mail, Upload, Music, Globe, Shield, ArrowLeft } from 'lucide-react'
import toast from 'react-hot-toast'

interface SystemConfig {
  id: string
  organization_name: string
  organization_description?: string
  contact_email: string
  support_email: string
  logo_url?: string
  favicon_url?: string
  primary_color: string
  secondary_color: string
  accent_color: string
  background_color: string
  text_color: string
  isrc_country_code: string
  isrc_registrant_code: string
  isrc_organization_name: string
  smtp_host?: string
  smtp_port: string
  smtp_username?: string
  smtp_password?: string
  smtp_use_tls: boolean
  from_email: string
  from_name: string
  max_file_size_mb: string
  allowed_file_types: string[]
  target_lufs: string
  max_true_peak: string
  libretime_url?: string
  libretime_sync_enabled: boolean
  libretime_sync_interval: string
  terms_of_service_url?: string
  privacy_policy_url?: string
  copyright_notice: string
  enable_public_gallery: boolean
  enable_artist_registration: boolean
  enable_isrc_assignment: boolean
  enable_play_tracking: boolean
  enable_rights_management: boolean
  enable_commercial_use_tracking: boolean
  social_links: Record<string, string>
  created_at: string
  updated_at: string
}

export default function SystemConfigPage() {
  const [config, setConfig] = useState<SystemConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('basic')
  const router = useRouter()

  useEffect(() => {
    fetchConfig()
  }, [])

  const fetchConfig = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('admin_token')
      if (!token) {
        router.push('/admin/login')
        return
      }

      const response = await fetch('http://localhost:8000/api/config/', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      if (response.ok) {
        const data = await response.json()
        setConfig(data)
      } else {
        toast.error('Failed to fetch configuration')
      }
    } catch (error) {
      console.error('Error fetching config:', error)
      toast.error('Error fetching configuration')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!config) return

    try {
      setSaving(true)
      const token = localStorage.getItem('admin_token')
      if (!token) {
        router.push('/admin/login')
        return
      }

      const response = await fetch('http://localhost:8000/api/config/', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(config)
      })

      if (response.ok) {
        toast.success('Configuration updated successfully')
        fetchConfig() // Refresh config
      } else {
        const error = await response.json()
        toast.error(error.detail || 'Failed to update configuration')
      }
    } catch (error) {
      console.error('Error updating config:', error)
      toast.error('Error updating configuration')
    } finally {
      setSaving(false)
    }
  }

  const handleInputChange = (field: keyof SystemConfig, value: any) => {
    if (!config) return
    setConfig(prev => prev ? { ...prev, [field]: value } : null)
  }

  const handleSocialLinkChange = (platform: string, value: string) => {
    if (!config) return
    setConfig(prev => prev ? {
      ...prev,
      social_links: {
        ...prev.social_links,
        [platform]: value
      }
    } : null)
  }

  const tabs = [
    { id: 'basic', label: 'Basic Info', icon: Settings },
    { id: 'branding', label: 'Branding', icon: Palette },
    { id: 'isrc', label: 'ISRC Settings', icon: Music },
    { id: 'email', label: 'Email', icon: Mail },
    { id: 'features', label: 'Features', icon: Shield },
    { id: 'social', label: 'Social Links', icon: Globe }
  ]

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gayphx-purple mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading configuration...</p>
        </div>
      </div>
    )
  }

  if (!config) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Settings className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">Configuration not found</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-yellow-400 to-orange-500 relative overflow-hidden">
      {/* Spiral Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `conic-gradient(from 0deg at 50% 50%, white 0deg, transparent 60deg, white 120deg, transparent 180deg, white 240deg, transparent 300deg)`,
          backgroundSize: '30px 30px'
        }}></div>
      </div>
      {/* Admin Warning Banner */}
      <div className="bg-yellow-400 border-b-4 border-yellow-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2">
          <div className="flex items-center justify-center">
            <Shield className="h-5 w-5 text-yellow-800 mr-2" />
            <span className="text-yellow-800 font-semibold">ADMIN AREA - Authorized Personnel Only</span>
          </div>
        </div>
      </div>

      {/* Header */}
      <div className="bg-white/10 backdrop-blur-md border-b border-white/20 shadow relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center">
              <Settings className="h-8 w-8 text-gayphx-purple mr-3" />
              <div>
                <h1 className="text-3xl font-bold text-white">System Configuration</h1>
                <p className="text-white/80">Configure your music platform settings</p>
              </div>
            </div>
            <button
              onClick={() => router.push('/admin')}
              className="btn-secondary"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Sidebar */}
          <div className="lg:col-span-1">
            <div className="card">
              <nav className="space-y-2">
                {tabs.map((tab) => {
                  const Icon = tab.icon
                  return (
                    <button
                      key={tab.id}
                      onClick={() => setActiveTab(tab.id)}
                      className={`w-full flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                        activeTab === tab.id
                          ? 'bg-gayphx-purple text-white'
                          : 'text-gray-600 hover:bg-gray-100'
                      }`}
                    >
                      <Icon className="h-4 w-4 mr-3" />
                      {tab.label}
                    </button>
                  )
                })}
              </nav>
            </div>
          </div>

          {/* Main Content */}
          <div className="lg:col-span-3">
            <div className="card">
              <form onSubmit={handleSubmit}>
                {/* Basic Information Tab */}
                {activeTab === 'basic' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">Basic Information</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Organization Name
                        </label>
                        <input
                          type="text"
                          value={config.organization_name}
                          onChange={(e) => handleInputChange('organization_name', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Contact Email
                        </label>
                        <input
                          type="email"
                          value={config.contact_email}
                          onChange={(e) => handleInputChange('contact_email', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Organization Description
                      </label>
                      <textarea
                        value={config.organization_description || ''}
                        onChange={(e) => handleInputChange('organization_description', e.target.value)}
                        rows={3}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                      />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Support Email
                        </label>
                        <input
                          type="email"
                          value={config.support_email}
                          onChange={(e) => handleInputChange('support_email', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Copyright Notice
                        </label>
                        <input
                          type="text"
                          value={config.copyright_notice}
                          onChange={(e) => handleInputChange('copyright_notice', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Branding Tab */}
                {activeTab === 'branding' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">Branding & Colors</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Logo URL
                        </label>
                        <input
                          type="url"
                          value={config.logo_url || ''}
                          onChange={(e) => handleInputChange('logo_url', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="https://example.com/logo.png"
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Favicon URL
                        </label>
                        <input
                          type="url"
                          value={config.favicon_url || ''}
                          onChange={(e) => handleInputChange('favicon_url', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="https://example.com/favicon.ico"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Primary Color
                        </label>
                        <div className="flex">
                          <input
                            type="color"
                            value={config.primary_color}
                            onChange={(e) => handleInputChange('primary_color', e.target.value)}
                            className="w-12 h-10 border border-gray-300 rounded-l-lg cursor-pointer"
                          />
                          <input
                            type="text"
                            value={config.primary_color}
                            onChange={(e) => handleInputChange('primary_color', e.target.value)}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-r-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                            placeholder="#667eea"
                          />
                        </div>
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Secondary Color
                        </label>
                        <div className="flex">
                          <input
                            type="color"
                            value={config.secondary_color}
                            onChange={(e) => handleInputChange('secondary_color', e.target.value)}
                            className="w-12 h-10 border border-gray-300 rounded-l-lg cursor-pointer"
                          />
                          <input
                            type="text"
                            value={config.secondary_color}
                            onChange={(e) => handleInputChange('secondary_color', e.target.value)}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-r-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                            placeholder="#764ba2"
                          />
                        </div>
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Accent Color
                        </label>
                        <div className="flex">
                          <input
                            type="color"
                            value={config.accent_color}
                            onChange={(e) => handleInputChange('accent_color', e.target.value)}
                            className="w-12 h-10 border border-gray-300 rounded-l-lg cursor-pointer"
                          />
                          <input
                            type="text"
                            value={config.accent_color}
                            onChange={(e) => handleInputChange('accent_color', e.target.value)}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-r-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                            placeholder="#f093fb"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* ISRC Settings Tab */}
                {activeTab === 'isrc' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">ISRC Configuration</h2>
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <p className="text-sm text-yellow-800">
                        <strong>Important:</strong> You must register with the US ISRC agency to get your official 3-character registrant code.
                        Visit <a href="https://usisrc.org/" target="_blank" rel="noopener noreferrer" className="underline">usisrc.org</a> to apply.
                      </p>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Country Code
                        </label>
                        <input
                          type="text"
                          value={config.isrc_country_code}
                          onChange={(e) => handleInputChange('isrc_country_code', e.target.value.toUpperCase())}
                          maxLength={2}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent uppercase"
                          placeholder="US"
                          required
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Registrant Code
                        </label>
                        <input
                          type="text"
                          value={config.isrc_registrant_code}
                          onChange={(e) => {
                            const value = e.target.value.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 3)
                            handleInputChange('isrc_registrant_code', value)
                          }}
                          maxLength={3}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent uppercase"
                          placeholder="ABC"
                          required
                        />
                        <p className="mt-1 text-xs text-gray-500">Get this from US ISRC agency</p>
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Organization Name
                        </label>
                        <input
                          type="text"
                          value={config.isrc_organization_name}
                          onChange={(e) => handleInputChange('isrc_organization_name', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="Your Organization"
                          required
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Email Tab */}
                {activeTab === 'email' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">Email Configuration</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          SMTP Host
                        </label>
                        <input
                          type="text"
                          value={config.smtp_host || ''}
                          onChange={(e) => handleInputChange('smtp_host', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="smtp.gmail.com"
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          SMTP Port
                        </label>
                        <input
                          type="text"
                          value={config.smtp_port}
                          onChange={(e) => handleInputChange('smtp_port', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="587"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          SMTP Username
                        </label>
                        <input
                          type="text"
                          value={config.smtp_username || ''}
                          onChange={(e) => handleInputChange('smtp_username', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="your-email@gmail.com"
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          SMTP Password
                        </label>
                        <input
                          type="password"
                          value={config.smtp_password || ''}
                          onChange={(e) => handleInputChange('smtp_password', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          placeholder="••••••••"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          From Email
                        </label>
                        <input
                          type="email"
                          value={config.from_email}
                          onChange={(e) => handleInputChange('from_email', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          From Name
                        </label>
                        <input
                          type="text"
                          value={config.from_name}
                          onChange={(e) => handleInputChange('from_name', e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                          required
                        />
                      </div>
                    </div>

                    <div className="flex items-center">
                      <input
                        type="checkbox"
                        id="smtp_use_tls"
                        checked={config.smtp_use_tls}
                        onChange={(e) => handleInputChange('smtp_use_tls', e.target.checked)}
                        className="h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                      />
                      <label htmlFor="smtp_use_tls" className="ml-2 block text-sm text-gray-700">
                        Use TLS encryption
                      </label>
                    </div>
                  </div>
                )}

                {/* Features Tab */}
                {activeTab === 'features' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">Feature Flags</h2>
                    
                    <div className="space-y-4">
                      {[
                        { key: 'enable_public_gallery', label: 'Public Gallery', description: 'Allow public viewing of approved music' },
                        { key: 'enable_artist_registration', label: 'Artist Registration', description: 'Allow new artists to sign up' },
                        { key: 'enable_isrc_assignment', label: 'ISRC Assignment', description: 'Enable ISRC code generation and assignment' },
                        { key: 'enable_play_tracking', label: 'Play Tracking', description: 'Track radio plays and statistics' },
                        { key: 'enable_rights_management', label: 'Rights Management', description: 'Manage music rights and permissions' },
                        { key: 'enable_commercial_use_tracking', label: 'Commercial Use Tracking', description: 'Track commercial use of music' }
                      ].map((feature) => (
                        <div key={feature.key} className="flex items-start">
                          <input
                            type="checkbox"
                            id={feature.key}
                            checked={config[feature.key as keyof SystemConfig] as boolean}
                            onChange={(e) => handleInputChange(feature.key as keyof SystemConfig, e.target.checked)}
                            className="h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded mt-1"
                          />
                          <div className="ml-3">
                            <label htmlFor={feature.key} className="text-sm font-medium text-gray-700">
                              {feature.label}
                            </label>
                            <p className="text-sm text-gray-500">{feature.description}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Social Links Tab */}
                {activeTab === 'social' && (
                  <div className="space-y-6">
                    <h2 className="text-xl font-semibold text-gray-900">Social Media Links</h2>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {[
                        { key: 'website', label: 'Website', placeholder: 'https://example.com' },
                        { key: 'facebook', label: 'Facebook', placeholder: 'https://facebook.com/yourpage' },
                        { key: 'twitter', label: 'Twitter', placeholder: 'https://twitter.com/yourhandle' },
                        { key: 'instagram', label: 'Instagram', placeholder: 'https://instagram.com/yourhandle' },
                        { key: 'youtube', label: 'YouTube', placeholder: 'https://youtube.com/yourchannel' },
                        { key: 'tiktok', label: 'TikTok', placeholder: 'https://tiktok.com/@yourhandle' }
                      ].map((social) => (
                        <div key={social.key}>
                          <label className="block text-sm font-medium text-gray-700 mb-2">
                            {social.label}
                          </label>
                          <input
                            type="url"
                            value={config.social_links[social.key] || ''}
                            onChange={(e) => handleSocialLinkChange(social.key, e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                            placeholder={social.placeholder}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Submit Button */}
                <div className="flex justify-end pt-6 border-t">
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex items-center px-6 py-2 bg-gayphx-purple text-white rounded-lg hover:bg-gayphx-pink disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {saving ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                        Saving...
                      </>
                    ) : (
                      <>
                        <Save className="h-4 w-4 mr-2" />
                        Save Configuration
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
