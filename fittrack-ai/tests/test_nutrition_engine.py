"""
FITTRACK AI — Test Suite: Nutrition Calculation Engine
"""

import sys
from pathlib import Path
import pytest

# Ensure ai-engine is in path
base_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(base_dir / "ai-engine"))

from ai_engine.nutrition_matcher import NutritionMatcher
from ai_engine.models import NutritionalValues


@pytest.fixture
def matcher():
    db_path = str(base_dir / "datasets" / "food_database.json")
    return NutritionMatcher(database_path=db_path)


def test_egg_fried_rice_matching(matcher):
    matches = matcher.find_matches("egg fried rice")
    assert len(matches) > 0
    top_match, score = matches[0]
    assert top_match["id"] == "ind_001"
    assert top_match["name"] == "Egg Fried Rice"
    assert score >= 0.95


def test_alias_matching_indian_breakfast(matcher):
    # Test Tamil/colloquial alias 'dosai'
    dosa_matches = matcher.find_matches("dosai")
    assert len(dosa_matches) > 0
    assert dosa_matches[0][0]["id"] == "ind_003"

    # Test 'idly'
    idli_matches = matcher.find_matches("idly")
    assert len(idli_matches) > 0
    assert idli_matches[0][0]["id"] == "ind_002"

    # Test 'chawal'
    rice_matches = matcher.find_matches("chawal")
    assert len(rice_matches) > 0
    assert rice_matches[0][0]["id"] == "ind_009"


def test_accurate_nutrient_scaling(matcher):
    matches = matcher.find_matches("Idli")
    idli_rec = matches[0][0]
    
    # 2 idlis = 120 grams. Per 100g calories = 136, protein = 4.2g
    nutrients = matcher.calculate_nutrients(idli_rec, weight_g=120.0)
    
    expected_calories = round(136 * 1.2, 1)  # 163.2
    expected_protein = round(4.2 * 1.2, 1)   # 5.0
    
    assert nutrients.calories == expected_calories
    assert nutrients.protein_g == expected_protein


def test_cooking_method_and_oil_adjustment(matcher):
    matches = matcher.find_matches("Potato Fry")
    aloo_rec = matches[0][0]
    
    standard = matcher.calculate_nutrients(aloo_rec, weight_g=100.0, oil_adjustment="normal")
    low_oil = matcher.calculate_nutrients(aloo_rec, weight_g=100.0, oil_adjustment="low")
    high_oil = matcher.calculate_nutrients(aloo_rec, weight_g=100.0, oil_adjustment="high")
    
    assert low_oil.fat_g < standard.fat_g
    assert high_oil.fat_g > standard.fat_g
    assert low_oil.calories < standard.calories
