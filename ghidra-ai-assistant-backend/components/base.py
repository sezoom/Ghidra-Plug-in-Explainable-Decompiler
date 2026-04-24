from abc import ABC, abstractmethod
from typing import Any, Dict, Type

from pydantic import BaseModel


class BaseComponent(ABC):
    name: str
    request_model: Type[BaseModel]
    response_model: Type[BaseModel]

    @abstractmethod
    def build_prompt(self, state: Dict[str, Any]) -> str:
        """Return full prompt for the LLM."""
        pass

    def post_process(self, result: Dict[str, Any]) -> Dict[str, Any]:
        """Optional: clean result after LLM call."""
        return result

    def run_control(
        self,
        result: Dict[str, Any],
        source_json_path: str,
    ) -> str | None:
        """Override in component to activate the control layer."""
        return None


# Global registry –
COMPONENT_REGISTRY: Dict[str, BaseComponent] = {}


def register_component(component_class: Type[BaseComponent]):
    instance = component_class()
    COMPONENT_REGISTRY[instance.name] = instance
    return component_class
