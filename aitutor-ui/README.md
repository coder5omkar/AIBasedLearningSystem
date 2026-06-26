# AI Tutor

An AI-powered education platform that generates personalized learning journeys using LLMs.

## Features

- **Subject Selection** - Choose from 15 academic and technology subjects
- **AI-Generated Syllabus** - LLM creates a structured syllabus with chapters, sections, and concepts
- **Concept-by-Concept Learning** - Study one concept at a time with AI-generated explanations
- **Doubt Chat** - Ask questions about the current concept to the AI tutor
- **MCQ Assessment** - Pass 5 MCQs (all correct) to unlock the next concept
- **Progress Tracking** - Resume learning from where you left off
- **One Subject at a Time** - Complete all concepts before starting a new subject
- **Multiple LLM Providers** - Supports Ollama (local) and OpenAI

## Tech Stack

- **Frontend**: React, Axios
- **Backend**: Spring Boot 3.x, Spring AI, Spring Security with JWT
- **Database**: H2 (file-based)
- **LLM**: Ollama or OpenAI

## Prerequisites

- Java 26+
- Node.js 18+
- npm
- Ollama (for local LLM) or OpenAI API key

## Quick Start

### 1. Clone and Setup

```bash
git clone <repo-url>
cd AITutor
```

### 2. Backend Setup

```bash
# Delete old H2 database for fresh schema
rm -rf data/

# Start backend
./mvnw spring-boot:run
```

Backend starts at `http://localhost:8080`.

### 3. Frontend Setup

```bash
cd aitutor-ui
npm install
npm start
```

Frontend starts at `http://localhost:3000`.

### 4. LLM Provider Setup

**Option A: Ollama (local)**
- Install [Ollama](https://ollama.ai)
- Pull a model: `ollama pull llama3.2:3b`
- In the app header, select provider: `ollama`, model: `llama3.2:3b`

**Option B: OpenAI**
- Get an API key from [OpenAI](https://platform.openai.com/api-keys)
- In the app header, select provider: `openai`, model: `gpt-4o-mini`
- Enter your API key

## Usage Flow

1. **Register/Login** - Create an account or log in
2. **Select Subject** - Choose a subject from the grid
3. **Generate Syllabus** - Let AI create a syllabus or upload your own
4. **Learn Concepts** - Navigate through the syllabus tree on the left
5. **Read Content** - View AI-generated explanations in the center panel
6. **Ask Doubts** - Use the chat at the bottom of the center panel
7. **Take MCQ Test** - Pass all 5 questions in the right panel
8. **Progress** - Repeat until all concepts in the subject are complete

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Header                           │
│  📚 AI Tutor  [Model Selector]  [Logout]           │
├──────────┬──────────────────────────┬───────────────┤
│ Syllabus │   Concept Content        │  MCQ Test     │
│  Tree    │   ──────────────────     │  Panel        │
│          │   Doubt Chat            │               │
│          │                          │               │
└──────────┴──────────────────────────┴───────────────┘
```

## API Endpoints

### Public
- `GET /api/subjects` - List all subjects

### Auth
- `POST /api/auth/register` - Register
- `POST /api/auth/login` - Login
- `POST /api/auth/forgot-password` - Forgot password

### Syllabus
- `POST /api/syllabus/generate` - Generate syllabus via LLM
- `POST /api/syllabus/upload` - Upload syllabus content
- `GET /api/syllabus/{id}/structure` - Get full structure
- `GET /api/syllabus/active` - Get user's active syllabus
- `PUT /api/syllabus/{id}` - Update syllabus
- `GET /api/syllabus/concept/{id}/content` - Get concept with LLM-generated content

### Learning
- `GET /api/learning/progress/{syllabusId}` - Get user progress
- `GET /api/learning/mcq/{conceptId}` - Get MCQs for a concept
- `POST /api/learning/mcq/{conceptId}/submit` - Submit MCQ answers

### Chat
- `POST /api/chat/ask-doubt` - Ask a doubt about a concept

## License

MIT
