"""
Evaluation script for the Ghidra AI Explainable Decompiler.
Tests 4 components with 2 configurations (k2, k2_controller) across N files and rounds.
"""

import json
import pathlib
import time
from dataclasses import dataclass, field

import requests

# ── Settings ──────────────────────────────────────────────────────────────────


@dataclass
class EvaluationSettings:
    # Fixed settings
    test_types: list[str] = field(default_factory=lambda: ["k2", "k2_controller"])
    data_path: str = "data/ghidra_json"
    nb_rounds: int = 3
    components: list[str] = field(
        default_factory=lambda: ["crypto", "deobfuscation", "memory_safety", "rename"]
    )
    backend_url: str = "http://127.0.0.1:8000"
    output_file: str = "results.json"

    # Mutable state (updated during loop)
    files_list: list[str] = field(default_factory=list)
    test_type: str = ""
    component_name: str = ""
    file_name: str = ""
    file_id: int = 0
    round_id: int = 0
    run_id: str = ""


# ── Payload builder ───────────────────────────────────────────────────────────


def build_payload(
    component_name: str, snapshot: dict, snapshot_path: str, use_controller: bool
) -> dict:
    """Build the request payload for a given component from a snapshot JSON."""
    func = snapshot["functions"][0]
    decompiled = func["decompiled_c"]
    func_name = func["name"]
    path = (
        snapshot_path if use_controller else snapshot_path
    )  # always send path for metrics

    base = {
        "decompiled_code": decompiled,
        "function_name": func_name,
        "source_json_path": path,
        "skip_reflection": not use_controller,  # ← True for k2, False for k2_controller
    }

    if component_name == "rename":
        base["variables"] = [
            {
                "target_id": local["name"],
                "kind": "local",
                "current_name": local["name"],
                "data_type": local["type"],
                "storage": "",
                "first_use": "0",
                "source_type": "DEFAULT",
                "is_auto_name": True,
                "token_count": 1,
            }
            for local in func.get("locals", [])
        ]

    return base


# ── Result parser ─────────────────────────────────────────────────────────────


def parse_result(response: dict, test_type: str) -> dict:
    report = response.get("control_report", {})
    return {
        "false_rate": report.get("false_rate", 0.0),
        "total_nb_of_tested": report.get("total_nb_of_tested", 0),
        "control_attempts": report.get("control_attempts", 1),
        "errors": report.get("errors", {"functions": [], "variables": [], "calls": []}),
    }


# ── Save results ──────────────────────────────────────────────────────────────


def save_results(results: dict, output_file: str) -> None:
    pathlib.Path(output_file).write_text(
        json.dumps(results, indent=2, ensure_ascii=False)
    )
    print(f"  → saved to {output_file}")


# ── Main evaluation loop ──────────────────────────────────────────────────────


def run_evaluation():
    settings = EvaluationSettings()
    results: dict = {}

    # Collect all JSON files from data_path
    data_dir = pathlib.Path(settings.data_path)
    if not data_dir.exists():
        raise FileNotFoundError(f"Data directory not found: {data_dir}")

    settings.files_list = sorted(data_dir.glob("*.json"))
    print(f"Found {len(settings.files_list)} files in {settings.data_path}")

    # ── Main loop ─────────────────────────────────────────────────────────────
    for test_type in settings.test_types:
        settings.test_type = test_type
        use_controller = test_type == "k2_controller"

        for component_name in settings.components:
            settings.component_name = component_name

            for file_path in settings.files_list:
                settings.file_name = file_path.stem  # e.g. "1_0_50"

                # Load snapshot
                try:
                    snapshot = json.loads(file_path.read_text(encoding="utf-8"))
                except Exception as e:
                    print(f"  [SKIP] could not load {file_path.name}: {e}")
                    continue

                for round_id in range(1, settings.nb_rounds + 1):
                    settings.round_id = round_id
                    settings.run_id = (
                        f"{test_type}.{component_name}.{settings.file_name}.{round_id}"
                    )

                    print(f"[{settings.run_id}]")

                    # Build payload
                    payload = build_payload(
                        component_name,
                        snapshot,
                        str(file_path.resolve()),
                        use_controller,
                    )

                    # Call backend
                    try:
                        response = requests.post(
                            f"{settings.backend_url}/analyze/{component_name}",
                            json=payload,
                            timeout=120,
                        )
                        response.raise_for_status()
                        response_data = response.json()
                    except Exception as e:
                        print(f"  [ERROR] {e}")
                        results[settings.run_id] = {
                            "test_type": test_type,
                            "component_name": component_name,
                            "file_name": settings.file_name,
                            "false_rate": None,
                            "total_nb_of_tested": None,
                            "control_attempts": None,
                            "errors": {"functions": [], "variables": [], "calls": []},
                            "error_message": str(e),
                        }
                        save_results(results, settings.output_file)
                        continue

                    # Parse and store result
                    metrics = parse_result(response_data, test_type)
                    results[settings.run_id] = {
                        "test_type": test_type,
                        "component_name": component_name,
                        "file_name": settings.file_name,
                        "false_rate": metrics["false_rate"],
                        "total_nb_of_tested": metrics["total_nb_of_tested"],
                        "control_attempts": metrics["control_attempts"],
                        "errors": metrics["errors"],
                    }

                    print(
                        f"  false_rate={metrics['false_rate']:.1%} "
                        f"tested={metrics['total_nb_of_tested']} "
                        f"attempts={metrics['control_attempts']}"
                    )

                    # Save after every run
                    save_results(results, settings.output_file)

                    # Small delay to avoid hammering the backend
                    time.sleep(3)

    print(f"\nEvaluation complete. {len(results)} runs saved to {settings.output_file}")


if __name__ == "__main__":
    run_evaluation()
