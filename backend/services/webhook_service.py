"""
Webhook service for sending notifications to external services
"""

import json
import hmac
import hashlib
import httpx
from typing import List, Dict, Any, Optional
from datetime import datetime
import structlog

from backend.models.webhook import Webhook, WebhookEvent, WebhookType
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update

logger = structlog.get_logger()


class WebhookService:
    """Service for managing and triggering webhooks"""

    @staticmethod
    async def trigger_webhook(
        db: AsyncSession,
        webhook: Webhook,
        event: WebhookEvent,
        payload: Dict[str, Any]
    ) -> bool:
        """
        Trigger a webhook for a specific event
        
        Args:
            db: Database session
            webhook: Webhook instance
            event: Event type
            payload: Payload to send
            
        Returns:
            True if successful, False otherwise
        """
        # Check if webhook is active and subscribes to this event
        if not webhook.active:
            return False
        
        if event.value not in webhook.events:
            return False

        # Prepare webhook payload
        webhook_payload = {
            "event": event.value,
            "timestamp": datetime.utcnow().isoformat(),
            "data": payload
        }

        # Add HMAC signature if secret is configured
        headers = webhook.headers or {}
        if webhook.secret:
            signature = WebhookService._generate_signature(
                json.dumps(webhook_payload, sort_keys=True),
                webhook.secret
            )
            headers["X-Webhook-Signature"] = f"sha256={signature}"

        # Format payload based on webhook type
        formatted_payload = WebhookService._format_payload(webhook.webhook_type, webhook_payload)

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(
                    webhook.url,
                    json=formatted_payload,
                    headers={
                        "Content-Type": "application/json",
                        "User-Agent": "LibreLog/1.0",
                        **headers
                    }
                )
                response.raise_for_status()

            # Update webhook status
            await db.execute(
                update(Webhook)
                .where(Webhook.id == webhook.id)
                .values(
                    last_triggered_at=datetime.utcnow(),
                    error_count=0,
                    last_error=None
                )
            )
            await db.commit()

            logger.info(
                "webhook_triggered",
                webhook_id=webhook.id,
                event=event.value,
                status_code=response.status_code
            )
            return True

        except Exception as e:
            error_msg = str(e)
            logger.error(
                "webhook_failed",
                webhook_id=webhook.id,
                event=event.value,
                error=error_msg,
                exc_info=True
            )

            # Update webhook error status
            await db.execute(
                update(Webhook)
                .where(Webhook.id == webhook.id)
                .values(
                    last_error=error_msg,
                    error_count=webhook.error_count + 1
                )
            )
            await db.commit()
            return False

    @staticmethod
    async def trigger_event(
        db: AsyncSession,
        event: WebhookEvent,
        payload: Dict[str, Any]
    ) -> int:
        """
        Trigger all active webhooks for a specific event
        
        Args:
            db: Database session
            event: Event type
            payload: Payload to send
            
        Returns:
            Number of webhooks triggered
        """
        # Find all active webhooks that subscribe to this event
        result = await db.execute(
            select(Webhook).where(
                Webhook.active == True,
                Webhook.events.contains([event.value])
            )
        )
        webhooks = result.scalars().all()

        triggered_count = 0
        for webhook in webhooks:
            if await WebhookService.trigger_webhook(db, webhook, event, payload):
                triggered_count += 1

        return triggered_count

    @staticmethod
    def _format_payload(webhook_type: WebhookType, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Format payload for specific webhook type"""
        if webhook_type == WebhookType.DISCORD:
            # Discord expects embeds format
            return {
                "embeds": [{
                    "title": payload.get("event", "Event"),
                    "description": json.dumps(payload.get("data", {}), indent=2),
                    "timestamp": payload.get("timestamp"),
                    "color": 0x3498db  # Blue
                }]
            }
        elif webhook_type == WebhookType.SLACK:
            # Slack expects blocks format
            return {
                "text": payload.get("event", "Event"),
                "blocks": [
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": f"*{payload.get('event', 'Event')}*\n```{json.dumps(payload.get('data', {}), indent=2)}```"
                        }
                    }
                ]
            }
        else:
            # Custom - return as-is
            return payload

    @staticmethod
    def _generate_signature(payload: str, secret: str) -> str:
        """Generate HMAC SHA256 signature for webhook payload"""
        return hmac.new(
            secret.encode('utf-8'),
            payload.encode('utf-8'),
            hashlib.sha256
        ).hexdigest()

    @staticmethod
    async def verify_signature(payload: str, signature: str, secret: str) -> bool:
        """Verify webhook signature"""
        expected_signature = WebhookService._generate_signature(payload, secret)
        return hmac.compare_digest(expected_signature, signature.replace("sha256=", ""))

