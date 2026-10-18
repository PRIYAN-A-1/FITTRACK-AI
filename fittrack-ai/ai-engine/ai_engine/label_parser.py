"""
FITTRACK AI — Nutrition Facts Label OCR Parser
Extracts structured nutrition data from camera OCR text with validation of per-100g vs per-serving metrics.
"""

import re
from typing import Dict, Any, Optional
from .models import LabelOcrResponse, NutritionalValues


class NutritionLabelParser:
    def __init__(self):
        # Regex patterns for standard international & Indian nutrition label layouts
        self.patterns = {
            "calories": [
                r"(?:energy|calories|caloric\s+value)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*(?:kcal|cal)?",
                r"([0-9]+(?:\.[0-9]+)?)\s*kcal"
            ],
            "protein": [
                r"protein[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?",
                r"protien[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ],
            "carbs": [
                r"(?:total\s+carbohydrate|carbohydrates|carbs|total\s+carbs)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ],
            "fat": [
                r"(?:total\s+fat|fat)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ],
            "fiber": [
                r"(?:dietary\s+fiber|fiber|fibre)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ],
            "sugar": [
                r"(?:total\s+sugars?|sugars?|added\s+sugars?)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ],
            "sodium": [
                r"sodium[\s:]*([0-9]+(?:\.[0-9]+)?)\s*(?:mg|g)?"
            ],
            "saturated_fat": [
                r"(?:saturated\s+fat|sat\s+fat)[\s:]*([0-9]+(?:\.[0-9]+)?)\s*g?"
            ]
        }

    def parse_label_text(self, ocr_text: str, product_name: Optional[str] = None) -> LabelOcrResponse:
        lines = [line.strip().lower() for line in ocr_text.splitlines() if line.strip()]
        full_text = " \n ".join(lines)

        # Detect per 100g vs per serving
        is_per_100g = True
        serving_size_raw = None
        serving_weight_g = 100.0

        if "per serving" in full_text or "serving size" in full_text:
            is_per_100g = False
            serving_match = re.search(r"serving\s+size[\s:]*([^\n,]+)", full_text)
            if serving_match:
                serving_size_raw = serving_match.group(1).strip()
                # Check for grams in serving size
                g_match = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*g", serving_size_raw)
                if g_match:
                    serving_weight_g = float(g_match.group(1))

        extracted_values: Dict[str, float] = {}

        for nutrient_key, regex_list in self.patterns.items():
            for regex in regex_list:
                match = re.search(regex, full_text)
                if match:
                    try:
                        val = float(match.group(1))
                        # Check if sodium was in grams instead of milligrams
                        if nutrient_key == "sodium":
                            # If value < 10, likely expressed in grams (e.g. 0.45g -> 450mg)
                            if val < 10.0 and "g" in match.group(0) and "mg" not in match.group(0):
                                val *= 1000.0
                        extracted_values[nutrient_key] = val
                        break
                    except ValueError:
                        continue

        # Confidence based on core macros found (calories, protein, carbs, fat)
        core_keys = ["calories", "protein", "carbs", "fat"]
        found_core = sum(1 for k in core_keys if k in extracted_values)
        confidence = round(found_core / len(core_keys), 2)

        nutrients = NutritionalValues(
            calories=extracted_values.get("calories", 0.0),
            protein_g=extracted_values.get("protein", 0.0),
            carbs_g=extracted_values.get("carbs", 0.0),
            fat_g=extracted_values.get("fat", 0.0),
            fiber_g=extracted_values.get("fiber", 0.0),
            sugar_g=extracted_values.get("sugar", 0.0),
            sodium_mg=extracted_values.get("sodium", 0.0),
            saturated_fat_g=extracted_values.get("saturated_fat", 0.0)
        )

        return LabelOcrResponse(
            product_name=product_name or "Packaged Nutrition Item",
            serving_size_raw=serving_size_raw,
            serving_weight_g=serving_weight_g,
            is_per_100g=is_per_100g,
            nutrients=nutrients,
            confidence=max(0.50, confidence),
            extracted_fields=extracted_values
        )
