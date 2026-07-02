from fastapi import APIRouter

from housing.schema import HousingRequest, HousingResult
from housing.service import HousingService

router = APIRouter(prefix="/housing", tags=["housing"])
_service = HousingService()


@router.post("/analyze", response_model=HousingResult)
def analyze_housing(body: HousingRequest) -> HousingResult:
    return _service.run(body)
