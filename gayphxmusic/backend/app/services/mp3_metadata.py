import io
import logging
from typing import Dict, Optional, Tuple
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, TIT2, TPE1, TALB, TYER, TCON, TPE2, TCOM, TENC, TSSE, TXXX
from mutagen.id3._util import ID3NoHeaderError

logger = logging.getLogger(__name__)


class MP3MetadataService:
    """Service for reading and writing MP3 metadata tags"""
    
    def __init__(self):
        pass
    
    def read_metadata(self, file_content: bytes) -> Dict[str, str]:
        """Read metadata from MP3 file content"""
        try:
            # Create a file-like object from bytes
            file_obj = io.BytesIO(file_content)
            audio = MP3(file_obj)
            
            metadata = {}
            
            # Read ID3 tags if they exist
            if audio.tags:
                metadata.update({
                    'title': str(audio.tags.get('TIT2', [''])[0]) if 'TIT2' in audio.tags else '',
                    'artist': str(audio.tags.get('TPE1', [''])[0]) if 'TPE1' in audio.tags else '',
                    'album': str(audio.tags.get('TALB', [''])[0]) if 'TALB' in audio.tags else '',
                    'year': str(audio.tags.get('TYER', [''])[0]) if 'TYER' in audio.tags else '',
                    'genre': str(audio.tags.get('TCON', [''])[0]) if 'TCON' in audio.tags else '',
                    'album_artist': str(audio.tags.get('TPE2', [''])[0]) if 'TPE2' in audio.tags else '',
                    'composer': str(audio.tags.get('TCOM', [''])[0]) if 'TCOM' in audio.tags else '',
                    'encoder': str(audio.tags.get('TENC', [''])[0]) if 'TENC' in audio.tags else '',
                    'software': str(audio.tags.get('TSSE', [''])[0]) if 'TSSE' in audio.tags else '',
                })
            
            # Add audio properties
            metadata.update({
                'duration': str(int(audio.info.length)) if audio.info.length else '',
                'bitrate': str(audio.info.bitrate) if audio.info.bitrate else '',
                'sample_rate': str(audio.info.sample_rate) if audio.info.sample_rate else '',
                'channels': str(audio.info.channels) if audio.info.channels else '',
            })
            
            return metadata
            
        except Exception as e:
            logger.error(f"Error reading MP3 metadata: {e}")
            return {}
    
    def write_metadata(self, file_content: bytes, metadata: Dict[str, str]) -> bytes:
        """Write metadata to MP3 file content and return updated bytes"""
        try:
            # Create a file-like object from bytes
            file_obj = io.BytesIO(file_content)
            audio = MP3(file_obj)
            
            # Create or get ID3 tags
            try:
                audio.add_tags()
            except ID3NoHeaderError:
                pass
            
            # Set basic tags
            if metadata.get('title'):
                audio.tags['TIT2'] = TIT2(encoding=3, text=metadata['title'])
            
            if metadata.get('artist'):
                audio.tags['TPE1'] = TPE1(encoding=3, text=metadata['artist'])
            
            if metadata.get('album'):
                audio.tags['TALB'] = TALB(encoding=3, text=metadata['album'])
            
            if metadata.get('year'):
                audio.tags['TYER'] = TYER(encoding=3, text=metadata['year'])
            
            if metadata.get('genre'):
                audio.tags['TCON'] = TCON(encoding=3, text=metadata['genre'])
            
            if metadata.get('album_artist'):
                audio.tags['TPE2'] = TPE2(encoding=3, text=metadata['album_artist'])
            
            if metadata.get('composer'):
                audio.tags['TCOM'] = TCOM(encoding=3, text=metadata['composer'])
            
            # Add custom tags for GayPHX
            if metadata.get('isrc'):
                audio.tags['TXXX:ISRC'] = TXXX(encoding=3, desc='ISRC', text=metadata['isrc'])
            
            if metadata.get('tracking_id'):
                audio.tags['TXXX:TRACKING_ID'] = TXXX(encoding=3, desc='GayPHX Tracking ID', text=metadata['tracking_id'])
            
            if metadata.get('submission_date'):
                audio.tags['TXXX:SUBMISSION_DATE'] = TXXX(encoding=3, desc='GayPHX Submission Date', text=metadata['submission_date'])
            
            # Set encoder info
            audio.tags['TENC'] = TENC(encoding=3, text='GayPHX Music Platform')
            audio.tags['TSSE'] = TSSE(encoding=3, text='GayPHX Music Platform v2.0')
            
            # Save to new bytes object
            output = io.BytesIO()
            audio.save(output)
            return output.getvalue()
            
        except Exception as e:
            logger.error(f"Error writing MP3 metadata: {e}")
            # Return original content if metadata writing fails
            return file_content
    
    def has_metadata(self, file_content: bytes) -> bool:
        """Check if MP3 file has any metadata tags"""
        try:
            file_obj = io.BytesIO(file_content)
            audio = MP3(file_obj)
            return audio.tags is not None and len(audio.tags) > 0
        except Exception as e:
            logger.error(f"Error checking MP3 metadata: {e}")
            return False
    
    def get_audio_properties(self, file_content: bytes) -> Dict[str, any]:
        """Get audio properties from MP3 file"""
        try:
            file_obj = io.BytesIO(file_content)
            audio = MP3(file_obj)
            
            return {
                'duration_seconds': int(audio.info.length) if audio.info.length else 0,
                'bitrate': audio.info.bitrate if audio.info.bitrate else 0,
                'sample_rate': audio.info.sample_rate if audio.info.sample_rate else 0,
                'channels': audio.info.channels if audio.info.channels else 0,
            }
        except Exception as e:
            logger.error(f"Error getting audio properties: {e}")
            return {
                'duration_seconds': 0,
                'bitrate': 0,
                'sample_rate': 0,
                'channels': 0,
            }


# Global instance
mp3_metadata_service = MP3MetadataService()

