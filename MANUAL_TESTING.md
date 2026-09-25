# NYAAI V2 — Human Manual Testing Checklist

This document provides a sequential, step-by-step testing script for human verification of physical hardware features and cross-platform UX parity across the **NYAAI Android Application** (`app/`) and the **Web Client** (`docs/`).

---

## 1. Prerequisites & Setup

### A. Android Device / Emulator
- **OS**: Android 8.0 (API 26) or higher.
- **Build & Install**:
  ```bash
  .\gradlew assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Permissions Required**: Camera (`android.permission.CAMERA`), Phone/Dialer (`android.permission.CALL_PHONE` / Dialer intent).

### B. Web Client
- Open `docs/index.html` or `docs/terminal.html` in Chrome or Edge (either locally or on GitHub Pages `https://lunaca47.github.io/Nyaai/`).
- Ensure backend container is running (`docker run -p 8000:8000 ...` or `uvicorn app.main:app`).

---

## 2. Test Suites

### Suite 1: Offline Bookmarks & Statutory Codex Browse
*Validates Room Database local caching, offline search, and bookmark persistence.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 1.1 | Launch Android app without internet (airplane mode). | App launches cleanly to Home screen without network crash. | [ ] |
| 1.2 | Navigate to **Codex** tab from bottom navigation bar. | Statutory codex loads with central and state statutes list. | [ ] |
| 1.3 | Enter `"Section 138"` in the search field. | Immediate filtering displaying Negotiable Instruments Act §138 with title and operative preview. | [ ] |
| 1.4 | Tap the **Bookmark icon** on BNS Section 318(4) and NI Act Section 138. | Bookmark icon changes to filled state; toast indicates "Added to Bookmarks". | [ ] |
| 1.5 | Toggle filter to **"Bookmarked Only"**. | Only the 2 bookmarked provisions are displayed. | [ ] |
| 1.6 | Force-close the application from Android Recent Apps, then relaunch. | Filter to Bookmarks: both saved provisions remain bookmarked (Room SQLite persistence). | [ ] |

---

### Suite 2: OCR Document Scanner
*Validates camera integration, ML Kit document edge detection, and text extraction into case intake.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 2.1 | Tap the **Scanner** icon on the bottom navigation bar. | System camera permission dialog appears on first launch. | [ ] |
| 2.2 | Grant camera permission. | Live camera viewfinder initializes with rectangular document framing overlay. | [ ] |
| 2.3 | Point camera at a legal notice or printed document (e.g. cheque bounce demand notice) and tap **Capture**. | App detects document borders, crops image, and runs Google ML Kit text recognition. | [ ] |
| 2.4 | Review scanned text preview sheet. | Extracted text correctly captures headers, dates, and statutory citations (e.g. "Section 138"). | [ ] |
| 2.5 | Tap **"Import into Case Intake"**. | Extracted text automatically populates the query input field in the Chat / Research screen. | [ ] |

---

### Suite 3: Legal SOS Emergency Assistance Dialer
*Validates emergency quick-action routing and telephony intent dispatch without unauthorized background dialing.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 3.1 | Navigate to **Legal SOS** tab (or tap SOS emergency banner on home screen). | Emergency dispatch screen displays 3 primary hotlines: **Police / Unified Emergency (112)**, **Women Helpline (1091)**, **Cyber Crime Fraud (1930)**. | [ ] |
| 3.2 | Tap **"Cyber Fraud (1930)"**. | Android system phone dialer opens immediately with `1930` pre-dialed on the keypad. | [ ] |
| 3.3 | Verify dialer safety. | App **does NOT** place an automated call without user intervention (`Intent.ACTION_DIAL` is used, requiring the user to tap the green call button). | [ ] |
| 3.4 | Tap **"Women Helpline (1091)"** and **"Police (112)"**. | Respective numbers `1091` and `112` are pre-filled in the dialer. | [ ] |
| 3.5 | Check procedural guidance card under each button. | Explains statutory mandate: e.g. for 1930, reporting within 2 hours ("Golden Hour") to freeze recipient UPI accounts. | [ ] |

---

### Suite 4: Matter Lifecycle & AES-256-GCM Encryption at Rest
*Validates encrypted client-side matter storage using Android KeyStore and signed-token access control.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 4.1 | Navigate to **Matters** screen and tap **"New Matter" (+)**. | Matter creation modal appears requesting Title, Client Reference, and Domain. | [ ] |
| 4.2 | Enter: `Title: "Illegal Eviction Notice"`, `Client: "Rahul Sharma"`, `Domain: "Tenancy"`. Save. | Matter appears in the active matters list with status `ACTIVE` and timestamp. | [ ] |
| 4.3 | Inspect app storage via ADB (`/data/data/com.nyaai/databases/` or SharedPreferences). | Matter metadata and sensitive notes are stored as encrypted ciphertext (AES-256-GCM via `MatterCryptoService`). | [ ] |
| 4.4 | Open the matter, add a note: `"Received lockout threat via WhatsApp on 24 Sep"`. | Note is encrypted and appended to the matter event timeline. | [ ] |
| 4.5 | Change matter status to `RESOLVED`. | Status updates; matter moves to archive section with full audit history preserved. | [ ] |

---

### Suite 5: Cross-Platform Citation Badge & Caveat Parity
*Validates identical citation verification hard-gate behavior between the Android app and Web client (`docs/terminal.html`).*

#### Test 5A: Model Tenancy Act (Model Law Caveat Enforcement)
- **Prompt**: `"Can the landlord demand 6 months advance rent as security deposit under the Model Tenancy Act?"`
- **Verification on Android**:
  - [ ] Answer identifies Section 11 of the Model Tenancy Act (2 months cap for residential).
  - [ ] **Amber Warning Card** is prominently displayed above answer:
    `"STATUTORY APPLICABILITY CAVEAT (MODEL LAW): References Model Tenancy Act, 2021 which is a non-binding model framework circulated under Entry 18 of the State List. Requires State legislative adoption. Existing State rent control legislation (e.g. Delhi Rent Control Act, 1958) applies unless your State has enacted this framework."`
  - [ ] Action badge reports: `ANNOTATED_MODEL_LAW`.
- **Verification on Web Terminal (`docs/terminal.html`)**:
  - [ ] Run identical query.
  - [ ] Action badge displays `ANNOTATED_MODEL_LAW` with amber border.
  - [ ] Identical Entry 18 State List advisory notice is rendered in response container.

#### Test 5B: Cheque Bounce Notice (Grounded Statutory Verification)
- **Prompt**: `"Client cheque of 2 lakhs bounced due to insufficient funds. What is the deadline to send demand notice under Section 138 NI Act?"`
- **Verification on Android**:
  - [ ] Green **VERIFIED CITATION** badge displayed.
  - [ ] Confirms mandatory statutory requirements: 30 days from return memo to issue legal notice, 15 days cure period for drawer to pay, 30 days to file complaint under Section 142.
- **Verification on Web Terminal**:
  - [ ] Green `PASSED / GROUNDED` badge with exact citation to Negotiable Instruments Act, 1881 §138 and §142.

#### Test 5C: Repealed Statute Migration (Colonial Law Notice)
- **Prompt**: `"Accused committed fraud. Prosecute under Section 420 IPC and file FIR under Section 154 CrPC."`
- **Verification on Android & Web**:
  - [ ] Red/Orange **STATUTORY REPEAL NOTICE** displayed.
  - [ ] Specifically warns: IPC and CrPC repealed as of July 1, 2024.
  - [ ] Migration substitutions shown:
    - Section 420 IPC $\rightarrow$ **Bharatiya Nyaya Sanhita, 2023 Section 318(4)**
    - Section 154 CrPC $\rightarrow$ **Bharatiya Nagarik Suraksha Sanhita, 2023 Section 173**
  - [ ] Action badge reports: `ANNOTATED_REPEALED`.

---

## 3. Human Sign-Off Sheet

| Tester Name | Platform Tested (Device / Web Browser) | Test Date | All 5 Suites Passed (Y/N) | Signature / Notes |
|-------------|----------------------------------------|-----------|---------------------------|-------------------|
|             |                                        |           |                           |                   |
