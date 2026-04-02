from pydantic import BaseModel, Field
from typing import List


class VariableCandidate(BaseModel):
    target_id: str
    kind: str
    current_name: str
    data_type: str
    storage: str
    first_use: str
    source_type: str
    is_auto_name: bool
    token_count: int


class RenameRequest(BaseModel):
    decompiled_code: str
    function_name: str
    variables: List[VariableCandidate] = Field(default_factory=list)


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


class MemorySafetyIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str


class MemorySafetyAnalysis(BaseModel):
    issues: List[MemorySafetyIssue]
    overall_assessment: str
