import { NextRequest, NextResponse } from 'next/server'

const API_BASE_URL = 'http://backend:8000'

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const search = searchParams.get('search')
    const is_active = searchParams.get('is_active')
    const limit = searchParams.get('limit') || '50'
    const offset = searchParams.get('offset') || '0'
    
    // Build query string
    const queryParams = new URLSearchParams()
    if (search) queryParams.append('search', search)
    if (is_active) queryParams.append('is_active', is_active)
    queryParams.append('limit', limit)
    queryParams.append('offset', offset)
    
    const queryString = queryParams.toString()
    const url = queryString ? `${API_BASE_URL}/api/admin/users?${queryString}` : `${API_BASE_URL}/api/admin/users`
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      const errorText = await response.text()
      return NextResponse.json(
        { error: 'Failed to fetch users', details: errorText },
        { status: response.status }
      )
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error('Users API Error:', error)
    return NextResponse.json(
      { error: 'Internal server error', details: error instanceof Error ? error.message : String(error) },
      { status: 500 }
    )
  }
}
