# FITTRACK AI — Testing & Verification Guide

This document details the automated testing architecture, verification commands, and test suites across the backend, AI engine, and Android client.

---

## 1. Automated Test Suites

The test suite is located in `/tests` and executes using `pytest` and `pytest-asyncio`.

### 1.1 Test Matrix

| Suite | File | Tests | Status | Scope |
|---|---|---|---|---|
| **End-to-End User Journey** | `test_full_user_journey.py` | 1 | **PASSED** | 12-step complete lifecycle: Register -> Login -> Dashboard -> Meal Confirm -> Dashboard Update -> Edit Portion -> Recalculation -> Barcode Lookup -> Voice Parsing -> Assistant Chat -> Meal Delete -> Dashboard Reversal |
| **Nutrition Calculation Engine** | `test_nutrition_engine.py` | 4 | **PASSED** | Math scaling ($P_{100} \times W / 100$), Egg Fried Rice lookup, Indian aliases, cooking oil adjustments |
| **Voice Food Parser** | `test_voice_parser.py` | 3 | **PASSED** | Multi-item phrases (*2 dosas with sambar*, *200g biryani*, *banana + eggs + milk*) |
| **Nutrition Label OCR** | `test_label_parser.py` | 2 | **PASSED** | Standard FDA label format & Indian IFCT per-100g table parsing |

**Total Automated Tests:** 10 / 10 Passing (100% Success Rate)

---

## 2. Executing Automated Tests (Windows)

To execute all tests using the virtual environment:
```powershell
cd fittrack-ai
.\backend\.venv\Scripts\python.exe -m pytest tests/ -v
```

Expected output:
```text
============================= test session starts =============================
platform win32 -- Python 3.13.13, pytest-9.0.2, pluggy-1.6.0
rootdir: C:\Users\priya\OneDrive\Videos\app\fittrack-ai
plugins: anyio-4.12.1, asyncio-1.4.0
collected 10 items

tests/test_full_user_journey.py::test_complete_user_lifecycle PASSED     [ 10%]
tests/test_label_parser.py::test_parse_standard_label PASSED             [ 20%]
tests/test_label_parser.py::test_parse_per_100g_label PASSED             [ 30%]
tests/test_nutrition_engine.py::test_egg_fried_rice_lookup PASSED       [ 40%]
tests/test_nutrition_engine.py::test_indian_breakfast_alias_matching PASSED [ 50%]
tests/test_nutrition_engine.py::test_portion_scaling_math PASSED         [ 60%]
tests/test_nutrition_engine.py::test_cooking_method_and_oil_adjustments PASSED [ 70%]
tests/test_voice_parser.py::test_parse_dosa_and_sambar PASSED           [ 80%]
tests/test_voice_parser.py::test_parse_biryani_grams PASSED              [ 90%]
tests/test_voice_parser.py::test_parse_mixed_items PASSED                [100%]

============================= 10 passed in 1.48s ==============================
```

---

## 3. Android Verification & Unit Testing

Execute Android unit tests with Gradle:
```powershell
cd android
.\gradlew.bat testDebugUnitTest
```

To build the debug APK:
```powershell
cd android
.\gradlew.bat assembleDebug
```
The resulting APK will be generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`
