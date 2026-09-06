# Google Play Console: Data Safety Form Answers for NYAAI

When completing the **App content > Data safety** section in Google Play Console, use the following exact responses corresponding to NYAAI’s architectural design.

---

## Section 1: Data Collection and Security Overview

1. **Does your app collect or share any of the required user data types?**  
   👉 **Yes** (Firebase Auth collects user account details).

2. **Is all of the user data collected by your app encrypted in transit?**  
   👉 **Yes** (All external API calls use TLS 1.3 / HTTPS).

3. **Do you provide a way for users to request that their data be deleted?**  
   👉 **Yes** (Users can clear local data via App settings or contact developer to purge Firebase Auth records).

---

## Section 2: Data Types Breakdown

### 1. Personal Info
- **Name:**
  - *Collected?* **Yes** (when signing in with Google).
  - *Shared?* **No**.
  - *Ephemeral?* **No** (stored in Firebase Auth).
  - *Required or Optional?* **Optional** (sign-in is optional).
  - *Purposes:* **App functionality, Account management**.
- **Email address:**
  - *Collected?* **Yes** (Google Sign-In / Firebase Auth).
  - *Shared?* **No**.
  - *Ephemeral?* **No**.
  - *Required or Optional?* **Optional**.
  - *Purposes:* **App functionality, Account management**.
- **User IDs (Firebase UID):**
  - *Collected?* **Yes**.
  - *Shared?* **No**.
  - *Ephemeral?* **No**.
  - *Purposes:* **App functionality, Account management**.

---

### 2. Audio Files / Voice Recordings
- **Voice or sound recordings:**
  - *Collected?* **No** (Processed ephemerally by on-device Speech Recognizer for real-time text transcription; never collected or stored).
  - *Shared?* **No**.

---

### 3. Photos and Videos
- **Photos / Images:**
  - *Collected?* **No** (Scanned legal documents are processed locally on-device using ML Kit Text Recognition; images are not collected or transmitted).
  - *Shared?* **No**.

---

### 4. Files and Docs
- **Files and documents:**
  - *Collected?* **No** (PDF/Text extraction is performed completely on-device).
  - *Shared?* **No**.

---

### 5. Financial, Health, Messages, Location
- **Financial info:** **No**
- **Health info:** **No**
- **Personal SMS / Messages:** **No**
- **Precise / Approximate Location:** **No**

---

## Summary for Play Console Reviewers
> "NYAAI operates primarily as an offline on-device legal research application. All legal document parsing (PDFBox/ML Kit) and FTS statutory lookups are executed on-device. The only external data transferred is user authentication (Google Sign-In via Firebase Auth) and user-initiated AI legal questions sent to Google Gemini via encrypted HTTPS. Voice audio and scanned documents are never stored or transmitted."
