OpenAI API Key setup and testing

1) How to create an OpenAI API key
- Visit https://platform.openai.com/
- Sign in or create an account
- Open the API Keys page: https://platform.openai.com/account/api-keys
- Click "Create new secret key" and copy the key immediately (it is shown only once)
- Store the key securely (do NOT paste into chat or commit into repo)

2) Environment variables (local development)
- Export the key and enable OpenAI provider:
  export OPENAI_API_KEY="sk-..."
  export APP_AI_PROVIDER=openai
  # optional controls:
  export APP_AI_MODEL=gpt-4o-mini
  export APP_AI_TEMPERATURE=0.2
  export APP_AI_MAX_TOKENS=800

- Start the backend (from repo root):
  cd backend
  ./gradlew bootRun

3) Quick test (non-persistent)
- Use the AI test endpoint (requires authentication header if app is protected):

  curl -X POST http://localhost:8080/api/ai/test \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <your-jwt>" \
    -d '{"teamId":"T-001","specId":123,"specText":"Build a consistent design language for our product."}'

- Response: array of suggestion previews (no DB writes)

4) Running end-to-end (submit spec)
- POST /api/teams/{teamId}/specs will enqueue AI processing. The app must be started with OPENAI_API_KEY and APP_AI_PROVIDER=openai to use real OpenAI.

5) Security notes
- Never commit or paste OPENAI_API_KEY into source control or chat.
- For CI/CD use secret management (GitHub Actions secrets, Vault, environment config in deployment).

If you set your OPENAI_API_KEY as environment variable on this machine and confirm, I can run a quick AI test request (will not store the API key).