# Job Agent: Spring AI + LangGraph4j on Spring Boot (Java)

Java port of the Python [Langgraph-Job-Agent](https://github.com/alumnx-ai-labs/Langgraph-Job-Agent), with **Spring AI** making the Claude calls. A recruiter pastes a messy job description; the pipeline extracts the fields, asks for anything missing, rejects duplicates, scores and rewrites the text if needed, and publishes it to MongoDB.

- **LangGraph4j** is the workflow: typed state, conditional edges, bounded retry loops, human-in-the-loop pause, SQLite checkpoints. It is the same graph code as the LangChain4j version.
- **Spring AI** makes the Claude calls through a `ChatClient` (extract, score, rewrite) and is auto-configured from `spring.ai.anthropic.*` in `application.yml`.
- **Spring Boot** hosts it: a `@RestController` for `/chat` and the built **React** chat UI served as static files.

Sibling versions of the same workflow: [Langchain4j-Job-Agent](https://github.com/DhawanRachakonda/Langchain4j-Job-Agent) (LangChain4j for the LLM calls) and [Embabel-Job-Agent](https://github.com/DhawanRachakonda/Embabel-Job-Agent) (no graph; the flow comes from method signatures). The only code that differs from the LangChain4j version is the LLM layer: `JdIntakePipeline` takes a Spring AI `ChatClient`, and `JobAgentConfig` gets it from the auto-configured builder.

Note: LangGraph4j also ships a `langgraph4j-spring-ai` module and a Spring AI agent executor (a ReAct-style agent). This pipeline is a fixed workflow rather than a tool-calling loop, so it uses the plain `StateGraph` and only Spring AI's chat client.

## Requirements

Java 21+, Maven, Node 18+ (for the UI), an Anthropic API key, and a MongoDB Atlas cluster (add your IP under Network Access, otherwise you get SSL handshake errors).

## Setup and run

```bash
cp .env.example .env      # fill in ANTHROPIC_API_KEY and MONGO_URI (or export them as environment variables)
cd frontend && npm install && npm run build && cd ..
mvn spring-boot:run
```

Open http://127.0.0.1:8080. For frontend hot reload: `cd frontend && npm run dev` (the backend must be running).

The app needs `MONGO_URI`: the `MongoJobStore` bean is only created when it is set, so without it startup fails with "required a bean of type JobStore".

## Tests

```bash
mvn test
```

Offline: a scripted fake Spring AI `ChatModel` wrapped in a `ChatClient`, an in-memory job store, real LangGraph4j and real SQLite checkpoints. They cover publish, duplicate rejection, one-at-a-time questions, the rewrite cap, escalation, a paused run surviving a restart, an API failure not being mistaken for an empty extraction, and the `/chat` contract through the Spring Boot context.

## Notes

- Jobs are stored in MongoDB database `jd_intake_db`, collection `jobs_springai` (override with `MONGO_DB` and `MONGO_COLLECTION`). Each sibling app uses its own collection, and the Python original uses `jobs`, so several apps can share one cluster without the duplicate check (same title and company) seeing each other's jobs.
- `ChatClient.entity()` reads the model's reply into the `ExtractedFields` and `QualityScore` records, so there is no parsing code here and the extraction prompt does not list the fields. An API error (bad key, network) and a reply that cannot be read both throw, so the run fails loudly instead of looking like an empty extraction.
- Users are told apart only by `thread_id`, which the React app keeps in memory and sends back with each answer. There is no login.
- Checkpoints are kept after a run ends (`releaseThread(false)`), like the Python `SqliteSaver`.
