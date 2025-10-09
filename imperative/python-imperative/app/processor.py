import asyncio
import time
from typing import List
from app.models import ProcessRequest, ProcessResponse
from app.http_client import HttpClient


def append_delay_param(url: str, delay_ms: int) -> str:
    """Append delay_ms parameter to URL"""
    separator = "&" if "?" in url else "?"
    return f"{url}{separator}delay_ms={delay_ms}"


async def process_events(request: ProcessRequest, http_client: HttpClient, concurrency: int) -> ProcessResponse:
    """Process events imperatively using asyncio.gather"""
    start_time = time.time()

    # Generate and transform items
    items = [i * 2 for i in range(1, request.count + 1) if (i * 2) % 2 == 0]
    
    processed_events = len(items)
    batches = (processed_events + request.batch - 1) // request.batch

    # Prepare URL
    url = append_delay_param(
        request.get_downstream_url(),
        request.get_io_delay_ms()
    )

    # Create tasks with concurrency control using semaphore
    semaphore = asyncio.Semaphore(concurrency)
    
    async def call_with_semaphore() -> bool:
        async with semaphore:
            return await http_client.call_with_retry(url)
    
    # Execute all tasks concurrently with controlled concurrency
    results = await asyncio.gather(*[call_with_semaphore() for _ in items])
    
    # Count successes and failures
    external_calls_ok = sum(1 for r in results if r)
    external_calls_fail = sum(1 for r in results if not r)

    duration_ms = int((time.time() - start_time) * 1000)

    return ProcessResponse.success(
        request=request,
        processed_events=processed_events,
        batches=batches,
        external_calls_ok=external_calls_ok,
        external_calls_fail=external_calls_fail,
        duration_ms=duration_ms,
    )

