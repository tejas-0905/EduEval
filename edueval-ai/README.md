# EduEval AI Engine

Local Python evaluation service for the Spring Boot backend.

## Start

```powershell
cd edueval-ai
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Spring Boot already points to this service with:

```yaml
app:
  ai-engine:
    base-url: http://localhost:8000
```

For PDF submissions, `pypdf` is enough when the PDF contains selectable text.
For image submissions, install the Tesseract OCR desktop app and make sure
`tesseract.exe` is available on your PATH.
