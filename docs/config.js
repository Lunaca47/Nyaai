/**
 * NYAAI Web Client Configuration
 * 
 * Production Deployment:
 * Set DEFAULT_API_URL to your deployed FastAPI backend URL
 * (e.g., "https://nyaai-backend.onrender.com", "https://nyaai-api.fly.dev", or "https://api.nyaai.org").
 * 
 * Local Development:
 * Defaults to "http://localhost:8000".
 * Developers may override this in browser console or devtools via:
 *   localStorage.setItem("nyaai_api_url", "http://127.0.0.1:8000")
 */

const NYAAI_CONFIG = {
  // Production / default API URL for the FastAPI statutory backend
  DEFAULT_API_URL: "http://localhost:8000",

  // Storage key for optional local developer overrides
  STORAGE_API_URL_KEY: "nyaai_api_url",
  STORAGE_GUEST_TOKEN_KEY: "nyaai_guest_token",

  /**
   * Resolves the active API base URL.
   * Priority:
   * 1. localStorage override (developer convenience)
   * 2. Checked-in DEFAULT_API_URL constant
   */
  getApiUrl: function() {
    try {
      if (typeof window !== "undefined" && window.localStorage) {
        const override = window.localStorage.getItem(this.STORAGE_API_URL_KEY);
        if (override && override.trim()) {
          return override.trim().replace(/\/+$/, "");
        }
      }
    } catch (_) {}
    return this.DEFAULT_API_URL.replace(/\/+$/, "");
  }
};

if (typeof window !== "undefined") {
  window.NYAAI_CONFIG = NYAAI_CONFIG;
}
