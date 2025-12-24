"""
Password validation utilities for enforcing password complexity requirements
"""

import re
from typing import Optional, List
from zxcvbn import zxcvbn


class PasswordValidationError(Exception):
    """Exception raised when password validation fails"""
    pass


def validate_password_complexity(
    password: str,
    min_length: int = 12,
    require_uppercase: bool = True,
    require_lowercase: bool = True,
    require_number: bool = True,
    require_special: bool = True,
    check_common_passwords: bool = True
) -> tuple[bool, Optional[str]]:
    """
    Validate password complexity requirements.
    
    Args:
        password: The password to validate
        min_length: Minimum password length (default: 12)
        require_uppercase: Require at least one uppercase letter
        require_lowercase: Require at least one lowercase letter
        require_number: Require at least one number
        require_special: Require at least one special character
        check_common_passwords: Check against common password lists using zxcvbn
        
    Returns:
        Tuple of (is_valid, error_message)
        - is_valid: True if password meets all requirements
        - error_message: Error message if validation fails, None if valid
    """
    errors: List[str] = []
    
    # Check minimum length
    if len(password) < min_length:
        errors.append(f"Password must be at least {min_length} characters long")
    
    # Check for uppercase letter
    if require_uppercase and not re.search(r'[A-Z]', password):
        errors.append("Password must contain at least one uppercase letter")
    
    # Check for lowercase letter
    if require_lowercase and not re.search(r'[a-z]', password):
        errors.append("Password must contain at least one lowercase letter")
    
    # Check for number
    if require_number and not re.search(r'\d', password):
        errors.append("Password must contain at least one number")
    
    # Check for special character
    if require_special and not re.search(r'[!@#$%^&*()_+\-=\[\]{};\':"\\|,.<>/?]', password):
        errors.append("Password must contain at least one special character")
    
    # Check password strength using zxcvbn
    if check_common_passwords:
        result = zxcvbn(password)
        if result['score'] < 2:  # Score 0-1 is weak, 2-3 is moderate, 4 is strong
            errors.append(
                f"Password is too weak. {result['feedback']['warning'] or 'Choose a stronger password'}"
            )
    
    if errors:
        return False, "; ".join(errors)
    
    return True, None


def validate_password(password: str, min_length: int = 12) -> None:
    """
    Validate password and raise PasswordValidationError if invalid.
    
    Args:
        password: The password to validate
        min_length: Minimum password length (default: 12)
        
    Raises:
        PasswordValidationError: If password does not meet requirements
    """
    is_valid, error_message = validate_password_complexity(password, min_length=min_length)
    if not is_valid:
        raise PasswordValidationError(error_message)


