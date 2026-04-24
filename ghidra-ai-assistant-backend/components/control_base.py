import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

# ── Result dataclass ──────────────────────────────────────────────────────────


@dataclass
class ControlReport:
    # functions
    nb_function_tested: int = 0
    nb_function_false: int = 0
    list_function_false: list[str] = field(default_factory=list)
    # local variables
    nb_local_variables_tested: int = 0
    nb_local_variables_false: int = 0
    list_local_variables_false: list[str] = field(default_factory=list)
    # calls
    nb_calls_tested: int = 0
    nb_calls_false: int = 0
    list_calls_false: list[str] = field(default_factory=list)

    @property
    def total_nb_of_tested(self) -> int:
        return (
            self.nb_function_tested
            + self.nb_local_variables_tested
            + self.nb_calls_tested
        )

    @property
    def false_rate(self) -> float:
        total_false = (
            self.nb_function_false + self.nb_local_variables_false + self.nb_calls_false
        )
        return (
            round(total_false / self.total_nb_of_tested, 3)
            if self.total_nb_of_tested
            else 0.0
        )

    def to_dict(self) -> dict:
        return {
            "functions": {
                "nb_tested": self.nb_function_tested,
                "nb_false": self.nb_function_false,
                "false_list": self.list_function_false,
            },
            "local_variables": {
                "nb_tested": self.nb_local_variables_tested,
                "nb_false": self.nb_local_variables_false,
                "false_list": self.list_local_variables_false,
            },
            "calls": {
                "nb_tested": self.nb_calls_tested,
                "nb_false": self.nb_calls_false,
                "false_list": self.list_calls_false,
            },
            "summary": {
                "total_nb_of_tested": self.total_nb_of_tested,
                "false_rate": self.false_rate,
            },
        }


# ── Load source JSON ──────────────────────────────────────────────────────────


def load_source_json(path: str) -> dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


# ── Core verification ─────────────────────────────────────────────────────────


def run_verification(
    claims: dict[
        str, list[str]
    ],  # {"functions": [...], "local_variables": [...], "calls": [...]}
    source_json: dict[str, Any],
) -> ControlReport:
    """
    claims keys:
        functions        – function names the LLM mentioned
        local_variables  – local variable names the LLM mentioned
        calls            – callee names the LLM mentioned

    Checks each claim against the ground-truth source_json.
    """
    report = ControlReport()
    functions = source_json.get("functions", [])

    # Build lookup sets from source JSON
    known_local_variables: set[str] = {
        local["name"] for f in functions for local in f.get("locals", [])
    }

    known_calls: set[str] = {
        call["name"] for f in functions for call in f.get("calls", [])
    }

    known_functions: set[str] = {f["name"] for f in functions} | known_calls

    # --- Check function names ---
    claimed_functions = claims.get("functions", [])
    report.nb_function_tested = len(claimed_functions)
    for name in claimed_functions:
        if name not in known_functions:
            report.list_function_false.append(name)
    report.nb_function_false = len(report.list_function_false)

    # --- Check local variables ---
    claimed_locals = claims.get("local_variables", [])
    report.nb_local_variables_tested = len(claimed_locals)
    for name in claimed_locals:
        if name not in known_local_variables:
            report.list_local_variables_false.append(name)
    report.nb_local_variables_false = len(report.list_local_variables_false)

    # --- Check calls ---
    claimed_calls = claims.get("calls", [])
    report.nb_calls_tested = len(claimed_calls)
    for name in claimed_calls:
        if name not in known_calls:
            report.list_calls_false.append(name)
    report.nb_calls_false = len(report.list_calls_false)

    return report


# ── Formatter ─────────────────────────────────────────────────────────────────


def format_report(report: ControlReport) -> str:
    # Overall verdict
    if report.false_rate == 0.0:
        verdict = "✔ PASS — all claims grounded in source"
    elif report.false_rate < 0.3:
        verdict = "⚠ PARTIAL — some claims unverified"
    else:
        verdict = "✘ FAIL — high hallucination risk"

    def line(label, tested, false_count):
        status = "✔" if false_count == 0 else "✘"
        return f"  {status} {label:<18} tested: {tested}, false: {false_count}"

    # Build errors section
    errors = []
    if any(
        [
            report.list_function_false,
            report.list_local_variables_false,
            report.list_calls_false,
        ]
    ):
        errors.append("✘ Unverified claims:")
        errors.append(
            f"  Functions:       {', '.join(report.list_function_false) if report.list_function_false else '—'}"
        )
        errors.append(
            f"  Local variables: {', '.join(report.list_local_variables_false) if report.list_local_variables_false else '—'}"
        )
        errors.append(
            f"  Calls:           {', '.join(report.list_calls_false) if report.list_calls_false else '—'}"
        )

    lines = [
        "──────────────────────────────────────────────────────────",
        "\n[ Control Layer ]\n",
        "Verifies that functions and variables mentioned by the LLM exist in the original decompiled code.\n",
        f"Verdict: {verdict}",
        line("Functions", report.nb_function_tested, report.nb_function_false),
        line(
            "Local variables",
            report.nb_local_variables_tested,
            report.nb_local_variables_false,
        ),
        line("Calls", report.nb_calls_tested, report.nb_calls_false),
        "",
        f"Total tested: {report.total_nb_of_tested}  |  False rate: {report.false_rate:.1%}",
    ]

    if errors:
        lines.append("")
        lines.extend(errors)

    lines.append("\n──────────────────────────────────────────────────────────")

    return "\n".join(lines)
