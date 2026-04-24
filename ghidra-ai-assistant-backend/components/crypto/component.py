from schemas import CryptoAnalysisRequest
from components.crypto.schema import CryptoAnalysisResult
from components.crypto.prompt import build_crypto_analysis_prompt
from components.base import BaseComponent, register_component


@register_component
class CryptoAnalysisComponent(BaseComponent):
    name = "crypto"
    request_model = CryptoAnalysisRequest
    response_model = CryptoAnalysisResult

    def build_prompt(self, state: dict) -> str:
        return build_crypto_analysis_prompt(state)
