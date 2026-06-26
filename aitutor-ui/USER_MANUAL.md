# AI Tutor - User Manual

## Getting Started

### 1. Registration & Login

1. Open the app in your browser (`http://localhost:3000`)
2. Click **"Create an account"** if you're new
3. Enter a username and password
4. After registration, log in with your credentials

### 2. Configure LLM Provider

The header always shows the model selector at the top-right:

- **Provider**: Choose `ollama` (local, free) or `openai` (requires API key)
- **Model**: Select the model version
- **API Key**: Required only for OpenAI

> **For Ollama**: Ensure Ollama is running locally and the selected model is pulled (`ollama pull llama3.2:3b`).

### 3. Select a Subject

After login, you'll see the subject selection page with 15 subjects across Academic and Technology categories:

1. Browse by category using the pill buttons at the top
2. Click any subject card to begin

> **Note**: If you have an active (incomplete) syllabus, the app will take you directly to your learning journey instead.

### 4. Set Up Your Syllabus

Choose how to create your learning path:

**Option A: Generate with AI**
- Click "Generate with AI"
- Wait 30-60 seconds for the LLM to create the syllabus
- The syllabus will have 3-5 chapters, each with sections and concepts

**Option B: Upload Your Own**
- Click "Upload Syllabus"
- Enter a title and paste your content
- Use the format shown in the placeholder

### 5. The Learning Interface

The learning page has three panels:

#### Left Panel: Syllabus Tree
- Shows all chapters, sections, and concepts
- **📖 Available**: Ready to learn (green indicator)
- **🔄 In Progress**: Started but not completed
- **✅ Completed**: Passed all MCQs
- **🔒 Locked**: Previous concept not yet completed
- Click any unlocked concept to study it

#### Center Panel: Concept Content + Doubt Chat
- **Top**: AI-generated explanation of the current concept
- Read the content carefully before attempting the MCQ
- **Bottom**: Chat interface to ask questions about the concept
  - Type any doubt and press Enter or click Send
  - The AI tutor will respond with explanations and examples

#### Right Panel: MCQ Test
- 5 multiple-choice questions about the current concept
- Click an option to select your answer
- All 5 must be answered correctly to pass
- Click "Submit" to check your answers
- If you fail, click "Retry Test" to try again with new questions
- After passing, the next concept unlocks automatically

### 6. Progress & Completion

- Progress is saved automatically after each MCQ submission
- You can close the app and resume from where you left off
- The syllabus tree shows your completion progress per chapter
- Once all concepts in a subject are completed, you can start a new subject

### 7. Updating Your Syllabus

While learning, you can update the syllabus content:
- Use the `PUT /api/syllabus/{id}` endpoint with updated content
- This re-parses the structure while keeping your progress
- You cannot change the subject until all concepts are completed

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Invalid credentials" on login | Ensure backend is running and H2 DB is populated |
| "You already have an active syllabus" | Complete your current subject first |
| MCQs not loading | Check that the LLM provider is running and accessible |
| Concept content shows "generation failed" | Try a different model or check provider connectivity |
| App shows blank screen | Check browser console for errors; ensure backend is running |
| Login page loop | Clear localStorage (token) and re-login |

## Tips

- **Study order matters**: Concepts unlock sequentially. Master each concept to progress.
- **Use the doubt chat**: If a concept is unclear, ask questions in the chat before taking the MCQ.
- **Pass rate**: All 5 MCQs must be correct. If you fail, retry generates fresh questions.
- **One subject focus**: You can only study one subject at a time. Complete it fully before switching.
- **Model matters**: For better explanations, use a larger model if available (e.g., `llama3.1:8b` or `gpt-4o`).

## Keyboard Shortcuts

- `Enter` in doubt chat: Send message
- `Shift+Enter` in doubt chat: New line
