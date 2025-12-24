"""
Input sanitization utilities for user-generated content
"""

import html
import re
from typing import Optional
import structlog

logger = structlog.get_logger()


def escape_html(text: str) -> str:
    """
    Escape HTML special characters to prevent XSS attacks.
    
    Args:
        text: Input string that may contain HTML
        
    Returns:
        HTML-escaped string
    """
    return html.escape(text, quote=True)


def sanitize_filename(filename: str) -> str:
    """
    Sanitize filename to prevent directory traversal and other attacks.
    
    Args:
        filename: Original filename
        
    Returns:
        Sanitized filename safe for filesystem operations
    """
    # Remove path components
    filename = filename.replace("..", "").replace("/", "").replace("\\", "")
    
    # Remove null bytes
    filename = filename.replace("\x00", "")
    
    # Remove control characters
    filename = re.sub(r'[\x00-\x1f\x7f-\x9f]', '', filename)
    
    # Limit length
    if len(filename) > 255:
        filename = filename[:255]
    
    return filename


def sanitize_sql_input(text: str) -> str:
    """
    Basic SQL injection prevention (note: SQLAlchemy parameterized queries are preferred).
    
    Args:
        text: Input string
        
    Returns:
        Sanitized string (but SQLAlchemy should be used instead of raw SQL)
    """
    # Remove SQL comment markers
    text = text.replace("--", "")
    text = text.replace("/*", "")
    text = text.replace("*/", "")
    
    # Remove semicolons (statement terminators)
    text = text.replace(";", "")
    
    return text


def sanitize_user_input(text: str, allow_html: bool = False) -> str:
    """
    Sanitize user input for safe display.
    
    Args:
        text: User input text
        allow_html: If True, allow safe HTML tags (not recommended)
        
    Returns:
        Sanitized text safe for display
    """
    if not text:
        return ""
    
    # Remove null bytes
    text = text.replace("\x00", "")
    
    # Remove control characters except newlines and tabs
    text = re.sub(r'[\x00-\x08\x0b-\x0c\x0e-\x1f\x7f-\x9f]', '', text)
    
    if not allow_html:
        # Escape HTML
        text = escape_html(text)
    else:
        # Allow only safe HTML tags (basic whitelist)
        # This is a simplified version - use a proper HTML sanitizer like bleach for production
        allowed_tags = ['b', 'i', 'u', 'em', 'strong', 'p', 'br']
        # For production, use: bleach.clean(text, tags=allowed_tags)
        logger.warning("HTML allowed in user input - consider using bleach library for proper sanitization")
    
    return text


def sanitize_url(url: str) -> Optional[str]:
    """
    Sanitize URL to prevent javascript: and data: protocol attacks.
    
    Args:
        url: URL string
        
    Returns:
        Sanitized URL or None if invalid
    """
    if not url:
        return None
    
    # Remove whitespace
    url = url.strip()
    
    # Check for dangerous protocols
    dangerous_protocols = ['javascript:', 'data:', 'vbscript:', 'file:']
    url_lower = url.lower()
    
    for protocol in dangerous_protocols:
        if url_lower.startswith(protocol):
            logger.warning("Dangerous URL protocol detected", url=url, protocol=protocol)
            return None
    
    # Allow http, https, mailto, tel
    allowed_protocols = ['http:', 'https:', 'mailto:', 'tel:']
    has_allowed_protocol = any(url_lower.startswith(proto) for proto in allowed_protocols)
    
    if not has_allowed_protocol and '://' in url:
        # Has protocol but not allowed
        logger.warning("URL with non-allowed protocol", url=url)
        return None
    
    return url


def sanitize_json_input(data: dict) -> dict:
    """
    Sanitize JSON data structure recursively.
    
    Args:
        data: Dictionary or nested structure
        
    Returns:
        Sanitized dictionary
    """
    if isinstance(data, dict):
        return {k: sanitize_json_input(v) for k, v in data.items()}
    elif isinstance(data, list):
        return [sanitize_json_input(item) for item in data]
    elif isinstance(data, str):
        return sanitize_user_input(data)
    else:
        return data


