"""
FITTRACK AI — Test Suite: Voice Meal Parser
"""

import sys
from pathlib import Path
import pytest

base_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(base_dir / "ai-engine"))

from ai_engine.nutrition_matcher import NutritionMatcher
from ai_engine.voice_parser import VoiceMealParser


@pytest.fixture
def parser():
    db_path = str(base_dir / "datasets" / "food_database.json")
    matcher = NutritionMatcher(database_path=db_path)
    return VoiceMealParser(matcher=matcher)


def test_parse_dosas_with_sambar(parser):
    transcript = "I ate two dosas with sambar"
    result = parser.parse_transcript(transcript)
    assert result.success is True
    assert len(result.parsed_items) >= 2
    
    names = [item.food_name for item in result.parsed_items]
    assert any("Dosa" in name for name in names)
    assert any("Sambar" in name for name in names)
    assert result.total_nutrients.calories > 200


def test_parse_grams_chicken_biryani(parser):
    transcript = "I had 200 grams chicken biryani"
    result = parser.parse_transcript(transcript)
    assert result.success is True
    assert len(result.parsed_items) == 1
    item = result.parsed_items[0]
    assert "Biryani" in item.food_name
    assert item.estimated_weight_g == 200.0
    # 200g of biryani should be ~430 kcal
    assert 400 <= item.nutrients.calories <= 460


def test_parse_multiple_items_and_connectors(parser):
    transcript = "One banana and two boiled eggs"
    result = parser.parse_transcript(transcript)
    assert result.success is True
    assert len(result.parsed_items) == 2
    names = [item.food_name for item in result.parsed_items]
    assert any("Banana" in n for n in names)
    assert any("Egg" in n for n in names)
