"""
FITTRACK AI — Offline Synchronization and Idempotent Batch Sync
"""

from typing import List, Dict, Any
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from ..deps import get_current_user
from ...core.database import get_db
from ...models.db_models import Profile, SyncOperation, MealEntry, MealItem, WaterLog, WeightLog

router = APIRouter(prefix="/sync", tags=["Offline Synchronization"])


class ClientSyncItem(BaseModel):
    operation_id: str
    entity_type: str  # meal_entry, water_log, weight_log
    operation: str    # INSERT, UPDATE, DELETE
    payload: Dict[str, Any]


class BatchSyncRequest(BaseModel):
    operations: List[ClientSyncItem]


@router.post("/batch")
async def process_batch_sync(
    batch: BatchSyncRequest,
    db: AsyncSession = Depends(get_db),
    current_user: Profile = Depends(get_current_user)
):
    processed_count = 0
    duplicate_count = 0
    errors = []

    for op in batch.operations:
        # Check idempotency
        stmt = select(SyncOperation).where(
            SyncOperation.user_id == current_user.user_id,
            SyncOperation.client_operation_id == op.operation_id
        )
        res = await db.execute(stmt)
        if res.scalars().first():
            duplicate_count += 1
            continue

        try:
            # Execute operation safely based on entity type
            if op.entity_type == "water_log" and op.operation == "INSERT":
                amount = int(op.payload.get("amount_ml", 250))
                w = WaterLog(
                    id=op.payload.get("id"),
                    user_id=current_user.user_id,
                    amount_ml=amount
                )
                db.add(w)

            elif op.entity_type == "meal_entry" and op.operation == "DELETE":
                meal_id = op.payload.get("id")
                if meal_id:
                    m_stmt = select(MealEntry).where(
                        MealEntry.id == meal_id,
                        MealEntry.user_id == current_user.user_id
                    )
                    m_res = await db.execute(m_stmt)
                    meal = m_res.scalars().first()
                    if meal:
                        await db.delete(meal)

            # Record sync operation for audit and deduplication
            record = SyncOperation(
                user_id=current_user.user_id,
                client_operation_id=op.operation_id,
                entity_type=op.entity_type,
                operation=op.operation,
                payload=op.payload,
                status="processed"
            )
            db.add(record)
            processed_count += 1
        except Exception as e:
            errors.append({"operation_id": op.operation_id, "error": str(e)})

    await db.commit()

    return {
        "status": "success",
        "processed_count": processed_count,
        "duplicate_count": duplicate_count,
        "errors": errors
    }
