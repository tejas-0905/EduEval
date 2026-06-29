# EduEval

EduEval has three local services:

- `frontend` - React/Vite app on port `5173`
- `edueval-backend` - Spring Boot API on port `8080`
- `edueval-ai` - Python/FastAPI evaluator on port `8000`

## Run The Project

Open three terminals from the project root.

### Environment variables

Set these before starting the backend. Replace values as needed for your machine.

#### Local Development (with H2 database)

```powershell
$env:EDUEVAL_DB_URL="jdbc:h2:mem:edueval;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
$env:EDUEVAL_DB_USERNAME="sa"
$env:EDUEVAL_DB_PASSWORD=""
$env:EDUEVAL_DB_DRIVER_CLASS_NAME="org.h2.Driver"
$env:EDUEVAL_JWT_SECRET="dev-secret-key-at-least-32-characters-for-local-testing"
$env:EDUEVAL_AI_BASE_URL="http://127.0.0.1:8000"
```

#### Production (with PostgreSQL)

```powershell
$env:EDUEVAL_DB_URL="jdbc:postgresql://localhost:5432/edueval"
$env:EDUEVAL_DB_USERNAME="postgres"
$env:EDUEVAL_DB_PASSWORD="keep-your-password"
$env:EDUEVAL_DB_DRIVER_CLASS_NAME="org.postgresql.Driver"
$env:EDUEVAL_JWT_SECRET="change-this-to-a-long-random-secret-at-least-32-characters"
$env:EDUEVAL_AI_BASE_URL="http://127.0.0.1:8000"
$env:CLOUDINARY_CLOUD_NAME="your_cloudinary_cloud_name"
$env:CLOUDINARY_API_KEY="your_cloudinary_api_key"
$env:CLOUDINARY_API_SECRET="your_cloudinary_api_secret"
$env:EDUEVAL_OCR_SPACE_API_KEY="your_ocr_space_api_key_here"
$env:EDUEVAL_OCR_SPACE_ENGINE="2"
$env:EDUEVAL_OCR_SPACE_MAX_UPLOAD_BYTES="900000"
$env:EDUEVAL_OCR_SPACE_IMAGE_MAX_SIDE="1600"
```

**Note:** For local development, you can omit all environment variables and the backend will use H2 database with sensible defaults. Cloudinary credentials are optional for local development and will fall back to local file URLs.

### 1. Python evaluator

```powershell
cd edueval-ai
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Image and scanned PDF submissions use OCR.Space. Engine 2 is the default because
it works reliably with the free API endpoint; set `EDUEVAL_OCR_SPACE_ENGINE=3`
only if your OCR.Space key supports it. The demo key `helloworld` is rate
limited, so use your own OCR.Space API key for reliable testing.

### 2. Spring Boot backend

```powershell
cd edueval-backend
.\mvnw.cmd spring-boot:run
```

### 3. React frontend

```powershell
cd frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:5173
```

The frontend calls the backend at `http://localhost:8080`, and the backend calls the Python evaluator at `http://127.0.0.1:8000`.
