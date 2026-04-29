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

    for issue in llm_result.get("issues", []):
        for name in issue.get("function_involved", []):
            cleaned = name.split("(")[0].strip()
            if cleaned:
                functions.append(cleaned)

        for name in issue.get("variable_involved", []):
            cleaned = name.split("(")[0].strip()
            if cleaned:
                local_variables.append(cleaned)

    return run_verification(
        {"functions": functions, "local_variables": local_variables, "calls": calls},
        source_json,
    )


def verify(llm_result: dict, source_json_path: str) -> str:
    return format_report(run_report(llm_result, source_json_path))
