package jobagent;

import org.springframework.ai.chat.client.ChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the real Spring Boot context (real LangGraph4j graph, real SQLite checkpoints) with the
 * model faked and the job store in memory, then drives the /chat contract through the controller.
 */
@SpringBootTest(classes = {JobAgentApplication.class, ChatControllerTest.Fakes.class})
class ChatControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        @Primary
        ChatClient fakeChatClient() {
            return JdIntakePipelineTest.fakeLlm(JdIntakePipelineTest.PARTIAL, JdIntakePipelineTest.GOOD);
        }

        @Bean
        JobStore jobStore() {
            return new InMemoryJobStore();
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("chat-controller-test");
        registry.add("job-agent.checkpoint-db", () -> dir.resolve("cp.sqlite").toString());
        registry.add("spring.ai.anthropic.api-key", () -> "test-key-not-used");
    }

    @Autowired
    ChatController chat;

    @Test
    void chatAsksOneFieldAtATimeThenPublishes() {
        var first = chat.chat(Map.of("message", "Backend engineer at Acme")).getBody();
        assertEquals(false, first.get("done"));
        assertEquals("What is the location?", first.get("reply"));
        String threadId = (String) first.get("thread_id");

        var second = chat.chat(Map.of("message", "Bangalore", "thread_id", threadId)).getBody();
        assertEquals("What is the exp?", second.get("reply"));

        var third = chat.chat(Map.of("message", "5+ years", "thread_id", threadId)).getBody();
        assertEquals("What is the work_mode?", third.get("reply"));

        var last = chat.chat(Map.of("message", "hybrid", "thread_id", threadId)).getBody();
        assertEquals(true, last.get("done"));
        assertTrue(((String) last.get("reply")).startsWith("Status: published"));
        assertEquals(null, last.get("thread_id"));
    }
}
