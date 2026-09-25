"""
Pytest configuration for backend test suite.
Ensures ENVIRONMENT is set to 'test' and an ephemeral test secret is configured.
"""
import os

os.environ.setdefault("ENVIRONMENT", "test")
os.environ.setdefault("AUTH_SECRET_KEY", "test_ephemeral_hmac_secret_key_for_backend_pytest_2026")
os.environ.setdefault("ALLOW_IN_MEMORY_FALLBACK", "True")
