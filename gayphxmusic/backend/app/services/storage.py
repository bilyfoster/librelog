from minio import Minio
from minio.error import S3Error
from app.core.config import settings
import os
from typing import Optional
import uuid
from datetime import timedelta


class StorageService:
    def __init__(self):
        self.client = Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure
        )
        self.bucket = settings.minio_bucket
        self._ensure_bucket_exists()

    def _ensure_bucket_exists(self):
        """Create bucket if it doesn't exist"""
        try:
            if not self.client.bucket_exists(self.bucket):
                self.client.make_bucket(self.bucket)
        except S3Error as e:
            print(f"Error creating bucket: {e}")

    def generate_presigned_upload_url(self, file_name: str, content_type: str = "audio/mpeg") -> dict:
        """Generate presigned URL for file upload"""
        object_name = f"submissions/{uuid.uuid4()}/{file_name}"
        
        try:
            url = self.client.presigned_put_object(
                self.bucket,
                object_name,
                expires=timedelta(hours=1)  # 1 hour
            )
            # URL is already using the external endpoint
            return {
                "upload_url": url,
                "object_name": object_name,
                "bucket": self.bucket
            }
        except S3Error as e:
            raise Exception(f"Error generating upload URL: {e}")

    def generate_presigned_download_url(self, object_name: str, expires: int = 3600) -> str:
        """Generate presigned URL for file download"""
        try:
            url = self.client.presigned_get_object(
                self.bucket,
                object_name,
                expires=timedelta(seconds=expires)
            )
            # Replace internal MinIO URL with external URL for browser access
            return url.replace('minio:9000', 'localhost:9002')
        except S3Error as e:
            raise Exception(f"Error generating download URL: {e}")

    def get_file_info(self, object_name: str) -> Optional[dict]:
        """Get file metadata"""
        try:
            stat = self.client.stat_object(self.bucket, object_name)
            return {
                "size": stat.size,
                "etag": stat.etag,
                "last_modified": stat.last_modified,
                "content_type": stat.content_type
            }
        except S3Error:
            return None

    def delete_file(self, object_name: str) -> bool:
        """Delete file from storage"""
        try:
            self.client.remove_object(self.bucket, object_name)
            return True
        except S3Error:
            return False

    def upload_file(self, file_path: str, object_name: str, content_type: str = "audio/mpeg") -> bool:
        """Upload file from local path"""
        try:
            self.client.fput_object(
                self.bucket,
                object_name,
                file_path,
                content_type=content_type
            )
            return True
        except S3Error:
            return False

    def upload_file_content(self, file_content: bytes, object_name: str, content_type: str = "audio/mpeg") -> bool:
        """Upload file from bytes content"""
        try:
            from io import BytesIO
            file_obj = BytesIO(file_content)
            self.client.put_object(
                self.bucket,
                object_name,
                file_obj,
                len(file_content),
                content_type=content_type
            )
            return True
        except S3Error:
            return False


# Global instance
storage_service = StorageService()
