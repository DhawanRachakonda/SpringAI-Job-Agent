package jobagent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Non-persistent store, used by the offline smoke test. */
public class InMemoryJobStore implements JobStore {

    private record Job(String id, String title, String company) {}

    private final List<Job> jobs = new ArrayList<>();

    @Override
    public Optional<String> findDuplicate(String title, String company) {
        return jobs.stream()
                .filter(j -> j.title().equals(title.strip().toLowerCase())
                        && j.company().equals(company.strip().toLowerCase()))
                .map(Job::id)
                .findFirst();
    }

    @Override
    public String publish(Map<String, String> parsed, String description, int qualityScore) {
        String id = "mem-" + (jobs.size() + 1);
        jobs.add(new Job(id, parsed.get("title").strip().toLowerCase(), parsed.get("company").strip().toLowerCase()));
        return id;
    }
}
