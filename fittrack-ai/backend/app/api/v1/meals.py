"""
FITTRACK AI — Meal Logging and Food Diary API Endpoints
"""

import uuid
from datetime import date, datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload

from ..deps import get_current_user
from ...core.database import get_db
from ...models.db_models import Profile, MealEntry, MealItem
from ...schemas.meal_schemas import MealCreate, MealUpdate, MealResponse

router = APIRouter(prefix="/meals", tags=["Meals"])


@router.get("", response_model=List[MealResponse])
async def get_meals(
    logged_date: Optional[date] = Query(None, description="Filter meals by specific date"),
    limit: int = Query(50, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(MealEntry.user_id == current_user.user_id)
    if logged_date:
        stmt = stmt.where(MealEntry.logged_date == logged_date)
    stmt = stmt.order_by(MealEntry.logged_date.desc(), MealEntry.created_at.desc()).limit(limit)

    res = await db.execute(stmt)
    return res.scalars().all()


@router.post("", response_model=MealResponse, status_code=status.HTTP_201_CREATED)
async def create_meal(
    meal_in: MealCreate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    tot_cals = sum(it.calories for it in meal_in.items)
    tot_prot = sum(it.protein_g for it in meal_in.items)
    tot_carbs = sum(it.carbs_g for it in meal_in.items)
    tot_fat = sum(it.fat_g for it in meal_in.items)
    tot_fiber = sum(it.fiber_g for it in meal_in.items)

    meal = MealEntry(
        id=str(uuid.uuid4()),
        user_id=current_user.user_id,
        meal_type=meal_in.meal_type,
        meal_title=meal_in.meal_title,
        logged_date=meal_in.logged_date or date.today(),
        total_calories=round(tot_cals, 1),
        total_protein_g=round(tot_prot, 1),
        total_carbs_g=round(tot_carbs, 1),
        total_fat_g=round(tot_fat, 1),
        total_fiber_g=round(tot_fiber, 1),
        photo_url=meal_in.photo_url,
        source=meal_in.source,
        notes=meal_in.notes
    )
    db.add(meal)

    for it in meal_in.items:
        item = MealItem(
            id=str(uuid.uuid4()),
            meal_id=meal.id,
            food_id=it.food_id,
            name=it.name,
            serving_unit=it.serving_unit,
            serving_weight_g=it.serving_weight_g,
            quantity=it.quantity,
            calories=round(it.calories, 1),
            protein_g=round(it.protein_g, 1),
            carbs_g=round(it.carbs_g, 1),
            fat_g=round(it.fat_g, 1),
            fiber_g=round(it.fiber_g, 1),
            cooking_method=it.cooking_method,
            oil_adjustment=it.oil_adjustment
        )
        db.add(item)

    await db.commit()

    stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(MealEntry.id == meal.id)
    res = await db.execute(stmt)
    return res.scalars().first()


@router.get("/{meal_id}", response_model=MealResponse)
async def get_meal(
    meal_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(
        MealEntry.id == meal_id,
        MealEntry.user_id == current_user.user_id
    )
    res = await db.execute(stmt)
    meal = res.scalars().first()
    if not meal:
        raise HTTPException(status_code=404, detail="Meal not found")
    return meal


@router.patch("/{meal_id}", response_model=MealResponse)
async def update_meal(
    meal_id: str,
    meal_update: MealUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(
        MealEntry.id == meal_id,
        MealEntry.user_id == current_user.user_id
    )
    res = await db.execute(stmt)
    meal = res.scalars().first()
    if not meal:
        raise HTTPException(status_code=404, detail="Meal not found")

    if meal_update.meal_title is not None:
        meal.meal_title = meal_update.meal_title
    if meal_update.meal_type is not None:
        meal.meal_type = meal_update.meal_type
    if meal_update.logged_date is not None:
        meal.logged_date = meal_update.logged_date
    if meal_update.photo_url is not None:
        meal.photo_url = meal_update.photo_url
    if meal_update.notes is not None:
        meal.notes = meal_update.notes
    if meal_update.is_favorite is not None:
        meal.is_favorite = meal_update.is_favorite

    if meal_update.items is not None:
        # Remove old items
        for old_item in list(meal.items):
            await db.delete(old_item)
        
        # Add updated items
        for it in meal_update.items:
            item = MealItem(
                id=str(uuid.uuid4()),
                meal_id=meal.id,
                food_id=it.food_id,
                name=it.name,
                serving_unit=it.serving_unit,
                serving_weight_g=it.serving_weight_g,
                quantity=it.quantity,
                calories=round(it.calories, 1),
                protein_g=round(it.protein_g, 1),
                carbs_g=round(it.carbs_g, 1),
                fat_g=round(it.fat_g, 1),
                fiber_g=round(it.fiber_g, 1),
                cooking_method=it.cooking_method,
                oil_adjustment=it.oil_adjustment
            )
            db.add(item)

        # Recalculate totals
        meal.total_calories = round(sum(it.calories for it in meal_update.items), 1)
        meal.total_protein_g = round(sum(it.protein_g for it in meal_update.items), 1)
        meal.total_carbs_g = round(sum(it.carbs_g for it in meal_update.items), 1)
        meal.total_fat_g = round(sum(it.fat_g for it in meal_update.items), 1)
        meal.total_fiber_g = round(sum(it.fiber_g for it in meal_update.items), 1)

    meal.updated_at = datetime.utcnow()
    await db.commit()

    stmt = select(MealEntry).options(selectinload(MealEntry.items)).where(MealEntry.id == meal.id)
    res = await db.execute(stmt)
    return res.scalars().first()


@router.delete("/{meal_id}")
async def delete_meal(
    meal_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    stmt = select(MealEntry).where(
        MealEntry.id == meal_id,
        MealEntry.user_id == current_user.user_id
    )
    res = await db.execute(stmt)
    meal = res.scalars().first()
    if not meal:
        raise HTTPException(status_code=404, detail="Meal not found")

    await db.delete(meal)
    await db.commit()
    return {"message": "Meal deleted successfully", "id": meal_id}
