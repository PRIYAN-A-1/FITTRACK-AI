"""
FITTRACK AI — Nutrition Calculation Engine
Matches recognized food items to verified nutrient database entries and applies portion scaling.
"""

import os
import json
from typing import Dict, List, Optional, Tuple, Any
from pathlib import Path
from .models import NutritionalValues, AlternativeFoodMatch


class NutritionMatcher:
    def __init__(self, database_path: Optional[str] = None):
        if database_path is None:
            # Default to datasets/food_database.json
            base_dir = Path(__file__).resolve().parent.parent.parent
            database_path = str(base_dir / "datasets" / "food_database.json")
        
        self.database_path = database_path
        self.catalog: List[Dict[str, Any]] = []
        self._load_catalog()

    def _load_catalog(self):
        if os.path.exists(self.database_path):
            with open(self.database_path, "r", encoding="utf-8") as f:
                self.catalog = json.load(f)
        else:
            self.catalog = []

    def find_matches(self, query: str, limit: int = 5) -> List[Tuple[Dict[str, Any], float]]:
        """
        Searches the catalog for matches using alias, exact, and token similarity.
        Returns list of (food_record, confidence_score) sorted by score.
        """
        clean_query = query.strip().lower()
        results: List[Tuple[Dict[str, Any], float]] = []

        for item in self.catalog:
            item_name = item.get("name", "").lower()
            aliases = [a.lower() for a in item.get("aliases", [])]
            
            # Exact match
            if clean_query == item_name:
                results.append((item, 1.0))
                continue
            
            # Exact alias match
            if clean_query in aliases:
                results.append((item, 0.98))
                continue

            # Query substring in name or vice versa
            if clean_query in item_name or item_name in clean_query:
                # Score based on length ratio
                score = min(len(clean_query), len(item_name)) / max(len(clean_query), len(item_name))
                results.append((item, 0.85 * score + 0.10))
                continue

            # Check aliases partial match
            alias_matched = False
            for alias in aliases:
                if clean_query in alias or alias in clean_query:
                    score = min(len(clean_query), len(alias)) / max(len(clean_query), len(alias))
                    results.append((item, 0.80 * score + 0.10))
                    alias_matched = True
                    break
            
            if alias_matched:
                continue

            # Token overlap similarity (Jaccard on words)
            query_words = set(clean_query.split())
            name_words = set(item_name.split())
            overlap = query_words.intersection(name_words)
            if overlap:
                jaccard = len(overlap) / len(query_words.union(name_words))
                results.append((item, 0.50 + 0.40 * jaccard))

        # Sort descending by score
        results.sort(key=lambda x: x[1], reverse=True)
        return results[:limit]

    def calculate_nutrients(
        self,
        food_record: Dict[str, Any],
        weight_g: float,
        cooking_method: str = "standard",
        oil_adjustment: str = "normal"
    ) -> NutritionalValues:
        """
        Calculates nutritional values using actual weight and cooking modifiers:
        Nutrient amount = Nutrient per 100g * (Selected weight in grams / 100.0)
        """
        per_100g = food_record.get("per_100g", {})
        ratio = max(0.0, weight_g) / 100.0

        calories = per_100g.get("calories", 0.0) * ratio
        protein = per_100g.get("protein_g", 0.0) * ratio
        carbs = per_100g.get("carbs_g", 0.0) * ratio
        fat = per_100g.get("fat_g", 0.0) * ratio
        fiber = per_100g.get("fiber_g", 0.0) * ratio
        sugar = per_100g.get("sugar_g", 0.0) * ratio
        sodium = per_100g.get("sodium_mg", 0.0) * ratio
        sat_fat = per_100g.get("saturated_fat_g", 0.0) * ratio
        cholesterol = per_100g.get("cholesterol_mg", 0.0) * ratio

        # Cooking method modifiers
        if cooking_method == "fried":
            calories *= 1.15
            fat *= 1.25
            sat_fat *= 1.20
        elif cooking_method == "boiled" or cooking_method == "steamed":
            fat *= 0.90
            calories = (protein * 4.0) + (carbs * 4.0) + (fat * 9.0)

        # Oil adjustments
        if oil_adjustment == "none":
            fat *= 0.75
            calories -= (fat * 0.25 * 9.0)
        elif oil_adjustment == "low":
            fat *= 0.85
            calories -= (fat * 0.15 * 9.0)
        elif oil_adjustment == "high":
            fat *= 1.25
            calories += (fat * 0.25 * 9.0)

        return NutritionalValues(
            calories=round(max(0.0, calories), 1),
            protein_g=round(max(0.0, protein), 1),
            carbs_g=round(max(0.0, carbs), 1),
            fat_g=round(max(0.0, fat), 1),
            fiber_g=round(max(0.0, fiber), 1),
            sugar_g=round(max(0.0, sugar), 1),
            sodium_mg=round(max(0.0, sodium), 1),
            saturated_fat_g=round(max(0.0, sat_fat), 1),
            cholesterol_mg=round(max(0.0, cholesterol), 1)
        )

    def get_alternatives(self, matched_food_id: str, query: str) -> List[AlternativeFoodMatch]:
        """Provides alternate food matches when user wants to correct food selection"""
        matches = self.find_matches(query, limit=4)
        alternatives = []
        for rec, score in matches:
            if rec.get("id") != matched_food_id:
                alternatives.append(AlternativeFoodMatch(
                    food_id=rec.get("id", ""),
                    name=rec.get("name", ""),
                    confidence=round(score, 2),
                    calories_per_100g=rec.get("per_100g", {}).get("calories", 0.0)
                ))
        return alternatives
