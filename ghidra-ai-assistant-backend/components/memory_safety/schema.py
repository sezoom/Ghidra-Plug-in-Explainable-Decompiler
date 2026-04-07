from pydantic import BaseModel
from typing import List


class MemorySafetyIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str


class MemorySafetyAnalysis(BaseModel):
    issues: List[MemorySafetyIssue]
    overall_assessment: str