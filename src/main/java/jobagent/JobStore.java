package jobagent;

import java.util.Map;
import java.util.Optional;

/** Persistence for published jobs. Mongo in production, in-memory for tests. */
public interface JobStore {

    /** Returns the id of an existing job with the same title+company (case/whitespace-insensitive). */
    Optional<String> findDuplicate(String title, String company);

    /** Inserts a published job and returns its new id. */
    String publish(Map<String, String> parsed, String description, int qualityScore);
}
