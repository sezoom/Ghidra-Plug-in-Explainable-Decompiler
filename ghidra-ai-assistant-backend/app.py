from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from schemas import RenameRequest, RenameSuggestion, MemorySafetyAnalysis
from langgraph_app import langgraph_app

app = FastAPI(title="Ghidra AI Explainable-Decompiler Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/rename", response_model=RenameSuggestion)
async def rename(req: RenameRequest):
    result = langgraph_app.invoke(
        {
            "decompiled_code": req.decompiled_code,
            "function_name": req.function_name,
            "variables": [variable.model_dump() for variable in req.variables],
            "task": "rename",
        }
    )
    return result["result"]


@app.post("/memory_safety", response_model=MemorySafetyAnalysis)
async def memory_safety(req: RenameRequest):
    result = langgraph_app.invoke(
        {
            "decompiled_code": req.decompiled_code,
            "function_name": req.function_name,
            "task": "memory_safety",
        }
    )
    return result["result"]


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8000)
