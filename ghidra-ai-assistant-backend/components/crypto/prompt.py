def build_crypto_analysis_prompt(state: dict) -> str:
    return f"""You are an expert in applied cryptography, secure coding, and reverse engineering.

Analyze the following Ghidra decompiled C code for cryptographic issues and observations, including:
- hardcoded cryptographic keys, IVs, salts, or secrets
- insecure or deprecated algorithms and modes (for example DES, RC4, ECB, weak hashes)
- misuse of secure algorithms (for example constant IVs, predictable nonces, unauthenticated encryption)
- weak randomness or predictable key material
- custom crypto that appears broken or suspicious
- key management or secret handling weaknesses
- any other security-relevant cryptographic concerns

Return ONLY one valid JSON object matching this structure exactly:

{{
  "issues": [
    {{
      "issue_type": "string",
      "description": "string",
      "location": "string",
      "severity": "high/medium/low",
      "suggestion": "string"
    }}
  ],
  "overall_assessment": "string"
}}

If no cryptographic issues or notable crypto concerns are found, return:
{{"issues": [], "overall_assessment": "No clear cryptographic issues detected."}}

Return ONLY the JSON. No markdown, no reasoning, no <think> tags.

Current function name: {state["function_name"]}

Decompiled code:
{state["decompiled_code"]}
"""
