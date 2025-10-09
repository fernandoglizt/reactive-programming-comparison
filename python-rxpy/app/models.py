from pydantic import BaseModel, Field
from datetime import datetime
import sys

class ProcessRequest(BaseModel):
    count: int = Field(..., gt=0)
    batch: int = Field(..., gt=0)
    io_delay_ms: int = 50
    downstream_url: str = "http://slow-io:8080/slow"

class ProcessResponse(BaseModel):
    ok: bool
    count: int
    batch: int
    io_delay_ms: int
    downstream_url: str
    processed_events: int
    batches: int
    external_calls_ok: int
    external_calls_fail: int
    duration_ms: int
    events_per_sec: float
    ts: str
    node: str
    notes: str = "rxpy reactive"

class HealthResponse(BaseModel):
    ok: bool = True
    ts: str = Field(default_factory=lambda: datetime.utcnow().isoformat())

class InfoResponse(BaseModel):
    name: str = "python-rxpy"
    version: str = "0.0.1-SNAPSHOT"
    pyVersion: str
    buildTime: str
    maxCount: int

