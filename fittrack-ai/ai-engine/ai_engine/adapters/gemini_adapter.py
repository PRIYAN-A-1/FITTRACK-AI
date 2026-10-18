"""
FITTRACK AI — Google Gemini Multimodal Vision Adapter
Invokes Gemini 1.5 Flash/Pro with structured JSON output for food detection and volume/portion estimation.
"""

import os
import json
import time
import uuid
import base64
from typing import Optional, List, Dict, Any
import httpx
from .base import BaseVisionAdapter
from ..models import (
    PlateScanResponse,
    DetectedFoodItem,
    BoundingBox,
    NutritionalValues
)
from ..nutrition_matcher import NutritionMatcher


FOOD_ANALYSIS_SYSTEM_PROMPT = """
You are a world-class AI nutritionist and computer vision system for FITTRACK AI.
Analyze the user's food photo and detect each distinct food item on the plate.
For each detected item, return:
1. name: common culinary name (e.g., 'Idli', 'Sambar', 'Egg Fried Rice', 'Chicken Biryani', 'Grilled Chicken Breast')
2. estimated_weight_g: estimated mass in grams based on visual plate proportions and typical density.
3. serving_unit: e.g. 'piece', 'cup', 'bowl', 'serving', 'plate'
4. quantity: numerical count (e.g. 2 for 2 idlis, 1 for 1 cup of rice)
5. confidence: float between 0.0 and 1.0
6. bounding_box: [ymin, xmin, ymax, xmax] normalized between 0.0 and 1.0
7. cooking_method: 'fried', 'boiled', 'steamed', 'grilled', 'raw', or 'standard'
8. ingredients: list of primary recognizable ingredients
Respond strictly with a JSON object containing:
{
  "detected_items": [
    {
      "name": string,
      "estimated_weight_g": float,
      "serving_unit": string,
      "quantity": float,
      "confidence": float,
      "bounding_box": [float, float, float, float],
      "cooking_method": string,
      "ingredients": [string]
    }
  ],
  "overall_confidence": float,
  "needs_clarification": bool,
  "clarification_question": string or null
}
"""


class GeminiVisionAdapter(BaseVisionAdapter):
    def __init__(self, api_key: Optional[str] = None, matcher: Optional[NutritionMatcher] = None):
        self.api_key = api_key or os.getenv("GEMINI_API_KEY", "")
        self.matcher = matcher or NutritionMatcher()
        self.model = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

    async def analyze_image(
        self,
        image_bytes: bytes,
        mime_type: str = "image/jpeg",
        user_hint: Optional[str] = None
    ) -> PlateScanResponse:
        start_time = time.time()
        scan_id = str(uuid.uuid4())

        if not self.api_key:
            from .mock_adapter import MockVisionAdapter
            mock = MockVisionAdapter(self.matcher)
            return await mock.analyze_image(image_bytes, mime_type, user_hint)

        b64_image = base64.b64encode(image_bytes).decode("utf-8")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"

        prompt_text = FOOD_ANALYSIS_SYSTEM_PROMPT
        if user_hint:
            prompt_text += f"\nUser Context/Hint: {user_hint}"

        payload = {
            "contents": [
                {
                    "parts": [
                        {"text": prompt_text},
                        {
                            "inline_data": {
                                "mime_type": mime_type,
                                "data": b64_image
                            }
                        }
                    ]
                }
            ],
            "generationConfig": {
                "response_mime_type": "application/json",
                "temperature": 0.2
            }
        }

        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            data = resp.json()

        # Parse candidate text
        try:
            candidate_text = data["candidates"][0]["content"]["parts"][0]["text"]
            parsed_json = json.loads(candidate_text)
        except Exception:
            from .mock_adapter import MockVisionAdapter
            mock = MockVisionAdapter(self.matcher)
            return await mock.analyze_image(image_bytes, mime_type, user_hint)

        raw_items = parsed_json.get("detected_items", [])
        overall_conf = float(parsed_json.get("overall_confidence", 0.85))
        needs_clarification = bool(parsed_json.get("needs_clarification", False))
        clarification_q = parsed_json.get("clarification_question")

        detected_items: List[DetectedFoodItem] = []
        tot_cals = 0.0
        tot_prot = 0.0
        tot_carbs = 0.0
        tot_fat = 0.0
        tot_fiber = 0.0
        tot_sugar = 0.0
        tot_sodium = 0.0

        for idx, item_data in enumerate(raw_items):
            food_name = item_data.get("name", "Unknown Food")
            weight = float(item_data.get("estimated_weight_g", 100.0))
            cooking = item_data.get("cooking_method", "standard")
            
            matches = self.matcher.find_matches(food_name, limit=1)
            if matches:
                food_rec, score = matches[0]
                food_id = food_rec.get("id")
                category = food_rec.get("category", "General")
                source = food_rec.get("source_attribution", "USDA/IFCT")
            else:
                food_rec = {"per_100g": {"calories": 150, "protein_g": 4, "carbs_g": 20, "fat_g": 5}}
                score = 0.75
                food_id = None
                category = "General"
                source = "Estimate"

            nutrients = self.matcher.calculate_nutrients(food_rec, weight, cooking_method=cooking)
            box_data = item_data.get("bounding_box")
            box = None
            if box_data and len(box_data) == 4:
                box = BoundingBox(ymin=box_data[0], xmin=box_data[1], ymax=box_data[2], xmax=box_data[3])

            alternatives = self.matcher.get_alternatives(food_id or "", food_name)

            detected_items.append(DetectedFoodItem(
                item_id=f"item_{idx + 1}",
                food_id=food_id,
                name=food_name,
                category=category,
                confidence=round(float(item_data.get("confidence", score)), 2),
                bounding_box=box,
                estimated_weight_g=weight,
                serving_unit=item_data.get("serving_unit", "serving"),
                serving_weight_g=weight,
                quantity=float(item_data.get("quantity", 1.0)),
                nutrients=nutrients,
                ingredients=item_data.get("ingredients", []),
                cooking_method=cooking,
                oil_adjustment="normal",
                portion_uncertainty_pct=15.0,
                database_source=source,
                alternatives=alternatives
            ))

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
            overall_confidence=overall_conf,
            needs_user_clarification=needs_clarification,
            clarification_question=clarification_q,
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
            model_provider="gemini-multimodal",
            model_version=self.model
        )
