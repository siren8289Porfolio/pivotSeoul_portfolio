from housing.preprocessing import normalize_district_name
from housing.rir import (
    calculate_confidence_score,
    calculate_housing_risk_score,
    calculate_rir,
    classify_housing_status,
)
from housing.schema import HousingRequest, HousingResult


def build_housing_result(request: HousingRequest) -> HousingResult:
    district = normalize_district_name(request.district)
    rir = calculate_rir(request.monthly_income, request.monthly_housing_cost)
    housing_status, is_red_zone = classify_housing_status(rir)

    return HousingResult(
        district=district,
        monthly_income=request.monthly_income,
        monthly_housing_cost=request.monthly_housing_cost,
        rir=rir,
        housing_status=housing_status,
        is_red_zone=is_red_zone,
        risk_score=calculate_housing_risk_score(rir),
        confidence_score=calculate_confidence_score(rir),
    )
