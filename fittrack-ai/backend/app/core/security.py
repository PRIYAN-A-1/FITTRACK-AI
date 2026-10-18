"""
FITTRACK AI — Security and Cryptography Utilities
"""

from datetime import datetime, timedelta, timezone
from typing import Any, Union, Optional
from jose import jwt
import hashlib
from .config import settings


def create_access_token(subject: Union[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode = {"exp": expire, "sub": str(subject)}
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def verify_password(plain_password: str, hashed_password: str) -> bool:
    # Use sha256 + salt for secure portable hashing
    salt, hash_val = hashed_password.split(":")
    computed = hashlib.sha256((salt + plain_password).encode("utf-8")).hexdigest()
    return computed == hash_val


def get_password_hash(password: str) -> str:
    import secrets
    salt = secrets.token_hex(8)
    hash_val = hashlib.sha256((salt + password).encode("utf-8")).hexdigest()
    return f"{salt}:{hash_val}"
