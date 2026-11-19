package jobagent;

import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chat backend for the React UI. Same contract as the Flask app and the other versions:
 * POST /chat {message, thread_id?} -> {reply, thread_id, done}.
 * Here {@code thread_id} is the LangGraph4j thread id that keys the checkpoints.
 */
@RestController
public class ChatController {

    private final CompiledGraph<JdState> graph;

    public ChatController(CompiledGraph<JdState> graph) {
        this.graph = graph;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        try {
            String message = String.valueOf(body.getOrDefault("message", ""));
            Object id = body.get("thread_id");

            String threadId;
            JdIntakePipeline.RunResult result;
            if (id instanceof String s && !s.isBlank()) {
                // An open thread means the pipeline is paused on a missing field: this message is the answer.
                threadId = s;
                result = JdIntakePipeline.resume(graph, threadId, message);
            } else {
                threadId = UUID.randomUUID().toString();
                result = JdIntakePipeline.start(graph, message, threadId);
            }

            Map<String, Object> out = new LinkedHashMap<>();
            if (result.paused()) {
                out.put("reply", result.question());
                out.put("thread_id", threadId);
                out.put("done", false);
            } else {
                out.put("reply", JdIntakePipeline.formatResult(result.finalState()));
                out.put("thread_id", null);
                out.put("done", true);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}
