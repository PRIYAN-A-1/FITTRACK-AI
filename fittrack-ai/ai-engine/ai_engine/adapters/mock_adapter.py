"""
FITTRACK AI — Mock Vision Adapter
Deterministic, high-fidelity multimodal food detection adapter for local development,
integration tests, and zero-external-API environments.
"""

import uuid
import time
from typing import Optional, List
from .base import BaseVisionAdapter
from ..models import (
    PlateScanResponse,
    DetectedFoodItem,
    BoundingBox,
    NutritionalValues
)
from ..nutrition_matcher import NutritionMatcher


class MockVisionAdapter(BaseVisionAdapter):
    def __init__(self, matcher: Optional[NutritionMatcher] = None):
        self.matcher = matcher or NutritionMatcher()

    async def analyze_image(
        self,
        image_bytes: bytes,
        mime_type: str = "image/jpeg",
        user_hint: Optional[str] = None
    ) -> PlateScanResponse:
        start_time = time.time()
        scan_id = str(uuid.uuid4())
        hint = (user_hint or "").lower()

        # Select scenario based on hint or default to multi-item plate
        if "breakfast" in hint or "idli" in hint or "dosa" in hint:
            raw_items = [
                {"name": "Idli", "weight_g": 120.0, "qty": 2.0, "unit": "piece", "box": BoundingBox(ymin=0.2, xmin=0.15, ymax=0.6, xmax=0.45)},
                {"name": "Plain Dosa", "weight_g": 85.0, "qty": 1.0, "unit": "piece", "box": BoundingBox(ymin=0.1, xmin=0.45, ymax=0.8, xmax=0.9)},
                {"name": "Sambar", "weight_g": 120.0, "qty": 1.0, "unit": "bowl", "box": BoundingBox(ymin=0.6, xmin=0.15, ymax=0.9, xmax=0.45)},
                {"name": "Coconut Chutney", "weight_g": 35.0, "qty": 1.0, "unit": "tablespoon", "box": BoundingBox(ymin=0.65, xmin=0.48, ymax=0.9, xmax=0.7)}
            ]
        elif "biryani" in hint or "chicken" in hint:
            raw_items = [
                {"name": "Chicken Biryani", "weight_g": 350.0, "qty": 1.0, "unit": "portion", "box": BoundingBox(ymin=0.15, xmin=0.15, ymax=0.85, xmax=0.75)},
                {"name": "Curd (Plain Dahi / Yogurt)", "weight_g": 100.0, "qty": 1.0, "unit": "cup", "box": BoundingBox(ymin=0.25, xmin=0.75, ymax=0.7, xmax=0.95)}
            ]
        elif "chapati" in hint or "roti" in hint or "paneer" in hint:
            raw_items = [
                {"name": "Chapati / Roti", "weight_g": 90.0, "qty": 2.0, "unit": "piece", "box": BoundingBox(ymin=0.1, xmin=0.1, ymax=0.7, xmax=0.55)},
                {"name": "Paneer Butter Masala", "weight_g": 180.0, "qty": 1.0, "unit": "bowl", "box": BoundingBox(ymin=0.15, xmin=0.55, ymax=0.85, xmax=0.95)}
            ]
        elif "burger" in hint or "fries" in hint or "fast food" in hint:
            raw_items = [
                {"name": "Cheeseburger", "weight_g": 220.0, "qty": 1.0, "unit": "burger", "box": BoundingBox(ymin=0.2, xmin=0.1, ymax=0.85, xmax=0.6)},
                {"name": "French Fries", "weight_g": 115.0, "qty": 1.0, "unit": "portion", "box": BoundingBox(ymin=0.25, xmin=0.6, ymax=0.8, xmax=0.95)}
            ]
        elif "egg fried rice" in hint or "egg rice" in hint or "fried rice" in hint:
            raw_items = [
                {"name": "Egg Fried Rice", "weight_g": 310.0, "qty": 1.0, "unit": "plate", "box": BoundingBox(ymin=0.15, xmin=0.15, ymax=0.85, xmax=0.85)}
            ]
        else:
            # Default authentic multi-item Indian meal plate (matches prompt specification:
            # Rice, Sambar, Rasam, Potato fry, Curd)
            raw_items = [
                {"name": "Steamed White Rice", "weight_g": 200.0, "qty": 1.0, "unit": "cup", "box": BoundingBox(ymin=0.25, xmin=0.25, ymax=0.75, xmax=0.75)},
                {"name": "Sambar", "weight_g": 130.0, "qty": 1.0, "unit": "bowl", "box": BoundingBox(ymin=0.05, xmin=0.05, ymax=0.35, xmax=0.4)},
                {"name": "Potato Fry (Aloo Fry)", "weight_g": 90.0, "qty": 1.0, "unit": "serving", "box": BoundingBox(ymin=0.05, xmin=0.6, ymax=0.35, xmax=0.95)},
                {"name": "Curd (Plain Dahi / Yogurt)", "weight_g": 100.0, "qty": 1.0, "unit": "cup", "box": BoundingBox(ymin=0.65, xmin=0.05, ymax=0.95, xmax=0.4)},
                {"name": "Rasam", "weight_g": 120.0, "qty": 1.0, "unit": "cup", "box": BoundingBox(ymin=0.65, xmin=0.6, ymax=0.95, xmax=0.95)}
            ]

        detected_items: List[DetectedFoodItem] = []
        tot_cals = 0.0
        tot_prot = 0.0
        tot_carbs = 0.0
        tot_fat = 0.0
        tot_fiber = 0.0
        tot_sugar = 0.0
        tot_sodium = 0.0

        for idx, it in enumerate(raw_items):
            matches = self.matcher.find_matches(it["name"], limit=1)
            if matches:
                food_rec, match_score = matches[0]
                food_id = food_rec.get("id")
                category = food_rec.get("category", "General")
                ingredients = food_rec.get("ingredients", [])
                source = food_rec.get("source_attribution", "IFCT")
            else:
                food_rec = {"per_100g": {"calories": 150, "protein_g": 4, "carbs_g": 20, "fat_g": 5}}
                match_score = 0.85
                food_id = f"custom_{idx}"
                category = "General"
                ingredients = []
                source = "Estimate"

            weight = it["weight_g"]
            nutrients = self.matcher.calculate_nutrients(food_rec, weight)
            alternatives = self.matcher.get_alternatives(food_id or "", it["name"])

            detected_item = DetectedFoodItem(
                item_id=f"item_{idx + 1}",
                food_id=food_id,
                name=it["name"],
                category=category,
                confidence=round(match_score, 2),
                bounding_box=it.get("box"),
                estimated_weight_g=weight,
                serving_unit=it.get("unit", "g"),
                serving_weight_g=weight,
                quantity=it.get("qty", 1.0),
                nutrients=nutrients,
                ingredients=ingredients,
                cooking_method="standard",
                oil_adjustment="normal",
                portion_uncertainty_pct=15.0,
                database_source=source,
                alternatives=alternatives
            )
            detected_items.append(detected_item)

            tot_cals += nutrients.calories
            tot_prot += nutrients.protein_g
            tot_carbs += nutrients.carbs_g
            tot_fat += nutrients.fat_g
            tot_fiber += nutrients.fiber_g
            tot_sugar += nutrients.sugar_g
            tot_sodium += nutrients.sodium_mg

        latency_ms = int((time.time() - start_time) * 1000)

        return PlateScanResponse(
            scan_id=scan_id,
            status="completed",
            items=detected_items,
            overall_confidence=0.92,
            needs_user_clarification=False,
            clarification_question=None,
            total_nutrients=NutritionalValues(
                calories=round(tot_cals, 1),
                protein_g=round(tot_prot, 1),
                carbs_g=round(tot_carbs, 1),
                fat_g=round(tot_fat, 1),
                fiber_g=round(tot_fiber, 1),
                sugar_g=round(tot_sugar, 1),
                sodium_mg=round(tot_sodium, 1)
            ),
            latency_ms=latency_ms,
            model_provider="mock-vision-engine",
            model_version="2.0-precision"
        )
