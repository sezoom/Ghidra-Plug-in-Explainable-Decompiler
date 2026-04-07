import json
import time
from typing import Dict, Any
from pydantic import ValidationError

from llm import make_llm, simple_k2_extract
from components.base import COMPONENT_REGISTRY
import os
from dotenv import load_dotenv

load_dotenv()

LLM_NAME = os.getenv("LLM1", "").strip().lower()
IS_K2 = LLM_NAME == "k2-think"

llm = make_llm(os.getenv("LLM1"), 0.1)


def analyze(task: str, payload: Dict[str, Any]) -> Dict[str, Any]:
    if task not in COMPONENT_REGISTRY:
        raise ValueError(f"Unknown task '{task}'. Available: {list(COMPONENT_REGISTRY.keys())}")

    component = COMPONENT_REGISTRY[task]

    # Validate request against the component-specific request model.
    request_obj = component.request_model.model_validate(payload)
    request_payload = request_obj.model_dump()
    prompt = component.build_prompt(request_payload)

    if IS_K2:
        return _invoke_k2_with_retry(prompt, component.response_model)

    structured_llm = llm.with_structured_output(component.response_model)
    response = structured_llm.invoke([{"role": "user", "content": prompt}])
    return component.post_process(response.model_dump())


def _invoke_k2_with_retry(prompt: str, schema_model, retries: int = 3, delay: float = 1.0) -> dict:
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
                raise ValueError(f"K2-Think failed after {retries} attempts: {e}") from e
            time.sleep(delay * (attempt + 1))
    raise ValueError(f"Unreachable: {last_error}")
