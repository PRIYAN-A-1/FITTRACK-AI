"""
FITTRACK AI — AI Food Scans and Multimodal Analysis Endpoints
"""

import os
import uuid
from datetime import datetime, date
from typing import Optional, List
from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ..deps import (
    get_current_user,
    get_ai_vision_adapter,
    get_voice_meal_parser,
    get_nutrition_label_parser,
    get_nutrition_matcher
)
from ...core.database import get_db
from ...core.config import settings
from ...models.db_models import Profile, FoodScan, MealEntry, MealItem
from ...schemas.meal_schemas import MealResponse, MealCreate, MealItemCreate
import sys
from pathlib import Path

ai_engine_path = str(Path(__file__).resolve().parent.parent.parent.parent / "ai-engine")
if ai_engine_path not in sys.path:
    sys.path.insert(0, ai_engine_path)

from ai_engine.models import (
    PlateScanResponse,
    VoiceMealParseResponse,
    LabelOcrResponse,
    DetectedFoodItem
)
from ai_engine.adapters.base import BaseVisionAdapter
from ai_engine.voice_parser import VoiceMealParser
from ai_engine.label_parser import NutritionLabelParser
from ai_engine.nutrition_matcher import NutritionMatcher

router = APIRouter(prefix="", tags=["Food AI"])


class VoiceParseRequest(BaseModel):
    transcript: str


class LabelOcrRequest(BaseModel):
    ocr_text: str
    product_name: Optional[str] = None


class ConfirmScanMealRequest(BaseModel):
    meal_type: str = "lunch"
    meal_title: str
    logged_date: Optional[date] = None
    items: List[MealItemCreate]
    photo_url: Optional[str] = None
    notes: Optional[str] = None


@router.post("/scans", response_model=PlateScanResponse)
async def create_food_scan(
    image: UploadFile = File(...),
    hint: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user),
    adapter: BaseVisionAdapter = Depends(get_ai_vision_adapter)
):
    # Validate image format
    if not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file must be a valid image")

    image_bytes = await image.read()
    if len(image_bytes) > 15 * 1024 * 1024:  # 15MB limit
        raise HTTPException(status_code=400, detail="Image exceeds maximum 15MB size limit")

    # Save to storage directory
    os.makedirs(settings.STORAGE_DIR, exist_ok=True)
    filename = f"{uuid.uuid4().hex}_{image.filename}"
    file_path = os.path.join(settings.STORAGE_DIR, filename)
    with open(file_path, "wb") as f:
        f.write(image_bytes)

    # Perform multimodal AI vision analysis
    result = await adapter.analyze_image(
        image_bytes=image_bytes,
        mime_type=image.content_type,
        user_hint=hint
    )

    # Persist scan record
    scan_record = FoodScan(
        id=result.scan_id,
        user_id=current_user.user_id,
        image_storage_path=file_path,
        status="completed",
        model_provider=result.model_provider,
        model_version=result.model_version,
        processing_latency_ms=result.latency_ms
    )
    db.add(scan_record)
    await db.commit()

    return result


@router.post("/scans/{scan_id}/confirm", response_model=MealResponse)
async def confirm_scan_as_meal(
    scan_id: str,
    meal_data: ConfirmScanMealRequest,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    # Calculate totals
    tot_cals = sum(it.calories for it in meal_data.items)
    tot_prot = sum(it.protein_g for it in meal_data.items)
    tot_carbs = sum(it.carbs_g for it in meal_data.items)
    tot_fat = sum(it.fat_g for it in meal_data.items)
    tot_fiber = sum(it.fiber_g for it in meal_data.items)

    meal = MealEntry(
        id=str(uuid.uuid4()),
        user_id=current_user.user_id,
        scan_id=scan_id if scan_id != "manual" else None,
        meal_type=meal_data.meal_type,
        meal_title=meal_data.meal_title,
        logged_date=meal_data.logged_date or date.today(),
        total_calories=round(tot_cals, 1),
        total_protein_g=round(tot_prot, 1),
        total_carbs_g=round(tot_carbs, 1),
        total_fat_g=round(tot_fat, 1),
        total_fiber_g=round(tot_fiber, 1),
        photo_url=meal_data.photo_url,
        source="ai_scan" if scan_id != "manual" else "manual",
        notes=meal_data.notes
    )
    db.add(meal)

    for it in meal_data.items:
        meal_item = MealItem(
            id=str(uuid.uuid4()),
            meal_id=meal.id,
            food_id=it.food_id,
            name=it.name,
            serving_unit=it.serving_unit,
            serving_weight_g=it.serving_weight_g,
            quantity=it.quantity,
            calories=round(it.calories, 1),
            protein_g=round(it.protein_g, 1),
            carbs_g=round(it.carbs_g, 1),
            fat_g=round(it.fat_g, 1),
            fiber_g=round(it.fiber_g, 1),
            cooking_method=it.cooking_method,
            oil_adjustment=it.oil_adjustment
        )
        db.add(meal_item)

    await db.commit()

    # Re-fetch with items loaded
    stmt = select(MealEntry).where(MealEntry.id == meal.id)
    res = await db.execute(stmt)
    saved_meal = res.scalars().first()
    return saved_meal


@router.post("/voice/parse", response_model=VoiceMealParseResponse)
async def parse_voice_meal(
    payload: VoiceParseRequest,
    current_user: Profile = Depends(get_current_user),
    parser: VoiceMealParser = Depends(get_voice_meal_parser)
):
    result = parser.parse_transcript(payload.transcript)
    return result


@router.post("/labels/analyze", response_model=LabelOcrResponse)
async def analyze_nutrition_label(
    payload: LabelOcrRequest,
    current_user: Profile = Depends(get_current_user),
    parser: NutritionLabelParser = Depends(get_nutrition_label_parser)
):
    result = parser.parse_label_text(payload.ocr_text, product_name=payload.product_name)
    return result
