# Artist Management Guide

## Overview
The GayPHX Music platform provides comprehensive artist management functionality that allows users to create, view, edit, and manage their artist profiles. This guide covers all aspects of artist management from both user and administrative perspectives.

## User Artist Management

### Creating Artists
Users can create multiple artist profiles associated with their account:

1. **Access**: Navigate to `/artists` from the dashboard or main navigation
2. **Authentication**: Users must be logged in with a valid token
3. **Creation Process**:
   - Click "Create New Artist" button
   - Fill out required information:
     - Artist Name (required)
     - Pronouns (optional)
     - Bio (optional)
     - Social Links (optional)
   - Submit the form

### Editing Artists
Users can edit their existing artist profiles:

1. **Access**: From the artist list, click "Edit" on any artist
2. **URL Pattern**: `/artists/{artist_id}/edit`
3. **Editable Fields**:
   - Artist Name
   - Pronouns
   - Bio
   - Social Media Links (Website, Instagram, Twitter, Facebook, YouTube, Spotify, SoundCloud, Bandcamp)
4. **Validation**:
   - Artist name is required
   - URLs must be valid format
   - Duplicate artist names are not allowed for the same user

### Viewing Artists
Users can view detailed information about their artists:

1. **Artist List**: `/artists` - Shows all artists for the current user
2. **Artist Detail**: `/artists/{artist_id}` - Shows detailed artist information
3. **Information Displayed**:
   - Basic information (name, pronouns, bio)
   - Social media links
   - Submission count
   - Creation and update dates
   - Active status

### Artist Deactivation
Users can deactivate artists (soft delete):

1. **Access**: From artist detail page, click "Deactivate"
2. **Behavior**:
   - Artists with submissions are soft-deleted (marked as inactive)
   - Artists without submissions are permanently deleted
   - Deactivated artists can be reactivated

## Administrative Artist Management

### Viewing All Artists
Administrators can view all artists across the platform:

1. **Access**: Admin dashboard → "Manage Users" → "View All Artists"
2. **URL**: `/admin/users` (with admin token)
3. **Features**:
   - Search functionality
   - Filter by active status
   - View submission counts
   - Access individual artist details

### Artist Details for Admins
Administrators can view comprehensive artist information:

1. **URL Pattern**: `/admin/users/{user_id}`
2. **Information Displayed**:
   - User's basic information
   - All artist profiles associated with the user's email
   - All submissions for each artist
   - Play statistics and tracking data

## Technical Implementation

### Backend API Endpoints

#### Artist Management
- `GET /api/artists/` - List user's artists
- `GET /api/artists/{id}` - Get specific artist
- `POST /api/artists/` - Create new artist
- `PUT /api/artists/{id}` - Update artist
- `DELETE /api/artists/{id}` - Delete/deactivate artist
- `POST /api/artists/{id}/reactivate` - Reactivate artist

#### Admin Endpoints
- `GET /api/admin/users` - List all users/artists (admin only)
- `GET /api/admin/users/{id}` - Get user details with all artists

### Frontend API Routes
- `GET /api/artists/[id]/route.ts` - Proxy for artist operations
- `PUT /api/artists/[id]/route.ts` - Handle artist updates
- `DELETE /api/artists/[id]/route.ts` - Handle artist deletion

### Authentication
- **User Tokens**: `auth_token`, `token`, or `artist_token`
- **Admin Tokens**: `admin_token` for administrative access
- **Token Validation**: JWT-based authentication with proper error handling

## User Interface Features

### Consistent Styling
- **Color Scheme**: Cyan-to-teal gradient for artist management sections
- **Patterns**: Diagonal stripes pattern for colorblind accessibility
- **Navigation**: Consistent header design with backdrop blur
- **Responsive Design**: Mobile-friendly layouts

### Accessibility Features
- **Colorblind Support**: Distinct patterns for different sections
- **Keyboard Navigation**: Full keyboard accessibility
- **Screen Reader Support**: Proper ARIA labels and semantic HTML
- **Focus Management**: Clear focus indicators

### Form Validation
- **Real-time Validation**: Errors clear as user types
- **URL Validation**: Proper URL format checking
- **Required Field Validation**: Clear error messages
- **Duplicate Prevention**: Prevents duplicate artist names

## Error Handling

### Common Error Scenarios
1. **Authentication Errors**: Redirect to login page
2. **Not Found Errors**: Show appropriate error messages
3. **Validation Errors**: Display field-specific error messages
4. **Network Errors**: Graceful error handling with retry options

### Error Messages
- Clear, user-friendly error messages
- Field-specific validation feedback
- General error handling for unexpected issues

## Data Models

### Artist Model
```typescript
interface Artist {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
  is_active: boolean
  created_at: string
  updated_at: string
  submission_count: number
}
```

### Social Links
Supported platforms:
- Website
- Instagram
- Twitter
- Facebook
- YouTube
- Spotify
- SoundCloud
- Bandcamp

## Security Considerations

### Access Control
- Users can only access their own artists
- Admins can access all artists
- Proper token validation on all endpoints
- CSRF protection through token-based authentication

### Data Validation
- Server-side validation for all inputs
- SQL injection prevention through ORM
- XSS protection through proper escaping
- Input sanitization for social links

## Troubleshooting

### Common Issues
1. **Artist Not Saving**: Check authentication token
2. **Edit Page Not Loading**: Verify artist ID and permissions
3. **Validation Errors**: Check required fields and URL formats
4. **Access Denied**: Ensure proper authentication

### Debug Steps
1. Check browser console for errors
2. Verify authentication tokens
3. Check network requests in developer tools
4. Verify backend API responses

## Future Enhancements

### Planned Features
- Bulk artist operations
- Artist import/export functionality
- Advanced search and filtering
- Artist collaboration features
- Enhanced analytics and reporting

### Technical Improvements
- Real-time updates
- Offline support
- Enhanced caching
- Performance optimizations

---

*Last updated: October 25, 2024*
*Version: 1.0*

