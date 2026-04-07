# Ghidra-Plug-in-Explainable-Decompiler

### Frontend
```aiignore
ghidra-ai-assistant/                  ← Ghidra Java plugin
├── extension.properties
├── build.gradle                      
└── src/
    └── main/
        └── java/
            └── ai/
                └── renamer/
                    ├── AIRenamerPlugin.java
                    ├── AIRenamerProvider.java
                    ├── AiService.java
                    ├── HttpAiService.java
                    ├── RenameResult.java
                    ├── MemorySafetyIssue.java
                    ├── MemorySafetyResult.java
                    └── DecompileHelper.java
```
#### Build the plugin
```bash
cd ~/path/to/ghidra-ai-assistant          
gradle clean buildExtension -PGHIDRA_INSTALL_DIR="/Applications/local/ghidra_11.2_PUBLIC/"
```



### Backend
```
ghidra-ai-assistant-backend/
├── llm.py                   
├── schemas.py                ← shared input models 
├── analyzer.py               ← central dispatcher 
├── app.py                    
├── components/
│   ├── __init__.py
│   ├── base.py               ← abstract base + registry
│   ├── rename/
│   │   ├── __init__.py
│   │   ├── schema.py         ← RenameSuggestion + RenameItem
│   │   ├── prompt.py         ← build_rename_prompt 
│   │   └── component.py      ← RenameComponent
│   └── memory_safety/
│       ├── __init__.py
│       ├── schema.py         ← MemorySafetyAnalysis + MemorySafetyIssue
│       ├── prompt.py         ← build_memory_safety_prompt 
│       └── component.py      ← MemorySafetyComponent
└── .env
```

#### Run backend:
```bash
cd ghidra-ai-assistant-backend
pip install -r requirements.txt
python app.py
```


----
### Troubleshot
if gradle not installed in mac
```bash
brew install gradle
```
for linux:

```bash
sudo apt update
sudo apt install gradle

```