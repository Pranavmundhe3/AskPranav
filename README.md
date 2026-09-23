# AskPranav

An AI agent that answers questions about Pranav's career - grounded in his real resume, project
write-ups, and GitHub READMEs - instead of a static resume page. Built on top of his existing
[Biography-service](https://github.com/Pranavmundhe3/Biography-service) portfolio backend.

Demonstrates, in Java/Spring rather than Python:

- **RAG** (ingestion → retrieval → generation) over real career content, stored in **pgvector**
- **Model-agnostic LLM integration** via Spring AI's `ChatClient` - swap OpenAI/Anthropic/Gemini by config, not code
- **Prompt-engineered guardrails** - answers are grounded-only, refuse to fabricate, decline off-topic questions
- **Agentic tool/function calling** - `matchJobDescription`, `getProjectDetails`, `getContactInfo`, and more
- **An MCP server** - the same tools are queryable directly from Claude Desktop / Claude Code
- **Multi-agent orchestration** - a hand-rolled Planner → Retriever/ToolCaller → Writer pipeline (see caveat below)

## Before you run this

Two things carried over from the source repo that need your attention, not silent trust:

1. **Rotate the exposed RDS password.** The original `Biography-service` repo has a live AWS RDS
   MySQL password committed in plaintext in its history. It was **not** copied into this repo (see
   `askpranav-service/src/main/resources/application.yml`, which uses env vars only) - but if that
   password is still live, rotate it.
2. **Replace the placeholder Project data.** There was no "Projects" concept in the source repo, so
   `DataSeeder` seeds one clearly-marked placeholder row. `getProjectDetails` and
   `matchJobDescription` will report placeholder text until you replace it via
   `POST /project/save-project` or edit `DataSeeder.java` directly. Likewise,
   `askpranav-service/src/main/resources/knowledge/resume.md` is a TODO stub - drop your real resume
   text in there for richer narrative retrieval.

## Repo layout

- `askpranav-service/` - the Spring Boot 3 / Java 17 backend (all the AI work lives here)
- `resume-client/` - the existing Angular frontend, carried over unmodified this round

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
        └── MCP client (Claude Desktop/Code) ───► same BiographyTools, exposed as MCP tools directly

KnowledgeBaseIngestionRunner (startup) ── reads JPA entities + knowledge/*.md + GitHub READMEs
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
- Docker (for local Postgres+pgvector) - **not currently installed on this machine**, install Docker
  Desktop or point `DB_HOST`/`DB_PORT` at an existing Postgres+pgvector instance instead
- An API key for at least one of: OpenAI, Anthropic, Google Gemini (Vertex AI)

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | yes | Postgres credentials |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | no (sensible defaults) | Postgres connection details |
| `ASKPRANAV_AI_PROVIDER` | no (`anthropic` default) | `openai` \| `anthropic` \| `gemini` |
| `ANTHROPIC_API_KEY` | if using Anthropic | |
| `OPENAI_API_KEY` | if using OpenAI | |
| `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION` | if using Gemini | Vertex AI project/region |
| `ASKPRANAV_ANTHROPIC_MODEL` | no (`claude-sonnet-5` default) | swap Claude model without a code change |
| `ASKPRANAV_GITHUB_REPOS` | no | comma-separated `owner/repo` slugs whose READMEs to ingest |
| `ASKPRANAV_INGESTION_MODE` | no (`if-empty` default) | `if-empty` \| `always` |

Never commit a `.env` file with real values - `.gitignore` already excludes it.

## Running locally

```bash
docker-compose up -d postgres
export DB_USERNAME=askpranav DB_PASSWORD=changeme
export ANTHROPIC_API_KEY=sk-ant-...
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

### Connecting an MCP client

With the app running, point Claude Desktop or Claude Code's MCP configuration at the running MCP
server (transport/URL depend on the Spring AI MCP server version pinned in `pom.xml` - check
`spring.ai.mcp.server` properties in `application.yml`). Once connected, ask your own assistant
things like "using the AskPranav tools, what are Pranav's certifications?" - it calls
`getCertifications()` directly, no HTTP client code needed on your end.

## Build & test (no API cost)

```bash
cd askpranav-service
./mvnw -q -DskipTests compile
./mvnw test
```

Unit tests cover document-mapping and tool DTO logic with mocks - no real database, vector store, or
LLM call required. Anything that needs a live model (`/ask/question` end to end) is a manual smoke
test since it spends your API budget.

## Versions flagged for verification

Several dependency versions in `askpranav-service/pom.xml` are placeholders pending confirmation
against current Maven Central / spring.io releases at build time: `spring-boot-starter-parent`,
`spring-ai-bom`, and which Gemini starter artifact id is current
(`spring-ai-starter-model-vertex-ai-gemini` vs `spring-ai-starter-model-google-genai`). Several
Spring AI API call shapes (`SearchRequest.builder()`, `ChatClient...entity(Class)`,
`MethodToolCallbackProvider`) are also marked `VERIFY` in code comments where they appear - they
reflect the documented 1.0 GA shape but should be checked against whatever version actually resolves.
