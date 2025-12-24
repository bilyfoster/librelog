"""
Tests for input sanitization utilities
"""

import pytest
from backend.utils.sanitization import (
    escape_html,
    sanitize_filename,
    sanitize_user_input,
    sanitize_url,
    sanitize_json_input
)


def test_escape_html():
    """Test HTML escaping"""
    assert escape_html("<script>alert('xss')</script>") == "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
    assert escape_html("Hello & World") == "Hello &amp; World"
    assert escape_html('"Quote"') == "&quot;Quote&quot;"
    assert escape_html("Normal text") == "Normal text"


def test_sanitize_filename():
    """Test filename sanitization"""
    # Remove path traversal
    assert sanitize_filename("../../../etc/passwd") == "etcpasswd"
    assert sanitize_filename("file/name.txt") == "filename.txt"
    assert sanitize_filename("file\\name.txt") == "filename.txt"
    
    # Remove null bytes
    assert "\x00" not in sanitize_filename("file\x00name.txt")
    
    # Preserve valid filenames
    assert sanitize_filename("valid_file.txt") == "valid_file.txt"
    assert sanitize_filename("my-file_123.txt") == "my-file_123.txt"
    
    # Limit length
    long_name = "a" * 300
    assert len(sanitize_filename(long_name)) == 255


def test_sanitize_user_input():
    """Test user input sanitization"""
    # Remove null bytes
    assert "\x00" not in sanitize_user_input("text\x00with\x00nulls")
    
    # Remove control characters
    assert sanitize_user_input("text\x01\x02\x03text") == "texttext"
    
    # Preserve newlines and tabs
    assert "\n" in sanitize_user_input("line1\nline2")
    assert "\t" in sanitize_user_input("col1\tcol2")
    
    # Escape HTML by default
    assert "&lt;" in sanitize_user_input("<script>")
    assert "&amp;" in sanitize_user_input("&")


def test_sanitize_url():
    """Test URL sanitization"""
    # Allow http/https
    assert sanitize_url("https://example.com") == "https://example.com"
    assert sanitize_url("http://example.com") == "http://example.com"
    
    # Block javascript:
    assert sanitize_url("javascript:alert('xss')") is None
    
    # Block data:
    assert sanitize_url("data:text/html,<script>alert('xss')</script>") is None
    
    # Block file:
    assert sanitize_url("file:///etc/passwd") is None
    
    # Allow mailto and tel
    assert sanitize_url("mailto:test@example.com") == "mailto:test@example.com"
    assert sanitize_url("tel:+1234567890") == "tel:+1234567890"
    
    # Remove whitespace
    assert sanitize_url("  https://example.com  ") == "https://example.com"
    
    # None for empty
    assert sanitize_url("") is None
    assert sanitize_url(None) is None


def test_sanitize_json_input():
    """Test JSON input sanitization"""
    # Simple dict
    data = {"key": "<script>alert('xss')</script>"}
    sanitized = sanitize_json_input(data)
    assert "&lt;script&gt;" in sanitized["key"]
    
    # Nested dict
    data = {
        "user": {
            "name": "<script>alert('xss')</script>",
            "email": "test@example.com"
        }
    }
    sanitized = sanitize_json_input(data)
    assert "&lt;script&gt;" in sanitized["user"]["name"]
    assert sanitized["user"]["email"] == "test@example.com"
    
    # List
    data = ["<script>alert('xss')</script>", "normal text"]
    sanitized = sanitize_json_input(data)
    assert "&lt;script&gt;" in sanitized[0]
    assert sanitized[1] == "normal text"
    
    # Non-string values preserved
    data = {"number": 123, "boolean": True, "null": None}
    sanitized = sanitize_json_input(data)
    assert sanitized["number"] == 123
    assert sanitized["boolean"] is True
    assert sanitized["null"] is None


def test_sanitize_user_input_edge_cases():
    """Test edge cases for user input sanitization"""
    # Empty string
    assert sanitize_user_input("") == ""
    
    # None
    assert sanitize_user_input(None) == ""
    
    # Very long string
    long_string = "a" * 10000
    sanitized = sanitize_user_input(long_string)
    assert len(sanitized) == len(long_string)
    assert sanitized == long_string
    
    # Unicode characters
    assert sanitize_user_input("Hello 世界") == "Hello 世界"
    assert sanitize_user_input("Emoji 🎉") == "Emoji 🎉"


