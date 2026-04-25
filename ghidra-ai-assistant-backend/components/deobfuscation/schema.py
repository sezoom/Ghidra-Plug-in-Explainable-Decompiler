from typing import Optional

from pydantic import BaseModel, Field


class DeobfuscationResult(BaseModel):
    clean_code: str = Field(description="Human-friendly cleaned and deobfuscated code")
    changes_summary: str = Field(
        default="", description="Short summary of major simplifications applied"
    )
    notes: str = Field(
        default="",
        description="Optional analyst notes about residual uncertainty or assumptions",
    )
    functions_analyzed: list[str] = []
    variables_analyzed: list[str] = []
