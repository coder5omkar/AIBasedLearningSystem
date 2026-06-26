# AI Tutor

[![Java 26](https://img.shields.io/badge/Java-26-%23ED8B00?logo=openjdk&logoColor=white)](https://jdk.java.net/26/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-%236DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI 2.0](https://img.shields.io/badge/Spring_AI-2.0-%236DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![React 18](https://img.shields.io/badge/React-18-%2361DAFB?logo=react&logoColor=white)](https://reactjs.org/)
[![H2 Database](https://img.shields.io/badge/Database-H2-%230072C6?logo=h2&logoColor=white)](https://www.h2database.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-%232496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Manifests-%23326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Ollama](https://img.shields.io/badge/LLM-Ollama-%23000000?logo=ollama&logoColor=white)](https://ollama.ai/)
[![OpenAI](https://img.shields.io/badge/LLM-OpenAI-%23412991?logo=openai&logoColor=white)](https://openai.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-%23000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An AI-powered education platform that generates personalized learning journeys using Large Language Models (LLMs).

## Features

- **Subject Selection** -- Choose from 15 academic and technology subjects
- **AI-Generated Syllabus** -- LLM creates a structured syllabus with chapters, sections, and concepts
- **Concept-by-Concept Learning** -- Study one concept at a time with AI-generated explanations
- **Doubt Chat** -- Ask questions about the current concept to the AI tutor
- **MCQ Assessment** -- Pass 5 MCQs (all correct) to unlock the next concept
- **Progress Tracking** -- Resume learning from where you left off
- **Multiple LLM Providers** -- Supports Ollama (local/free) and OpenAI

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser (React SPA)                        │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                     Nginx (reverse proxy)                     │ │
│  │         /api/* ──────────────────> aitutor-backend:8080       │ │
│  └───────────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────────┐
│                   Spring Boot Backend (:8080)                       │
│  ┌────────────┬────────────┬─────────────┬──────────┬────────────┐ │
│  │ Controllers│  Services  │    JPA      │ Security │  Spring AI │ │
│  │  (REST)    │ (Business) │ (H2 DB)     │  (JWT)   │ (LLM Abst) │ │
│  └────────────┴────────────┴─────────────┴──────────┴────────────┘ │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────────┐
│                      LLM Provider                                   │
│  ┌─────────────────────┐    ┌─────────────────────────────────────┐ │
│  │  Ollama (local)     │    │  OpenAI (cloud)                     │ │
│  │  :11434             │    │  api.openai.com                     │ │
│  └─────────────────────┘    └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### Three-Panel Learning Interface

```
┌──────────────────────────────────────────────────────────────────────┐
│                       Header                                        │
│   AI Tutor    [Model Selector]    [username]    [Logout]            │
├──────────────────┬───────────────────────────┬──────────────────────┤
│   Syllabus Tree  │   Concept Content         │   MCQ Test Panel    │
│                  │                           │                      │
│   ┌──────────┐   │   ┌───────────────────┐   │   ┌──────────────┐  │
│   │ Chapter 1│   │   │  AI-generated     │   │   │ Question 1/5 │  │
│   │  ├ Section│   │   │  explanation      │   │   │  ○ option A  │  │
│   │  ├ Section│   │   │  with markdown    │   │   │  ○ option B  │  │
│   │ └─────────┘   │   └───────────────────┘   │   │  ○ option C  │  │
│   │ ┌──────────┐  │   ┌───────────────────┐   │   │  ○ option D  │  │
│   │ │ Chapter 2│  │   │  Doubt Chat       │   │   └──────────────┘  │
│   │ │  ├ Section│  │   │  ┌─────────────┐  │   │   Submit          │
│   │ └──────────┘  │   │  │ Type a doubt │  │   │                    │
│   └───────────────┘   │  └─────────────┘  │   │                    │
│                       └───────────────────┘   │                    │
└──────────────────────────────────────────────────────────────────────┘
```

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Java 26** | Runtime |
| **Spring Boot 4.1.0** | Application framework |
| **Spring AI 2.0.0** | LLM provider abstraction (Ollama + OpenAI) |
| **Spring Security + JWT** | Authentication & authorization |
| **Spring Data JPA** | ORM / persistence |
| **H2 Database** | Embedded file-based SQL database |
| **Lombok** | Boilerplate reduction |
| **Maven** | Build tool |

### Frontend
| Technology | Purpose |
|------------|---------|
| **React 18** | UI framework |
| **Axios** | HTTP client |
| **react-markdown** | AI content rendering |
| **react-scripts 5** | Build tooling (CRA) |
| **Nginx** | Production reverse proxy |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Docker / Docker Compose** | Containerized deployment |
| **Kubernetes** | Orchestration (manifests included) |
| **Ollama** | Local LLM inference |
| **OpenAI** | Cloud LLM provider |

## Quick Start

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)
- 8 GB+ RAM recommended (for local LLM)

### Docker Compose (Recommended)

```bash
cd AITutor
docker compose up -d
```

This starts three containers:

| Container | Port | Description |
|-----------|------|-------------|
| `aitutor-ollama` | 11434 | Local LLM inference |
| `aitutor-backend` | 8080 | Spring Boot REST API |
| `aitutor-frontend` | 3000 | React SPA via Nginx |

> **Note:** The backend waits for Ollama to be healthy and pulls the `llama3.2:3b` model on first start. Allow 1-5 minutes depending on your network.

Open **http://localhost:3000** and register an account.

### GPU Acceleration

If you have an NVIDIA GPU, use the GPU override:

```bash
cd AITutor
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d
```

This enables GPU passthrough for Ollama, significantly accelerating inference (~85 tok/s GPU vs ~8 tok/s CPU).

### Manual Docker Run

#### 1. Ollama

```bash
docker run -d \
  --name aitutor-ollama \
  -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  -e OLLAMA_KEEP_ALIVE=24h \
  ollama/ollama:latest
```

Pull the model:

```bash
docker exec aitutor-ollama ollama pull llama3.2:3b
```

#### 2. Backend

```bash
docker run -d \
  --name aitutor-backend \
  -p 8080:8080 \
  -v app_data:/data \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -e OLLAMA_HOST=http://host.docker.internal:11434 \
  -e OLLAMA_MODEL=llama3.2:3b \
  aitutor-backend
```

Build first: `docker build -t aitutor-backend AITutor`

#### 3. Frontend

```bash
docker build -t aitutor-frontend aitutor-ui
docker run -d \
  --name aitutor-frontend \
  -p 3000:80 \
  aitutor-frontend
```

### Without Docker (Development)

#### Backend

```bash
cd AITutor
rm -rf data/
./mvnw spring-boot:run
```

#### Frontend

```bash
cd aitutor-ui
npm install
npm start
```

#### LLM Provider

**Ollama**: Install [Ollama](https://ollama.ai), run `ollama pull llama3.2:3b`, then select `ollama` / `llama3.2:3b` in the app header.

**OpenAI**: Get an API key, then select `openai` / `gpt-4o-mini` in the app header and enter your key.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/subjects` | List all subjects |
| `POST` | `/api/auth/register` | Register |
| `POST` | `/api/auth/login` | Login |
| `POST` | `/api/auth/forgot-password` | Forgot password |
| `POST` | `/api/syllabus/generate` | Generate syllabus via LLM |
| `POST` | `/api/syllabus/upload` | Upload syllabus content |
| `GET` | `/api/syllabus/{id}/structure` | Get full structure |
| `GET` | `/api/syllabus/active` | Get active syllabus |
| `PUT` | `/api/syllabus/{id}` | Update syllabus |
| `GET` | `/api/syllabus/concept/{id}/content` | Get concept with LLM content |
| `GET` | `/api/learning/progress/{syllabusId}` | Get progress |
| `GET` | `/api/learning/mcq/{conceptId}` | Get MCQs for concept |
| `POST` | `/api/learning/mcq/{conceptId}/submit` | Submit MCQ answers |
| `POST` | `/api/chat/ask-doubt` | Ask a doubt |

## Project Structure

```
AIBasedLearningSystem/
├── AITutor/                     # Spring Boot backend
│   ├── src/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── docker-compose.gpu.yml
│   ├── pom.xml
│   └── k8s/                     # Kubernetes manifests
├── aitutor-ui/                  # React frontend
│   ├── src/
│   ├── public/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
└── README.md
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_HOST` | `http://localhost:11434` | Ollama host for entrypoint |
| `OLLAMA_MODEL` | `llama3.2:3b` | Default model to pull |
| `SPRING_PROFILES_ACTIVE` | -- | Spring profile (`docker`) |

## License

MIT
