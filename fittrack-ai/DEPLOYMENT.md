# FITTRACK AI — Deployment & Infrastructure Guide

This guide covers production deployment for the FastAPI backend, Supabase PostgreSQL, and Google Play release packaging.

---

## 1. Backend Production Deployment

### 1.1 Containerization with Docker

Create a `Dockerfile` for the backend:
```dockerfile
FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY backend/ .
COPY ai-engine/ /app/ai-engine/
COPY datasets/ /app/datasets/

ENV PYTHONPATH="/app:/app/ai-engine"

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

### 1.2 Cloud Hosting Options
- **Google Cloud Run**: Serverless container hosting with automatic scaling.
- **Render / Railway / AWS ECS**: Standard Docker web service with persistent storage for `/uploads`.

---

## 2. Supabase Cloud Database Configuration

1. Create a new Supabase Project at [supabase.com](https://supabase.com).
2. Open the **SQL Editor** in the Supabase Dashboard.
3. Paste and execute `database/schema.sql`.
4. Paste and execute `database/seed_data.sql` to populate default nutrient definitions and composite food items.
5. In your production environment, update `DATABASE_URL`:
   ```bash
   DATABASE_URL=postgresql+asyncpg://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres
   ```

---

## 3. Android Production Release (Google Play)

### 3.1 Keystore Generation
Generate a release signing key using `keytool`:
```powershell
keytool -genkey -v -keystore fittrack-release.jks -alias fittrack-key -keyalg RSA -keysize 2048 -validity 10000
```

### 3.2 Configure Signing in `android/app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "fittrack-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3.3 Build Android App Bundle (AAB)
```powershell
cd android
.\gradlew.bat bundleRelease
```
The resulting AAB will be output to:
`android/app/build/outputs/bundle/release/app-release.aab`
Ready for upload to Google Play Console.
