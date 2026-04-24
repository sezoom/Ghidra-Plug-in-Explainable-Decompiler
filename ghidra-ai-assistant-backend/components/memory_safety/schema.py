from typing import List

from pydantic import BaseModel


class MemorySafetyIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str
    functions_involved: list[str]
    local_variables_involved: list[str]
    calls_involved: list[str]


class MemorySafetyAnalysis(BaseModel):
    issues: List[MemorySafetyIssue]
    overall_assessment: str
