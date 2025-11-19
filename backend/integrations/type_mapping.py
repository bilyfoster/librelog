"""
Type mapping utility for compatibility between LibreTime and LibreLog track types
"""

# Mapping from LibreTime Library codes to LibreLog Track types
LIBRETIME_TO_LIBRELOG = {
    "MUS": "MUS",  # Music
    "ADV": "ADV",  # Advertisement
    "PSA": "PSA",  # Public Service Announcement
    "LIN": "LIN",  # Liner
    "INT": "INT",  # Interstitial
    "PRO": "PRO",  # Promo
    "SHO": "SHO",  # Show
    "IDS": "IDS",  # ID/Station ID
    "COM": "COM",  # Commercial
    "NEW": "NEW",  # News
    "VOT": "VOT",  # Voice Over Track
}

# Reverse mapping (LibreLog to LibreTime)
LIBRELOG_TO_LIBRETIME = {v: k for k, v in LIBRETIME_TO_LIBRELOG.items()}

# All valid LibreLog track types
VALID_LIBRELOG_TYPES = set(LIBRELOG_TO_LIBRETIME.keys())

# All valid LibreTime library codes
VALID_LIBRETIME_CODES = set(LIBRETIME_TO_LIBRELOG.keys())


def map_libretime_to_librelog(libretime_code: str) -> str:
    """
    Map LibreTime library code to LibreLog track type
    
    Args:
        libretime_code: LibreTime library code (e.g., "MUS", "VOT")
        
    Returns:
        LibreLog track type
        
    Raises:
        ValueError: If the code is not recognized
    """
    if libretime_code not in LIBRETIME_TO_LIBRELOG:
        raise ValueError(f"Unknown LibreTime library code: {libretime_code}")
    return LIBRETIME_TO_LIBRELOG[libretime_code]


def map_librelog_to_libretime(librelog_type: str) -> str:
    """
    Map LibreLog track type to LibreTime library code
    
    Args:
        librelog_type: LibreLog track type (e.g., "MUS", "VOT")
        
    Returns:
        LibreTime library code
        
    Raises:
        ValueError: If the type is not recognized
    """
    if librelog_type not in LIBRELOG_TO_LIBRETIME:
        raise ValueError(f"Unknown LibreLog track type: {librelog_type}")
    return LIBRELOG_TO_LIBRETIME[librelog_type]


def is_valid_librelog_type(track_type: str) -> bool:
    """Check if a track type is valid for LibreLog"""
    return track_type in VALID_LIBRELOG_TYPES


def is_valid_libretime_code(code: str) -> bool:
    """Check if a library code is valid for LibreTime"""
    return code in VALID_LIBRETIME_CODES

