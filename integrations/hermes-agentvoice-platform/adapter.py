"""Hermes Agent platform plugin for outbound AgentVoice delivery.

Built against Hermes Agent 0.18.2 (f64e4f4f5768c18a53f44890747653bafcab2796).
Android messages enter Hermes through the API server; cron and proactive
output use this plugin to reach the AgentVoice relay webhook.
"""

import os
import uuid
from typing import Any, Dict, List, Optional

try:
    import httpx
    HTTPX_AVAILABLE = True
except ImportError:
    HTTPX_AVAILABLE = False
    httpx = None

from gateway.config import Platform, PlatformConfig
from gateway.platforms.base import BasePlatformAdapter, SendResult


def _settings(config=None) -> tuple[str, str]:
    extra = getattr(config, "extra", {}) or {}
    relay_url = (
        os.getenv("AGENTVOICE_RELAY_URL", "").strip()
        or str(extra.get("relay_url", "")).strip()
    ).rstrip("/")
    token = (
        os.getenv("AGENTVOICE_WEBHOOK_TOKEN", "").strip()
        or str(extra.get("webhook_token", "")).strip()
    )
    return relay_url, token


def check_requirements() -> bool:
    relay_url, token = _settings()
    return HTTPX_AVAILABLE and bool(relay_url and token)


def validate_config(config) -> bool:
    relay_url, token = _settings(config)
    return HTTPX_AVAILABLE and bool(relay_url and token)


def is_connected(config) -> bool:
    return validate_config(config)


def _payload(chat_id: str, message: str) -> Dict[str, Any]:
    return {
        "eventId": f"hermes_{uuid.uuid4().hex}",
        "conversationId": str(chat_id),
        "content": message,
        "source": "cron",
        "metadata": {"deliveryPlatform": "agentvoice"},
    }


async def _post(config, chat_id: str, message: str) -> Dict[str, Any]:
    if not HTTPX_AVAILABLE:
        return {"error": "AgentVoice delivery requires httpx"}
    relay_url, token = _settings(config)
    if not relay_url or not token:
        return {"error": "AGENTVOICE_RELAY_URL and AGENTVOICE_WEBHOOK_TOKEN are required"}
    if not str(chat_id).strip():
        return {"error": "AgentVoice delivery requires a Hermes session ID as chat_id"}

    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            response = await client.post(
                f"{relay_url}/api/webhook/receive",
                headers={"X-Relay-Token": token},
                json=_payload(str(chat_id), message),
            )
        if response.status_code >= 300:
            return {"error": f"AgentVoice relay HTTP {response.status_code}: {response.text[:200]}"}
        body = response.json()
        return {
            "success": True,
            "platform": "agentvoice",
            "chat_id": str(chat_id),
            "message_id": body.get("id") or uuid.uuid4().hex[:12],
        }
    except Exception as exc:
        return {"error": f"AgentVoice delivery failed: {exc}"}


class AgentVoiceAdapter(BasePlatformAdapter):
    """Outbound-only adapter used by live gateway delivery."""

    def __init__(self, config: PlatformConfig):
        super().__init__(config, Platform("agentvoice"))

    async def connect(self, *, is_reconnect: bool = False) -> bool:
        if not validate_config(self.config):
            return False
        self._mark_connected()
        return True

    async def disconnect(self) -> None:
        self._running = False
        self._mark_disconnected()

    async def send(
        self,
        chat_id: str,
        content: str,
        reply_to: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> SendResult:
        result = await _post(self.config, chat_id, content)
        if result.get("success"):
            return SendResult(success=True, message_id=result.get("message_id"))
        return SendResult(success=False, error=str(result.get("error") or "Delivery failed"))

    async def send_typing(self, chat_id: str, metadata=None) -> None:
        return None

    async def get_chat_info(self, chat_id: str) -> Dict[str, Any]:
        return {"name": chat_id, "type": "dm"}


def _env_enablement() -> Optional[Dict[str, Any]]:
    relay_url, token = _settings()
    if not relay_url or not token:
        return None
    seed: Dict[str, Any] = {"relay_url": relay_url, "webhook_token": token}
    home = os.getenv("AGENTVOICE_HOME_CHANNEL", "").strip()
    if home:
        seed["home_channel"] = {"chat_id": home, "name": "AgentVoice"}
    return seed


async def _standalone_send(
    pconfig,
    chat_id: str,
    message: str,
    *,
    thread_id: Optional[str] = None,
    media_files: Optional[List[str]] = None,
    force_document: bool = False,
) -> Dict[str, Any]:
    return await _post(pconfig, chat_id, message)


def register(ctx) -> None:
    ctx.register_platform(
        name="agentvoice",
        label="AgentVoice",
        adapter_factory=lambda cfg: AgentVoiceAdapter(cfg),
        check_fn=check_requirements,
        validate_config=validate_config,
        is_connected=is_connected,
        required_env=["AGENTVOICE_RELAY_URL", "AGENTVOICE_WEBHOOK_TOKEN"],
        install_hint="httpx is already a Hermes dependency",
        env_enablement_fn=_env_enablement,
        cron_deliver_env_var="AGENTVOICE_HOME_CHANNEL",
        standalone_sender_fn=_standalone_send,
        max_message_length=65536,
        emoji="AV",
        pii_safe=True,
        allow_update_command=False,
        platform_hint=(
            "AgentVoice is an Android text and voice client. Use readable markdown, "
            "keep spoken responses natural, and preserve the active Hermes session."
        ),
    )
