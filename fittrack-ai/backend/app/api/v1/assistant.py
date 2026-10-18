"""
FITTRACK AI — AI Nutrition Assistant API Endpoints
Provides evidence-based dietary insights, protein suggestions, and culturally relevant recommendations.
"""

from datetime import date
from typing import List
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ..deps import get_current_user
from ...core.database import get_db
from ...models.db_models import Profile, MealEntry, NutritionGoal
from ...schemas.tracking_schemas import AssistantChatRequest, AssistantChatResponse

router = APIRouter(prefix="/assistant", tags=["AI Nutrition Assistant"])


@router.post("/chat", response_model=AssistantChatResponse)
async def chat_with_assistant(
    chat_req: AssistantChatRequest,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    user_query = ""
    for msg in reversed(chat_req.messages):
        if msg.role == "user":
            user_query = msg.content.lower()
            break

    # Context: today's meals & goals
    cals_consumed = 0.0
    protein_consumed = 0.0
    meals_count = 0

    if chat_req.include_today_context:
        stmt = select(MealEntry).where(
            MealEntry.user_id == current_user.user_id,
            MealEntry.logged_date == date.today()
        )
        res = await db.execute(stmt)
        meals = res.scalars().all()
        meals_count = len(meals)
        cals_consumed = round(sum(m.total_calories for m in meals), 1)
        protein_consumed = round(sum(m.total_protein_g for m in meals), 1)

    suggested_actions = []

    # Intelligent intent-based answering with Indian & global nutrition knowledge
    if "protein" in user_query or "high protein" in user_query:
        reply = (
            f"Based on your daily profile, you have consumed {protein_consumed}g of protein today.\n\n"
            "Here are excellent culturally relevant high-protein options to reach your target:\n"
            "• **Paneer (Cottage Cheese):** ~18g protein per 100g (great grilled or in low-oil bhurji)\n"
            "• **Soya Chunks / Meal Maker:** ~52g protein per 100g (one of the densest plant sources)\n"
            "• **Eggs:** ~6.3g protein per large boiled egg\n"
            "• **Greek Yogurt / Hung Curd:** ~10g protein per 100g with gut-friendly probiotics\n"
            "• **Sprouted Moong / Chana Salad:** ~7-9g protein per cup with high dietary fiber\n"
            "• **Chicken Breast / Fish:** ~31g protein per 100g cooked portion."
        )
        suggested_actions = ["Log 100g Paneer", "Log 2 Boiled Eggs", "View Today's Macros"]

    elif "summary" in user_query or "today" in user_query or "how am i doing" in user_query:
        reply = (
            f"Today you have logged {meals_count} meals totaling **{int(cals_consumed)} kcal** "
            f"and **{protein_consumed}g of protein**.\n\n"
            f"Your daily target is 2,000 kcal. You have {max(0, int(2000 - cals_consumed))} kcal remaining. "
            "Remember to hydrate well—aim for at least 2.5 Liters of water throughout the day!"
        )
        suggested_actions = ["Scan Next Meal", "Log 250ml Water", "View Food Diary"]

    elif "indian" in user_query or "dinner" in user_query or "lunch" in user_query:
        reply = (
            "Here is a balanced Indian meal recommendation:\n"
            "• **Carb Source:** 2 Whole wheat chapatis or 1 cup steamed brown/white rice (~150 kcal)\n"
            "• **Lentil/Protein:** 1 bowl thick Toor or Moong Dal Tadka with cumin and garlic (~140 kcal, 7g protein)\n"
            "• **Vegetable Fiber:** 1 portion stir-fried cabbage poriyal or bhindi masala (~90 kcal)\n"
            "• **Probiotic:** 1 small katori fresh homemade curd / dahi (~70 kcal, 4g protein)\n"
            "Total estimated energy: ~450 kcal with balanced fiber and micronutrients."
        )
        suggested_actions = ["Log Chapati & Dal", "Scan Plate Photo"]

    elif "loss" in user_query or "deficit" in user_query or "weight" in user_query:
        reply = (
            "For steady, healthy weight management, maintaining a moderate 300–500 kcal daily deficit is recommended. "
            "Avoid extreme restriction under 1,200 kcal as it can cause metabolic slowdown and muscle loss. "
            "Prioritize eating at least 1.4–1.8g of protein per kilogram of body weight to preserve lean muscle."
        )
        suggested_actions = ["Adjust Calorie Goal", "Log Current Weight"]

    else:
        reply = (
            f"Hello {current_user.full_name or 'there'}! I'm your FITTRACK AI nutrition assistant. "
            f"Today you have consumed {int(cals_consumed)} kcal and {protein_consumed}g protein across {meals_count} meals.\n\n"
            "You can ask me anything about:\n"
            "• Identifying high-protein Indian or global foods\n"
            "• Reviewing your daily macro balance (protein, carbs, fat)\n"
            "• Estimating portions for dishes like biryani, dosa, or paneer\n"
            "• Meal planning for healthy fat loss or muscle gain."
        )
        suggested_actions = ["What are high protein snacks?", "How are my macros today?", "Suggest a healthy dinner"]

    return AssistantChatResponse(
        reply=reply,
        suggested_actions=suggested_actions
    )
