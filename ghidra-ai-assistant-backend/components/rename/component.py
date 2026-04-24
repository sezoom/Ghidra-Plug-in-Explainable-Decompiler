from components.base import BaseComponent, register_component
from components.rename import control
from components.rename.prompt import build_rename_prompt
from components.rename.schema import RenameSuggestion
from schemas import RenameRequest


@register_component
class RenameComponent(BaseComponent):
    name = "rename"
    request_model = RenameRequest
    response_model = RenameSuggestion

    def build_prompt(self, state: dict) -> str:
        return build_rename_prompt(state)

    def run_control_report(self, result: dict, source_json_path: str):
        return control.run_report(result, source_json_path)
