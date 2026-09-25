import time
import logging
from typing import Dict, List, Optional
from fastapi import Request, HTTPException, status

logger = logging.getLogger(__name__)

class SlidingWindowRateLimiter:
    """
    In-memory sliding window rate limiter per client IP / identifier.
    Guards endpoints against automated crawling or high-frequency abuse.
    """
    def __init__(self, requests_per_minute: int, scope: str = "default"):
        self.requests_per_minute = requests_per_minute
        self.scope = scope
        self.window_seconds = 60.0
        self.history: Dict[str, List[float]] = {}

    def _get_client_key(self, request: Request) -> str:
        # Check X-Forwarded-For header if behind a reverse proxy or load balancer
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            ip = forwarded.split(",")[0].strip()
        elif request.client and request.client.host:
            ip = request.client.host
        else:
            ip = "127.0.0.1"

        # Check authorization bearer token if present to scope by guest/user token
        auth = request.headers.get("Authorization")
        if auth and auth.startswith("Bearer "):
            token_prefix = auth[7:23]
            return f"{self.scope}:{ip}:{token_prefix}"
        return f"{self.scope}:{ip}"

    def check(self, request: Request):
        now = time.time()
        client_key = self._get_client_key(request)

        timestamps = self.history.get(client_key, [])
        # Expire timestamps older than window_seconds
        cutoff = now - self.window_seconds
        valid_timestamps = [t for t in timestamps if t > cutoff]

        if len(valid_timestamps) >= self.requests_per_minute:
            oldest = valid_timestamps[0]
            retry_after = max(1, int(oldest + self.window_seconds - now) + 1)
            self.history[client_key] = valid_timestamps
            logger.warning(f"Rate limit exceeded for key {client_key} on {self.scope}: {len(valid_timestamps)} reqs in window.")
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded for {self.scope}: max {self.requests_per_minute} requests per minute. Retry after {retry_after} seconds.",
                headers={"Retry-After": str(retry_after)}
            )

        valid_timestamps.append(now)
        self.history[client_key] = valid_timestamps

    def reset(self):
        """Clears all tracking history (useful for tests)."""
        self.history.clear()


# Pre-configured instances
generate_rate_limiter = SlidingWindowRateLimiter(requests_per_minute=20, scope="generate")
search_rate_limiter = SlidingWindowRateLimiter(requests_per_minute=60, scope="retrieval_search")


def limit_generate(request: Request):
    generate_rate_limiter.check(request)


def limit_search(request: Request):
    search_rate_limiter.check(request)
