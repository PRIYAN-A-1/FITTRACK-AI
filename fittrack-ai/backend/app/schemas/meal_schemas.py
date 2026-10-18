"""
FITTRACK AI — Meal and Food Schemas
"""

from typing import List, Optional
from datetime import date, time, datetime
from pydantic import BaseModel, Field, ConfigDict


class MealItemCreate(BaseModel):
    food_id: Optional[str] = None
    name: str
    serving_unit: str = "g"
    serving_weight_g: float = 100.0
    quantity: float = 1.0
    calories: float
    protein_g: float
    carbs_g: float
    fat_g: float
    fiber_g: float = 0.0
    cooking_method: str = "standard"
    oil_adjustment: str = "normal"


class MealItemResponse(MealItemCreate):
    model_config = ConfigDict(from_attributes=True)

    id: str
    meal_id: str


class MealCreate(BaseModel):
    meal_type: str = Field(..., description="breakfast, lunch, dinner, snack")
    meal_title: str
    logged_date: Optional[date] = None
    items: List[MealItemCreate] = []
    photo_url: Optional[str] = None
    source: str = "manual"  # ai_scan, barcode, voice, manual
    notes: Optional[str] = None


class MealUpdate(BaseModel):
    meal_type: Optional[str] = None
    meal_title: Optional[str] = None
    logged_date: Optional[date] = None
    items: Optional[List[MealItemCreate]] = None
    photo_url: Optional[str] = None
    notes: Optional[str] = None
    is_favorite: Optional[bool] = None


class MealResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    meal_type: str
    meal_title: str
    logged_date: date
    logged_time: Optional[time] = None
    total_calories: float
    total_protein_g: float
    total_carbs_g: float
    total_fat_g: float
    total_fiber_g: float
    photo_url: Optional[str] = None
    source: str
    is_favorite: bool
    items: List[MealItemResponse] = []
    created_at: datetime
    updated_at: datetime


class CustomFoodCreate(BaseModel):
    name: str
    brand: Optional[str] = None
    barcode: Optional[str] = None
    serving_unit: str = "serving"
    serving_weight_g: float = 100.0
    calories: float
    protein_g: float
    carbs_g: float
    fat_g: float
    fiber_g: float = 0.0
    sugar_g: float = 0.0
    sodium_mg: float = 0.0


class CustomFoodResponse(CustomFoodCreate):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
