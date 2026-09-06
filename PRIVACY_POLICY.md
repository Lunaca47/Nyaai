# Privacy Policy for NYAAI

**Last Updated:** September 7, 2026  
**Effective Date:** September 7, 2026

NYAAI ("we", "our", or "the App") is an AI-powered legal assistant and research companion developed by the NYAAI team. We are committed to protecting your privacy, data security, and ensuring transparency in how information is handled within our mobile application.

This Privacy Policy explains how our application processes, uses, and safeguards information in compliance with the **Google Play Developer Program Policies** and India's **Digital Personal Data Protection (DPDP) Act, 2023**.

---

## 1. Information We Collect and Process

### A. Authentication & Account Information
When you choose to sign in to NYAAI using Google Sign-In or Firebase Authentication:
- **Collected Data:** Your name, email address, profile photo URL, and unique Firebase user ID.
- **Purpose:** To manage user authentication sessions and personalize your application profile in settings.
- **Third-Party Processing:** Authentication is securely handled by **Google Firebase Authentication**. We do not sell or share this data with third-party advertisers or data brokers.

### B. Speech and Audio Data (Microphone Permission)
- **Requested Permission:** `android.permission.RECORD_AUDIO`
- **Usage:** Used exclusively when you tap the microphone button in the chat interface to transcribe spoken questions into text.
- **Processing:** Audio is processed in real time by the device's default speech recognition engine (e.g., Google Speech Services). 
- **Storage:** **NYAAI does NOT record, retain, store, or transmit your audio files to any proprietary remote servers.** Audio data is processed ephemerally on-the-fly and discarded immediately upon transcription.

### C. Legal Documents and Camera/Media Access (OCR Scanner)
- **Usage:** Used when you select a legal notice, contract, FIR, or agreement image/PDF for on-device text recognition.
- **Processing:** Image text recognition is executed **100% locally on your device** using Google ML Kit Text Recognition. 
- **Storage:** Scanned images and extracted text remain locally in your device's memory/cache. They are never uploaded to our servers or stored externally without your explicit request.

### D. Offline Legal Queries & Database
- **Processing:** The core statutory database (covering BNS 2023, BNSS 2023, BSA 2023, and the Constitution of India) is packaged inside the APK and resides in a local SQLite/Room database on your device.
- **Storage:** Offline searches and matching are performed entirely on-device with zero network latency and zero network transmission.

### E. Online AI Reasoning (Google Gemini Generative AI)
- **Usage:** When you ask complex legal questions requiring online generative synthesis, the text of your query and relevant statutory context are sent via encrypted HTTPS/TLS to Google Generative AI (Gemini API).
- **Processing:** The Gemini API processes text prompts to generate AI responses in accordance with Google's API Terms of Service. Personal identifying information is not required or transmitted in standard legal prompts.

---

## 2. Device Permissions Used

| Permission | Category | Purpose | Data Retention |
|---|---|---|---|
| `INTERNET` | Normal | Fetching Gemini AI responses, Firebase authentication | HTTPS encrypted transmission |
| `RECORD_AUDIO` | Sensitive | Speech-to-text input in the chat interface | Real-time only; 0 seconds retention |

---

## 3. Data Storage, Security, and Encryption

- **Encryption in Transit:** All network communications (Firebase authentication and Gemini AI requests) are encrypted using standard **Transport Layer Security (TLS 1.3 / HTTPS)** protocols.
- **Local Storage:** Bookmarks, saved chat threads, and app settings are stored locally on your device using Android Room Database and `EncryptedSharedPreferences`.
- **Zero Third-Party Tracking:** NYAAI does not contain third-party ad networks, advertising identifiers (AAID), or analytics trackers.

---

## 4. Legal Disclaimer

NYAAI is an educational, research, and legal technology tool. It provides automated statutory insights and AI-generated text for informational purposes only. **NYAAI does not provide formal legal advice, legal representation, or form an attorney-client relationship.** Users should always consult a qualified advocate licensed by the Bar Council of India for binding legal counsel.

---

## 5. User Rights and Data Deletion

Under the DPDP Act and Google Play policies, you have full control over your personal data:
- **Local Data Deletion:** You can delete all locally stored bookmarks and chat history at any time through the app settings or by clearing the app data in Android system settings.
- **Account Deletion:** You can sign out or request complete deletion of your Firebase user profile by contacting our support team. Upon request, all associated authentication records will be permanently purged within 30 days.

---

## 6. Children’s Privacy

NYAAI is intended for legal professionals, students, and general adults (ages 18 and older). We do not knowingly collect or solicit personal information from children under the age of 13.

---

## 7. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect feature additions or regulatory changes. The latest version will always be published within the application and in the Google Play Store listing.

---

## 8. Contact Information

If you have questions, feedback, or data privacy requests regarding NYAAI, please reach out to us:

- **Entity:** NYAAI LegalTech Team  
- **Email:** support@nyaai.in (or barmavathprashanth2@gmail.com)  
- **Jurisdiction:** Hyderabad, Telangana, India
