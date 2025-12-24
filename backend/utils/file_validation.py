"""
File upload validation utilities
"""

from fastapi import UploadFile, HTTPException, status
from typing import Optional
import structlog

logger = structlog.get_logger()

# File size limits in bytes
MAX_AUDIO_SIZE = 50 * 1024 * 1024  # 50MB
MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5MB
MAX_DOCUMENT_SIZE = 10 * 1024 * 1024  # 10MB

# Allowed MIME types
ALLOWED_AUDIO_TYPES = [
    "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav",
    "audio/ogg", "audio/vorbis", "audio/flac", "audio/aac", "audio/mp4"
]

ALLOWED_IMAGE_TYPES = [
    "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
]

ALLOWED_DOCUMENT_TYPES = [
    "application/pdf", "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain", "text/csv"
]


def validate_file_size(file: UploadFile, max_size: int, file_type: str = "file") -> None:
    """
    Validate file size.
    
    Args:
        file: UploadFile object
        max_size: Maximum file size in bytes
        file_type: Type of file for error message
        
    Raises:
        HTTPException: If file size exceeds limit
    """
    # Check content-length header if available
    content_length = file.size if hasattr(file, 'size') else None
    
    if content_length and content_length > max_size:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"{file_type} file too large. Maximum size: {max_size / (1024*1024):.0f}MB"
        )
    
    # Note: For multipart uploads, we may need to check size during read
    # This is a basic check - full validation happens during file processing


def validate_mime_type(file: UploadFile, allowed_types: list[str], file_type: str = "file") -> None:
    """
    Validate MIME type.
    
    Args:
        file: UploadFile object
        allowed_types: List of allowed MIME types
        file_type: Type of file for error message
        
    Raises:
        HTTPException: If MIME type is not allowed
    """
    if file.content_type and file.content_type not in allowed_types:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid {file_type} type. Allowed types: {', '.join(allowed_types)}"
        )


def validate_audio_file(file: UploadFile) -> None:
    """Validate audio file upload"""
    validate_file_size(file, MAX_AUDIO_SIZE, "Audio")
    validate_mime_type(file, ALLOWED_AUDIO_TYPES, "audio")


def validate_image_file(file: UploadFile) -> None:
    """Validate image file upload"""
    validate_file_size(file, MAX_IMAGE_SIZE, "Image")
    validate_mime_type(file, ALLOWED_IMAGE_TYPES, "image")


def validate_document_file(file: UploadFile) -> None:
    """Validate document file upload"""
    validate_file_size(file, MAX_DOCUMENT_SIZE, "Document")
    validate_mime_type(file, ALLOWED_DOCUMENT_TYPES, "document")


