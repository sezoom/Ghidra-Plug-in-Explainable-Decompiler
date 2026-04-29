from typing import List

from pydantic import BaseModel


class CryptoAnalysisIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str
    function_involved: list[str] = []
    variable_involved: list[str] = []


class CryptoAnalysisResult(BaseModel):
    issues: List[CryptoAnalysisIssue]
    overall_assessment: str
