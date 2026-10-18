"""
FITTRACK AI — Base Vision Adapter Interface
"""

from abc import ABC, abstractmethod
from typing import Optional
from ..models import PlateScanResponse


class BaseVisionAdapter(ABC):
    @abstractmethod
    async def analyze_image(
        self,
        image_bytes: bytes,
        mime_type: str = "image/jpeg",
        user_hint: Optional[str] = None
    ) -> PlateScanResponse:
        """
        Processes food image bytes and returns candidate detected food items,
        portion estimations, bounding boxes, and nutrient values.
        """
        pass
