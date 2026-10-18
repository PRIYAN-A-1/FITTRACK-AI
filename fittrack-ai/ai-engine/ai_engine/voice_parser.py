"""
FITTRACK AI — Voice Meal Parsing Engine
Extracts food items, numerical quantities, and serving units from spoken transcripts
(English, Indian English, Tanglish/Tamil colloquial expressions).
"""

import re
from typing import List, Dict, Tuple, Optional
from .models import (
    VoiceMealParseResponse,
    VoiceMealItemParsed,
    NutritionalValues
)
from .nutrition_matcher import NutritionMatcher


NUMBER_WORDS = {
    "zero": 0.0, "half": 0.5, "quarter": 0.25,
    "one": 1.0, "a": 1.0, "an": 1.0, "single": 1.0,
    "two": 2.0, "pair": 2.0,
    "three": 3.0, "four": 4.0, "five": 5.0, "six": 6.0,
    "seven": 7.0, "eight": 8.0, "nine": 9.0, "ten": 10.0,
    # Tanglish / Tamil number transliterations
    "oru": 1.0, "rendu": 2.0, "moonu": 3.0, "naalu": 4.0, "anju": 5.0, "arru": 6.0
}

UNIT_GRAM_MAP = {
    "gram": 1.0, "grams": 1.0, "g": 1.0, "gm": 1.0, "gms": 1.0,
    "kg": 1000.0, "kilo": 1000.0, "kilogram": 1000.0,
    "cup": 160.0, "cups": 160.0,
    "bowl": 180.0, "bowls": 180.0, "katori": 150.0,
    "plate": 300.0, "plates": 300.0,
    "glass": 240.0, "glasses": 240.0,
    "teaspoon": 5.0, "teaspoons": 5.0, "tsp": 5.0,
    "tablespoon": 15.0, "tablespoons": 15.0, "tbsp": 15.0,
    "piece": 1.0, "pieces": 1.0, "pc": 1.0, "pcs": 1.0,
    "slice": 30.0, "slices": 30.0
}


class VoiceMealParser:
    def __init__(self, matcher: Optional[NutritionMatcher] = None):
        self.matcher = matcher or NutritionMatcher()

    def parse_transcript(self, transcript: str) -> VoiceMealParseResponse:
        clean_text = transcript.lower().strip()
        
        # Remove conversational filler phrases
        fillers = [
            "i ate", "i had", "i have eaten", "i drank", "log", "add", "record",
            "please add", "can you add", "for breakfast", "for lunch", "for dinner",
            "and", "with", "along with", "plus", "also"
        ]
        
        # Replace connectors with delimiters for splitting items
        working_text = clean_text
        for connector in [" and ", " with ", " along with ", " plus ", ", ", " & "]:
            working_text = working_text.replace(connector, " | ")

        segments = [s.strip() for s in working_text.split("|") if s.strip()]
        
        parsed_items: List[VoiceMealItemParsed] = []
        tot_cals = 0.0
        tot_prot = 0.0
        tot_carbs = 0.0
        tot_fat = 0.0
        tot_fiber = 0.0
        tot_sugar = 0.0
        tot_sodium = 0.0

        for segment in segments:
            # Strip initial fillers from each segment
            seg = segment
            for f in fillers:
                if seg.startswith(f + " "):
                    seg = seg[len(f) + 1:].strip()

            parsed_item = self._parse_single_item(seg)
            if parsed_item:
                parsed_items.append(parsed_item)
                tot_cals += parsed_item.nutrients.calories
                tot_prot += parsed_item.nutrients.protein_g
                tot_carbs += parsed_item.nutrients.carbs_g
                tot_fat += parsed_item.nutrients.fat_g
                tot_fiber += parsed_item.nutrients.fiber_g
                tot_sugar += parsed_item.nutrients.sugar_g
                tot_sodium += parsed_item.nutrients.sodium_mg

        return VoiceMealParseResponse(
            raw_transcript=transcript,
            parsed_items=parsed_items,
            total_nutrients=NutritionalValues(
                calories=round(tot_cals, 1),
                protein_g=round(tot_prot, 1),
                carbs_g=round(tot_carbs, 1),
                fat_g=round(tot_fat, 1),
                fiber_g=round(tot_fiber, 1),
                sugar_g=round(tot_sugar, 1),
                sodium_mg=round(tot_sodium, 1)
            ),
            unparsed_tokens=[],
            success=len(parsed_items) > 0
        )

    def _parse_single_item(self, segment: str) -> Optional[VoiceMealItemParsed]:
        words = segment.split()
        if not words:
            return None

        qty = 1.0
        unit = "serving"
        food_tokens = []
        skip_indices = set()

        # Step 1: Detect quantity (e.g., '2', '200', 'two', 'half')
        for i, w in enumerate(words):
            if re.match(r"^\d+(\.\d+)?$", w):
                qty = float(w)
                skip_indices.add(i)
                break
            elif w in NUMBER_WORDS:
                qty = NUMBER_WORDS[w]
                skip_indices.add(i)
                break

        # Step 2: Detect unit (e.g., 'grams', 'cups', 'dosas', 'pieces')
        for i, w in enumerate(words):
            if i in skip_indices:
                continue
            cleaned_w = re.sub(r"[^\w]", "", w)
            if cleaned_w in UNIT_GRAM_MAP:
                unit = cleaned_w
                skip_indices.add(i)
                break

        # Step 3: Remaining words form the food name
        for i, w in enumerate(words):
            if i not in skip_indices:
                food_tokens.append(w)

        food_phrase = " ".join(food_tokens).strip()
        if not food_phrase:
            # Check if the unit itself was a food name pluralized (e.g. 'two dosas', 'three idlis')
            if unit in ["dosas", "dosa", "idlis", "idli", "eggs", "bananas", "apples"]:
                food_phrase = unit
                unit = "piece"
            else:
                return None

        # Clean plural s for search
        search_query = food_phrase
        if search_query.endswith("s") and len(search_query) > 3 and not search_query.endswith("ss"):
            search_query_singular = search_query[:-1]
        else:
            search_query_singular = search_query

        matches = self.matcher.find_matches(search_query_singular, limit=1)
        if not matches:
            matches = self.matcher.find_matches(search_query, limit=1)

        if matches:
            food_rec, confidence = matches[0]
            matched_id = food_rec.get("id")
            official_name = food_rec.get("name", food_phrase)
            default_serving_weight = float(food_rec.get("default_serving_weight_g", 100.0))
        else:
            food_rec = {"per_100g": {"calories": 140, "protein_g": 3.5, "carbs_g": 20, "fat_g": 4}}
            confidence = 0.65
            matched_id = None
            official_name = food_phrase.title()
            default_serving_weight = 100.0

        # Calculate weight in grams
        if unit in ["gram", "grams", "g", "gm", "gms"]:
            weight_g = qty
        elif unit in UNIT_GRAM_MAP:
            weight_g = qty * UNIT_GRAM_MAP[unit]
        else:
            # Piece or serving multiplier
            weight_g = qty * default_serving_weight

        nutrients = self.matcher.calculate_nutrients(food_rec, weight_g)

        return VoiceMealItemParsed(
            matched_food_id=matched_id,
            food_name=official_name,
            quantity=qty,
            unit=unit,
            estimated_weight_g=round(weight_g, 1),
            nutrients=nutrients,
            confidence=round(confidence, 2)
        )
