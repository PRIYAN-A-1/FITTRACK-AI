"""
FITTRACK AI — Tracking, Goals, Dashboard and Assistant Schemas
"""

from typing import List, Optional
from datetime import date, datetime
from pydantic import BaseModel, Field, ConfigDict
from .meal_schemas import MealResponse


class NutritionGoalUpdate(BaseModel):
    daily_calories: Optional[int] = Field(None, ge=800, le=6000)
    protein_g: Optional[float] = Field(None, ge=10.0, le=400.0)
    carbs_g: Optional[float] = Field(None, ge=20.0, le=800.0)
    fat_g: Optional[float] = Field(None, ge=10.0, le=250.0)
    fiber_g: Optional[float] = Field(None, ge=5.0, le=100.0)
    water_goal_ml: Optional[int] = Field(None, ge=500, le=10000)


class NutritionGoalResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    daily_calories: int
    protein_g: float
    carbs_g: float
    fat_g: float
    fiber_g: float
    water_goal_ml: int
    is_active: bool
    start_date: date


class WaterLogCreate(BaseModel):
    amount_ml: int = Field(..., gt=0, le=3000)
    logged_date: Optional[date] = None


class WaterLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    logged_date: date
    amount_ml: int
    created_at: datetime


class WeightLogCreate(BaseModel):
    weight_kg: float = Field(..., gt=20.0, le=400.0)
    notes: Optional[str] = None
    logged_date: Optional[date] = None


class WeightLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    logged_date: date
    weight_kg: float
    notes: Optional[str] = None
    created_at: datetime


class MacroProgress(BaseModel):
    consumed: float
    target: float
    percentage: float
    remaining: float


class DailyDashboardResponse(BaseModel):
    date: date
    calories_consumed: float
    calories_target: int
    calories_remaining: float
    exercise_burned: float = 0.0
    net_calories: float = 0.0
    protein: MacroProgress
    carbs: MacroProgress
    fat: MacroProgress
    fiber_consumed: float
    fiber_target: float
    water_consumed_ml: int
    water_target_ml: int
    meals_count: int
    meals: List[MealResponse]
    streak_days: int = 1
    daily_insight: str


class AssistantMessage(BaseModel):
    role: str  # user, assistant, system
    content: str


class AssistantChatRequest(BaseModel):
    messages: List[AssistantMessage]
    include_today_context: bool = True


class AssistantChatResponse(BaseModel):
    reply: str
    suggested_actions: List[str] = []
