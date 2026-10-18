"""
FITTRACK AI — AI Engine Package
"""

from .models import (
    PlateScanResponse,
    DetectedFoodItem,
    NutritionalValues,
    VoiceMealParseResponse,
    LabelOcrResponse
)
from .nutrition_matcher import NutritionMatcher
from .voice_parser import VoiceMealParser
from .label_parser import NutritionLabelParser
from .adapters.base import BaseVisionAdapter
from .adapters.mock_adapter import MockVisionAdapter
from .adapters.gemini_adapter import GeminiVisionAdapter

__all__ = [
    "PlateScanResponse",
    "DetectedFoodItem",
    "NutritionalValues",
    "VoiceMealParseResponse",
    "LabelOcrResponse",
    "NutritionMatcher",
    "VoiceMealParser",
    "NutritionLabelParser",
    "BaseVisionAdapter",
    "MockVisionAdapter",
    "GeminiVisionAdapter"
]
