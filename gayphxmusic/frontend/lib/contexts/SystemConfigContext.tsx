'use client'

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

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

interface SystemConfigContextType {
  config: SystemConfig | null
  loading: boolean
  error: string | null
  refreshConfig: () => Promise<void>
}

const SystemConfigContext = createContext<SystemConfigContextType | undefined>(undefined);

interface SystemConfigProviderProps {
  children: ReactNode
}

export function SystemConfigProvider({ children }: SystemConfigProviderProps) {
  const [config, setConfig] = useState<SystemConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchConfig = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const response = await fetch('/api/system-config')
      
      if (!response.ok) {
        throw new Error(`Failed to fetch config: ${response.status}`)
      }
      
      const data = await response.json()
      setConfig(data)
    } catch (err) {
      console.error('Error fetching system configuration:', err)
      setError(err instanceof Error ? err.message : 'Failed to fetch configuration')
      
      // Set fallback config if fetch fails
      setConfig({
        id: 'fallback',
        organization_name: 'GayPHX Music Platform',
        organization_description: 'Music support platform for LGBTQ+ artists',
        contact_email: 'music@gayphx.com',
        support_email: 'support@gayphx.com',
        primary_color: '#667eea',
        secondary_color: '#764ba2',
        accent_color: '#f093fb',
        background_color: '#f8f9fa',
        text_color: '#333333',
        isrc_country_code: 'US',
        isrc_registrant_code: 'GPH',
        isrc_organization_name: 'GayPHX Music',
        smtp_port: '587',
        smtp_use_tls: true,
        from_email: 'noreply@gayphx.com',
        from_name: 'GayPHX Music Platform',
        max_file_size_mb: '150',
        allowed_file_types: ['mp3'],
        target_lufs: '-14.0',
        max_true_peak: '-1.0',
        libretime_sync_enabled: false,
        libretime_sync_interval: '15',
        copyright_notice: '© 2025 GayPHX Music Platform',
        enable_public_gallery: true,
        enable_artist_registration: true,
        enable_isrc_assignment: true,
        enable_play_tracking: true,
        enable_rights_management: true,
        enable_commercial_use_tracking: true,
        social_links: {},
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
    } finally {
      setLoading(false)
    }
  }

  const refreshConfig = async () => {
    await fetchConfig()
  }

  useEffect(() => {
    fetchConfig()
  }, [])

  return (
    <SystemConfigContext.Provider value={{ config, loading, error, refreshConfig }}>
      {children}
    </SystemConfigContext.Provider>
  )
}

export function useSystemConfig() {
  const context = useContext(SystemConfigContext)
  if (context === undefined) {
    throw new Error('useSystemConfig must be used within a SystemConfigProvider')
  }
  return context
}

// Helper hook for getting specific config values with fallbacks
export function useConfigValue<K extends keyof SystemConfig>(
  key: K,
  fallback: SystemConfig[K]
): SystemConfig[K] {
  const { config } = useSystemConfig()
  return config?.[key] ?? fallback
}
