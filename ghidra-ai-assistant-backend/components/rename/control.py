from components.control_base import (
    ControlReport,
    format_report,
    load_source_json,
    run_verification,
)


def run_report(llm_result: dict, source_json_path: str) -> ControlReport:
    source_json = load_source_json(source_json_path)

    functions: list[str] = []
    local_variables: list[str] = []
    calls: list[str] = []

    # function_rename is a single dict
    function_rename = llm_result.get("function_rename")
    if isinstance(function_rename, dict):
        old_name = function_rename.get("old_name", "").split("(")[0].strip()
        if old_name:
            functions.append(old_name)

    # variable_renames is a list
    for rename in llm_result.get("variable_renames", []):
        old_name = rename.get("old_name", "").split("(")[0].strip()
        if old_name:
            local_variables.append(old_name)

    return run_verification(
        {"functions": functions, "local_variables": local_variables, "calls": calls},
        source_json,
    )


def verify(llm_result: dict, source_json_path: str) -> str:
    """Returns formatted string — kept for backward compatibility."""
    return format_report(run_report(llm_result, source_json_path))
