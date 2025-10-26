import { NextRequest, NextResponse } from 'next/server'

const BACKEND_URL = process.env.NODE_ENV === 'production' 
                    ? 'http://backend:8000' 
                    : 'http://localhost:8000'

export async function GET(request: NextRequest) {
  try {
    const authHeader = request.headers.get('authorization')
    
    if (!authHeader) {
      return NextResponse.json({ error: 'Authorization header required' }, { status: 401 })
    }

    const response = await fetch(`${BACKEND_URL}/api/plays/statistics`, {
      method: 'GET',
      headers: {
        'Authorization': authHeader,
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      const errorData = await response.json()
      return NextResponse.json(errorData, { status: response.status })
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error('Error fetching play statistics:', error)
    return NextResponse.json(
      { error: 'Failed to fetch play statistics' },
      { status: 500 }
    )
  }
}
