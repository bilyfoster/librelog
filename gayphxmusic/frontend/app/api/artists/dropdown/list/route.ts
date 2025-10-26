import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.NODE_ENV === 'production' 
                    ? 'http://backend:8000' 
                    : 'http://localhost:8000';

export async function GET(request: NextRequest) {
  const token = request.headers.get('Authorization');
  if (!token) {
    return NextResponse.json({ detail: 'Not authenticated' }, { status: 401 });
  }

  try {
    const backendResponse = await fetch(`${BACKEND_URL}/api/artists/dropdown/list`, {
      headers: {
        'Authorization': token,
      },
    });

    const data = await backendResponse.json();
    return NextResponse.json(data, { status: backendResponse.status });
  } catch (error) {
    console.error('Error proxying artists dropdown GET:', error);
    return NextResponse.json({ detail: 'Internal server error' }, { status: 500 });
  }
}

