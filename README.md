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

`DataSeeder` seeds the first-run data (`Experience`, `Skills`, `Certifications`, `Summary`, `Projects`,
`Publication`), taken from the resume. After that the app **watches the knowledge folder while it runs**
(see "Watching the knowledge folder"): drop in a new or updated resume or write-up and it is picked up
within seconds, no restart. Resume files (`.docx`/`.pdf`/`.pptx`) in that folder are git-ignored on purpose,
since they carry contact details; add your own locally. One still-open item: the seeded job-search `Project`
row's `techStack` carries two `[TBD]` placeholders (web scraping framework, vector DB) straight from the
resume text - fill those in via `POST /project/save-project` once decided.

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

KnowledgeBaseIngestionRunner (startup) ── reads the DB rows + GitHub READMEs ── chunks + embeds ──► pgvector
KnowledgeFolderScanner (every 10s)     ── new/changed/deleted files in knowledge/ ──► pgvector (per-file chunks)
                                       └─ resume-type PDF/Word ── Gemini extracts ── validated ──► DB rows the UI shows
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
| `ASKPRANAV_ADMIN_USER`, `ASKPRANAV_ADMIN_PASSWORD` | for writes | Login for the `POST .../save-*` endpoints. No default password: unset means all writes are rejected |
| `ASKPRANAV_MCP_USER`, `ASKPRANAV_MCP_PASSWORD` | for MCP | Login for `GET /sse` and `POST /mcp/message`. No default password: unset means those endpoints reject everyone (the admin account can still reach them) |
| `ASKPRANAV_CONTACT_TO` | for the contact form | Address that receives contact-form messages (no default, the repo is public) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | for the contact form | SMTP login; for Gmail use an App Password |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM` | no | SMTP server (default `smtp.gmail.com:587`) and sender address (defaults to `MAIL_USERNAME`) |
| `ASKPRANAV_GITHUB_REPOS` | no | Comma-separated repo (`owner/repo` or URL) or account (`https://github.com/owner`) entries; an account pulls all its public non-fork repos' READMEs |
| `ASKPRANAV_GITHUB_REPOS` | no | comma-separated `owner/repo` slugs whose READMEs to ingest |
| `ASKPRANAV_INGESTION_MODE` | no (`if-empty` default) | `if-empty` \| `always` |
| `ASKPRANAV_KNOWLEDGE_DIR` | no (`src/main/resources/knowledge`) | Folder that is watched while the app runs |
| `ASKPRANAV_KNOWLEDGE_SCAN_MS` | no (`10000`) | How often the folder is checked |

Never commit a `.env` file with real values - `.gitignore` already excludes it. Put values that contain
spaces in double quotes (Gmail app passwords are shown as four groups of letters), for example
`MAIL_PASSWORD="abcd efgh ijkl mnop"`; otherwise `source .env` fails on that line.

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

The portfolio site (Angular 10) shows the resume data from the backend and has an **Ask Pranav** chat
page. Pages and the endpoints they call (base URL = `askpranavApiUrl` in `src/environments/environment.ts`,
`http://localhost:5000` for local runs):

| Page | Route | Backend endpoint |
|---|---|---|
| Home | `/home` | `GET /summary/summary-details` |
| Ask Pranav (chat) | `/ask` | `POST /ask/question` |
| Skills | `/skills` | `GET /skill/skill-details` |
| Work Experience (+ certifications) | `/experience` | `GET /experience/experience-details`, `GET /certification/certification-details` |
| Projects | `/projects` | `GET /project/project-details` |
| Publications | `/publications` | `GET /publication/publication-details` |
| About Me (+ education) | `/about-me` | `GET /personal/personal-details`, `GET /education/education-details` |

The UI shows the data, has the chat, and has a contact form (`POST /contact/send`, see below). It does
not edit data: the backend's `POST /*/save-*` endpoints are for you (they need the admin login, see
"Securing the write endpoints"), and the `GET /skill/skill-details/by-type` and
`GET /experience/experience-details/by-company` filters are not used by any page.

Experience descriptions are stored one bullet per line and rendered as a bullet list; the seed data
follows the resume's bullets.

**Running it locally (three terminals):**

```bash
# terminal 1 - backend on :5000 (Postgres container must be up: docker compose up -d postgres)
set -a; source .env; set +a
cd askpranav-service && ./mvnw spring-boot:run

# terminal 2 - build the Angular client and keep rebuilding on every save
cd resume-client
npm install --legacy-peer-deps      # once; the flag is needed because of an old ng-bootstrap peer range
npm run build:watch

# terminal 3 - serve the built client on :4200
cd resume-client && npm run serve:dist
```

Open <http://localhost:4200>. **Why not `ng serve`?** Angular CLI 10's dev server crashes on Node 20+
(`No such module: http_parser`), and building with webpack 4 on Node 17+ needs
`--openssl-legacy-provider`; the `build:watch` script sets that flag and `serve:dist` is a tiny static
server with router fallback (`serve-dist.js`). On Node 14/16 plain `npm start` (`ng serve`) should work
as well, but that was not tried. Before a production build, set `askpranavApiUrl` in
`environment.prod.ts` to the deployed backend URL and set `ASKPRANAV_CORS_ALLOWED_ORIGIN` on the
backend to the site's origin (the backend allows `http://localhost:4200` by default).

Answer text in the chat is rendered through Angular interpolation only (no `innerHTML`), so model
output cannot inject markup. Verified locally in headless Chrome: all pages render backend data, the
chat request reaches `POST /ask/question` with correct CORS, and rate-limit and outage messages appear in
the conversation. A successful model answer was not seen because the Gemini quota was exhausted.

### Securing the write endpoints

Spring Security protects everything that changes data or costs money to call. Reads (`GET`), the chat and
the contact form are public; **every other write needs the admin login** (HTTP Basic), including any
endpoint added later (default-deny); **the MCP endpoints (`GET /sse`, `POST /mcp/message`) need either the
MCP or the admin login** - see "Connecting an MCP client" above. CSRF protection is off because there is
no cookie or session, only an `Authorization` header per request. With no `ASKPRANAV_ADMIN_PASSWORD` /
`ASKPRANAV_MCP_PASSWORD` set, that account does not exist at all.

Locally:

```bash
# 1. put strong passwords in .env (the file is git-ignored)
openssl rand -base64 24            # generate one, then add:  ASKPRANAV_ADMIN_PASSWORD=<that value>
#                                    ASKPRANAV_ADMIN_USER=admin   (optional, "admin" is the default)
openssl rand -base64 24            # generate a second one:    ASKPRANAV_MCP_PASSWORD=<that value>
#                                    ASKPRANAV_MCP_USER=mcp     (optional, "mcp" is the default)

# 2. restart the backend so it picks it up
set -a; source .env; set +a; cd askpranav-service && ./mvnw spring-boot:run

# 3. writes now need the login (401 without it)
curl -u "$ASKPRANAV_ADMIN_USER:$ASKPRANAV_ADMIN_PASSWORD" -X POST localhost:5000/project/save-project \
  -H "Content-Type: application/json" \
  -d '{"name":"New project","duration":"2026","shortDescription":"...","techStack":"..."}'
```

Saved rows appear on the site immediately, but the chat only learns them at the next ingestion
(`ASKPRANAV_INGESTION_MODE=always` for one start, which now clears the old chunks first).
To edit a row, POST it with its `id`. Over plain HTTP the password travels unencrypted, so put the
backend behind HTTPS before using this beyond localhost. The tests in `SecurityConfigTest` cover 401 for
no/wrong password, 201 for the admin, default-deny for other methods, and the browser preflight.

### Contact form

`POST /contact/send` (name, email, message) is public but limited to 3 messages per 10 minutes per IP, has
a hidden honeypot field bots fill in, and strips line breaks from the name so it cannot inject mail
headers. `ContactController` calls `ContactService`, which sends the mail with the visitor's address as
`Reply-To`, so replying answers them directly.

To receive mail, set `ASKPRANAV_CONTACT_TO` and an SMTP login in `.env`. For Gmail: enable 2-Step
Verification, create an App Password (Google Account > Security > App passwords), and set
`MAIL_USERNAME=<your gmail address>` and `MAIL_PASSWORD=<the app password>`. Until then the form answers
"The message could not be sent right now." To try it without sending real mail, run any local SMTP sink
and start the backend with `MAIL_HOST=127.0.0.1 MAIL_PORT=2525 MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS=false`.

### Watching the knowledge folder

Every 10 seconds the app compares the folder (`ASKPRANAV_KNOWLEDGE_DIR`) with what it has already processed,
by SHA-256, remembered in the `knowledge_file` table so a restart redoes nothing unless a file changed.

| You do | Chat (vector index) | Database and site |
|---|---|---|
| Add or edit a `.md`/`.txt`/`.pdf`/`.docx`/`.pptx` file | old chunks replaced by new ones | unchanged |
| Add or edit a **resume** (PDF/Word whose name contains `resume`, `cv` or `lebenslauf`) | same, plus the row-derived chunks are rebuilt | Gemini reads it into structured data, which replaces summary, experience, education, skills, certifications, publications and languages |
| Delete a file | its chunks are removed | rows imported from it are **kept** |

The pages read the database, so an import shows the next time a page is opened or reloaded (no live push).

How the resume import protects your data: the model's output is validated first (it must have a summary and
work experience with bullets) and written in one transaction, so a bad, partial or failed extraction leaves the
existing rows untouched. Projects are added or updated by name but never deleted (this application itself is
not on the resume); hobbies, email and links are never modified; and values a resume does not state (a client
name, a certification date, a grade) are kept from the existing row. A failure such as the model's quota is
recorded and retried after 10 minutes, not on every scan. A file still being copied (modified in the last
2 seconds) waits for the next scan. The first scan after this feature is introduced only records a resume that
is already there, since the database was seeded from it; anything added afterwards is processed.

Limits: the import step needs Gemini to be reachable. Folder watching and re-embedding were tested live
(add, edit, delete); the model-based resume import is covered by unit tests with a stand-in extractor and a
live attempt that was refused by the exhausted Gemini quota (which left the database untouched, as designed),
so it has not yet been seen working end to end. The import replaces those sections wholesale, so edit the
resume file rather than the rows if the file is meant to be the source of truth.

### Rate limit

`POST /ask/question` allows **2 questions per 5 minutes per client IP** (sliding window), because each
question makes several Gemini calls and the URL is public. The 3rd question inside the window gets
`429 Too Many Requests` with a `Retry-After` header, and the chat page shows the message. Tune it with
`ASKPRANAV_RATE_LIMIT_MAX` / `ASKPRANAV_RATE_LIMIT_WINDOW_SECONDS`. Limits: counters live in memory
(reset on restart, per instance). The MCP endpoint (`/sse`) is read-only and requires login (see "Connecting
an MCP client"), but is not itself rate-limited - a logged-in MCP client can call tools as fast as it likes.
Behind a reverse proxy, set `ASKPRANAV_FORWARD_HEADERS=native`, otherwise every visitor shares the
proxy's IP and one person can lock everyone out.

### Connecting an MCP client

With the app running, the MCP server speaks **SSE** (Spring AI's default transport) at
`http://localhost:5000/sse`, with messages posted to `/mcp/message`. There is no `/mcp` Streamable
HTTP endpoint. It exposes 9 tools: `getCareerSummary`, `getExperience`, `getSkills`, `getEducation`,
`getCertifications`, `getPublications`, `getProjectDetails`, `getContactInfo`, `matchJobDescription`.

**The MCP endpoints require login** (`ASKPRANAV_MCP_USER` / `ASKPRANAV_MCP_PASSWORD`, HTTP Basic - the
admin account also works). With no `ASKPRANAV_MCP_PASSWORD` set, no MCP account exists and every request
to `/sse` or `/mcp/message` is rejected (401), even a legitimate one. This is the same no-default-password
design as the admin login - see "Securing the write endpoints".

Claude Code:

```bash
claude mcp add --transport sse askpranav http://localhost:5000/sse \
  --header "Authorization: Basic $(printf '%s:%s' "$ASKPRANAV_MCP_USER" "$ASKPRANAV_MCP_PASSWORD" | base64 -w0)"
```

Clients that only launch local (stdio) servers can go through the generic `mcp-remote` bridge instead,
which reads the same two env vars into an `Authorization` header:

```json
{ "mcpServers": { "askpranav": {
  "command": "npx",
  "args": ["-y", "mcp-remote", "http://localhost:5000/sse", "--header", "Authorization:${AUTH_HEADER}"],
  "env": { "AUTH_HEADER": "Basic <base64 of user:password, computed the same way as above>" }
} } }
```

Then ask your assistant things like "using the AskPranav tools, what are Pranav's certifications?" or
paste a job description and ask it to run `matchJobDescription`. The handshake, tool listing,
`getCertifications`, `matchJobDescription`, and the login itself (no/wrong/right credentials, on both the
MCP and the admin account) were verified against a running instance; the `mcp-remote` entry has not been
tried yet.

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
