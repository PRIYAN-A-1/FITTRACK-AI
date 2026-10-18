"""
FITTRACK AI — Test Suite: Nutrition Facts Label OCR Parser
"""

import sys
from pathlib import Path
import pytest

base_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(base_dir / "ai-engine"))

from ai_engine.label_parser import NutritionLabelParser


@pytest.fixture
def label_parser():
    return NutritionLabelParser()


def test_parse_standard_nutrition_label(label_parser):
    sample_ocr = """
    Nutrition Facts
    Serving Size: 1 bar (50g)
    Calories: 210
    Total Fat: 7g
    Saturated Fat: 2g
    Total Carbohydrate: 24g
    Dietary Fiber: 4g
    Total Sugars: 9g
    Protein: 15g
    Sodium: 140mg
    """
    res = label_parser.parse_label_text(sample_ocr, product_name="Protein Energy Bar")
    assert res.nutrients.calories == 210.0
    assert res.nutrients.protein_g == 15.0
    assert res.nutrients.carbs_g == 24.0
    assert res.nutrients.fat_g == 7.0
    assert res.nutrients.fiber_g == 4.0
    assert res.nutrients.sodium_mg == 140.0
    assert res.is_per_100g is False
    assert res.serving_weight_g == 50.0
    assert res.confidence >= 0.80


def test_parse_per_100g_indian_label(label_parser):
    sample_ocr = """
    Nutritional Information per 100g:
    Energy: 380 kcal
    Protein: 9.5 g
    Carbohydrates: 72.0 g
    Total Fat: 5.5 g
    Dietary Fibre: 6.0 g
    Sodium: 320 mg
    """
    res = label_parser.parse_label_text(sample_ocr)
    assert res.nutrients.calories == 380.0
    assert res.nutrients.protein_g == 9.5
    assert res.nutrients.carbs_g == 72.0
    assert res.nutrients.fat_g == 5.5
    assert res.is_per_100g is True
