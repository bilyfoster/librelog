import { NextRequest, NextResponse } from 'next/server'

const API_BASE_URL = 'http://backend:8000'

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    console.log('Admin login request:', JSON.stringify(body, null, 2))
    
    const response = await fetch(`${API_BASE_URL}/api/admin/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })

    console.log('Backend response status:', response.status)
    
    const responseText = await response.text()
    console.log('Backend response text:', responseText)
    
    let data
    try {
      data = JSON.parse(responseText)
    } catch (parseError) {
      console.error('JSON parse error:', parseError)
      console.error('Response text:', responseText)
      return NextResponse.json(
        { error: 'Invalid JSON response from backend', details: responseText },
        { status: 500 }
      )
    }
    
    if (!response.ok) {
      return NextResponse.json(data, { status: response.status })
    }

    return NextResponse.json(data)
  } catch (error) {
    console.error('Admin login API Error:', error)
    return NextResponse.json(
      { error: 'Internal server error', details: error instanceof Error ? error.message : String(error) },
      { status: 500 }
    )
  }
}
