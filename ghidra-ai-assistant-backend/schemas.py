from typing import List, Optional

from pydantic import BaseModel, Field


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


class AnalyzeRequest(BaseModel):
    task: str
    decompiled_code: str
    function_name: str
    variables: List[VariableCandidate] = Field(default_factory=list)


class RenameRequest(BaseModel):
    decompiled_code: str
    function_name: str
    variables: List[VariableCandidate] = Field(default_factory=list)
    source_json_path: str | None = None


class MemorySafetyRequest(BaseModel):
    decompiled_code: str
    function_name: str
    source_json_path: str | None = None


#  class CryptoAnalysisRequest(BaseModel):
#     decompiled_code: str
#     function_name: str
#     source_json_path: str | None = None


# class DeobfuscationRequest(BaseModel):
#     decompiled_code: str
#     function_name: str
#     source_json_path: str | None = None

# Compatibility re-exports for callers that still import response models from schemas.
from components.memory_safety.schema import MemorySafetyAnalysis, MemorySafetyIssue
from components.rename.schema import RenameItem, RenameSuggestion

# from components.crypto.schema import CryptoAnalysisIssue, CryptoAnalysisResult
# from components.deobfuscation.schema import DeobfuscationResult

__all__ = [
    "VariableCandidate",
    "AnalyzeRequest",
    "RenameRequest",
    "MemorySafetyRequest",
    # "CryptoAnalysisRequest",
    # "DeobfuscationRequest",
    "RenameItem",
    "RenameSuggestion",
    "MemorySafetyIssue",
    "MemorySafetyAnalysis",
    # "CryptoAnalysisIssue",
    # "CryptoAnalysisResult",
    # "DeobfuscationResult",
]
