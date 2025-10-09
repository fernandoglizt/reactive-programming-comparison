import httpx
import asyncio
from typing import Optional


class HttpClient:
    def __init__(self, timeout_ms: int, max_connections: int, retry_attempts: int):
        self.timeout_ms = timeout_ms
        self.retry_attempts = retry_attempts
        
        limits = httpx.Limits(
            max_connections=max_connections * 2,
            max_keepalive_connections=max_connections,
        )
        
        timeout = httpx.Timeout(
            timeout=timeout_ms / 1000.0,
            connect=timeout_ms / 1000.0,
            read=timeout_ms / 1000.0,
            write=timeout_ms / 1000.0,
        )
        
        self.client: Optional[httpx.AsyncClient] = None
        self._limits = limits
        self._timeout = timeout

    async def start(self):
        """Initialize the HTTP client"""
        self.client = httpx.AsyncClient(
            limits=self._limits,
            timeout=self._timeout,
            http2=False,
        )

    async def close(self):
        """Close the HTTP client"""
        if self.client:
            await self.client.aclose()

    async def call_with_retry(self, url: str) -> bool:
        """Call URL with retry logic"""
        if not self.client:
            return False

        for attempt in range(self.retry_attempts + 1):
            try:
                resp = await self.client.get(url)
                
                # Discard body
                await resp.aread()
                
                if 200 <= resp.status_code < 300:
                    return True
                    
            except Exception:
                pass
            
            # Exponential backoff
            if attempt < self.retry_attempts:
                backoff = min(0.1 * (2 ** attempt), 0.3)
                await asyncio.sleep(backoff)
        
        return False

