# AskPranav

An AI agent that answers questions about Pranav's career - grounded in his real resume, project
write-ups, and GitHub READMEs - instead of a static resume page. Built on top of his existing
[Biography-service](https://github.com/Pranavmundhe3/Biography-service) portfolio backend.

Demonstrates, in Java/Spring rather than Python:

- **RAG** (ingestion → retrieval → generation) over real career content, stored in **pgvector**
- **LLM integration** through Spring AI's `ChatClient`, running on **Gemini** (chat and embeddings) via the native Google GenAI client
- **Prompt-engineered guardrails** - answers are grounded-only, refuse to fabricate, decline off-topic questions
- **Agentic tool/function calling** - `matchJobDescription`, `getProjectDetails`, `getPublications`, `getContactInfo`, and more
- **An MCP server** - the same tools are queryable directly from any MCP-compatible AI client
- **Multi-agent orchestration** - a hand-rolled Planner → Retriever/ToolCaller → Writer pipeline (see caveat below)

## Before you run this

One thing carried over from the source repo that needs your attention, not silent trust:

1. **Rotate the exposed RDS password.** The original `Biography-service` repo has a live AWS RDS
   MySQL password committed in plaintext in its history. It was **not** copied into this repo (see
   `askpranav-service/src/main/resources/application.yml`, which uses env vars only) - but if that
   password is still live, rotate it.

`DataSeeder` now seeds real data (`Experience`, `Skills`, `Certifications`, `Summary`, `Project`,
`Publication`) sourced from the resume in `knowledge/`, and `KnowledgeFolderLoader` ingests every
file dropped into `askpranav-service/src/main/resources/knowledge/` (`.md`/`.txt` verbatim,
`.docx`/`.pdf`/`.pptx`/etc. via Apache Tika) - no more hardcoded filename list. Drop an updated
resume or a new write-up in there and restart (`ASKPRANAV_INGESTION_MODE=always` while iterating)
to re-ingest it. Resume files (`.docx`/`.pdf`/`.pptx`) in that folder are git-ignored on purpose, since they
carry contact details - add your own locally and the app reads it at runtime. One still-open item: the seeded `Project` row's `techStack` carries two `[TBD]`
placeholders (web scraping framework, vector DB) straight from the resume text - fill those in via
`POST /project/save-project` or by editing `DataSeeder.java` once decided.

## Repo layout

- `askpranav-service/` - the Spring Boot 3 / Java 17 backend (all the AI work lives here)
- `resume-client/` - the Angular 10 portfolio frontend, now with an **Ask Pranav** chat page (`/ask`)

## Architecture

```
recruiter / hiring manager
        │
        ├── POST /ask/question ─────► OrchestrationService
        │                              ├─ PlannerAgent   (classify: RAG_SEARCH | TOOL_CALL | BOTH)
        │                              ├─ RetrieverAgent (pgvector similarity search)
        │                              ├─ ToolCallAgent  (BiographyTools via ChatClient tool-calling)
        │                              └─ WriterAgent    (grounded synthesis, guardrailed system prompt)
        │
        └── any MCP client ─────────────────► same BiographyTools, exposed as MCP tools directly

KnowledgeBaseIngestionRunner (startup) ── reads JPA entities + knowledge/* (any file type, via Tika) + GitHub READMEs
                                        ── chunks + embeds ──► pgvector (VectorStore)
```

**Honesty note on "multi-agent orchestration":** Java has no LangGraph. `OrchestrationService` is a
hand-rolled linear pipeline, not a graph engine - it demonstrates the same
plan-then-execute-then-synthesize concept. Spring AI's `ChatClient` already auto-invokes registered
tools within a single call; the orchestrator's actual value is the explicit Planner routing step
(skip vector search for pure lookup questions, skip tool-binding for pure narrative ones), not
reimplementing tool-calling itself.

## Prerequisites

- Java 17 (`java -version`)
- Docker (for local Postgres+pgvector). On Windows, Docker Desktop needs WSL2, which needs CPU
  virtualization enabled and a full restart (not shut down) after `wsl --install`. Or point
  `DB_HOST`/`DB_PORT` at an existing Postgres+pgvector instance instead. If a native Postgres already
  owns port 5432, set `DB_PORT=5433` - the compose file honors it.
- A Gemini API key (Google AI Studio). Free-tier keys have small per-minute and daily quotas, and one
  question makes several model calls, so expect 429s once the quota is spent (the API then answers 503).

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | yes | Postgres credentials |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | no (sensible defaults) | Postgres connection details |
| `GEMINI_API_KEY` | yes | Drives chat and embeddings through Spring AI's native Google GenAI client |
| `ASKPRANAV_GEMINI_MODEL` | no (`gemini-3.6-flash`) | Chat model; change it if Google retires the current one |
| `ASKPRANAV_GEMINI_EMBEDDING_MODEL` | no (`gemini-embedding-001`) | Embedding model |
| `EMBEDDING_DIMENSIONS` | no (`768` default) | Must match the embedding model (768 for `gemini-embedding-001` as configured) |
| `ASKPRANAV_RATE_LIMIT_MAX`, `ASKPRANAV_RATE_LIMIT_WINDOW_SECONDS` | no (`2`, `300`) | Per-IP limit on `POST /ask/question`: 2 questions per 5 minutes |
| `ASKPRANAV_FORWARD_HEADERS` | no (`none`) | Set to `native` behind a reverse proxy so the limiter sees each visitor's real IP |
| `ASKPRANAV_GITHUB_REPOS` | no | comma-separated `owner/repo` slugs whose READMEs to ingest |
| `ASKPRANAV_INGESTION_MODE` | no (`if-empty` default) | `if-empty` \| `always` |

Never commit a `.env` file with real values - `.gitignore` already excludes it.

## Running locally

```bash
cp .env.example .env        # then fill in DB_PASSWORD and GEMINI_API_KEY
docker compose up -d postgres
# load .env into your shell (bash): set -a; source .env; set +a
cd askpranav-service
./mvnw spring-boot:run
```

Smoke test:

```bash
curl -X POST localhost:5000/ask/question \
  -H "Content-Type: application/json" \
  -d '{"question":"What certifications does Pranav have?"}'

curl -X POST localhost:5000/ask/question \
  -H "Content-Type: application/json" \
  -d '{"question":"Here is a JD: Looking for a Java engineer with Spring Boot and AWS experience. How does his background map to this?"}'
```

The existing CRUD endpoints (`/personal/personal-details`, `/experience/experience-details`, etc.)
are unchanged in shape from the source repo and still work the same way.

### Chat UI

Open <http://localhost:5000/> once the app is running. It is a single static page
(`askpranav-service/src/main/resources/static/index.html`) served by the Spring Boot app itself, so it
needs no build step and no CORS setup. It calls `POST /ask/question`, shows the answer, and lists the
sources the answer was grounded in. Each question is answered independently (no conversation memory).
To host the page on a different site, set `API_BASE` at the top of the file to the backend URL and set
`ASKPRANAV_CORS_ALLOWED_ORIGIN` on the backend to that site's origin.

### Angular client (`resume-client`)

The portfolio site has an **Ask Pranav** entry in its top navigation, routed to `/ask`
(`src/app/ask/`, `src/app/ask.service.ts`). It is the same chat as the static page above: suggestion
chips, typing indicator, sources under each answer, and the backend's rate-limit / outage messages
shown in the conversation. Answer text is rendered through Angular interpolation only (no
`innerHTML`), so model output cannot inject markup. Also fixed: the empty route used to redirect to a
non-existent `register` route; it now goes to `home`.

```bash
# terminal 1: the backend (see "Running locally")
# terminal 2:
cd resume-client
npm install
npx ng serve            # http://localhost:4200, talks to http://localhost:5000
```

The backend URL comes from `src/environments/environment.ts` (`askpranavApiUrl`). Before a production
build, set it in `environment.prod.ts` to the deployed backend's public URL (it ships empty on purpose,
so a forgotten value fails loudly instead of pointing at the wrong host), and set
`ASKPRANAV_CORS_ALLOWED_ORIGIN` on the backend to the site's origin. The backend allows
`http://localhost:4200` by default. Node.js is required for this part and was not available where this
was written, so **the Angular code has not been compiled or run yet**; run `npx ng build` and
`npx ng test` first (the new `ask.component.spec.ts` covers parsing, the request, and the 429 path).

### Rate limit

`POST /ask/question` allows **2 questions per 5 minutes per client IP** (sliding window), because each
question makes several Gemini calls and the URL is public. The 3rd question inside the window gets
`429 Too Many Requests` with a `Retry-After` header, and the chat page shows the message. Tune it with
`ASKPRANAV_RATE_LIMIT_MAX` / `ASKPRANAV_RATE_LIMIT_WINDOW_SECONDS`. Limits: counters live in memory
(reset on restart, per instance), and the MCP endpoint (`/sse`) is not rate-limited or authenticated.
Behind a reverse proxy, set `ASKPRANAV_FORWARD_HEADERS=native`, otherwise every visitor shares the
proxy's IP and one person can lock everyone out.

### Connecting an MCP client

With the app running, the MCP server speaks **SSE** (Spring AI's default transport) at
`http://localhost:5000/sse`, with messages posted to `/mcp/message`. There is no `/mcp` Streamable
HTTP endpoint. It exposes 9 tools: `getCareerSummary`, `getExperience`, `getSkills`, `getEducation`,
`getCertifications`, `getPublications`, `getProjectDetails`, `getContactInfo`, `matchJobDescription`.

In any MCP-compatible client (an AI desktop app, an IDE assistant, a CLI agent), add a remote server of
type **SSE** with the URL above. Clients that only launch local (stdio) servers can go through the
generic `mcp-remote` bridge instead; the config entry looks like this:

```json
{ "mcpServers": { "askpranav": { "command": "npx", "args": ["-y", "mcp-remote", "http://localhost:5000/sse"] } } }
```

Then ask your assistant things like "using the AskPranav tools, what are Pranav's certifications?" or
paste a job description and ask it to run `matchJobDescription`. The handshake, tool listing,
`getCertifications` and `matchJobDescription` were verified against a running instance over raw
JSON-RPC; the `mcp-remote` entry has not been tried yet. The endpoint is unauthenticated, so
only expose it beyond localhost behind something that adds auth.

## Build & test (no API cost)

```bash
cd askpranav-service
./mvnw -q -DskipTests compile
./mvnw test
```

Unit tests cover document-mapping and tool DTO logic with mocks - no real database, vector store, or
LLM call required. Anything that needs a live model (`/ask/question` end to end) is a manual smoke
test since it spends your API budget.

## Version notes

- **Spring AI 1.1.8 on Spring Boot 3.5.15**, chosen deliberately. Gemini 3 models require a
  `thought_signature` to be round-tripped on tool calls; the generic compatibility-endpoint client in
  Spring AI 1.0.x dropped it, so tool-calling questions failed with HTTP 400. The native
  `spring-ai-starter-model-google-genai` client in 1.1.x handles it. Spring AI 2.0.x needs Spring Boot 4,
  which is a much larger migration.
- Gemini is the only model provider (chat and embeddings). Adding another later means adding its
  starter and a `ChatModel` bean.
- Google retires Gemini models quickly (2.x models were already refused for new keys in 2026). If chat
  starts returning 404 "no longer available", change `ASKPRANAV_GEMINI_MODEL` - no code change needed.
