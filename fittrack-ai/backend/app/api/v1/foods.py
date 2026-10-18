"""
FITTRACK AI — Food Catalog, Barcode and Custom Foods API Endpoints
"""

import json
import os
import uuid
from pathlib import Path
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ..deps import get_current_user, get_nutrition_matcher
from ...core.database import get_db
from ...models.db_models import Profile, CustomFood
from ...schemas.meal_schemas import CustomFoodCreate, CustomFoodResponse
import sys

ai_engine_path = str(Path(__file__).resolve().parent.parent.parent.parent / "ai-engine")
if ai_engine_path not in sys.path:
    sys.path.insert(0, ai_engine_path)

from ai_engine.nutrition_matcher import NutritionMatcher

router = APIRouter(prefix="/foods", tags=["Foods"])

# Load barcode database
_barcode_cache = {}
for p in [
    Path(__file__).resolve().parents[4] / "datasets" / "barcode_database.json",
    Path(__file__).resolve().parents[3] / "datasets" / "barcode_database.json",
    Path.cwd() / "datasets" / "barcode_database.json"
]:
    if p.exists():
        with open(p, "r", encoding="utf-8") as f:
            for item in json.load(f):
                _barcode_cache[str(item.get("barcode", "")).strip()] = item
        break


@router.get("/search")
async def search_foods(
    q: str = Query(..., min_length=1, description="Food search query"),
    limit: int = Query(20, ge=1, le=50),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user),
    matcher: NutritionMatcher = Depends(get_nutrition_matcher)
):
    # 1. Search verified catalog
    catalog_matches = matcher.find_matches(q, limit=limit)
    formatted_results = []
    for food_rec, score in catalog_matches:
        formatted_results.append({
            "id": food_rec.get("id"),
            "name": food_rec.get("name"),
            "category": food_rec.get("category"),
            "cuisine": food_rec.get("cuisine"),
            "serving_unit": food_rec.get("serving_unit", "serving"),
            "serving_weight_g": food_rec.get("serving_weight_g", 100.0),
            "per_100g": food_rec.get("per_100g", {}),
            "is_custom": False,
            "match_confidence": round(score, 2),
            "source": food_rec.get("source_attribution", "USDA / IFCT")
        })

    # 2. Search user's custom foods
    stmt = select(CustomFood).where(
        CustomFood.user_id == current_user.user_id,
        CustomFood.name.ilike(f"%{q}%")
    ).limit(10)
    res = await db.execute(stmt)
    custom_items = res.scalars().all()
    for c in custom_items:
        formatted_results.insert(0, {
            "id": c.id,
            "name": c.name,
            "category": "Custom Food",
            "cuisine": "Custom",
            "serving_unit": c.serving_unit,
            "serving_weight_g": c.serving_weight_g,
            "per_100g": {
                "calories": round((c.calories / c.serving_weight_g) * 100, 1),
                "protein_g": round((c.protein_g / c.serving_weight_g) * 100, 1),
                "carbs_g": round((c.carbs_g / c.serving_weight_g) * 100, 1),
                "fat_g": round((c.fat_g / c.serving_weight_g) * 100, 1),
                "fiber_g": round((c.fiber_g / c.serving_weight_g) * 100, 1)
            },
            "is_custom": True,
            "match_confidence": 1.0,
            "source": "My Custom Foods"
        })

    return formatted_results[:limit]


@router.get("/barcode/{code}")
async def lookup_barcode(
    code: str,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    clean_code = code.strip()

    # 1. Check local packaged food cache
    if clean_code in _barcode_cache:
        product = _barcode_cache[clean_code]
        return {
            "found": True,
            "barcode": clean_code,
            "name": product.get("product_name"),
            "brand": product.get("brand"),
            "serving_size": product.get("serving_size"),
            "serving_weight_g": product.get("serving_weight_g", 100.0),
            "per_100g": product.get("per_100g"),
            "source": product.get("source", "Open Food Facts")
        }

    # 2. Check user's custom foods with barcode
    stmt = select(CustomFood).where(
        CustomFood.user_id == current_user.user_id,
        CustomFood.barcode == clean_code
    )
    res = await db.execute(stmt)
    custom_match = res.scalars().first()
    if custom_match:
        return {
            "found": True,
            "barcode": clean_code,
            "name": custom_match.name,
            "brand": custom_match.brand,
            "serving_size": custom_match.serving_unit,
            "serving_weight_g": custom_match.serving_weight_g,
            "per_100g": {
                "calories": round((custom_match.calories / custom_match.serving_weight_g) * 100, 1),
                "protein_g": round((custom_match.protein_g / custom_match.serving_weight_g) * 100, 1),
                "carbs_g": round((custom_match.carbs_g / custom_match.serving_weight_g) * 100, 1),
                "fat_g": round((custom_match.fat_g / custom_match.serving_weight_g) * 100, 1)
            },
            "source": "Custom Saved Barcode"
        }

    # Not found
    raise HTTPException(status_code=404, detail=f"Barcode {clean_code} not found in database. You can take a photo of the nutrition label instead.")


@router.post("/custom", response_model=CustomFoodResponse, status_code=201)
async def create_custom_food(
    food_in: CustomFoodCreate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    custom_food = CustomFood(
        id=str(uuid.uuid4()),
        user_id=current_user.user_id,
        name=food_in.name,
        brand=food_in.brand,
        barcode=food_in.barcode,
        serving_unit=food_in.serving_unit,
        serving_weight_g=food_in.serving_weight_g,
        calories=food_in.calories,
        protein_g=food_in.protein_g,
        carbs_g=food_in.carbs_g,
        fat_g=food_in.fat_g,
        fiber_g=food_in.fiber_g,
        sugar_g=food_in.sugar_g,
        sodium_mg=food_in.sodium_mg
    )
    db.add(custom_food)
    await db.commit()
    await db.refresh(custom_food)
    return custom_food
