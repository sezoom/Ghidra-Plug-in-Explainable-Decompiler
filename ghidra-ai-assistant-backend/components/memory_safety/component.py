from components.base import BaseComponent, register_component
from components.memory_safety import control
from components.memory_safety.prompt import build_memory_safety_prompt
from components.memory_safety.schema import MemorySafetyAnalysis
from schemas import MemorySafetyRequest


@register_component
class MemorySafetyComponent(BaseComponent):
    name = "memory_safety"
    request_model = MemorySafetyRequest
    response_model = MemorySafetyAnalysis

    def build_prompt(self, state: dict) -> str:
        return build_memory_safety_prompt(state)

    def run_control(self, result: dict, source_json_path: str) -> str:  # ← new
        return control.verify(result, source_json_path)
