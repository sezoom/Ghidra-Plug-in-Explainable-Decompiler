from pydantic import BaseModel
from typing import List

class RenameRequest(BaseModel):
    decompiled_code: str
    function_name: str

class VariableRename(BaseModel):
    old_name: str
    new_name: str

class RenameSuggestion(BaseModel):
    new_function_name: str
    variable_renames: List[VariableRename]
    explanation: str

class MemorySafetyIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str

class MemorySafetyAnalysis(BaseModel):
    issues: List[MemorySafetyIssue]
    overall_assessment: str