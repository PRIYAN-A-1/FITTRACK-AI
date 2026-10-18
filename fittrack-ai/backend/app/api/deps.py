"""
FITTRACK AI — API Dependency Injection and Security Guards
"""

import sys
from pathlib import Path
from typing import AsyncGenerator
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import jwt, JWTError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

# Add ai-engine to sys.path so backend can import from it directly
for candidate in [
    Path(__file__).resolve().parents[3] / "ai-engine",
    Path(__file__).resolve().parents[2] / "ai-engine",
    Path.cwd() / "ai-engine"
]:
    if candidate.exists() and str(candidate) not in sys.path:
        sys.path.insert(0, str(candidate))

from ai_engine.nutrition_matcher import NutritionMatcher
from ai_engine.voice_parser import VoiceMealParser
from ai_engine.label_parser import NutritionLabelParser
from ai_engine.adapters.base import BaseVisionAdapter
from ai_engine.adapters.mock_adapter import MockVisionAdapter
from ai_engine.adapters.gemini_adapter import GeminiVisionAdapter

from ..core.config import settings
from ..core.database import get_db
from ..models.db_models import Profile

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.API_V1_STR}/auth/login")

# Singletons for speed
_matcher = NutritionMatcher()
_voice_parser = VoiceMealParser(_matcher)
_label_parser = NutritionLabelParser()


def get_nutrition_matcher() -> NutritionMatcher:
    return _matcher


def get_voice_meal_parser() -> VoiceMealParser:
    return _voice_parser


def get_nutrition_label_parser() -> NutritionLabelParser:
    return _label_parser


def get_ai_vision_adapter() -> BaseVisionAdapter:
    if settings.AI_VISION_PROVIDER == "gemini" and settings.GEMINI_API_KEY:
        return GeminiVisionAdapter(api_key=settings.GEMINI_API_KEY, matcher=_matcher)
    return MockVisionAdapter(matcher=_matcher)


async def get_current_user(
    db: AsyncSession = Depends(get_db),
    token: str = Depends(oauth2_scheme)
) -> Profile:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate authentication credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    stmt = select(Profile).where(Profile.user_id == user_id)
    result = await db.execute(stmt)
    profile = result.scalars().first()

    if profile is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User profile not found")
    
    return profile
