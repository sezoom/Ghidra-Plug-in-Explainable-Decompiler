import components  # noqa: F401 - ensure registry side effects run
from analyzer import analyze
from components.base import COMPONENT_REGISTRY
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from schemas import (
    AnalyzeRequest,
    CryptoAnalysisRequest,
    DeobfuscationRequest,
    MemorySafetyRequest,
    RenameRequest,
)

app = FastAPI(title="Ghidra AI Explainable-Decompiler Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/analyze/{component_id}")
async def analyze_component(component_id: str, req: dict):
    """Preferred unified endpoint."""
    if component_id not in COMPONENT_REGISTRY:
        raise HTTPException(
            status_code=404,
            detail={
                "error": f"Unknown component '{component_id}'",
                "available_components": sorted(COMPONENT_REGISTRY.keys()),
            },
        )
    try:
        return analyze(component_id, req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8000)
