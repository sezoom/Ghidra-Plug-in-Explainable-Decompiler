from __future__ import annotations
from typing import Optional
from langchain_openai import ChatOpenAI
from langchain_google_genai import ChatGoogleGenerativeAI
import os
from dotenv import load_dotenv
import re
env_path = os.getenv('PATH')
load_dotenv(override=True)
dotenv_path = os.getenv('PATH')

if dotenv_path:
    os.environ['PATH'] = env_path + ':' + dotenv_path

K2_THINK_API_KEY=os.getenv("K2_THINK_API_KEY")

#pasrsing the output of K2-Think by removeing the chain of thought and keep the answer only. the prompt request final answer only but the output has two tags <think> and <answer>
ANSWER_OPEN_RE  = re.compile(r"<answer\b[^>]*>", re.IGNORECASE)
ANSWER_CLOSE_RE = re.compile(r"</answer\s*>", re.IGNORECASE)
THINK_BLOCK_RE  = re.compile(r"<think\b[^>]*>.*?</think\s*>", re.IGNORECASE | re.DOTALL)
CODE_BLOCK_RE = re.compile(
    r"```(?:plaintext|anb|text)?\s*(.*?)```",
    re.IGNORECASE | re.DOTALL,
)
def extract_k2_think_answer(text: str) -> str:

    opens = list(ANSWER_OPEN_RE.finditer(text))
    if not opens:
        return "Answer not found"
    start = opens[-1].end()
    close = ANSWER_CLOSE_RE.search(text, start)
    body = text[start: close.start()] if close else text[start:]
    m = CODE_BLOCK_RE.search(body)
    if m:
        # if  the content inside the first ```...``` block
        code = m.group(1)
        return code
    else:
        return body

def simple_k2_extract(text: str) -> str:
    if not text:
        return ""
    text = text.strip()
    if "</think>" in text:
        text = text.split("</think>")[-1].strip()
    return text

def make_llm(model: Optional[str] = None, temperature: float = 0.1) -> ChatOpenAI:
    if "gemini" in model:
                return ChatGoogleGenerativeAI(
                    model=model,
                    temperature=temperature
                )
    else:
        if "k2-think" in model:
            return  ChatOpenAI(
                    model="MBZUAI-IFM/K2-Think-v2",
                    api_key=K2_THINK_API_KEY,
                    base_url="https://api.k2think.ai/v1",
                temperature=temperature)
        else:
            return ChatOpenAI(model=model, temperature=temperature)
