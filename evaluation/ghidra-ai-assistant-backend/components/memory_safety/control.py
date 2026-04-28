from components.control_base import (
    ControlReport,
    format_report,
    load_source_json,
    run_verification,
)


def run_report(llm_result: dict, source_json_path: str) -> ControlReport:
    """Returns raw ControlReport for the feedback loop."""
    source_json = load_source_json(source_json_path)

    functions: list[str] = []
    local_variables: list[str] = []
    calls: list[str] = []

    for issue in llm_result.get("issues", []):
        functions += [
            c.split("(")[0].strip() for c in issue.get("functions_involved", [])
        ]
        local_variables += [
            c.split("(")[0].strip() for c in issue.get("local_variables_involved", [])
        ]
        calls += [c.split("(")[0].strip() for c in issue.get("calls_involved", [])]

    return run_verification(
        {"functions": functions, "local_variables": local_variables, "calls": calls},
        source_json,
    )


def verify(llm_result: dict, source_json_path: str) -> str:
    """Returns formatted string — kept for backward compatibility."""
    return format_report(run_report(llm_result, source_json_path))
