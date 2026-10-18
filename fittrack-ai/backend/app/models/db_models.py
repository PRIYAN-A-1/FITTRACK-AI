"""
FITTRACK AI — SQLAlchemy Database Models
"""

import uuid
from datetime import datetime, date, time
from typing import List, Optional
from sqlalchemy import (
    Column, String, Integer, Float, Boolean, Date, Time, DateTime,
    ForeignKey, Text, JSON
)
from sqlalchemy.orm import relationship
from ..core.database import Base


class Profile(Base):
    __tablename__ = "profiles"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), unique=True, nullable=False, index=True)
    email = Column(String(255), unique=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(255), nullable=True)
    avatar_url = Column(Text, nullable=True)
    gender = Column(String(32), default="not_specified")
    date_of_birth = Column(Date, nullable=True)
    height_cm = Column(Float, default=170.0)
    current_weight_kg = Column(Float, default=70.0)
    target_weight_kg = Column(Float, default=68.0)
    activity_level = Column(String(32), default="moderately_active")
    dietary_lifestyle = Column(String(64), default="balanced")
    health_connect_enabled = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    goals = relationship("NutritionGoal", back_populates="profile", cascade="all, delete-orphan")
    meals = relationship("MealEntry", back_populates="profile", cascade="all, delete-orphan")
    water_logs = relationship("WaterLog", back_populates="profile", cascade="all, delete-orphan")
    weight_logs = relationship("WeightLog", back_populates="profile", cascade="all, delete-orphan")


class UserPreference(Base):
    __tablename__ = "user_preferences"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), unique=True, nullable=False)
    preferred_cuisine = Column(String(64), default="Indian")
    preferred_units = Column(String(16), default="metric")
    calorie_rollover = Column(Boolean, default=False)
    exercise_calorie_adjustment = Column(Boolean, default=True)
    dark_mode = Column(Boolean, default=True)
    haptic_feedback = Column(Boolean, default=True)
    sound_effects = Column(Boolean, default=False)
    updated_at = Column(DateTime, default=datetime.utcnow)


class NutritionGoal(Base):
    __tablename__ = "nutrition_goals"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    daily_calories = Column(Integer, default=2000, nullable=False)
    protein_g = Column(Float, default=120.0, nullable=False)
    carbs_g = Column(Float, default=220.0, nullable=False)
    fat_g = Column(Float, default=65.0, nullable=False)
    fiber_g = Column(Float, default=30.0)
    water_goal_ml = Column(Integer, default=2500)
    is_active = Column(Boolean, default=True)
    start_date = Column(Date, default=date.today)
    updated_at = Column(DateTime, default=datetime.utcnow)

    profile = relationship("Profile", back_populates="goals")


class FoodCatalog(Base):
    __tablename__ = "food_catalog"

    id = Column(String(64), primary_key=True)
    name = Column(String(255), nullable=False, index=True)
    category = Column(String(128), nullable=False)
    cuisine = Column(String(64), default="Global")
    default_serving_unit = Column(String(64), default="serving")
    default_serving_weight_g = Column(Float, default=100.0)
    density_g_per_ml = Column(Float, default=1.0)
    ingredients = Column(JSON, default=list)
    aliases = Column(JSON, default=list)
    source_attribution = Column(String(255), default="USDA / IFCT")


class FoodNutrient(Base):
    __tablename__ = "food_nutrients"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    food_id = Column(String(64), ForeignKey("food_catalog.id", ondelete="CASCADE"), unique=True, nullable=False)
    calories = Column(Float, nullable=False, default=0.0)
    protein_g = Column(Float, nullable=False, default=0.0)
    carbs_g = Column(Float, nullable=False, default=0.0)
    fat_g = Column(Float, nullable=False, default=0.0)
    fiber_g = Column(Float, default=0.0)
    sugar_g = Column(Float, default=0.0)
    sodium_mg = Column(Float, default=0.0)
    saturated_fat_g = Column(Float, default=0.0)
    cholesterol_mg = Column(Float, default=0.0)


class CustomFood(Base):
    __tablename__ = "custom_foods"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    name = Column(String(255), nullable=False)
    brand = Column(String(255), nullable=True)
    barcode = Column(String(64), nullable=True, index=True)
    serving_unit = Column(String(64), default="serving")
    serving_weight_g = Column(Float, default=100.0)
    calories = Column(Float, nullable=False)
    protein_g = Column(Float, nullable=False)
    carbs_g = Column(Float, nullable=False)
    fat_g = Column(Float, nullable=False)
    fiber_g = Column(Float, default=0.0)
    sugar_g = Column(Float, default=0.0)
    sodium_mg = Column(Float, default=0.0)
    created_at = Column(DateTime, default=datetime.utcnow)


class FoodScan(Base):
    __tablename__ = "food_scans"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    image_storage_path = Column(Text, nullable=False)
    status = Column(String(32), default="completed")
    model_provider = Column(String(64), default="mock-vision-engine")
    model_version = Column(String(64), default="1.0")
    processing_latency_ms = Column(Integer, default=150)
    created_at = Column(DateTime, default=datetime.utcnow)


class MealEntry(Base):
    __tablename__ = "meal_entries"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    scan_id = Column(String(36), ForeignKey("food_scans.id", ondelete="SET NULL"), nullable=True)
    meal_type = Column(String(32), nullable=False)  # breakfast, lunch, dinner, snack
    meal_title = Column(String(255), nullable=False)
    logged_date = Column(Date, default=date.today, nullable=False, index=True)
    logged_time = Column(Time, default=lambda: datetime.utcnow().time())
    total_calories = Column(Float, nullable=False, default=0.0)
    total_protein_g = Column(Float, nullable=False, default=0.0)
    total_carbs_g = Column(Float, nullable=False, default=0.0)
    total_fat_g = Column(Float, nullable=False, default=0.0)
    total_fiber_g = Column(Float, default=0.0)
    total_sugar_g = Column(Float, default=0.0)
    total_sodium_mg = Column(Float, default=0.0)
    photo_url = Column(Text, nullable=True)
    notes = Column(Text, nullable=True)
    source = Column(String(32), default="ai_scan")
    is_favorite = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    profile = relationship("Profile", back_populates="meals")
    items = relationship("MealItem", back_populates="meal", cascade="all, delete-orphan", lazy="selectin")


class MealItem(Base):
    __tablename__ = "meal_items"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    meal_id = Column(String(36), ForeignKey("meal_entries.id", ondelete="CASCADE"), nullable=False, index=True)
    food_id = Column(String(64), ForeignKey("food_catalog.id", ondelete="SET NULL"), nullable=True)
    name = Column(String(255), nullable=False)
    serving_unit = Column(String(64), default="g")
    serving_weight_g = Column(Float, default=100.0)
    quantity = Column(Float, default=1.0)
    calories = Column(Float, nullable=False)
    protein_g = Column(Float, nullable=False)
    carbs_g = Column(Float, nullable=False)
    fat_g = Column(Float, nullable=False)
    fiber_g = Column(Float, default=0.0)
    cooking_method = Column(String(64), default="standard")
    oil_adjustment = Column(String(32), default="normal")

    meal = relationship("MealEntry", back_populates="items")


class WaterLog(Base):
    __tablename__ = "water_logs"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    logged_date = Column(Date, default=date.today, nullable=False, index=True)
    amount_ml = Column(Integer, default=250)
    created_at = Column(DateTime, default=datetime.utcnow)

    profile = relationship("Profile", back_populates="water_logs")


class WeightLog(Base):
    __tablename__ = "weight_logs"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False, index=True)
    logged_date = Column(Date, default=date.today, nullable=False)
    weight_kg = Column(Float, nullable=False)
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    profile = relationship("Profile", back_populates="weight_logs")


class SyncOperation(Base):
    __tablename__ = "sync_operations"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), nullable=False, index=True)
    client_operation_id = Column(String(128), unique=True, nullable=False)
    entity_type = Column(String(64), nullable=False)
    operation = Column(String(16), nullable=False)
    payload = Column(JSON, nullable=False)
    status = Column(String(16), default="processed")
    processed_at = Column(DateTime, default=datetime.utcnow)
