from pydantic import BaseModel
from typing import Dict, List

class RenameRequest(BaseModel):
    decompiled_code: str
    function_name: str

class RenameSuggestion(BaseModel):
    new_function_name: str
    variable_renames: Dict[str, str]
    explanation: str

class MemorySafetyIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str          # high / medium / low
    suggestion: str

class MemorySafetyAnalysis(BaseModel):
    issues: List[MemorySafetyIssue]
    overall_assessment: str