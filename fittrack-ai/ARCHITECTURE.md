# FITTRACK AI — System Architecture & Design Specification

## 1. Architectural Overview

FITTRACK AI employs Clean Architecture and MVVM (Model-View-ViewModel) across both the Android native client and the FastAPI backend. It is designed around an offline-first philosophy where user interactions immediately update local state and queue operations for cloud synchronization.

```
+-------------------------------------------------------------------------+
|                           ANDROID CLIENT                                |
|                                                                         |
|  [Presentation Layer - Jetpack Compose & Material 3]                    |
|   Dashboard | Scanner | ScanResult | Diary | Voice | Barcode | Assistant|
|                          |                                              |
|  [Domain / ViewModel Layer]                                             |
|   StateFlow UI State | Coroutine Scopes | Business Logic                |
|                          |                                              |
|  [Data Layer]                                                           |
|   - Repositories (Offline-First Single Source of Truth)                 |
|   - Local: Room SQLite Database + DataStore Encrypted Session           |
|   - Remote: Retrofit 2 + OkHttp + Moshi Converter                       |
|   - Device Services: CameraX + Google ML Kit Vision                     |
+-------------------------------------------------------------------------+
                                 |  HTTPS / TLS 1.3
                                 v
+-------------------------------------------------------------------------+
|                           FASTAPI BACKEND                               |
|                                                                         |
|  [API & Routing Layer]                                                  |
|   /auth | /api/v1/scans | /api/v1/foods | /api/v1/meals                 |
|   /api/v1/dashboard | /api/v1/assistant | /api/v1/voice                 |
|                          |                                              |
|  [Service & Core Layer]                                                 |
|   JWT Authentication | Pydantic v2 Request Validation | Rate Limiter    |
|                          |                                              |
|  [Database Layer - SQLAlchemy 2.0 Async]                                |
|   PostgreSQL / Supabase + Row-Level Security (RLS)                      |
|   SQLite + aiosqlite (Local / Zero-Config Embedded Engine)              |
+-------------------------------------------------------------------------+
                                 |
                                 v
+-------------------------------------------------------------------------+
|                           AI INFERENCE ENGINE                           |
|                                                                         |
|  [Multimodal Vision Adapter Interface]                                  |
|   - GeminiVisionAdapter (Google Gemini 1.5 Flash Vision API)            |
|   - MockVisionAdapter (Heuristic Multi-Item Plate Simulation)           |
|                                                                         |
|  [Nutrition Calculation & Matching Engine]                              |
|   - Token & Levenshtein Fuzzy Search Catalog Matcher                    |
|   - USDA FoodData Central & Indian Food Composition (IFCT) Composites   |
|   - Mathematical Portion Scaling: per_100g * (weight_g / 100)           |
|   - Cooking Method & Oil Multipliers (Boiled, Steamed, Fried)           |
|                                                                         |
|  [Speech & Natural Language Parser]                                     |
|   - Natural Language Parser (Quantities, Units, Food Items)             |
|   - English, Indian English, Tanglish number word normalization         |
|                                                                         |
|  [OCR Label Parser]                                                     |
|   - Regular expression extraction of Nutrition Facts tables             |
|   - Per-100g vs Per-Serving validation & serving size ratios            |
+-------------------------------------------------------------------------+
```

---

## 2. Android Client Design

### 2.1 MVVM & Reactive Flow Pattern
- Every screen is backed by a dedicated `ViewModel` exposing immutable `StateFlow<UiState>`.
- UI interactions trigger one-way ViewModel intents (`processImageBytes`, `saveMeal`, `updateItemPortion`, `deleteMeal`).
- The UI layer is purely declarative, rendering states seamlessly via Jetpack Compose.

### 2.2 Offline-First Data Strategy
- Local Room database acts as the single source of truth for the UI (`MealDao`, `TrackingDaos`).
- When an action is taken (e.g. meal saved or deleted):
  1. Local Room database is updated optimistically.
  2. UI recalculates daily totals immediately with zero perceived latency.
  3. Network call is dispatched to the backend.
  4. If offline, the operation is queued and synced upon reconnect.

### 2.3 CameraX & ML Kit Vision Integration
- `CameraScannerScreen` binds CameraX `Preview` and `ImageCapture` to Android's `LifecycleOwner`.
- Shutter action captures raw compressed JPEG bytes and transmits them to the AI pipeline.
- `BarcodeScannerScreen` utilizes Google ML Kit Barcode Scanning to detect UPC-A, UPC-E, EAN-8, and EAN-13 barcodes in real-time.

---

## 3. Backend & Database Architecture

### 3.1 Asynchronous FastAPI Runtime
- High concurrency with Python `asyncio` and `uvicorn`.
- Endpoints use dependency injection (`Depends`) for authentication verification, database session management, and singleton service provision.

### 3.2 Dual Database Compatibility
- **Production Cloud**: Supabase PostgreSQL with 20 tables and Row-Level Security policies ensuring tenant data isolation.
- **Local Testing**: SQLite with `aiosqlite` for instant local execution and automated unit/integration tests without external cloud dependencies.

---

## 4. AI Engine & Nutrition Math

### 4.1 Portion Scaling Formula
For every detected item:
$$\text{Nutrient Value} = \text{Nutrient}_{\text{per 100g}} \times \frac{\text{Selected Weight in Grams}}{100}$$

### 4.2 Cooking Method & Oil Adjustments
- **Fried Method**: Calories $\times 1.15$, Fat $\times 1.25$
- **Low Oil**: Fat $\times 0.85$, Calories decreased by $(0.15 \times \text{fat} \times 9\text{ kcal})$
- **High Oil**: Fat $\times 1.25$, Calories increased by $(0.25 \times \text{fat} \times 9\text{ kcal})$

### 4.3 Uncertainty & Confidence
Every AI prediction returns an `overall_confidence` score between 0.0 and 1.0. If confidence drops below 0.70, the Android client presents a confirmation alert inviting user review before committing to the diary.
