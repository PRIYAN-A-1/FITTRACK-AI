"""
FITTRACK AI — Test Suite: Full Backend REST API Integration
Tests the entire user journey: registration, login, scanning, meal confirmation,
dashboard recalculation, meal editing, meal deletion, and assistant chat.
"""

import sys
from pathlib import Path
import pytest
from httpx import AsyncClient, ASGITransport

base_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(base_dir / "backend"))
sys.path.insert(0, str(base_dir / "ai-engine"))

from backend.app.main import app
from backend.app.core.database import engine, Base


@pytest.mark.asyncio
async def test_full_user_journey():
    # Ensure database schema is created
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # 1. Health check
        health_resp = await client.get("/health")
        assert health_resp.status_code == 200
        assert health_resp.json()["status"] == "healthy"

        # 2. Register new user
        reg_payload = {
            "email": "testuser_fit@example.com",
            "password": "Password123!",
            "full_name": "Test Athlete",
            "current_weight_kg": 72.0,
            "target_weight_kg": 68.0,
            "activity_level": "moderately_active"
        }
        reg_resp = await client.post("/auth/register", json=reg_payload)
        assert reg_resp.status_code in [201, 400]
        
        # If user existed, login
        login_resp = await client.post("/auth/login", json={
            "email": "testuser_fit@example.com",
            "password": "Password123!"
        })
        assert login_resp.status_code == 200
        token = login_resp.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # 3. Check Initial Dashboard
        dash_initial = await client.get("/api/v1/dashboard/daily", headers=headers)
        assert dash_initial.status_code == 200
        initial_cals = dash_initial.json()["calories_consumed"]

        # 4. Confirm a meal (e.g. Egg Fried Rice, 300g)
        meal_payload = {
            "meal_type": "lunch",
            "meal_title": "Egg Fried Rice Lunch",
            "items": [
                {
                    "food_id": "ind_001",
                    "name": "Egg Fried Rice",
                    "serving_unit": "plate",
                    "serving_weight_g": 300.0,
                    "quantity": 1.0,
                    "calories": 534.0,
                    "protein_g": 20.4,
                    "carbs_g": 72.6,
                    "fat_g": 17.7,
                    "fiber_g": 3.6,
                    "cooking_method": "standard",
                    "oil_adjustment": "normal"
                }
            ],
            "notes": "Post workout lunch"
        }
        confirm_resp = await client.post("/api/v1/scans/manual/confirm", json=meal_payload, headers=headers)
        assert confirm_resp.status_code == 200
        saved_meal = confirm_resp.json()
        assert saved_meal["total_calories"] == 534.0
        meal_id = saved_meal["id"]

        # 5. Verify Dashboard totals updated immediately
        dash_after = await client.get("/api/v1/dashboard/daily", headers=headers)
        assert dash_after.status_code == 200
        after_cals = dash_after.json()["calories_consumed"]
        assert after_cals == initial_cals + 534.0
        assert dash_after.json()["meals_count"] >= 1

        # 6. Edit Meal Portion (User adjusts portion down to 200g)
        edit_payload = {
            "items": [
                {
                    "food_id": "ind_001",
                    "name": "Egg Fried Rice",
                    "serving_unit": "plate",
                    "serving_weight_g": 200.0,
                    "quantity": 1.0,
                    "calories": 356.0,
                    "protein_g": 13.6,
                    "carbs_g": 48.4,
                    "fat_g": 11.8,
                    "fiber_g": 2.4,
                    "cooking_method": "standard",
                    "oil_adjustment": "normal"
                }
            ]
        }
        edit_resp = await client.patch(f"/api/v1/meals/{meal_id}", json=edit_payload, headers=headers)
        assert edit_resp.status_code == 200
        assert edit_resp.json()["total_calories"] == 356.0

        # Verify Dashboard immediately reflects the reduced portion
        dash_after_edit = await client.get("/api/v1/dashboard/daily", headers=headers)
        assert dash_after_edit.json()["calories_consumed"] == initial_cals + 356.0

        # 7. Barcode lookup
        barcode_resp = await client.get("/api/v1/foods/barcode/8901030865412", headers=headers)
        assert barcode_resp.status_code == 200
        assert "Amul" in barcode_resp.json()["brand"]

        # 8. Voice meal parsing endpoint
        voice_resp = await client.post(
            "/api/v1/voice/parse",
            json={"transcript": "I had 200 grams chicken biryani"},
            headers=headers
        )
        assert voice_resp.status_code == 200
        assert len(voice_resp.json()["parsed_items"]) >= 1

        # 9. AI Assistant chat
        chat_resp = await client.post(
            "/api/v1/assistant/chat",
            json={"messages": [{"role": "user", "content": "What are good high protein Indian options?"}]},
            headers=headers
        )
        assert chat_resp.status_code == 200
        assert "Paneer" in chat_resp.json()["reply"]

        # 10. Delete the meal
        del_resp = await client.delete(f"/api/v1/meals/{meal_id}", headers=headers)
        assert del_resp.status_code == 200

        # Verify Dashboard reverses the contribution immediately
        dash_final = await client.get("/api/v1/dashboard/daily", headers=headers)
        assert dash_final.json()["calories_consumed"] == initial_cals
