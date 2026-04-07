from abc import ABC, abstractmethod
from typing import Type, Dict, Any
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


# Global registry –
COMPONENT_REGISTRY: Dict[str, BaseComponent] = {}


def register_component(component_class: Type[BaseComponent]):
    instance = component_class()
    COMPONENT_REGISTRY[instance.name] = instance
    return component_class