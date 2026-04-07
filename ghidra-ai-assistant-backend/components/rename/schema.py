from pydantic import BaseModel
from typing import List

class RenameItem(BaseModel):
    target_id: str
    kind: str
    old_name: str
    new_name: str
    explanation: str


class RenameSuggestion(BaseModel):
    function_rename: RenameItem
    variable_renames: List[RenameItem]
    summary: str