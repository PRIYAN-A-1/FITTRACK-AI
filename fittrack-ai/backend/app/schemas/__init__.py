"""Schemas Package"""
from .auth_schemas import UserRegister, UserLogin, TokenResponse, ProfileResponse, ProfileUpdate
from .meal_schemas import MealCreate, MealUpdate, MealResponse, MealItemCreate, MealItemResponse, CustomFoodCreate, CustomFoodResponse
from .tracking_schemas import (
    NutritionGoalResponse, NutritionGoalUpdate, WaterLogCreate, WaterLogResponse,
    WeightLogCreate, WeightLogResponse, DailyDashboardResponse, AssistantChatRequest, AssistantChatResponse
)

__all__ = [
    "UserRegister", "UserLogin", "TokenResponse", "ProfileResponse", "ProfileUpdate",
    "MealCreate", "MealUpdate", "MealResponse", "MealItemCreate", "MealItemResponse",
    "CustomFoodCreate", "CustomFoodResponse",
    "NutritionGoalResponse", "NutritionGoalUpdate", "WaterLogCreate", "WaterLogResponse",
    "WeightLogCreate", "WeightLogResponse", "DailyDashboardResponse",
    "AssistantChatRequest", "AssistantChatResponse"
]
