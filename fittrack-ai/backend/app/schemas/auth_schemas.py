"""
FITTRACK AI — Authentication and Profile Schemas
"""

from typing import Optional
from datetime import date, datetime
from pydantic import BaseModel, EmailStr, Field, ConfigDict


class UserRegister(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)
    full_name: Optional[str] = None
    gender: Optional[str] = "not_specified"
    height_cm: Optional[float] = 170.0
    current_weight_kg: Optional[float] = 70.0
    target_weight_kg: Optional[float] = 68.0
    activity_level: Optional[str] = "moderately_active"
    dietary_lifestyle: Optional[str] = "balanced"


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    email: str
    full_name: Optional[str] = None


class ProfileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    email: str
    full_name: Optional[str]
    gender: str
    height_cm: float
    current_weight_kg: float
    target_weight_kg: float
    activity_level: str
    dietary_lifestyle: str
    health_connect_enabled: bool
    created_at: datetime


class ProfileUpdate(BaseModel):
    full_name: Optional[str] = None
    gender: Optional[str] = None
    height_cm: Optional[float] = None
    current_weight_kg: Optional[float] = None
    target_weight_kg: Optional[float] = None
    activity_level: Optional[str] = None
    dietary_lifestyle: Optional[str] = None
    health_connect_enabled: Optional[bool] = None
