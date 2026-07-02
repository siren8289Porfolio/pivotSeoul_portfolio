from housing.result_builder import build_housing_result
from housing.schema import HousingRequest, HousingResult


class HousingService:
    def run(self, request: HousingRequest) -> HousingResult:
        return build_housing_result(request)
