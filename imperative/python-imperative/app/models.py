from pydantic import BaseModel, field_validator
from datetime import datetime
import socket


class ProcessRequest(BaseModel):
    count: int
    batch: int
    io_delay_ms: int = 0
    downstream_url: str | None = None

    @field_validator("count")
    @classmethod
    def validate_count(cls, v):
        if v <= 0:
            raise ValueError("count must be > 0")
        return v

    @field_validator("batch")
    @classmethod
    def validate_batch(cls, v):
        if v <= 0:
            raise ValueError("batch must be > 0")
        return v

    def get_downstream_url(self) -> str:
        return self.downstream_url or "http://slow-io:8080/slow"

    def get_io_delay_ms(self) -> int:
        return max(0, self.io_delay_ms)


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
    ts: datetime
    node: str
    notes: str

    @staticmethod
    def success(
        request: ProcessRequest,
        processed_events: int,
        batches: int,
        external_calls_ok: int,
        external_calls_fail: int,
        duration_ms: int,
    ):
        events_per_sec = (
            processed_events * 1000.0 / duration_ms if duration_ms > 0 else 0.0
        )
        hostname = socket.gethostname()

        return ProcessResponse(
            ok=True,
            count=request.count,
            batch=request.batch,
            io_delay_ms=request.get_io_delay_ms(),
            downstream_url=request.get_downstream_url(),
            processed_events=processed_events,
            batches=batches,
            external_calls_ok=external_calls_ok,
            external_calls_fail=external_calls_fail,
            duration_ms=duration_ms,
            events_per_sec=events_per_sec,
            ts=datetime.now(),
            node=hostname,
            notes="imperative with asyncio gather",
        )

