import asyncio
import os
import time
import logging
from datetime import datetime

import rx
from rx.scheduler.eventloop import AsyncIOScheduler
import rx.operators as ops

from .http_client import HttpClient
from .models import ProcessRequest, ProcessResponse

logger = logging.getLogger(__name__)

def _now_iso() -> str:
    return datetime.utcnow().isoformat()

async def process_events(
    req: ProcessRequest,
    http: HttpClient,
    batch_concurrency: int,
    item_concurrency: int,
) -> ProcessResponse:
    start_ns = time.time_ns()
    host = os.uname().nodename if hasattr(os, "uname") else os.getenv("HOSTNAME", "unknown")

    logger.info(f"Starting RxPY processing: count={req.count}, batch={req.batch}, ioDelayMs={req.io_delay_ms}, downstreamURL={req.downstream_url}")

    loop = asyncio.get_running_loop()
    sched = AsyncIOScheduler(loop=loop)

    seed = {"processed": 0, "ok": 0}

    item_sem = asyncio.Semaphore(item_concurrency)
    batch_sem = asyncio.Semaphore(batch_concurrency)
    
    async def process_item(_v):
        async with item_sem:
            return await http.call_downstream(req.downstream_url, req.io_delay_ms)
    
    async def process_batch(batch):
        async with batch_sem:
            tasks = [asyncio.create_task(process_item(item)) for item in batch]
            results = await asyncio.gather(*tasks)
            return results
    
    source = rx.from_iterable(range(1, req.count + 1), scheduler=sched).pipe(
        ops.map(lambda i: i * 2),
        ops.filter(lambda i: i % 2 == 0),
        ops.buffer_with_count(req.batch),
        ops.flat_map(lambda batch: rx.from_future(asyncio.ensure_future(process_batch(batch)))),
        ops.flat_map(lambda results: rx.from_iterable(results)),
        ops.reduce(
            lambda acc, ok: {
                "processed": acc["processed"] + 1,
                "ok": acc["ok"] + (1 if ok else 0)
            },
            seed
        )
    )

    fut = loop.create_future()
    
    def _on_next(val): 
        if not fut.done(): 
            fut.set_result(val)
    
    def _on_error(err):
        if not fut.done(): 
            fut.set_exception(err)
    
    def _on_completed():
        if not fut.done(): 
            fut.set_result(seed)

    subscription = source.subscribe(
        on_next=_on_next, 
        on_error=_on_error, 
        on_completed=_on_completed, 
        scheduler=sched
    )
    
    try:
        acc = await fut
    finally:
        subscription.dispose()

    processed = int(acc["processed"])
    ok_calls = int(acc["ok"])
    fail_calls = processed - ok_calls

    duration_ms = int((time.time_ns() - start_ns) / 1_000_000)
    eps = (processed * 1000.0 / duration_ms) if duration_ms > 0 else 0.0
    batches = (processed + req.batch - 1) // req.batch

    logger.info(f"RxPY processing completed: processedEvents={processed}, externalCallsOk={ok_calls}, externalCallsFail={fail_calls}, durationMs={duration_ms}, eventsPerSec={eps}")

    return ProcessResponse(
        ok=fail_calls == 0,
        count=req.count,
        batch=req.batch,
        io_delay_ms=req.io_delay_ms,
        downstream_url=req.downstream_url,
        processed_events=processed,
        batches=batches,
        external_calls_ok=ok_calls,
        external_calls_fail=fail_calls,
        duration_ms=duration_ms,
        events_per_sec=eps,
        ts=_now_iso(),
        node=host,
        notes="rxpy reactive",
    )

