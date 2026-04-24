from components.control_base import format_report, load_source_json, run_verification


def verify(llm_result: dict, source_json_path: str) -> str:
    """
    Extract claims from the LLM result, verify against source JSON,
    return a formatted control string.
    """
    source_json = load_source_json(source_json_path)
    # ── Extract claims from LLM output ───────────────────────────────────────
    functions: list[str] = []
    local_variables: list[str] = []
    calls: list[str] = []
    for issue in llm_result.get("issues", []):
        functions += issue.get("functions_involved", [])
        local_variables += issue.get("local_variables_involved", [])
        calls += issue.get("calls_involved", [])

    # Strip signatures — keep only the name before '('
    functions = [f.split("(")[0].strip() for f in functions]
    calls = [c.split("(")[0].strip() for c in calls]

    claims = {
        "functions": functions,
        "local_variables": local_variables,
        "calls": calls,
    }
    # ─────────────────────────────────────────────────────────────────────────

    report = run_verification(claims, source_json)
    return format_report(report)
