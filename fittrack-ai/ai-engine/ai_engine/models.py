"""
FITTRACK AI — AI Engine Pydantic Data Models
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    ymin: float = 0.0
    xmin: float = 0.0
    ymax: float = 1.0
    xmax: float = 1.0


class NutritionalValues(BaseModel):
    calories: float = Field(0.0, description="Energy in kcal")
    protein_g: float = Field(0.0, description="Protein in grams")
    carbs_g: float = Field(0.0, description="Carbohydrates in grams")
    fat_g: float = Field(0.0, description="Total fat in grams")
    fiber_g: float = Field(0.0, description="Dietary fiber in grams")
    sugar_g: float = Field(0.0, description="Total sugars in grams")
    sodium_mg: float = Field(0.0, description="Sodium in milligrams")
    saturated_fat_g: float = Field(0.0, description="Saturated fat in grams")
    cholesterol_mg: float = Field(0.0, description="Cholesterol in milligrams")


class AlternativeFoodMatch(BaseModel):
    food_id: str
    name: str
    confidence: float
    calories_per_100g: float


class DetectedFoodItem(BaseModel):
    item_id: str
    food_id: Optional[str] = None
    name: str
    category: str = "General"
    confidence: float = Field(..., ge=0.0, le=1.0)
    bounding_box: Optional[BoundingBox] = None
    estimated_weight_g: float = Field(..., gt=0.0)
    serving_unit: str = "g"
    serving_weight_g: float = 100.0
    quantity: float = 1.0
    nutrients: NutritionalValues
    ingredients: List[str] = []
    cooking_method: str = "standard"  # raw, boiled, grilled, fried, curried
    oil_adjustment: str = "normal"    # none, low, normal, high
    portion_uncertainty_pct: float = 15.0
    database_source: str = "USDA / IFCT"
    alternatives: List[AlternativeFoodMatch] = []


class PlateScanResponse(BaseModel):
    scan_id: str
    status: str = "completed"
    items: List[DetectedFoodItem]
    overall_confidence: float
    needs_user_clarification: bool = False
    clarification_question: Optional[str] = None
    total_nutrients: NutritionalValues
    latency_ms: int = 0
    model_provider: str = "adapter"
    model_version: str = "1.0"


class VoiceMealItemParsed(BaseModel):
    matched_food_id: Optional[str] = None
    food_name: str
    quantity: float
    unit: str
    estimated_weight_g: float
    nutrients: NutritionalValues
    confidence: float


class VoiceMealParseResponse(BaseModel):
    raw_transcript: str
    parsed_items: List[VoiceMealItemParsed]
    total_nutrients: NutritionalValues
    unparsed_tokens: List[str] = []
    success: bool = True


class LabelOcrResponse(BaseModel):
    product_name: Optional[str] = "Packaged Food"
    serving_size_raw: Optional[str] = None
    serving_weight_g: float = 100.0
    is_per_100g: bool = True
    nutrients: NutritionalValues
    confidence: float = 0.90
    extracted_fields: Dict[str, Any] = {}
