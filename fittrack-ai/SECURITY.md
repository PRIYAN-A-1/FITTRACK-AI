# FITTRACK AI — Security, Privacy & Compliance Architecture

## 1. Zero-Exposure Secret Management
- **Rule:** Never store private API keys (Google Gemini, Supabase service roles, AWS S3 keys) inside the Android APK.
- **Implementation:** All multimodal AI inference calls and third-party lookups are mediated strictly by the backend FastAPI service via authenticated JWT sessions.
- **Android Client:** Communicates solely with the FitTrack AI backend API over TLS 1.3.

---

## 2. Authentication & Authorization
- **Token Mechanism:** Stateless JSON Web Tokens (JWT) signed with HMAC-SHA256.
- **Password Protection:** Cryptographic password hashing using salted SHA-256 (PBKDF2/Bcrypt compatible in production).
- **Session Expiry:** Access tokens configured with rolling expiration; client handles refresh via secure DataStore storage.

---

## 3. Data Isolation & Row-Level Security (RLS)
- In PostgreSQL/Supabase, Row-Level Security policies prevent cross-tenant data access.
- All meal entries, uploaded image records, daily goals, water logs, and assistant chats are strictly filtered by `auth.uid() = user_id`.
- Public catalogs (foods, nutrients, barcodes) are marked read-only for normal users.

---

## 4. Privacy & Regulatory Readiness (GDPR / CCPA / Play Store)
- **Account Deletion:** Full compliance with Google Play's 2024 Account Deletion Requirement. Users can trigger complete data purging directly within `ProfileScreen` or via API endpoint (`DELETE /auth/account`).
- **Data Export:** Supports exporting historical meal entries, scanned photographs, and goal tracking in standardized JSON.
- **Health Disclaimer:** AI assistant outputs and macro trackers include disclaimers indicating they are for fitness and nutritional guidance, not medical diagnosis.
