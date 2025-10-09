import os
import sys
import logging
from datetime import datetime

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from .models import ProcessRequest, ProcessResponse, HealthResponse, InfoResponse
from .http_client import HttpClient
from .pipeline_rx import process_events

PORT = int(os.getenv("PORT", "8084"))
MAX_COUNT = int(os.getenv("MAX_COUNT", "200000"))
BATCH_CONCURRENCY = int(os.getenv("BATCH_CONCURRENCY", "4"))
ITEM_CONCURRENCY = int(os.getenv("ITEM_CONCURRENCY", "64"))
DOWNSTREAM_TIMEOUT_MS = int(os.getenv("DOWNSTREAM_TIMEOUT_MS", "2000"))
RETRY_ATTEMPTS = int(os.getenv("RETRY_ATTEMPTS", "1"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

logging.basicConfig(
    level=getattr(logging, LOG_LEVEL.upper(), logging.INFO),
    format='{"time":"%(asctime)s","level":"%(levelname)s","message":"%(message)s"}',
)

logger = logging.getLogger(__name__)

app = FastAPI()
http_client = None

@app.on_event("startup")
async def startup():
    global http_client
    http_client = HttpClient(
        max_conns=ITEM_CONCURRENCY,
        timeout_ms=DOWNSTREAM_TIMEOUT_MS,
        retry_attempts=RETRY_ATTEMPTS,
    )
    logger.info(f"Starting python-rxpy service: port={PORT}, maxCount={MAX_COUNT}, batchConcurrency={BATCH_CONCURRENCY}, itemConcurrency={ITEM_CONCURRENCY}, downstreamTimeoutMs={DOWNSTREAM_TIMEOUT_MS}, retryAttempts={RETRY_ATTEMPTS}")

@app.on_event("shutdown")
async def shutdown():
    if http_client:
        await http_client.aclose()
    logger.info("python-rxpy service stopped")

@app.get("/healthz")
async def healthz():
    return JSONResponse(HealthResponse().model_dump())

@app.get("/info")
async def info():
    return JSONResponse(InfoResponse(
        pyVersion=sys.version.split()[0],
        buildTime=datetime.utcnow().isoformat(),
        maxCount=MAX_COUNT,
    ).model_dump())

@app.post("/process", response_model=ProcessResponse)
async def process(payload: ProcessRequest):
    logger.info(f"Processing request: count={payload.count}, batch={payload.batch}, ioDelayMs={payload.io_delay_ms}, downstreamUrl={payload.downstream_url}")
    
    if payload.count <= 0:
        logger.warning(f"Invalid count: {payload.count}")
        raise HTTPException(status_code=400, detail="count must be > 0")
    
    if payload.batch <= 0:
        logger.warning(f"Invalid batch: {payload.batch}")
        raise HTTPException(status_code=400, detail="batch must be > 0")
    
    if payload.count > MAX_COUNT:
        logger.warning(f"Count exceeds maximum: {payload.count} > {MAX_COUNT}")
        raise HTTPException(status_code=400, detail=f"count exceeds maximum allowed: {MAX_COUNT}")

    io_delay = max(0, payload.io_delay_ms)
    req = ProcessRequest(
        count=payload.count,
        batch=payload.batch,
        io_delay_ms=io_delay,
        downstream_url=payload.downstream_url or "http://slow-io:8080/slow",
    )
    
    assert http_client is not None
    resp = await process_events(
        req=req,
        http=http_client,
        batch_concurrency=BATCH_CONCURRENCY,
        item_concurrency=ITEM_CONCURRENCY,
    )
    
    return JSONResponse(resp.model_dump())

