# NYAAI V2 — Human Manual Testing Checklist

This document provides a sequential, step-by-step testing script for human verification of physical hardware features and cross-platform UX parity across the **NYAAI Android Application** (`app/`) and the **Web Client** (`docs/`).

---

## 1. Prerequisites & Setup

### A. Android Device / Emulator
- **OS**: Android 8.0 (API 26) or higher.
- **Build & Install**:
  ```bash
  # Option 1: Assemble and install debug APK
  .\gradlew assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk

  # Option 2: Sideload signed production release APK directly
  adb install -r docs/app-release.apk
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
*Validates emergency quick-action routing and telephony intent dispatch across the 17-helpline directory without unauthorized background dialing.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 3.1 | Navigate to **Legal SOS** tab (or tap SOS emergency top-bar button on home screen). | Emergency dispatch screen displays 17 national hotlines, with primary cards for **Unified Emergency (112)**, **National Legal Aid NALSA (15100)**, **Women Helpline NCW (1091 / 7827170170)**, and **Cyber Crime Fraud (1930)**. | [ ] |
| 3.2 | Tap **"Cyber Fraud (1930)"**. | Android system phone dialer opens immediately with `1930` pre-dialed on the keypad. | [ ] |
| 3.3 | Verify dialer safety. | App **does NOT** place an automated call without user intervention (`Intent.ACTION_DIAL` is used, requiring the user to tap the green call button). | [ ] |
| 3.4 | Tap **"National Legal Aid (15100)"** and **"Women Helpline (1091)"**. | Respective numbers `15100` and `1091` are pre-filled in the dialer. | [ ] |
| 3.5 | Check procedural guidance card under each button. | Explains statutory mandate: e.g. for 1930, reporting within 2 hours ("Golden Hour") to freeze recipient UPI accounts; for 15100, free legal aid under Section 12 of the Legal Services Authorities Act. | [ ] |

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

#### Test 5D: Hallucinated / Non-Existent Section (`REJECTED_UNGROUNDED` Hard Gate)
- **Prompt**: `"What are the search and seizure powers of the police under Section 999 of the Bharatiya Nyaya Sanhita?"`
- **Verification on Android & Web**:
  - [ ] Hard gate blocks ungrounded output.
  - [ ] BNS has only 358 substantive sections; Section 999 does not exist.
  - [ ] Action badge reports: `REJECTED_UNGROUNDED` (or flags citation as unverified).
  - [ ] User receives an ungrounded citation alert instead of fabricated legal text.

#### Test 5E: BNSS Arrest Warrant Procedure (Section 70/72 Verification)
- **Prompt**: `"Can a police officer execute an arrest warrant without showing the warrant to the accused under BNSS?"`
- **Verification on Android & Web**:
  - [ ] Green `PASSED / GROUNDED` citation badge displayed.
  - [ ] Cites **Section 70** (Form of warrant) and **Section 72** (Notification of substance of warrant) of BNSS 2023.
  - [ ] Confirms police officer executing warrant shall notify substance thereof and show warrant if required.

#### Test 5F: Landmark Supreme Court Precedent Grounding (`PASSED`)
- **Prompt**: `"Is registration of FIR mandatory for cognizable offenses under Section 173 BNSS Lalita Kumari?"`
- **Verification on Android & Web**:
  - [ ] Green `PASSED` citation badge displayed for judicial precedent.
  - [ ] Grounds citation to **Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1** (Constitution Bench).
  - [ ] Highlights ratio: Registration of FIR is mandatory under Section 154 CrPC / 173 BNSS if information discloses commission of a cognizable offense; no preliminary inquiry permissible in such cases.

#### Test 5G: Superseded Judicial Precedent Detection (`ANNOTATED_SUPERSEDED_PRECEDENT`)
- **Prompt**: `"Is preliminary inquiry required before arresting accused under SC ST Act Dr Subhash Kashinath Mahajan?"`
- **Verification on Android & Web**:
  - [ ] Amber `ANNOTATED_SUPERSEDED_PRECEDENT` warning badge displayed.
  - [ ] Identifies **Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454**.
  - [ ] Prominently displays statutory superseded caveat: Superseded by Parliament via Section 18A of the Scheduled Castes and the Scheduled Tribes (Prevention of Atrocities) Amendment Act, 2018 (constitutionality upheld in *Prathvi Raj Chauhan*, (2020) 4 SCC 727; directions recalled by Supreme Court in (2020) 4 SCC 761).

#### Test 5H: Hallucinated / Fabricated Case Precedent (`REJECTED_UNGROUNDED` Hard Gate)
- **Prompt**: `"According to Ramesh Kumar v. State of Wonderland, criminal complaints can be quashed without hearing."`
- **Verification on Android & Web**:
  - [ ] Red `REJECTED_UNGROUNDED` citation badge displayed.
  - [ ] Citation rejected as ungrounded: Precedent not verified in the 21 Supreme Court landmark corpus.
  - [ ] Fallback procedural guidance provided warning user not to rely on unverified citations in formal filings.

---

### Suite 6: Matter Workspace Citations & Precedents Tab
*Validates the Matter Detail screen Citations tab displaying both statutory provisions and judicial precedents with verified status badges.*

| Step | Action | Expected Result | Pass/Fail |
|------|--------|-----------------|-----------|
| 6.1 | Open an active matter in the Matter Workspace. | Matter Detail screen loads with tab bar (Overview, Timeline, Action Plan, Evidence, Citations, Questions). | [ ] |
| 6.2 | Tap on the **Citations** tab (Tab 4). | Citations screen displays two distinct sections: **Statutory Provisions** and **Judicial Precedents & Case Law**. | [ ] |
| 6.3 | Verify statutory cards. | In-force statutes (e.g. BNSS Section 173, BNS Section 318) display green `PASSED` badge with confirmed currentness date (`2026-09`). | [ ] |
| 6.4 | Verify landmark precedent cards. | Good-law precedents (e.g. *Lalita Kumari*, *Bhajan Lal*) display green `PASSED` badge with "Good Law • Grounded Supreme Court Landmark Precedent". | [ ] |
| 6.5 | Verify superseded precedent cards. | Superseded precedents (e.g. *Dr. Subhash Kashinath Mahajan*) display amber `ANNOTATED_SUPERSEDED_PRECEDENT` badge with Section 18A amendment advisory. | [ ] |
| 6.6 | Verify empty state. | If no authorities are linked to a new matter, displays `"No statutory provisions or case law precedents linked yet."` | [ ] |

---

## 3. Human Sign-Off Sheet

| Tester Name | Platform Tested (Device / Web Browser) | Test Date | All 6 Suites Passed (Y/N) | Signature / Notes |
|-------------|----------------------------------------|-----------|---------------------------|-------------------|
|             |                                        |           |                           |                   |
