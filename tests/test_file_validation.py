"""
Tests for file upload validation
"""

import pytest
from fastapi import UploadFile
from io import BytesIO
from backend.utils.file_validation import (
    validate_audio_file,
    validate_image_file,
    validate_document_file,
    validate_file_size,
    validate_mime_type,
    MAX_AUDIO_SIZE,
    MAX_IMAGE_SIZE,
    MAX_DOCUMENT_SIZE,
    ALLOWED_AUDIO_TYPES,
    ALLOWED_IMAGE_TYPES,
    ALLOWED_DOCUMENT_TYPES
)
from fastapi import HTTPException


def create_mock_file(content: bytes, filename: str, content_type: str) -> UploadFile:
    """Create a mock UploadFile for testing"""
    file_obj = UploadFile(
        filename=filename,
        file=BytesIO(content)
    )
    # Set content type and size manually since UploadFile doesn't accept them in constructor
    file_obj.content_type = content_type
    file_obj.size = len(content)
    return file_obj


def test_validate_audio_file_size():
    """Test audio file size validation"""
    # Valid size
    small_file = create_mock_file(b"x" * 1024, "test.mp3", "audio/mpeg")
    validate_audio_file(small_file)  # Should not raise
    
    # Too large
    large_file = create_mock_file(b"x" * (MAX_AUDIO_SIZE + 1), "test.mp3", "audio/mpeg")
    with pytest.raises(HTTPException) as exc_info:
        validate_audio_file(large_file)
    assert exc_info.value.status_code == 413


def test_validate_audio_file_mime_type():
    """Test audio file MIME type validation"""
    # Valid MIME type
    valid_file = create_mock_file(b"test", "test.mp3", "audio/mpeg")
    validate_audio_file(valid_file)  # Should not raise
    
    # Invalid MIME type
    invalid_file = create_mock_file(b"test", "test.txt", "text/plain")
    with pytest.raises(HTTPException) as exc_info:
        validate_audio_file(invalid_file)
    assert exc_info.value.status_code == 400


def test_validate_image_file():
    """Test image file validation"""
    # Valid image
    valid_image = create_mock_file(b"test", "test.jpg", "image/jpeg")
    validate_image_file(valid_image)  # Should not raise
    
    # Too large
    large_image = create_mock_file(b"x" * (MAX_IMAGE_SIZE + 1), "test.jpg", "image/jpeg")
    with pytest.raises(HTTPException) as exc_info:
        validate_image_file(large_image)
    assert exc_info.value.status_code == 413


def test_validate_document_file():
    """Test document file validation"""
    # Valid document
    valid_doc = create_mock_file(b"test", "test.pdf", "application/pdf")
    validate_document_file(valid_doc)  # Should not raise
    
    # Invalid MIME type
    invalid_doc = create_mock_file(b"test", "test.exe", "application/x-msdownload")
    with pytest.raises(HTTPException) as exc_info:
        validate_document_file(invalid_doc)
    assert exc_info.value.status_code == 400


def test_validate_file_size_helper():
    """Test file size validation helper"""
    # Valid size
    small_file = create_mock_file(b"x" * 1024, "test.txt", "text/plain")
    validate_file_size(small_file, MAX_DOCUMENT_SIZE, "Document")  # Should not raise
    
    # Too large
    large_file = create_mock_file(b"x" * (MAX_DOCUMENT_SIZE + 1), "test.txt", "text/plain")
    with pytest.raises(HTTPException) as exc_info:
        validate_file_size(large_file, MAX_DOCUMENT_SIZE, "Document")
    assert exc_info.value.status_code == 413


def test_validate_mime_type_helper():
    """Test MIME type validation helper"""
    # Valid type
    valid_file = create_mock_file(b"test", "test.jpg", "image/jpeg")
    validate_mime_type(valid_file, ALLOWED_IMAGE_TYPES, "image")  # Should not raise
    
    # Invalid type
    invalid_file = create_mock_file(b"test", "test.exe", "application/x-msdownload")
    with pytest.raises(HTTPException) as exc_info:
        validate_mime_type(invalid_file, ALLOWED_IMAGE_TYPES, "image")
    assert exc_info.value.status_code == 400

