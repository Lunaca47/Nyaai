/**
 * NYAAI V2 — Live Headless Browser End-to-End Test
 * 
 * Verifies real client-side execution in a real browser engine (Google Chrome):
 *   1. Starts local static server serving docs/ on port 8089.
 *   2. Starts or verifies FastAPI backend running on port 8000.
 *   3. Launches Headless Chrome via puppeteer-core.
 *   4. Loads docs/terminal.html, verifies guest-token acquisition and DOM initialization.
 *   5. Tests Case 1: Landlord Tenancy Deposit Query -> Verifies Model Law badge and MTA Section 11 text.
 *   6. Tests Case 2: Cheque Bounce Notice Query -> Verifies Verified Statutory Grounding badge and NI Act 138 text.
 *   7. Tests Case 3: Offline / Server Unreachable Diagnostic -> Overrides API URL to dead port, verifies honest fallback UI (NO hallucinations / fake confidence).
 */

const http = require("http");
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");

// Path to installed puppeteer-core in scratch directory
const SCRATCH_MODULES = "C:\\Users\\barma\\.gemini\\antigravity\\brain\\57be468b-9cc8-4a1b-9ffb-f77c05128c88\\scratch\\node_modules";
const puppeteer = require(path.join(SCRATCH_MODULES, "puppeteer-core"));

const CHROME_PATH = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const DOCS_DIR = path.resolve(__dirname, "..", "docs");
const STATIC_PORT = 8089;
const BACKEND_PORT = 8000;

const MIME_TYPES = {
  ".html": "text/html",
  ".js": "application/javascript",
  ".css": "text/css",
  ".json": "application/json",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon"
};

// 1. Static HTTP Server for docs/
function startStaticServer() {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      let reqPath = req.url.split("?")[0];
      if (reqPath === "/") reqPath = "/terminal.html";
      const filePath = path.join(DOCS_DIR, reqPath);

      if (!filePath.startsWith(DOCS_DIR)) {
        res.writeHead(403);
        res.end("Forbidden");
        return;
      }

      fs.readFile(filePath, (err, data) => {
        if (err) {
          res.writeHead(404);
          res.end("Not Found");
          return;
        }
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, { "Content-Type": MIME_TYPES[ext] || "application/octet-stream" });
        res.end(data);
      });
    });

    server.listen(STATIC_PORT, "127.0.0.1", () => {
      console.log(`[OK] Static docs server listening on http://127.0.0.1:${STATIC_PORT}`);
      resolve(server);
    });
  });
}

// 2. Helper to check if backend is up
function checkBackend() {
  return new Promise((resolve) => {
    const req = http.get(`http://127.0.0.1:${BACKEND_PORT}/docs`, (res) => {
      resolve(res.statusCode === 200);
    });
    req.on("error", () => resolve(false));
    req.setTimeout(1500, () => { req.destroy(); resolve(false); });
  });
}

// 3. Start uvicorn backend if not running
async function ensureBackend() {
  const isUp = await checkBackend();
  if (isUp) {
    console.log(`[OK] FastAPI backend is already running on port ${BACKEND_PORT}.`);
    return null;
  }

  console.log(`[*] Starting FastAPI backend via uvicorn on port ${BACKEND_PORT}...`);
  const backendDir = path.resolve(__dirname, "..", "backend");
  const proc = spawn("python", ["-m", "uvicorn", "app.main:app", "--port", String(BACKEND_PORT)], {
    cwd: backendDir,
    env: {
      ...process.env,
      ENVIRONMENT: "test",
      AUTH_SECRET_KEY: "test_ephemeral_hmac_secret_key_for_backend_pytest_2026",
      ALLOW_IN_MEMORY_FALLBACK: "True"
    },
    stdio: "pipe"
  });
  proc.stdout.on("data", (d) => process.stdout.write(d));
  proc.stderr.on("data", (d) => process.stderr.write(d));

  // Poll until ready
  for (let i = 0; i < 60; i++) {
    await new Promise((r) => setTimeout(r, 1000));
    if (await checkBackend()) {
      console.log(`[OK] FastAPI backend is ready on port ${BACKEND_PORT}.`);
      return proc;
    }
  }
  throw new Error("FastAPI backend failed to start within 60 seconds.");
}

// 4. Main Test Suite
async function runLiveBrowserTests() {
  console.log("================================================================================");
  console.log("NYAAI V2 -- REAL HEADLESS BROWSER END-TO-END VALIDATION (CHROME + PUPPETEER)");
  console.log("================================================================================");

  let staticServer;
  let backendProc;
  let browser;

  try {
    staticServer = await startStaticServer();
    backendProc = await ensureBackend();

    console.log(`[*] Launching Headless Chrome (${CHROME_PATH})...`);
    browser = await puppeteer.launch({
      executablePath: CHROME_PATH,
      headless: "new",
      args: [
        "--no-sandbox",
        "--disable-setuid-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage"
      ]
    });

    const page = await browser.newPage();
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        console.error(`[Browser Console Error]: ${msg.text()}`);
      }
    });

    console.log(`[*] Navigating to http://127.0.0.1:${STATIC_PORT}/terminal.html...`);
    await page.goto(`http://127.0.0.1:${STATIC_PORT}/terminal.html`, { waitUntil: "networkidle0" });

    // Verify Config & Base URL loaded from config.js
    const configApiUrl = await page.evaluate(() => window.NYAAI_CONFIG ? window.NYAAI_CONFIG.getApiUrl() : null);
    console.log(`[OK] Browser loaded window.NYAAI_CONFIG.getApiUrl(): "${configApiUrl}"`);
    if (!configApiUrl || !configApiUrl.includes(":8000")) {
      throw new Error(`Expected configApiUrl to point to :8000, got: ${configApiUrl}`);
    }

    // --------------------------------------------------------------------------
    // Test Case 1: Landlord Tenancy Deposit (Model Law Caveat & MTA 11 Grounding)
    // --------------------------------------------------------------------------
    console.log("\n--------------------------------------------------------------------------------");
    console.log("TEST CASE 1: Tenancy Security Deposit (Model Tenancy Act §11)");
    console.log("--------------------------------------------------------------------------------");
    
    await page.waitForSelector("#terminalInput");
    await page.type("#terminalInput", "My landlord refuses to refund my security deposit of 1.5 lakhs after I vacated the flat on proper notice.");
    await page.click("#sendBtn");

    console.log("[*] Query submitted, waiting for API response rendering...");
    await page.waitForFunction(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      return bubbles.length >= 2; // welcome bubble + response bubble
    }, { timeout: 35000 });

    const case1Data = await page.evaluate(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      const last = bubbles[bubbles.length - 1];
      return {
        html: last.innerHTML,
        text: last.innerText
      };
    });

    console.log("[*] Rendered Response Excerpt:", case1Data.text.slice(0, 300).replace(/\n/g, " "));

    // Assertions for Case 1
    const hasMtaCitation = case1Data.text.includes("Model Tenancy Act") || case1Data.text.includes("Section 11");
    const hasModelLawBadge = case1Data.text.includes("Model Law (State Adoption Required)") || case1Data.html.includes("badge-pastel-blue");
    const hasHybridScore = case1Data.text.includes("Hybrid RRF") || case1Data.text.includes("Provisions Grounded");

    console.log(`  • MTA Citation Grounded: ${hasMtaCitation ? "PASS" : "FAIL"}`);
    console.log(`  • Model Law Badge Displayed: ${hasModelLawBadge ? "PASS" : "FAIL"}`);
    console.log(`  • Real Hybrid Score Badge: ${hasHybridScore ? "PASS" : "FAIL"}`);

    if (!hasMtaCitation) throw new Error("Case 1 did not contain statutory grounding to Model Tenancy Act!");
    if (!hasModelLawBadge) throw new Error("Case 1 did not display Model Law (State Adoption Required) badge!");

    // --------------------------------------------------------------------------
    // Test Case 2: Cheque Bounce Notice (Section 138 NI Act Grounding)
    // --------------------------------------------------------------------------
    console.log("\n--------------------------------------------------------------------------------");
    console.log("TEST CASE 2: Cheque Bounce (Negotiable Instruments Act §138)");
    console.log("--------------------------------------------------------------------------------");

    await page.type("#terminalInput", "A client gave me a cheque of 2 lakhs which bounced due to funds insufficient, what legal notice to send under Section 138?");
    await page.click("#sendBtn");

    console.log("[*] Query submitted, waiting for API response rendering...");
    await page.waitForFunction(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      return bubbles.length >= 3;
    }, { timeout: 35000 });

    const case2Data = await page.evaluate(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      const last = bubbles[bubbles.length - 1];
      return {
        html: last.innerHTML,
        text: last.innerText
      };
    });

    console.log("[*] Rendered Response Excerpt:", case2Data.text.slice(0, 300).replace(/\n/g, " "));

    const hasNiAct = case2Data.text.includes("Negotiable Instruments Act") || case2Data.text.includes("138");
    const hasVerifiedBadge = case2Data.text.includes("Verified Statutory Grounding") || case2Data.html.includes("badge-pastel-sage");

    console.log(`  • NI Act Section 138 Grounded: ${hasNiAct ? "PASS" : "FAIL"}`);
    console.log(`  • Verified Grounding Badge: ${hasVerifiedBadge ? "PASS" : "FAIL"}`);

    if (!hasNiAct) throw new Error("Case 2 did not contain grounding to Negotiable Instruments Act 138!");
    if (!hasVerifiedBadge) throw new Error("Case 2 did not display Verified Statutory Grounding badge!");

    // --------------------------------------------------------------------------
    // Test Case 3: Offline / Server Unreachable Diagnostic UI
    // --------------------------------------------------------------------------
    console.log("\n--------------------------------------------------------------------------------");
    console.log("TEST CASE 3: Backend Unreachable Diagnostic (No Hallucinations / Honesty Check)");
    console.log("--------------------------------------------------------------------------------");

    // Point CONFIG to an unreachable port
    await page.evaluate(() => {
      window.CONFIG.API_BASE_URL = "http://127.0.0.1:59999";
    });

    await page.type("#terminalInput", "What is the procedure if a product is defective under Consumer Protection Act?");
    await page.click("#sendBtn");

    console.log("[*] Submitted to dead port, waiting for honest error banner...");
    await page.waitForFunction(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      return bubbles.length >= 4;
    }, { timeout: 20000 });

    const case3Data = await page.evaluate(() => {
      const bubbles = document.querySelectorAll("#chatHistory .chat-bubble.chat-ai:not(.mono)");
      const last = bubbles[bubbles.length - 1];
      return {
        html: last.innerHTML,
        text: last.innerText
      };
    });

    console.log("[*] Rendered Error Banner Excerpt:", case3Data.text.slice(0, 250).replace(/\n/g, " "));

    const hasUnreachableBanner = case3Data.text.includes("Live Legal Backend Unreachable") || case3Data.text.includes("Cannot Connect to Statutory Research Engine");
    const hasZeroFakeConfidence = !case3Data.text.includes("99.8% Advocate Procedural Grounding") && !case3Data.text.includes("98.5%");

    console.log(`  • Honest Unreachable Warning Banner: ${hasUnreachableBanner ? "PASS" : "FAIL"}`);
    console.log(`  • Zero Fake Confidence / No Hallucinations: ${hasZeroFakeConfidence ? "PASS" : "FAIL"}`);

    if (!hasUnreachableBanner) throw new Error("Case 3 did not display honest backend unreachable banner!");
    if (!hasZeroFakeConfidence) throw new Error("Case 3 displayed fake hardcoded confidence string!");

    console.log("\n================================================================================");
    console.log("ALL REAL BROWSER PUPPETEER TESTS PASSED SUCCESSFULLY (3/3)");
    console.log("================================================================================");

  } finally {
    if (browser) await browser.close();
    if (staticServer) staticServer.close();
    if (backendProc) {
      console.log("[*] Shutting down spawned backend process...");
      backendProc.kill();
    }
  }
}

runLiveBrowserTests().catch((err) => {
  console.error("\n[X] BROWSER TEST FAILED:", err);
  process.exit(1);
});
