"""
FITTRACK AI — Core Application Settings
"""

from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "FITTRACK AI API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Security
    SECRET_KEY: str = "fittrack-ai-super-secret-key-change-in-production-2026"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days
    
    # Supabase / PostgreSQL Database (defaults to async sqlite for local development/testing)
    DATABASE_URL: str = "sqlite+aiosqlite:///./fittrack.db"
    SUPABASE_URL: str = ""
    SUPABASE_KEY: str = ""
    SUPABASE_SERVICE_ROLE_KEY: str = ""
    
    # AI Engine
    AI_VISION_PROVIDER: str = "mock"  # mock, gemini, openai
    GEMINI_API_KEY: str = ""
    OPENAI_API_KEY: str = ""
    
    # Storage
    STORAGE_DIR: str = "./uploads"
    
    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]

    model_config = SettingsConfigDict(
        case_sensitive=True,
        env_file=".env",
        extra="allow"
    )


settings = Settings()
