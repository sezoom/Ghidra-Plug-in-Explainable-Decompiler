def build_deobfuscation_prompt(state: dict) -> str:
    prompt = f"""You are an expert reverse engineer and code deobfuscation assistant.

Your task is to transform the following Ghidra decompiled C-like code into a human-friendly, clean, and readable programming style.

Goals:
- simplify confusing expressions and noisy decompiler artifacts
- deobfuscate intentionally obscured logic where possible
- preserve the original semantics
- use descriptive temporary names when needed, but do not invent external behavior not supported by the code
- produce code that a human analyst can read and reason about quickly
- keep the output as code, not as prose

Return ONLY one valid JSON object matching this structure exactly:

{{
 "clean_code": "string",
 "changes_summary": "string",
 "notes": "string"
 "functions_analyzed": ["string"]
 "variables_analyzed": ["string"]
}}

Rules:
- "clean_code" must contain the rewritten simplified/deobfuscated code.
- "changes_summary" should briefly summarize the main transformations.
- "notes" may mention ambiguities, assumptions, or parts that could not be confidently resolved.
- "functions_analyzed": include a list of the original function names that were analyzed.
- "variables_analyzed": include a list of the original variable names that were analyzed.

- Do NOT include markdown fences.
- Do NOT include reasoning.
- Do NOT include <think> tags.
- Do NOT include any extra fields.
- Return ONLY the JSON object.

Current function name: {state["function_name"]}

Decompiled code:
{state["decompiled_code"]}
"""

    # ── correction feedback (injected by the control loop) ───────────────
    correction_note = state.get("correction_note")
    if correction_note:
        prompt += f"\n\n⚠ Correction required:\n{correction_note}"
    # ─────────────────────────────────────────────────────────────────────
    return prompt
