# 🎵 GayPHX Music Platform

A complete music submission and ISRC management platform built for the LGBTQ+ community in Phoenix. Artists can submit their tracks, request official ISRC codes, and showcase their work in a public gallery.

## 🌈 Features

### For Artists
- **Artist Management**: Create, edit, and manage multiple artist profiles with rich social links
- **Profile Customization**: Add pronouns, bio, and comprehensive social media integration
- **Streamlined Submissions**: Select from existing artists with auto-filled data
- **Easy Submission**: Upload tracks with drag-and-drop interface (MP3 only)
- **ISRC Assignment**: Get official ISRC codes for approved tracks
- **Artist Dashboard**: Track submissions, play statistics, and download certificates
- **Magic Link Auth**: Secure passwordless login
- **Public Gallery**: Showcase approved tracks (opt-in) with play tracking
- **Social Integration**: Link Instagram, Twitter, Spotify, SoundCloud, Bandcamp, and more
- **Play Analytics**: Track radio plays vs gallery plays with detailed statistics

### For Admins
- **Multi-Admin Support**: Manage multiple admin users with different roles
- **Review Dashboard**: Approve/reject submissions with audio playback
- **ISRC Management**: Generate and assign official codes
- **LibreTime Integration**: Connect to radio automation systems
- **Play Tracking**: Monitor track plays and analytics
- **User Management**: Complete oversight of all artists
- **Export Tools**: CSV/JSON exports for radio integration
- **Analytics**: Track submission statistics and trends

### Technical Features
- **File Storage**: Secure S3-compatible storage with presigned URLs
- **Email Notifications**: Automated confirmations and updates
- **Audio Analysis**: LUFS and peak level checking
- **Rights Management**: PRO affiliation and licensing tracking
- **Play Tracking**: LibreTime integration for radio play monitoring
- **Multi-Admin System**: Role-based admin management
- **Artist Profiles**: Rich artist profile management system
- **Dynamic Configuration**: System-wide configuration management
- **Docker Deployment**: Production-ready containerized setup

## 🛠️ Tech Stack

- **Frontend**: Next.js 14, React, TypeScript, Tailwind CSS
- **Backend**: FastAPI, Python 3.11, SQLAlchemy
- **Database**: PostgreSQL with Alembic migrations
- **Storage**: MinIO (S3-compatible)
- **Email**: SMTP integration
- **Deployment**: Docker Compose with Nginx

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd gayphxmusic
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start the services**
   ```bash
   docker compose up -d
   ```

4. **Initialize the database**
   ```bash
   # Run database migrations
   docker compose exec backend alembic upgrade head
   
   # Create admin user
   docker compose exec backend python scripts/seed-admin.py
   ```

5. **Access the application**
   - Frontend: http://localhost:3000
   - API Docs: http://localhost:8000/docs
   - Admin: http://localhost:3000/admin (admin@gayphx.com / admin123)

## 📁 Project Structure

```
gayphxmusic/
├── backend/                 # FastAPI backend
│   ├── app/
│   │   ├── api/            # API endpoints
│   │   ├── models/         # Database models
│   │   ├── services/       # Business logic
│   │   └── core/           # Configuration
│   ├── alembic/            # Database migrations
│   └── requirements.txt
├── frontend/               # Next.js frontend
│   ├── app/                # App router pages
│   ├── lib/                # API client
│   └── package.json
├── nginx/                  # Reverse proxy config
├── scripts/                # Utility scripts
└── docker-compose.yml      # Service orchestration
```

## 🔧 Configuration

### Environment Variables

```bash
# Database
POSTGRES_PASSWORD=your_secure_password

# MinIO/S3 Storage
MINIO_ACCESS_KEY=your_access_key
MINIO_SECRET_KEY=your_secret_key

# Email
SMTP_URL=smtp://user:pass@smtp.gmail.com:587

# ISRC Configuration
ISRC_COUNTRY=US
ISRC_REGISTRANT=GPHX

# Security
JWT_SECRET=your_super_secret_jwt_key
```

### ISRC Configuration

The platform generates ISRC codes in the format: `CC-XXXYY-NNNNN`
- `CC`: Country code (e.g., US)
- `XXX`: Registrant code (e.g., GPHX)
- `YY`: Year (e.g., 25 for 2025)
- `NNNNN`: Sequential number

## 📊 API Endpoints

### Public Endpoints
- `POST /api/submissions/` - Create submission
- `GET /api/submissions/track/{id}` - Track submission
- `POST /api/auth/request-magic-link` - Request login link

### Artist Management
- `GET /api/artists/` - List artists for current user
- `POST /api/artists/` - Create new artist profile
- `GET /api/artists/{id}` - Get artist details
- `PUT /api/artists/{id}` - Update artist profile
- `DELETE /api/artists/{id}` - Delete artist profile
- `GET /api/artists/dropdown/list` - Get artists for dropdown

### Admin Endpoints
- `POST /api/admin/login` - Admin authentication
- `GET /api/admin/submissions` - List submissions
- `PUT /api/admin/submissions/{id}` - Update submission
- `POST /api/admin/submissions/{id}/assign-isrc` - Assign ISRC
- `GET /api/admin/admins` - List admin users
- `POST /api/admin/admins` - Create admin user
- `GET /api/admin/profile` - Get admin profile
- `PUT /api/admin/profile` - Update admin profile

### LibreTime Integration
- `GET /api/plays/libretime-config` - Get LibreTime configuration
- `POST /api/plays/libretime-config` - Save LibreTime configuration
- `POST /api/plays/libretime-validate` - Validate API key
- `POST /api/plays/sync-libretime` - Sync with LibreTime
- `GET /api/plays/statistics` - Get play statistics
- `GET /api/plays/recent-plays` - Get recent plays

### Export Endpoints
- `GET /api/exports/csv` - Download catalog CSV
- `GET /api/exports/json` - JSON feed for LibreTime
- `GET /api/exports/libretime` - LibreTime-compatible format

## 🎯 Usage

### For Artists

1. **Create Artist Profile**
   - Go to `/artists` to manage your artist profiles
   - Create rich profiles with social links and bio
   - Set up default information for submissions

2. **Submit Music**
   - Visit `/submit-new` for the enhanced submission form
   - Select from your existing artist profiles
   - Artist information auto-fills from your profile
   - Upload your track (MP3, WAV, M4A, FLAC)
   - Request ISRC code if needed
   - Submit for review

3. **Track Submissions**
   - Use the tracking ID from confirmation email
   - Or login to your artist dashboard
   - Monitor approval status and ISRC assignment

4. **Download Certificates**
   - Access your dashboard after ISRC assignment
   - Download official ISRC certificates
   - Export your catalog

### For Admins

1. **Manage System**
   - Access admin dashboard at `/admin`
   - Manage multiple admin users with different roles
   - Configure LibreTime integration
   - Monitor play tracking and analytics

2. **Review Submissions**
   - Listen to submitted tracks
   - Approve, reject, or request more info
   - Add admin notes
   - Track submission status

3. **Assign ISRC Codes**
   - For approved tracks with ISRC requests
   - Codes are automatically generated
   - Artists receive email notifications

4. **Monitor Analytics**
   - View play tracking data from LibreTime
   - Monitor submission statistics
   - Track user activity and engagement

5. **Export Data**
   - Download CSV catalogs
   - Generate JSON feeds for radio automation
   - View comprehensive analytics

## 🔒 Security

- JWT-based authentication
- Magic link login for artists
- Presigned URLs for file uploads
- Input validation and sanitization
- CORS configuration
- Rate limiting on API endpoints

## 🚀 Deployment

### Production Checklist

1. **Environment Setup**
   - [ ] Update all environment variables
   - [ ] Set strong passwords and secrets
   - [ ] Configure SMTP settings
   - [ ] Set up ISRC registrant code

2. **Database**
   - [ ] Run migrations: `alembic upgrade head`
   - [ ] Create admin user
   - [ ] Set up database backups

3. **Storage**
   - [ ] Configure MinIO or S3 bucket
   - [ ] Set up file retention policies
   - [ ] Test file upload/download

4. **Email**
   - [ ] Configure SMTP settings
   - [ ] Test email delivery
   - [ ] Set up email templates

5. **SSL/TLS**
   - [ ] Configure SSL certificates
   - [ ] Update nginx configuration
   - [ ] Test HTTPS endpoints

6. **Monitoring**
   - [ ] Set up health checks
   - [ ] Configure logging
   - [ ] Monitor disk space and performance

## 📚 Documentation

- [System Status](SYSTEM_STATUS.md) - Current system status and known issues
- [Testing Guide](TESTING.md) - How to test the platform
- [ISRC Compliance Guide](ISRC_COMPLIANCE_GUIDE.md) - ISRC code management
- [Admin Features Summary](ADMIN_FEATURES_SUMMARY.md) - Admin functionality overview
- [Artist Management Guide](ARTIST_MANAGEMENT_GUIDE.md) - Complete guide to artist profile management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions or support, contact:
- Email: music@gayphx.com
- Website: https://music.gayphx.com

## 🙏 Acknowledgments

Built with love for the LGBTQ+ community in Phoenix. Special thanks to all the artists who make this platform possible.

---

**GayPHX Music Platform** - Empowering queer artists, one track at a time. 🌈🎵