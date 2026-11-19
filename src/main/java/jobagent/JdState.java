package jobagent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Typed pipeline state (the Java counterpart of the Python {@code JDState} TypedDict).
 * Values live in a map so LangGraph4j can checkpoint them; the accessors give typed reads.
 */
public class JdState extends AgentState {

    /** Channels with a reducer. {@code log} appends; every other key is last-write-wins. */
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "log", Channels.<String>appender(ArrayList::new));

    public JdState(Map<String, Object> initData) {
        super(initData);
    }

    public String rawJd()               { return this.<String>value("raw_jd").orElse(""); }
    public String answer()              { return this.<String>value("answer").orElse(""); }
    public String rewrittenJd()         { return this.<String>value("rewritten_jd").orElse(""); }
    public String qualityFeedback()     { return this.<String>value("quality_feedback").orElse(""); }
    public int qualityScore()           { return this.<Integer>value("quality_score").orElse(0); }
    public int revisionCount()          { return this.<Integer>value("revision_count").orElse(0); }
    public int missingInfoAttempts()    { return this.<Integer>value("missing_info_attempts").orElse(0); }
    public Optional<String> duplicateOf() { return this.<String>value("duplicate_of").filter(s -> !s.isEmpty()); }
    public String status()              { return this.<String>value("status").orElse(""); }
    public String jobId()               { return this.<String>value("job_id").orElse(""); }

    public Map<String, String> parsed() {
        return new LinkedHashMap<>(this.<Map<String, String>>value("parsed").orElse(Map.of()));
    }

    public List<String> missingFields() {
        return new ArrayList<>(this.<List<String>>value("missing_fields").orElse(List.of()));
    }

    public List<String> log() {
        return this.<List<String>>value("log").orElse(List.of());
    }

    /** The draft currently being worked on: the rewrite if there is one, else the original. */
    public String currentDraft() {
        return rewrittenJd().isEmpty() ? rawJd() : rewrittenJd();
    }
}
