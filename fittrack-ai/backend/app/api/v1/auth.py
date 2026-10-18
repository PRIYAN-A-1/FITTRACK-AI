"""
FITTRACK AI — Authentication and Profile API Endpoints
"""

import uuid
from datetime import date
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ...core.security import create_access_token, verify_password, get_password_hash
from ...core.database import get_db
from ...models.db_models import Profile, UserPreference, NutritionGoal
from ...schemas.auth_schemas import (
    UserRegister, UserLogin, TokenResponse, ProfileResponse, ProfileUpdate
)
from ..deps import get_current_user

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register(user_in: UserRegister, db: AsyncSession = Depends(get_db)):
    # Check if email exists
    stmt = select(Profile).where(Profile.email == user_in.email)
    res = await db.execute(stmt)
    if res.scalars().first():
        raise HTTPException(status_code=400, detail="User with this email already exists")

    user_id = str(uuid.uuid4())
    hashed_pw = get_password_hash(user_in.password)

    profile = Profile(
        user_id=user_id,
        email=user_in.email,
        hashed_password=hashed_pw,
        full_name=user_in.full_name or user_in.email.split("@")[0].title(),
        gender=user_in.gender or "not_specified",
        height_cm=user_in.height_cm or 170.0,
        current_weight_kg=user_in.current_weight_kg or 70.0,
        target_weight_kg=user_in.target_weight_kg or 68.0,
        activity_level=user_in.activity_level or "moderately_active",
        dietary_lifestyle=user_in.dietary_lifestyle or "balanced"
    )
    db.add(profile)

    # User Preferences
    pref = UserPreference(user_id=user_id, preferred_cuisine="Indian")
    db.add(pref)

    # Calculate evidence-based daily goals: Harris-Benedict BMR + activity factor
    weight = profile.current_weight_kg
    height = profile.height_cm
    bmr = (10 * weight) + (6.25 * height) - (5 * 28) + 5
    activity_multipliers = {
        "sedentary": 1.2,
        "lightly_active": 1.375,
        "moderately_active": 1.55,
        "very_active": 1.725
    }
    tdee = bmr * activity_multipliers.get(profile.activity_level, 1.4)
    target_cals = int(round(tdee))
    
    # Macro split: 25% protein, 45% carbs, 30% fat
    target_protein = round((target_cals * 0.25) / 4.0, 1)
    target_carbs = round((target_cals * 0.45) / 4.0, 1)
    target_fat = round((target_cals * 0.30) / 9.0, 1)

    goal = NutritionGoal(
        user_id=user_id,
        daily_calories=target_cals,
        protein_g=target_protein,
        carbs_g=target_carbs,
        fat_g=target_fat,
        fiber_g=30.0,
        water_goal_ml=2500,
        is_active=True,
        start_date=date.today()
    )
    db.add(goal)

    await db.commit()
    await db.refresh(profile)

    token = create_access_token(subject=user_id)
    return TokenResponse(
        access_token=token,
        token_type="bearer",
        user_id=user_id,
        email=profile.email,
        full_name=profile.full_name
    )


@router.post("/login", response_model=TokenResponse)
async def login(credentials: UserLogin, db: AsyncSession = Depends(get_db)):
    stmt = select(Profile).where(Profile.email == credentials.email)
    res = await db.execute(stmt)
    profile = res.scalars().first()

    if not profile or not verify_password(credentials.password, profile.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    token = create_access_token(subject=profile.user_id)
    return TokenResponse(
        access_token=token,
        token_type="bearer",
        user_id=profile.user_id,
        email=profile.email,
        full_name=profile.full_name
    )


@router.get("/me", response_model=ProfileResponse)
async def get_my_profile(current_user: Profile = Depends(get_current_user)):
    return current_user


@router.patch("/me", response_model=ProfileResponse)
async def update_my_profile(
    profile_in: ProfileUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    update_data = profile_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(current_user, field, value)

    await db.commit()
    await db.refresh(current_user)
    return current_user


@router.post("/logout")
async def logout(current_user: Profile = Depends(get_current_user)):
    return {"message": "Successfully logged out", "status": "ok"}


@router.delete("/account")
async def delete_account(
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    await db.delete(current_user)
    await db.commit()
    return {"message": "User account and all associated health records permanently deleted", "status": "ok"}
