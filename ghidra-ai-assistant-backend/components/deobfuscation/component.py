from components.base import BaseComponent, register_component
from components.deobfuscation import control
from components.deobfuscation.prompt import build_deobfuscation_prompt
from components.deobfuscation.schema import DeobfuscationResult
from schemas import DeobfuscationRequest


@register_component
class DeobfuscationComponent(BaseComponent):
    name = "deobfuscation"
    request_model = DeobfuscationRequest
    response_model = DeobfuscationResult

    def build_prompt(self, state: dict) -> str:
        return build_deobfuscation_prompt(state)

    def run_control_report(self, result: dict, source_json_path: str):
        return control.run_report(result, source_json_path)
