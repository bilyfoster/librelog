"""
AudioDeliveryService for delivering audio files to playback systems
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from backend.models.audio_delivery import AudioDelivery, DeliveryMethod, DeliveryStatus
from backend.models.audio_cut import AudioCut
import structlog
import os
import hashlib

logger = structlog.get_logger()

# Try to import delivery libraries
try:
    import paramiko
    PARAMIKO_AVAILABLE = True
except ImportError:
    PARAMIKO_AVAILABLE = False
    logger.warning("paramiko not available, SFTP delivery will not work")

try:
    import pysftp
    PYSFTP_AVAILABLE = True
except ImportError:
    PYSFTP_AVAILABLE = False


class AudioDeliveryService:
    """Service for delivering audio files to playback systems"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def deliver_audio(
        self,
        cut_id: int,
        delivery_method: str,
        target_server: str,
        target_path: Optional[str] = None,
        max_retries: int = 3
    ) -> AudioDelivery:
        """Deliver an audio file to a playback system"""
        cut_result = await self.db.execute(select(AudioCut).where(AudioCut.id == cut_id))
        cut = cut_result.scalar_one_or_none()
        if not cut:
            raise ValueError(f"Cut with id {cut_id} not found")
        
        if not cut.audio_file_path or not os.path.exists(cut.audio_file_path):
            raise ValueError(f"Audio file not found for cut {cut_id}")
        
        # Calculate source checksum
        source_checksum = self._calculate_checksum(cut.audio_file_path)
        
        # Create delivery record
        delivery = AudioDelivery(
            cut_id=cut_id,
            copy_id=cut.copy_id,
            delivery_method=delivery_method,
            target_server=target_server,
            target_path=target_path,
            status=DeliveryStatus.PENDING.value,
            max_retries=max_retries,
            source_checksum=source_checksum
        )
        
        self.db.add(delivery)
        await self.db.flush()
        
        # Attempt delivery
        try:
            await self._perform_delivery(delivery, cut.audio_file_path)
        except Exception as e:
            logger.error("Delivery failed", delivery_id=delivery.id, error=str(e))
            delivery.status = DeliveryStatus.FAILED.value
            delivery.error_message = str(e)
            if delivery.retry_count < delivery.max_retries:
                delivery.status = DeliveryStatus.RETRYING.value
        
        await self.db.commit()
        await self.db.refresh(delivery)
        
        return delivery
    
    async def _perform_delivery(self, delivery: AudioDelivery, source_path: str):
        """Perform the actual delivery"""
        delivery.status = DeliveryStatus.IN_PROGRESS.value
        delivery.delivery_started_at = datetime.now(timezone.utc)
        await self.db.commit()
        
        if delivery.delivery_method == DeliveryMethod.SFTP.value:
            await self._deliver_via_sftp(delivery, source_path)
        elif delivery.delivery_method == DeliveryMethod.RSYNC.value:
            await self._deliver_via_rsync(delivery, source_path)
        elif delivery.delivery_method == DeliveryMethod.API.value:
            await self._deliver_via_api(delivery, source_path)
        else:
            raise ValueError(f"Unsupported delivery method: {delivery.delivery_method}")
        
        # Verify delivery
        if delivery.target_path:
            delivered_checksum = await self._verify_delivery(delivery)
            if delivered_checksum:
                delivery.delivered_checksum = delivered_checksum
                delivery.checksum_verified = (delivered_checksum == delivery.source_checksum)
        
        delivery.status = DeliveryStatus.SUCCESS.value
        delivery.delivery_completed_at = datetime.now(timezone.utc)
    
    async def _deliver_via_sftp(self, delivery: AudioDelivery, source_path: str):
        """Deliver via SFTP"""
        if not PARAMIKO_AVAILABLE:
            raise ValueError("paramiko not available for SFTP delivery")
        
        # Get SFTP credentials from environment or settings
        sftp_user = os.getenv("SFTP_USER")
        sftp_password = os.getenv("SFTP_PASSWORD")
        sftp_key_path = os.getenv("SFTP_KEY_PATH")
        
        if not sftp_user:
            raise ValueError("SFTP credentials not configured")
        
        try:
            transport = paramiko.Transport((delivery.target_server, 22))
            if sftp_key_path and os.path.exists(sftp_key_path):
                private_key = paramiko.RSAKey.from_private_key_file(sftp_key_path)
                transport.connect(username=sftp_user, pkey=private_key)
            else:
                transport.connect(username=sftp_user, password=sftp_password)
            
            sftp = paramiko.SFTPClient.from_transport(transport)
            
            # Ensure target directory exists
            target_dir = os.path.dirname(delivery.target_path) if delivery.target_path else "/"
            try:
                sftp.makedirs(target_dir)
            except:
                pass  # Directory might already exist
            
            # Upload file
            target_file = delivery.target_path or os.path.basename(source_path)
            sftp.put(source_path, target_file)
            
            sftp.close()
            transport.close()
            
            logger.info("SFTP delivery successful", delivery_id=delivery.id, target=target_file)
        except Exception as e:
            raise Exception(f"SFTP delivery failed: {str(e)}")
    
    async def _deliver_via_rsync(self, delivery: AudioDelivery, source_path: str):
        """Deliver via rsync"""
        import subprocess
        
        target = f"{delivery.target_server}:{delivery.target_path or '/'}"
        cmd = ["rsync", "-avz", source_path, target]
        
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
            if result.returncode != 0:
                raise Exception(f"rsync failed: {result.stderr}")
            
            logger.info("rsync delivery successful", delivery_id=delivery.id, target=target)
        except Exception as e:
            raise Exception(f"rsync delivery failed: {str(e)}")
    
    async def _deliver_via_api(self, delivery: AudioDelivery, source_path: str):
        """Deliver via API (e.g., LibreTime)"""
        import httpx
        
        api_url = os.getenv("LIBRETIME_API_URL", f"http://{delivery.target_server}/api/v2")
        
        try:
            async with httpx.AsyncClient() as client:
                with open(source_path, "rb") as f:
                    files = {"file": (os.path.basename(source_path), f, "audio/mpeg")}
                    response = await client.post(
                        f"{api_url}/files/upload",
                        files=files,
                        timeout=300.0
                    )
                    if response.status_code != 200:
                        raise Exception(f"API upload failed: {response.text}")
            
            logger.info("API delivery successful", delivery_id=delivery.id, api_url=api_url)
        except Exception as e:
            raise Exception(f"API delivery failed: {str(e)}")
    
    async def _verify_delivery(self, delivery: AudioDelivery) -> Optional[str]:
        """Verify that file was delivered correctly"""
        # This would need to connect to target server and calculate checksum
        # For now, return None (verification would be implementation-specific)
        return None
    
    async def retry_delivery(self, delivery_id: int) -> AudioDelivery:
        """Retry a failed delivery"""
        result = await self.db.execute(select(AudioDelivery).where(AudioDelivery.id == delivery_id))
        delivery = result.scalar_one_or_none()
        if not delivery:
            raise ValueError(f"Delivery with id {delivery_id} not found")
        
        if delivery.status == DeliveryStatus.SUCCESS.value:
            raise ValueError("Delivery already succeeded")
        
        delivery.retry_count += 1
        delivery.last_retry_at = datetime.now(timezone.utc)
        
        if delivery.retry_count > delivery.max_retries:
            delivery.status = DeliveryStatus.FAILED.value
            await self.db.commit()
            return delivery
        
        # Get cut and retry
        cut_result = await self.db.execute(select(AudioCut).where(AudioCut.id == delivery.cut_id))
        cut = cut_result.scalar_one_or_none()
        
        if cut and cut.audio_file_path:
            try:
                await self._perform_delivery(delivery, cut.audio_file_path)
            except Exception as e:
                delivery.status = DeliveryStatus.FAILED.value
                delivery.error_message = str(e)
        
        await self.db.commit()
        await self.db.refresh(delivery)
        
        return delivery
    
    def _calculate_checksum(self, file_path: str) -> str:
        """Calculate MD5 checksum"""
        hash_md5 = hashlib.md5()
        try:
            with open(file_path, "rb") as f:
                for chunk in iter(lambda: f.read(4096), b""):
                    hash_md5.update(chunk)
            return hash_md5.hexdigest()
        except Exception as e:
            logger.error("Failed to calculate checksum", file_path=file_path, error=str(e))
            return ""

