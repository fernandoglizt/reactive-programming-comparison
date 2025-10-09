import asyncio
import httpx

class HttpClient:
    def __init__(self, max_conns: int, timeout_ms: int, retry_attempts: int):
        limits = httpx.Limits(
            max_connections=max_conns,
            max_keepalive_connections=max_conns
        )
        timeout = httpx.Timeout(
            connect=timeout_ms/1000,
            read=timeout_ms/1000,
            write=timeout_ms/1000,
            pool=timeout_ms/1000,
        )
        self._client = httpx.AsyncClient(limits=limits, timeout=timeout, http2=False)
        self._retry_attempts = retry_attempts
        self._timeout_ms = timeout_ms

    @staticmethod
    def _append_delay(url: str, delay_ms: int) -> str:
        sep = '&' if '?' in url else '?'
        return f"{url}{sep}delay_ms={delay_ms}"

    async def call_downstream(self, base_url: str, delay_ms: int) -> bool:
        url = self._append_delay(base_url, delay_ms)

        for attempt in range(self._retry_attempts + 1):
            try:
                async with self._client.stream("GET", url) as resp:
                    ok = 200 <= resp.status_code < 300
                    await resp.aclose()
                    if ok:
                        return True
            except Exception:
                pass

            if attempt < self._retry_attempts:
                backoff = min(0.3, 0.1 * (2 ** attempt))
                await asyncio.sleep(backoff)

        return False

    async def aclose(self):
        await self._client.aclose()

