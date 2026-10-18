"""
FITTRACK AI — Production FastAPI Backend Application
"""

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from .core.config import settings
from .core.database import engine, Base
from .api.v1.auth import router as auth_router
from .api.v1.scans import router as scans_router
from .api.v1.foods import router as foods_router
from .api.v1.meals import router as meals_router
from .api.v1.dashboard import router as dashboard_router
from .api.v1.tracking import router as tracking_router
from .api.v1.assistant import router as assistant_router
from .api.v1.sync import router as sync_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize database tables on startup
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    # Ensure uploads directory exists
    os.makedirs(settings.STORAGE_DIR, exist_ok=True)
    yield


app = FastAPI(
    title="FITTRACK AI API",
    version=settings.VERSION,
    description=(
        "Production-ready backend for FITTRACK AI — AI Food Scanner & Nutrition Tracking. "
        "Provides multimodal food recognition, portion estimation, nutrition calculation, "
        "voice meal parsing, barcode lookup, daily dashboards, and offline synchronization."
    ),
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.BACKEND_CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Uploads directory for meal photos
os.makedirs(settings.STORAGE_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=settings.STORAGE_DIR), name="uploads")

# Include Routers under /api/v1
app.include_router(auth_router, prefix=settings.API_V1_STR)
app.include_router(scans_router, prefix=settings.API_V1_STR)
app.include_router(foods_router, prefix=settings.API_V1_STR)
app.include_router(meals_router, prefix=settings.API_V1_STR)
app.include_router(dashboard_router, prefix=settings.API_V1_STR)
app.include_router(tracking_router, prefix=settings.API_V1_STR)
app.include_router(assistant_router, prefix=settings.API_V1_STR)
app.include_router(sync_router, prefix=settings.API_V1_STR)

# Also include auth router directly at root for /auth/* compliance
app.include_router(auth_router)


@app.get("/", tags=["Health"])
@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "healthy",
        "service": "FITTRACK AI API",
        "version": settings.VERSION,
        "vision_provider": settings.AI_VISION_PROVIDER
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
