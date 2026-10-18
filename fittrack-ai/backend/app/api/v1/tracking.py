"""
FITTRACK AI — Goals, Water, and Weight Tracking API Endpoints
"""

import uuid
from datetime import date
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ..deps import get_current_user
from ...core.database import get_db
from ...models.db_models import Profile, NutritionGoal, WaterLog, WeightLog
from ...schemas.tracking_schemas import (
    NutritionGoalResponse, NutritionGoalUpdate,
    WaterLogCreate, WaterLogResponse,
    WeightLogCreate, WeightLogResponse
)

router = APIRouter(prefix="", tags=["Tracking & Goals"])


@router.get("/goals", response_model=NutritionGoalResponse)
async def get_goals(
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(NutritionGoal).where(
        NutritionGoal.user_id == current_user.user_id,
        NutritionGoal.is_active == True
    )
    res = await db.execute(stmt)
    goal = res.scalars().first()
    if not goal:
        # Create default goal
        goal = NutritionGoal(
            user_id=current_user.user_id,
            daily_calories=2000,
            protein_g=120.0,
            carbs_g=220.0,
            fat_g=65.0,
            fiber_g=30.0,
            water_goal_ml=2500,
            is_active=True,
            start_date=date.today()
        )
        db.add(goal)
        await db.commit()
        await db.refresh(goal)
    return goal


@router.patch("/goals", response_model=NutritionGoalResponse)
async def update_goals(
    goal_in: NutritionGoalUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(NutritionGoal).where(
        NutritionGoal.user_id == current_user.user_id,
        NutritionGoal.is_active == True
    )
    res = await db.execute(stmt)
    goal = res.scalars().first()
    if not goal:
        raise HTTPException(status_code=404, detail="Active goal not found")

    update_data = goal_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(goal, field, value)

    await db.commit()
    await db.refresh(goal)
    return goal


@router.post("/water", response_model=WaterLogResponse)
async def log_water(
    water_in: WaterLogCreate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    log = WaterLog(
        id=str(uuid.uuid4()),
        user_id=current_user.user_id,
        logged_date=water_in.logged_date or date.today(),
        amount_ml=water_in.amount_ml
    )
    db.add(log)
    await db.commit()
    await db.refresh(log)
    return log


@router.get("/water", response_model=List[WaterLogResponse])
async def get_water_logs(
    logged_date: Optional[date] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(WaterLog).where(WaterLog.user_id == current_user.user_id)
    if logged_date:
        stmt = stmt.where(WaterLog.logged_date == logged_date)
    stmt = stmt.order_by(WaterLog.created_at.desc())
    res = await db.execute(stmt)
    return res.scalars().all()


@router.post("/weight", response_model=WeightLogResponse)
async def log_weight(
    weight_in: WeightLogCreate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    target_date = weight_in.logged_date or date.today()
    stmt = select(WeightLog).where(
        WeightLog.user_id == current_user.user_id,
        WeightLog.logged_date == target_date
    )
    res = await db.execute(stmt)
    existing = res.scalars().first()

    if existing:
        existing.weight_kg = weight_in.weight_kg
        existing.notes = weight_in.notes
        log = existing
    else:
        log = WeightLog(
            id=str(uuid.uuid4()),
            user_id=current_user.user_id,
            logged_date=target_date,
            weight_kg=weight_in.weight_kg,
            notes=weight_in.notes
        )
        db.add(log)

    # Update profile current weight
    current_user.current_weight_kg = weight_in.weight_kg

    await db.commit()
    await db.refresh(log)
    return log


@router.get("/weight", response_model=List[WeightLogResponse])
async def get_weight_history(
    limit: int = Query(30, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(WeightLog).where(
        WeightLog.user_id == current_user.user_id
    ).order_by(WeightLog.logged_date.desc()).limit(limit)
    res = await db.execute(stmt)
    return res.scalars().all()
