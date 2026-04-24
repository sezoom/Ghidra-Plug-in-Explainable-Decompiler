import json
import os
import time
from typing import Any, Dict

from components.base import COMPONENT_REGISTRY
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
    prompt = component.build_prompt(request_payload)

    if IS_K2:
        result = _invoke_k2_with_retry(prompt, component.response_model)
    else:
        structured_llm = llm.with_structured_output(component.response_model)
        response = structured_llm.invoke([{"role": "user", "content": prompt}])
        result = component.post_process(response.model_dump())

    # ── control layer ────────────────────────────────────────────────────────
    source_json_path = payload.get("source_json_path")
    if source_json_path:
        control_output = component.run_control(result, source_json_path)
        if control_output is not None:
            result["control_output"] = control_output
    # ─────────────────────────────────────────────────────────────────────────

    return result


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
