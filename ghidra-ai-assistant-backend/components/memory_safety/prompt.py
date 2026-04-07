def build_memory_safety_prompt(state: dict) -> str:
    return f"""You are an expert in C memory safety and reverse engineering.

Analyze the following Ghidra decompiled C code for memory safety risks:
- buffer overflows
- unsafe pointer usage
- null pointer dereferences
- use-after-free
- unsafe string or formatting functions
- integer overflows in allocations
- any other classic memory safety issues

Return ONLY one valid JSON object matching this structure exactly:

{{
  "issues": [
    {{
      "issue_type": "string",
      "description": "string",
      "location": "string",
      "severity": "high/medium/low",
      "suggestion": "string"
    }}
  ],
  "overall_assessment": "string"
}}

If no issues, return: {{"issues": [], "overall_assessment": "No clear memory safety issues detected."}}

Return ONLY the JSON. No markdown, no reasoning, no <think> tags.

Current function name: {state["function_name"]}

Decompiled code:
{state["decompiled_code"]}
"""