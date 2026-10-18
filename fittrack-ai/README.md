# FITTRACK AI — Production AI Food Scanner & Nutrition Tracking Android Application

[![FitTrack AI CI Pipeline](https://github.com/fittrack-ai/fittrack-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/fittrack-ai/fittrack-ai/actions/workflows/ci.yml)
[![FastAPI](https://img.shields.io/badge/Backend-FastAPI-009688?logo=fastapi)](https://fastapi.tiangolo.com)
[![Jetpack Compose](https://img.shields.io/badge/Android-Jetpack%20Compose-4285F4?logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-7C3AED)](https://m3.material.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **FITTRACK AI** is a production-grade, privacy-first mobile nutrition ecosystem inspired by the usability and speed of SnapCalorie (`com.snapcalorie.alpha002`). Built with native Android (Kotlin, Jetpack Compose, CameraX, Room, ML Kit), a high-performance Python FastAPI backend, and a real multimodal vision AI engine for multi-item food recognition, portion weight estimation, regional Indian food mapping, speech logging, and verified USDA / IFCT nutrition matching.

---

## 🌟 Key Features

1. **Multimodal AI Plate Vision**
   - Detects multiple food items on a single plate (e.g. *South Indian Thali: White Rice, Sambar, Rasam, Potato Fry, Curd* or *Breakfast: Idlis, Dosa, Coconut Chutney*).
   - Estimates portion size, gram weight, cooking methods (boiled, steamed, shallow fried, deep fried), and oil adjustment levels.
   - Provider-agnostic adapter (`GeminiVisionAdapter` with Google Gemini 1.5 Flash + fallback `MockVisionAdapter`).

2. **Verified Composite Nutrition Database**
   - Integrates USDA FoodData Central and Indian Food Composition Tables (IFCT / NIN).
   - Exact mathematical scaling: `nutrient = per_100g * (weight_g / 100)`.
   - Never hallucinates macro numbers; confidence metrics and portion uncertainty tags are attached to every item.

3. **Multi-Modal Meal Logging**
   - **AI Vision Scanner**: Camera capture with reticle, flash, and gallery import.
   - **Voice Logging**: Natural language speech recognition (e.g., *"I ate two dosas with sambar"* or *"200 grams chicken biryani"*).
   - **Barcode Scanner**: Instant retail lookup across UPC & EAN-13 packaged foods with serving size adjustment.
   - **Nutrition Label OCR**: Camera-based Nutrition Facts label extraction with per-100g vs per-serving validation.

4. **Rich Mobile Dashboard & Visual Diary**
   - Interactive macro progress rings (Protein, Carbohydrates, Healthy Fats, Fiber).
   - Real-time calorie budget counters and water hydration tracker with quick +250ml logging.
   - Daily, weekly, and monthly calendar timeline with instant item portion editing and swipe-to-delete.
   - Offline-first Room database synchronization with backend state reversal.

5. **AI Nutrition Coach & Analytics**
   - Context-aware conversational assistant grounded in the user's authentic daily food logs.
   - Suggests high-protein Indian alternatives, checks nutrient deficiencies, and offers meal swaps.
   - Weekly calorie adherence charts and macronutrient distribution analytics.

---

## 🏗️ Monorepo Architecture

```
fittrack-ai/
├── android/                   # Native Android Studio Project (Gradle, Kotlin, Compose)
│   ├── app/
│   │   ├── src/main/java/com/fittrack/ai/
│   │   │   ├── core/          # Theme (Obsidian & Emerald), Navigation, Components
│   │   │   ├── data/          # Room DB, Retrofit API client, Repositories, DataStore
│   │   │   ├── presentation/  # Compose Screens & ViewModels (Dashboard, Scanner, Diary, etc.)
│   │   │   ├── FitTrackApp.kt # Application initialization
│   │   │   └── MainActivity.kt# Edge-to-edge Compose host activity
│   │   └── build.gradle.kts
│   ├── gradlew.bat            # Gradle 8.9 wrapper for Windows
│   └── settings.gradle.kts
├── backend/                   # Python FastAPI Backend
│   ├── app/
│   │   ├── api/v1/            # Auth, Scans, Foods, Meals, Dashboard, Tracking, Assistant
│   │   ├── core/              # Config (Pydantic v2), JWT Security, Async Database
│   │   ├── models/            # SQLAlchemy DB Models (11 tables)
│   │   ├── schemas/           # Pydantic v2 validation schemas
│   │   └── main.py            # FastAPI entry point & CORS
│   └── requirements.txt
├── ai-engine/                 # AI & Nutrition Inference Services
│   ├── ai_engine/
│   │   ├── adapters/          # GeminiVisionAdapter & MockVisionAdapter
│   │   ├── nutrition_matcher.py # Fuzzy search, composite database matching, portion math
│   │   ├── voice_parser.py    # Speech-to-text regex parser for English, Indian English, Tanglish
│   │   └── label_parser.py    # OCR nutrition facts extractor
├── database/                  # SQL Schemas & Seed Data
│   ├── schema.sql             # 20 Supabase PostgreSQL tables with RLS
│   └── seed_data.sql          # Seed nutrient definitions & catalog items
├── datasets/                  # Curated Reference Data
│   ├── food_database.json     # 20+ Indian & USDA composite foods with full micronutrients
│   └── barcode_database.json  # EAN-13/UPC packaged foods (Amul, Quaker, Epigamia, etc.)
├── tests/                     # Automated Test Suite (10/10 Passing)
│   ├── test_full_user_journey.py
│   ├── test_nutrition_engine.py
│   ├── test_voice_parser.py
│   └── test_label_parser.py
└── docs/                      # Technical Documentation
```

---

## 🚀 Quick Start Guide (Windows)

### Prerequisites
- **Python 3.10+** (Python 3.11 or 3.13 tested)
- **JDK 17 or JDK 21**
- **Android Studio Ladybug (2024.2+)** or Android SDK command-line tools

### 1. Backend Setup & Run
```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
Interactive OpenAPI documentation will be live at: `http://localhost:8000/docs`

### 2. Run Test Suite
```powershell
cd fittrack-ai
.\backend\.venv\Scripts\python.exe -m pytest tests/ -v
```
All 10 automated test suites test:
1. Complete 12-step end-to-end user journey
2. Mathematical nutrition engine portion scaling and cooking oil modifiers
3. Voice food & quantity entity parsing
4. Packaged food label OCR extraction

### 3. Android Studio Setup
1. Launch Android Studio.
2. Select **Open Project** and navigate to `c:\Users\priya\OneDrive\Videos\app\fittrack-ai\android`.
3. Allow Gradle 8.9 sync to finish.
4. Select an emulator or physical device (Android 8.0+ / API 26+) and press **Run (Shift + F10)**.

To compile via command line:
```powershell
cd android
.\gradlew.bat assembleDebug
```

---

## 🔒 Security & Privacy
- **Zero API Key Exposure:** All Gemini AI credentials, database passwords, and secrets reside strictly on the backend.
- **Row-Level Security (RLS):** Supabase PostgreSQL policies isolate all user profiles, scans, and diaries.
- **Privacy Compliance:** One-tap permanent account deletion deletes all profile, meal, and image data.

---

## 📄 License
This project is developed under the MIT License.
