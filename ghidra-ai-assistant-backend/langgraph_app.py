from langgraph.graph import StateGraph, START, END
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from schemas import RenameSuggestion, MemorySafetyAnalysis, RenameRequest
from pydantic import BaseModel
from typing import Optional
import os
from dotenv import load_dotenv

load_dotenv()

llm = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0,
    api_key=os.getenv("OPENAI_API_KEY")
)

rename_structured = llm.with_structured_output(RenameSuggestion)
safety_structured = llm.with_structured_output(MemorySafetyAnalysis)

class State(BaseModel):
    decompiled_code: str
    function_name: str
    task: str
    result: dict | None = None

def analyze_node(state: State) -> dict:
    if state.task == "rename":
        prompt = f"""You are an expert reverse engineer.
Rename the function and every variable in this Ghidra decompiled C code to meaningful, readable names.

Return JSON matching this structure exactly:
- new_function_name: string
- variable_renames: array of objects with keys old_name and new_name
- explanation: string

Current function name: {state.function_name}

Decompiled code:
{state.decompiled_code}
"""
        suggestion = rename_structured.invoke([HumanMessage(content=prompt)])
        return {"result": suggestion.model_dump()}

    elif state.task == "memory_safety":
        prompt = f"""You are an expert in C memory safety and reverse engineering.
Analyze the following Ghidra decompiled C code for memory safety risks:
- buffer overflows (stack/heap)
- unsafe pointer usage / arithmetic
- null pointer dereferences
- use-after-free
- unsafe string functions
- integer overflows in allocations
- any other classic memory safety issues

Return JSON matching the schema exactly. If no issues, return an empty issues list.

Decompiled code:
{state.decompiled_code}
Current function: {state.function_name}
"""
        analysis = safety_structured.invoke([HumanMessage(content=prompt)])
        return {"result": analysis.model_dump()}

    raise ValueError(f"Unknown task: {state.task}")

graph = StateGraph(State)
graph.add_node("analyze", analyze_node)
graph.add_edge(START, "analyze")
graph.add_edge("analyze", END)

langgraph_app = graph.compile()