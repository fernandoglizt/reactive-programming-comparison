import os
import logging
from contextlib import asynccontextmanager
from datetime import datetime
from fastapi import FastAPI, HTTPException
from app.models import ProcessRequest, ProcessResponse
from app.http_client import HttpClient
from app.processor import process_events

# Configuration from environment
PORT = int(os.getenv("PORT", "8094"))
MAX_COUNT = int(os.getenv("MAX_COUNT", "200000"))
CONCURRENCY = int(os.getenv("CONCURRENCY", "128"))
DOWNSTREAM_TIMEOUT_MS = int(os.getenv("DOWNSTREAM_TIMEOUT_MS", "2000"))
RETRY_ATTEMPTS = int(os.getenv("RETRY_ATTEMPTS", "1"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

# Setup logging
logging.basicConfig(
    level=getattr(logging, LOG_LEVEL),
    format='{"time":"%(asctime)s","level":"%(levelname)s","message":"%(message)s"}',
)
logger = logging.getLogger(__name__)

# Global HTTP client
http_client: HttpClient = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifespan"""
    global http_client
    
    logger.info(
        f"Starting Python Imperative server on port {PORT} "
        f"(max_count={MAX_COUNT}, concurrency={CONCURRENCY})"
    )
    
    # Startup: initialize HTTP client
    http_client = HttpClient(
        timeout_ms=DOWNSTREAM_TIMEOUT_MS,
        max_connections=CONCURRENCY,
        retry_attempts=RETRY_ATTEMPTS,
    )
    await http_client.start()
    
    yield
    
    # Shutdown: close HTTP client
    if http_client:
        await http_client.close()
    
    logger.info("Server stopped")


# Create FastAPI app
app = FastAPI(title="Python Imperative", version="1.0.0", lifespan=lifespan)


@app.post("/process", response_model=ProcessResponse)
async def process(request: ProcessRequest):
    """Process events endpoint"""
    if request.count > MAX_COUNT:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "count exceeds maximum allowed",
                "max_count": MAX_COUNT,
                "requested": request.count,
            },
        )
    
    try:
        response = await process_events(request, http_client, CONCURRENCY)
        
        logger.info(
            f"Processed {response.processed_events} events in {response.duration_ms}ms "
            f"({response.events_per_sec:.2f} events/sec) - "
            f"OK: {response.external_calls_ok}, Fail: {response.external_calls_fail}"
        )
        
        return response
    except ValueError as e:
        raise HTTPException(status_code=400, detail={"error": str(e)})
    except Exception as e:
        logger.error(f"Error processing events: {e}")
        raise HTTPException(status_code=500, detail={"error": f"Internal server error: {str(e)}"})


@app.get("/healthz")
async def healthz():
    """Health check endpoint"""
    return {"ok": True, "ts": datetime.now()}


@app.get("/info")
async def info():
    """Info endpoint"""
    import sys
    import socket
    
    return {
        "name": "python-imperative",
        "version": "1.0.0",
        "python_version": f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}",
        "max_count": MAX_COUNT,
        "concurrency": CONCURRENCY,
        "node": socket.gethostname(),
        "notes": "Imperative with asyncio gather",
    }

