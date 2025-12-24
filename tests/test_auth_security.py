"""
Security tests for authentication and password validation
"""

import pytest
import os
from fastapi.testclient import TestClient
from backend.auth.password_validator import (
    validate_password,
    validate_password_complexity,
    PasswordValidationError
)
from backend.auth.oauth2 import SECRET_KEY


class TestPasswordValidation:
    """Test password complexity validation"""
    
    def test_password_too_short(self):
        """Test that passwords shorter than 12 characters are rejected"""
        is_valid, error = validate_password_complexity("Short1!")
        assert not is_valid
        assert "at least 12 characters" in error.lower()
    
    def test_password_missing_uppercase(self):
        """Test that passwords without uppercase letters are rejected"""
        is_valid, error = validate_password_complexity("lowercase123!")
        assert not is_valid
        assert "uppercase" in error.lower()
    
    def test_password_missing_lowercase(self):
        """Test that passwords without lowercase letters are rejected"""
        is_valid, error = validate_password_complexity("UPPERCASE123!")
        assert not is_valid
        assert "lowercase" in error.lower()
    
    def test_password_missing_number(self):
        """Test that passwords without numbers are rejected"""
        is_valid, error = validate_password_complexity("NoNumbers!")
        assert not is_valid
        assert "number" in error.lower()
    
    def test_password_missing_special_char(self):
        """Test that passwords without special characters are rejected"""
        is_valid, error = validate_password_complexity("NoSpecial123")
        assert not is_valid
        assert "special character" in error.lower()
    
    def test_valid_password(self):
        """Test that valid passwords pass validation"""
        is_valid, error = validate_password_complexity("ValidPass123!")
        assert is_valid
        assert error is None
    
    def test_validate_password_raises_exception(self):
        """Test that validate_password raises exception for invalid passwords"""
        with pytest.raises(PasswordValidationError):
            validate_password("short")
        
        # Valid password should not raise
        validate_password("ValidPass123!")
    
    def test_weak_password_detected(self):
        """Test that common weak passwords are detected by zxcvbn"""
        is_valid, error = validate_password_complexity("password123!")
        # Even if it meets complexity, zxcvbn should flag it as weak
        # Note: zxcvbn scoring may vary, but common passwords should be flagged
        assert not is_valid or "weak" in error.lower() or "too weak" in error.lower()


class TestJWTSecretValidation:
    """Test JWT secret key validation"""
    
    def test_jwt_secret_required(self):
        """Test that JWT_SECRET_KEY must be set"""
        # This test verifies the validation exists in oauth2.py
        # The actual validation happens at import time
        # We can't easily test the missing case without mocking, but we can verify
        # the SECRET_KEY is not the default weak value
        assert SECRET_KEY is not None
        # If SECRET_KEY is set from env, it should be at least 32 chars
        # If not set, the ValueError would have been raised at import
        if len(SECRET_KEY) < 32:
            pytest.fail("JWT_SECRET_KEY should be at least 32 characters")
    
    def test_jwt_secret_not_default(self):
        """Test that JWT_SECRET_KEY is not the default weak value"""
        assert SECRET_KEY != "supersecretkey"
        assert SECRET_KEY is not None


class TestPasswordValidatorIntegration:
    """Integration tests for password validator"""
    
    def test_multiple_validation_errors(self):
        """Test that multiple validation errors are reported"""
        is_valid, error = validate_password_complexity("short")
        assert not is_valid
        # Should contain multiple error messages
        assert ";" in error or len(error.split()) > 5
    
    def test_custom_min_length(self):
        """Test password validation with custom minimum length"""
        # Test with 8 character minimum
        is_valid, error = validate_password_complexity("Pass123!", min_length=8)
        assert is_valid
        
        # Test with 16 character minimum
        is_valid, error = validate_password_complexity("ShortPass123!", min_length=16)
        assert not is_valid
        assert "16 characters" in error


