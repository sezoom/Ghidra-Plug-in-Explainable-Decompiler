# Ghidra-Plugin Explainable-Decompiler


![k2think.png](./images/k2think.png)

AI Explainable-Decompiler is a modular Ghidra plugin and backend system for AI-assisted reverse engineering using K2-Think reasoning model. It supports multiple analysis components such as rename suggestions, memory safety analysis, crypto analysis, and deobfuscation through a scalable frontend and backend architecture.

---

## Overview

### Feature Summary

| Component | Purpose | Output Type |
|---|-----|---|
| Rename | Suggest better function and variable names |  Structured rename result       |
| Memory Safety | Detect classic memory safety issues  | Issue list + overall assessment |
| Crypto Analysis | Detect cryptographic weaknesses  | Issue list + overall assessment |
| Deobfuscation | Simplify and deobfuscate code  | Clean code + notes |


![k2think.png](./images/clib1.gif)
[Watch the demo video](./images/clib1.mp4)
---

## Architecture
The project is split into two main parts:

- **Frontend (Ghidra plugin, Java)**  
  Provides the UI inside Ghidra, extracts decompiled code and metadata, sends requests to the backend, and renders results.

- **Backend (Python / FastAPI)**  
  Receives analysis requests, dispatches them to the correct component, interacts with the LLM, validates structured output, and returns results to the frontend.

![GhidraPlugin.drawio.png](images/GhidraPlugin.drawio.png)

### Frontend structure

```text
ai.explainable
├─ plugin
│  ├─ AIExplainablePlugin.java
│  ├─ AnalysisProvider.java
│  ├─ AnalysisContext.java
│  ├─ AnalysisView.java
│  └─ ComponentRegistry.java
├─ backend
│  ├─ BackendClient.java
│  └─ HttpBackendClient.java
├─ components
│  ├─ AnalysisComponent.java
│  ├─ rename
│  │  ├─ RenameComponent.java
│  │  ├─ RenameRequest.java
│  │  ├─ RenameResult.java
│  │  ├─ RenameItem.java
│  │  ├─ VariableCandidate.java
│  │  └─ RenamePreviewBuilder.java
│  ├─ memory
│  │  ├─ MemorySafetyComponent.java
│  │  ├─ MemorySafetyRequest.java
│  │  ├─ MemorySafetyResult.java
│  │  └─ MemorySafetyIssue.java
│  ├─ deobfuscation
│  │  ├─ DeobfuscationComponent.java
│  │  ├─ DeobfuscationRequest.java
│  │  └─ DeobfuscationResult.java
│  └─ crypto
│     ├─ CryptoAnalysisComponent.java
│     ├─ CryptoAnalysisRequest.java
│     ├─ CryptoAnalysisIssue.java
│     └─ CryptoAnalysisResult.java
└─ decompiler
   └─ DecompileHelper.java
```

### Backend structure

```text
backend/
├─ app.py
├─ schemas.py
├─ llm.py
├─ analyzer.py
├─ components/
│  ├─ __init__.py
│  ├─ rename/
│  │  ├─ component.py
│  │  ├─ prompt.py
│  │  └─ schema.py
│  ├─ memory_safety/
│  │  ├─ component.py
│  │  ├─ prompt.py
│  │  └─ schema.py
│  ├─ crypto/
│  │  ├─ component.py
│  │  ├─ prompt.py
│  │  └─ schema.py
│  └─ deobfuscation/
│     ├─ component.py
│     ├─ prompt.py
│     └─ schema.py
```

---


## Frontend Flow

1. User loads a function in Ghidra.
2. The plugin decompiles the function.
3. The selected analysis component builds a request.
4. The frontend sends the request to the backend using:

```http
POST /analyze/{component_id}
```

5. The backend dispatches the request to the matching component.
6. The backend returns structured JSON.
7. The frontend renders the result in the plugin UI.

---

## Backend API

### Unified endpoint

```http
POST /analyze/{component_id}
Content-Type: application/json
```

Supported `component_id` values:

- `rename`
- `memory_safety`
- `crypto`
- `deobfuscation`

---

## Backend Request Structures

### Rename request

```json
{
  "decompiled_code": "string",
  "function_name": "string",
  "variables": [
    {
      "target_id": "string",
      "kind": "string",
      "current_name": "string",
      "data_type": "string",
      "storage": "string",
      "first_use": "string",
      "source_type": "string",
      "is_auto_name": true,
      "token_count": 0
    }
  ]
}
```

### Memory safety request

```json
{
  "decompiled_code": "string",
  "function_name": "string"
}
```

### Crypto analysis request

```json
{
  "decompiled_code": "string",
  "function_name": "string"
}
```

### Deobfuscation request

```json
{
  "decompiled_code": "string",
  "function_name": "string"
}
```

---

## Backend Response Structures

### Rename response

```json
{
  "function_rename": {
    "target_id": "function",
    "kind": "function",
    "old_name": "old_name",
    "new_name": "new_name",
    "explanation": "reason for rename"
  },
  "variable_renames": [
    {
      "target_id": "string",
      "kind": "string",
      "old_name": "string",
      "new_name": "string",
      "explanation": "string"
    }
  ],
  "summary": "string"
}
```

### Memory safety response

```json
{
  "issues": [
    {
      "issue_type": "string",
      "description": "string",
      "location": "string",
      "severity": "critical | high | medium | low",
      "suggestion": "string"
    }
  ],
  "overall_assessment": "string"
}
```

### Crypto analysis response

```json
{
  "issues": [
    {
      "issue_type": "string",
      "description": "string",
      "location": "string",
      "severity": "critical | high | medium | low",
      "suggestion": "string"
    }
  ],
  "overall_assessment": "string"
}
```

### Deobfuscation response

```json
{
  "clean_code": "string",
  "changes_summary": "string",
  "notes": "string"
}
```

---

## Example API Calls

### Rename

```http
POST /analyze/rename
Content-Type: application/json
```

```json
{
  "decompiled_code": "int FUN_140001000(...) { ... }",
  "function_name": "FUN_140001000",
  "variables": [
    {
      "target_id": "local:rbp-0x10@140001020",
      "kind": "local",
      "current_name": "local_10",
      "data_type": "int",
      "storage": "Stack[-0x10]:4",
      "first_use": "140001020",
      "source_type": "DEFAULT",
      "is_auto_name": true,
      "token_count": 3
    }
  ]
}
```

### Memory safety

```http
POST /analyze/memory_safety
Content-Type: application/json
```

```json
{
  "decompiled_code": "char buf[16]; gets(buf);",
  "function_name": "main"
}
```

### Crypto

```http
POST /analyze/crypto
Content-Type: application/json
```

```json
{
  "decompiled_code": "custom crypto logic here",
  "function_name": "FUN_140009000"
}
```

### Deobfuscation

```http
POST /analyze/deobfuscation
Content-Type: application/json
```

```json
{
  "decompiled_code": "obfuscated decompiled code here",
  "function_name": "FUN_140001234"
}
```

---

## How to Install the Plugin

### 1. Build the frontend extension

From the frontend project directory:

```bash
gradle clean buildExtension -PGHIDRA_INSTALL_DIR="/path/to/ghidra"
```

This produces a Ghidra extension zip.

### 2. Install in Ghidra

Open Ghidra and go to:

```text
File -> Install Extensions
```

Then:
- click the add/install option
- select the built extension zip
- restart Ghidra

![sc3.png](images/sc3.png)

### 3. Verify the plugin is available

After restart, open the plugin from the Ghidra UI and confirm:
- the analysis panel appears
- the component selector is visible
- backend requests can be sent

---

## How to Run the Backend

From the backend directory:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --reload
```

By default, the frontend expects the backend at:

```text
http://127.0.0.1:8000
```

---

## Dependencies

### Frontend
- Java
- Gradle
- Ghidra
- Gson
- RSyntaxTextArea

Example dependency for syntax highlighting:

```gradle
implementation 'com.fifesoft:rsyntaxtextarea:3.3.4'
```

### Backend
- Python 3.10+
- FastAPI
- Uvicorn
- Pydantic
- python-dotenv
- LangChain / model integration libraries as configured

---

## Extending the System

To add a new analysis component:

### Frontend
1. Create a new component under `components/<name>/`
2. Implement `AnalysisComponent`
3. Add request/result classes
4. Register it in `ComponentRegistry`

### Backend
1. Create `components/<name>/component.py`
2. Add `prompt.py`
3. Add `schema.py`
4. Register the component in `components/__init__.py`

If both sides use the same `component_id`, the unified API path works automatically:

```http
POST /analyze/<component_id>
```

---
