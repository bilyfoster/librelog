"""
Middleware package for LibreLog API
"""

# Import from main middleware.py file (note: this is a file, not a package)
import sys
import importlib.util

# Load middleware.py as a module
spec = importlib.util.spec_from_file_location("backend.middleware_module", "/app/backend/middleware.py")
middleware_module = importlib.util.module_from_spec(spec)
sys.modules["backend.middleware_module"] = middleware_module
spec.loader.exec_module(middleware_module)

# Import classes from the module
AuthMiddleware = middleware_module.AuthMiddleware
LoggingMiddleware = middleware_module.LoggingMiddleware
RequestSizeLimitMiddleware = middleware_module.RequestSizeLimitMiddleware

# Import from submodules
from backend.middleware.rate_limit import RateLimitMiddleware
from backend.middleware.security_headers import SecurityHeadersMiddleware

__all__ = [
    "AuthMiddleware",
    "LoggingMiddleware",
    "RequestSizeLimitMiddleware",
    "RateLimitMiddleware",
    "SecurityHeadersMiddleware",
]

