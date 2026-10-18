"""
FITTRACK AI — Home Dashboard and Analytics API Endpoints
"""

from datetime import date, timedelta
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload

from ..deps import get_current_user
from ...core.database import get_db
from ...models.db_models import Profile, MealEntry, NutritionGoal, WaterLog
from ...schemas.tracking_schemas import (
    DailyDashboardResponse, MacroProgress, MealResponse
)

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get("/daily", response_model=DailyDashboardResponse)
async def get_daily_dashboard(
    target_date: Optional[date] = Query(None, description="Dashboard date (default today)"),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    selected_date = target_date or date.today()

    # 1. Fetch user's active nutrition goal
    goal_stmt = select(NutritionGoal).where(
        NutritionGoal.user_id == current_user.user_id,
        NutritionGoal.is_active == True
    )
    goal_res = await db.execute(goal_stmt)
    goal = goal_res.scalars().first()
    
    cal_target = goal.daily_calories if goal else 2000
    protein_target = goal.protein_g if goal else 120.0
    carbs_target = goal.carbs_g if goal else 220.0
    fat_target = goal.fat_g if goal else 65.0
    fiber_target = goal.fiber_g if goal else 30.0
    water_target = goal.water_goal_ml if goal else 2500

    # 2. Fetch all meals for selected date
    meal_stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(
        MealEntry.user_id == current_user.user_id,
        MealEntry.logged_date == selected_date
    ).order_by(MealEntry.created_at.asc())
    meal_res = await db.execute(meal_stmt)
    meals = meal_res.scalars().all()

    # Sum consumed nutrients
    cals_consumed = round(sum(m.total_calories for m in meals), 1)
    protein_consumed = round(sum(m.total_protein_g for m in meals), 1)
    carbs_consumed = round(sum(m.total_carbs_g for m in meals), 1)
    fat_consumed = round(sum(m.total_fat_g for m in meals), 1)
    fiber_consumed = round(sum(m.total_fiber_g for m in meals), 1)

    cals_remaining = max(0.0, round(cal_target - cals_consumed, 1))

    # 3. Fetch water logs for selected date
    water_stmt = select(WaterLog).where(
        WaterLog.user_id == current_user.user_id,
        WaterLog.logged_date == selected_date
    )
    water_res = await db.execute(water_stmt)
    water_logs = water_res.scalars().all()
    water_consumed = sum(w.amount_ml for w in water_logs)

    # 4. Generate contextual daily insight
    if cals_consumed == 0:
        insight = "Start logging your first meal today! Tap the camera to scan your plate in 3 seconds."
    elif protein_consumed < (protein_target * 0.4):
        insight = "You're low on protein today. Consider adding paneer, dal, eggs, or Greek yogurt to your next meal."
    elif cals_remaining < 300:
        insight = "Great pacing! You have reached 85%+ of your calorie target with solid macronutrient balance."
    else:
        insight = f"On track! You have {int(cals_remaining)} kcal remaining for dinner or healthy snacks."

    def make_progress(consumed: float, target: float) -> MacroProgress:
        pct = round((consumed / target) * 100, 1) if target > 0 else 0.0
        rem = max(0.0, round(target - consumed, 1))
        return MacroProgress(consumed=consumed, target=target, percentage=pct, remaining=rem)

    return DailyDashboardResponse(
        date=selected_date,
        calories_consumed=cals_consumed,
        calories_target=cal_target,
        calories_remaining=cals_remaining,
        exercise_burned=0.0,
        net_calories=cals_consumed,
        protein=make_progress(protein_consumed, protein_target),
        carbs=make_progress(carbs_consumed, carbs_target),
        fat=make_progress(fat_consumed, fat_target),
        fiber_consumed=fiber_consumed,
        fiber_target=fiber_target,
        water_consumed_ml=water_consumed,
        water_target_ml=water_target,
        meals_count=len(meals),
        meals=meals,
        streak_days=3,
        daily_insight=insight
    )


@router.get("/weekly")
async def get_weekly_insights(
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    end_d = date.today()
    start_d = end_d - timedelta(days=6)

    stmt = select(MealEntry).where(
        MealEntry.user_id == current_user.user_id,
        MealEntry.logged_date >= start_d,
        MealEntry.logged_date <= end_d
    )
    res = await db.execute(stmt)
    meals = res.scalars().all()

    daily_totals = {}
    for i in range(7):
        d = start_d + timedelta(days=i)
        daily_totals[d.isoformat()] = {"calories": 0.0, "protein_g": 0.0, "carbs_g": 0.0, "fat_g": 0.0}

    for m in meals:
        key = m.logged_date.isoformat()
        if key in daily_totals:
            daily_totals[key]["calories"] += m.total_calories
            daily_totals[key]["protein_g"] += m.total_protein_g
            daily_totals[key]["carbs_g"] += m.total_carbs_g
            daily_totals[key]["fat_g"] += m.total_fat_g

    avg_cals = round(sum(d["calories"] for d in daily_totals.values()) / 7, 1)
    avg_protein = round(sum(d["protein_g"] for d in daily_totals.values()) / 7, 1)

    return {
        "start_date": start_d.isoformat(),
        "end_date": end_d.isoformat(),
        "daily_breakdown": daily_totals,
        "average_daily_calories": avg_cals,
        "average_daily_protein_g": avg_protein,
        "consistency_score_pct": 85.0
    }
