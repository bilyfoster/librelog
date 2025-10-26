from sqlalchemy.orm import Session
from app.core.config import settings
from app.models.isrc import ISRC
from app.models.submission import Submission
from app.models.admin_user import AdminUser
from datetime import datetime
import uuid


class ISRCGenerator:
    def __init__(self, db: Session):
        self.db = db
        self._load_config()
    
    def _load_config(self):
        """Load ISRC configuration from database"""
        from app.models.system_config import SystemConfig
        config = self.db.query(SystemConfig).first()
        if config:
            self.country_code = config.isrc_country_code
            self.registrant_code = config.isrc_registrant_code
        else:
            # Fallback to settings if no database config
            self.country_code = settings.isrc_country
            self.registrant_code = settings.isrc_registrant

    def generate_isrc(self, submission_id: str, assigned_by: str) -> ISRC:
        """Generate a new ISRC code for a submission"""
        current_year = datetime.now().year
        
        # Get or create sequence counter for current year
        from sqlalchemy import text
        result = self.db.execute(text("""
            INSERT INTO isrc_sequence (year, current_sequence) 
            VALUES (:year, 0) 
            ON CONFLICT (year) 
            DO UPDATE SET current_sequence = isrc_sequence.current_sequence + 1
            RETURNING current_sequence
        """), {"year": current_year})
        
        sequence_number = result.scalar()
        
        # Generate ISRC code (proper format: CC-XXX-YY-NNNNN = 12 characters)
        year_suffix = str(current_year)[-2:]  # Last 2 digits of year
        isrc_code = f"{self.country_code}{self.registrant_code}{year_suffix}{sequence_number:05d}"
        
        # Create ISRC record
        isrc = ISRC(
            submission_id=submission_id,
            isrc_code=isrc_code,
            country_code=self.country_code,
            registrant_code=self.registrant_code,
            year=current_year,
            sequence_number=sequence_number,
            assigned_by=assigned_by
        )
        
        self.db.add(isrc)
        
        # Update submission status
        submission = self.db.query(Submission).filter(Submission.id == submission_id).first()
        if submission:
            submission.status = "isrc_assigned"
            submission.reviewed_at = datetime.utcnow()
            submission.reviewed_by = assigned_by
        
        self.db.commit()
        return isrc

    def get_isrc_by_submission(self, submission_id: str) -> ISRC:
        """Get ISRC for a submission"""
        return self.db.query(ISRC).filter(ISRC.submission_id == submission_id).first()

    def validate_isrc_format(self, isrc_code: str) -> bool:
        """Validate ISRC code format"""
        if not isrc_code:
            return False
        
        # Format: CC-XXX-YY-NNNNN
        # CC = 2 character country code
        # XXX = 3 character registrant code  
        # YY = 2 digit year
        # NNNNN = 5 digit sequence number
        
        parts = isrc_code.split('-')
        if len(parts) != 4:
            return False
        
        country, registrant, year, sequence = parts
        
        if len(country) != 2 or len(registrant) != 3 or len(year) != 2 or len(sequence) != 5:
            return False
        
        try:
            int(sequence)  # Sequence must be numeric
            int(year)  # Year must be numeric
        except ValueError:
            return False
        
        return True

    def get_isrc_stats(self) -> dict:
        """Get ISRC generation statistics"""
        from sqlalchemy import func
        
        current_year = datetime.now().year
        
        # Get total ISRCs generated this year
        total_this_year = self.db.query(ISRC).filter(ISRC.year == current_year).count()
        
        # Get total ISRCs generated all time
        total_all_time = self.db.query(ISRC).count()
        
        # Get next sequence number
        from sqlalchemy import text
        result = self.db.execute(text("""
            SELECT current_sequence FROM isrc_sequence WHERE year = :year
        """), {"year": current_year})
        
        next_sequence = result.scalar() or 0
        
        return {
            "current_year": current_year,
            "total_this_year": total_this_year,
            "total_all_time": total_all_time,
            "next_sequence": next_sequence + 1,
            "country_code": self.country_code,
            "registrant_code": self.registrant_code
        }


# Create a simple generator function for backward compatibility
def generate_isrc_code() -> str:
    """Generate a simple ISRC code without database dependency"""
    from app.core.config import settings
    current_year = datetime.now().year
    year_suffix = str(current_year)[-2:]  # Last 2 digits of year
    
    # Simple sequence based on timestamp
    import time
    sequence = int(time.time()) % 100000  # 5 digit sequence
    
    # Generate ISRC code (proper format: CC-XXX-YY-NNNNN)
    return f"{settings.isrc_country}-{settings.isrc_registrant}-{year_suffix}-{sequence:05d}"


# For backward compatibility
isrc_generator = None  # Will be initialized when needed
