from pydantic import BaseModel
from typing import List


class CryptoAnalysisIssue(BaseModel):
    issue_type: str
    description: str
    location: str
    severity: str
    suggestion: str


class CryptoAnalysisResult(BaseModel):
    issues: List[CryptoAnalysisIssue]
    overall_assessment: str
