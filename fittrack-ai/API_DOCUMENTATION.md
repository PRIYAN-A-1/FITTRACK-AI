# FITTRACK AI — REST API Documentation

Base URL: `http://localhost:8000`  
OpenAPI / Swagger Spec: `http://localhost:8000/docs`  
ReDoc: `http://localhost:8000/redoc`

---

## 1. Authentication Endpoints

### 1.1 Register New User
- **Method / Path:** `POST /auth/register`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securepassword123",
  "full_name": "Priya Sharma",
  "current_weight_kg": 68.0,
  "target_weight_kg": 65.0,
  "activity_level": "moderately_active"
}
```
- **Success Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user_id": "890209e7-497c-47bc-8a7e-a1074eef7146",
  "email": "user@example.com",
  "full_name": "Priya Sharma"
}
```

### 1.2 User Login
- **Method / Path:** `POST /auth/login`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securepassword123"
}
```
- **Success Response (200 OK):** Auth token payload.

---

## 2. AI Food Vision & Scanning Endpoints

### 2.1 Scan Food Photograph
- **Method / Path:** `POST /api/v1/scans`
- **Headers:** `Authorization: Bearer <TOKEN>`, `Content-Type: multipart/form-data`
- **Form Data:**
  - `image`: Binary JPEG/PNG file (Max 10MB)
  - `hint`: Optional user description (e.g., *"Egg Fried Rice"*)
- **Success Response (200 OK):**
```json
{
  "scan_id": "c1f8a846-f94d-4c38-8929-e85df64998b3",
  "image_url": "/uploads/c1f8a846-f94d-4c38-8929-e85df64998b3.jpg",
  "overall_confidence": 0.94,
  "items": [
    {
      "item_id": "item_0",
      "name": "Egg Fried Rice",
      "food_id": "ifct_egg_fried_rice",
      "confidence": 0.94,
      "estimated_weight_g": 250.0,
      "serving_unit": "g",
      "quantity": 1.0,
      "cooking_method": "shallow_fried",
      "oil_adjustment": "normal",
      "nutrients": {
        "calories": 420.0,
        "protein_g": 13.5,
        "carbs_g": 62.0,
        "fat_g": 13.0,
        "fiber_g": 2.2,
        "sugar_g": 1.5,
        "sodium_mg": 450.0
      }
    }
  ]
}
```

### 2.2 Confirm & Save Scanned Meal
- **Method / Path:** `POST /api/v1/scans/{scan_id}/confirm`
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Request Body:**
```json
{
  "meal_type": "lunch",
  "meal_title": "Egg Fried Rice",
  "logged_date": "2026-10-18",
  "items": [
    {
      "food_id": "ifct_egg_fried_rice",
      "name": "Egg Fried Rice",
      "serving_unit": "g",
      "serving_weight_g": 250.0,
      "quantity": 1.0,
      "calories": 420.0,
      "protein_g": 13.5,
      "carbs_g": 62.0,
      "fat_g": 13.0,
      "fiber_g": 2.2,
      "cooking_method": "shallow_fried",
      "oil_adjustment": "normal"
    }
  ]
}
```

---

## 3. Voice & OCR Scanning

### 3.1 Parse Speech Transcript
- **Method / Path:** `POST /api/v1/voice/parse`
- **Request Body:**
```json
{
  "transcript": "I ate two dosas with sambar"
}
```
- **Success Response (200 OK):**
```json
{
  "transcript": "I ate two dosas with sambar",
  "detected_foods": [
    {
      "raw_text": "two dosas",
      "food_name": "Dosa",
      "food_id": "ifct_dosa",
      "quantity": 2.0,
      "unit": "piece",
      "weight_g": 160.0,
      "nutrients": {
        "calories": 266.0,
        "protein_g": 5.6,
        "carbs_g": 41.6,
        "fat_g": 9.0,
        "fiber_g": 2.4
      }
    },
    {
      "raw_text": "sambar",
      "food_name": "Sambar",
      "food_id": "ifct_sambar",
      "quantity": 1.0,
      "unit": "bowl",
      "weight_g": 150.0,
      "nutrients": {
        "calories": 115.0,
        "protein_g": 4.5,
        "carbs_g": 16.5,
        "fat_g": 3.4,
        "fiber_g": 4.8
      }
    }
  ],
  "total_nutrients": {
    "calories": 381.0,
    "protein_g": 10.1,
    "carbs_g": 58.1,
    "fat_g": 12.4,
    "fiber_g": 7.2
  }
}
```

### 3.2 Analyze Nutrition Facts Label OCR
- **Method / Path:** `POST /api/v1/labels/analyze`
- **Request Body:**
```json
{
  "ocr_text": "Nutrition Facts\nServing size: 100g\nCalories: 360\nProtein: 12g\nTotal Carbohydrate: 70g\nTotal Fat: 3g",
  "product_name": "Rolled Oats"
}
```

---

## 4. Foods & Barcode Catalog

### 4.1 Lookup Barcode
- **Method / Path:** `GET /api/v1/foods/barcode/{code}`
- **Parameters:** `code` (e.g. `8901262010015` or `030000010204`)
- **Success Response (200 OK):** Product record with verified nutrients per 100g and per serving.

### 4.2 Search Food Database
- **Method / Path:** `GET /api/v1/foods/search?q=biryani&limit=10`

---

## 5. Meals Management & Food Diary

- `GET /api/v1/meals?logged_date=2026-10-18`: Retrieve all meals for date.
- `POST /api/v1/meals`: Create manual or quick meal entry.
- `PATCH /api/v1/meals/{id}`: Update meal item portion weight or recipe.
- `DELETE /api/v1/meals/{id}`: Delete meal and reverse contribution.

---

## 6. Dashboard, Tracking & AI Assistant

- `GET /api/v1/dashboard/daily?target_date=2026-10-18`: Daily calorie target, consumed, remaining, macros breakdown, hydration, and meal cards.
- `POST /api/v1/water`: Log water in milliliters (`{"amount_ml": 250}`).
- `POST /api/v1/weight`: Log body weight (`{"weight_kg": 68.4}`).
- `POST /api/v1/assistant/chat`: Conversational AI nutrition queries grounded in the user's authentic meal history.
