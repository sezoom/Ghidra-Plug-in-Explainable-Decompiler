from schemas import MemorySafetyRequest
from components.memory_safety.schema import MemorySafetyAnalysis
from components.memory_safety.prompt import build_memory_safety_prompt
from components.base import BaseComponent, register_component


@register_component
class MemorySafetyComponent(BaseComponent):
    name = "memory_safety"
    request_model = MemorySafetyRequest
    response_model = MemorySafetyAnalysis

    def build_prompt(self, state: dict) -> str:
        return build_memory_safety_prompt(state)
