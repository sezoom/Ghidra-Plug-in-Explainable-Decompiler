import json
import os
import time
from typing import Any, Dict

from components.base import COMPONENT_REGISTRY
from components.control_base import format_report
from dotenv import load_dotenv
from llm import make_llm, simple_k2_extract
from pydantic import ValidationError

load_dotenv()

LLM_NAME = os.getenv("LLM1", "").strip().lower()
IS_K2 = LLM_NAME == "k2-think"

llm = make_llm(os.getenv("LLM1"), 0.1)


def analyze(task: str, payload: Dict[str, Any]) -> Dict[str, Any]:

    if task not in COMPONENT_REGISTRY:
        raise ValueError(
            f"Unknown task '{task}'. Available: {list(COMPONENT_REGISTRY.keys())}"
        )

    component = COMPONENT_REGISTRY[task]
    request_obj = component.request_model.model_validate(payload)
    request_payload = request_obj.model_dump()
    source_json_path = payload.get("source_json_path")

    # ── initial LLM call ─────────────────────────────────────────────────────
    result = _invoke_llm(component, request_payload)

    # ── feedback loop ────────────────────────────────────────────────────────
    if source_json_path:
        result = _correction_loop(component, request_payload, result, source_json_path)

    return result


def _invoke_llm(component, request_payload: dict) -> dict:
    """Single LLM call — shared by initial call and correction loop."""
    prompt = component.build_prompt(request_payload)
    if IS_K2:
        return _invoke_k2_with_retry(prompt, component.response_model)
    structured_llm = llm.with_structured_output(component.response_model)
    response = structured_llm.invoke([{"role": "user", "content": prompt}])
    return component.post_process(response.model_dump())


def _correction_loop(
    component,
    request_payload: dict,
    result: dict,
    source_json_path: str,
    max_attempts: int = 3,
) -> dict:
    """
    Verify LLM output against source JSON.
    If unverified claims exist, feed them back to the LLM and retry.
    Attach the final control report to the result.
    """
    for attempt in range(1, max_attempts + 1):
        # run control layer
        report = component.run_control_report(result, source_json_path)

        if report is None:
            # component has no control — attach nothing, return as-is
            return result

        if report.false_rate == 0.0 or attempt == max_attempts:
            result["control_output"] = format_report(report, attempt)
            return result

        # FAIL — build correction prompt and retry
        print(
            f"[control] attempt {attempt}/{max_attempts} "
            f"false_rate={report.false_rate:.1%} — retrying with correction prompt"
        )
        corrected_payload = _inject_correction(request_payload, report)
        result = _invoke_llm(component, corrected_payload)

    return result  # unreachable but satisfies type checker


def _inject_correction(request_payload: dict, report) -> dict:
    """
    Inject unverified claims as a correction note into the prompt payload.
    The component's build_prompt() will include it if it reads 'correction_note'.
    """
    lines = ["The following items you referenced do not exist in the source code."]
    lines.append(
        "Please revise your answer using only names present in the decompiled code.\n"
    )

    if report.list_function_false:
        lines.append(
            f"Unknown functions:        {', '.join(report.list_function_false)}"
        )
    if report.list_local_variables_false:
        lines.append(
            f"Unknown local variables:  {', '.join(report.list_local_variables_false)}"
        )
    if report.list_calls_false:
        lines.append(f"Unknown calls:            {', '.join(report.list_calls_false)}")

    corrected = dict(request_payload)
    corrected["correction_note"] = "\n".join(lines)
    return corrected


def _invoke_k2_with_retry(
    prompt: str, schema_model, retries: int = 5, delay: float = 1.0
) -> dict:
    last_error = None
    for attempt in range(retries):
        try:
            response = llm.invoke([{"role": "user", "content": prompt}])
            text = response.content if hasattr(response, "content") else str(response)
            cleaned = simple_k2_extract(text)
            if not cleaned.strip():
                raise ValueError("Empty response from K2")
            parsed = json.loads(cleaned)
            validated = schema_model.model_validate(parsed)
            return validated.model_dump()
        except (json.JSONDecodeError, ValidationError, ValueError) as e:
            last_error = e
            if attempt == retries - 1:
                raise ValueError(
                    f"K2-Think failed after {retries} attempts: {e}"
                ) from e
            time.sleep(delay * (attempt + 1))
    raise ValueError(f"Unreachable: {last_error}")
