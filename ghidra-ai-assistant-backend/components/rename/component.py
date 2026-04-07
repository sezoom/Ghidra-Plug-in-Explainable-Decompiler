from schemas import RenameRequest
from components.rename.schema import RenameSuggestion
from components.rename.prompt import build_rename_prompt
from components.base import BaseComponent, register_component


@register_component
class RenameComponent(BaseComponent):
    name = "rename"
    request_model = RenameRequest
    response_model = RenameSuggestion

    def build_prompt(self, state: dict) -> str:
        return build_rename_prompt(state)