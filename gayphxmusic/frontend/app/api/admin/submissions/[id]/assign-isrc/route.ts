import { NextRequest, NextResponse } from 'next/server'

const API_BASE_URL = 'http://backend:8000'

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    console.log('Frontend API route called for ISRC assignment')
    const body = await request.json()
    console.log('Request body:', body)
    
    // Add required assigned_by field if not provided
    const requestBody = {
      ...body,
      assigned_by: body.assigned_by || 'admin'
    }
    console.log('Request body with assigned_by:', requestBody)
    
    const response = await fetch(`${API_BASE_URL}/api/admin/submissions/${params.id}/assign-isrc`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
    })

    if (!response.ok) {
      const errorText = await response.text()
      return NextResponse.json(
        { error: 'Failed to assign ISRC', details: errorText },
        { status: response.status }
      )
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error('ISRC Assignment API Error:', error)
    return NextResponse.json(
      { error: 'Internal server error', details: error instanceof Error ? error.message : String(error) },
      { status: 500 }
    )
  }
}
