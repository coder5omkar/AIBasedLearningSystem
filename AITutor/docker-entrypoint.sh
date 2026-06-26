#!/bin/sh
set -e

OLLAMA_HOST="${OLLAMA_HOST:-http://ollama:11434}"
MODEL="${OLLAMA_MODEL:-llama3.2:3b}"

echo "Waiting for Ollama at $OLLAMA_HOST ..."
until curl -s "$OLLAMA_HOST/api/tags" > /dev/null 2>&1; do
  sleep 2
done
echo "Ollama is ready."

echo "Pulling model $MODEL ..."
curl -s -X POST "$OLLAMA_HOST/api/pull" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"$MODEL\"}" > /dev/null 2>&1
echo "Model $MODEL is ready."

exec java -jar app.jar
