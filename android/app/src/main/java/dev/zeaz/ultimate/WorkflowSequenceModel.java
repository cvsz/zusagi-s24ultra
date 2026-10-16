package dev.zeaz.ultimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Local-only workflow draft model. It validates and previews; it never executes work. */
final class WorkflowSequenceModel {
    private static final int MAX_STEPS = 20;
    private final LinkedHashMap<String, Step> steps = new LinkedHashMap<>();

    void add(String id, String title, List<String> dependencies) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]{1,40}"))
            throw new IllegalArgumentException("Invalid step id");
        if (title == null || title.trim().isEmpty() || title.trim().length() > 120)
            throw new IllegalArgumentException("Step title must be 1 to 120 characters");
        if (steps.containsKey(id)) throw new IllegalArgumentException("Duplicate step id");
        if (steps.size() >= MAX_STEPS) throw new IllegalArgumentException("Maximum of 20 steps");
        Set<String> unique = new HashSet<>();
        if (dependencies != null) for (String dependency : dependencies) {
            if (dependency == null || dependency.trim().isEmpty()) continue;
            String value = dependency.trim();
            if (!unique.add(value)) throw new IllegalArgumentException("Duplicate dependency: " + value);
        }
        steps.put(id, new Step(id, title.trim(), new ArrayList<>(unique)));
    }

    List<Step> steps() { return Collections.unmodifiableList(new ArrayList<>(steps.values())); }

    Validation validate() {
        List<String> errors = new ArrayList<>();
        for (Step step : steps.values()) for (String dependency : step.dependencies)
            if (!steps.containsKey(dependency)) errors.add(step.id + " depends on unknown step " + dependency);
        if (!errors.isEmpty()) return new Validation(errors);
        Map<String, Integer> marks = new HashMap<>();
        for (String id : steps.keySet()) if (hasCycle(id, marks)) {
            errors.add("Workflow contains a dependency cycle");
            break;
        }
        return new Validation(errors);
    }

    List<String> previewOrder() {
        Validation validation = validate();
        if (!validation.isValid()) throw new IllegalStateException(validation.errors.get(0));
        List<String> order = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        while (order.size() < steps.size()) {
            boolean progressed = false;
            for (Step step : steps.values()) {
                if (emitted.contains(step.id) || !emitted.containsAll(step.dependencies)) continue;
                emitted.add(step.id);
                order.add(step.id);
                progressed = true;
            }
            if (!progressed) throw new IllegalStateException("Workflow contains a dependency cycle");
        }
        return order;
    }

    private boolean hasCycle(String id, Map<String, Integer> marks) {
        Integer mark = marks.get(id);
        if (mark != null) return mark == 1;
        marks.put(id, 1);
        for (String dependency : steps.get(id).dependencies)
            if (steps.containsKey(dependency) && hasCycle(dependency, marks)) return true;
        marks.put(id, 2);
        return false;
    }

    static final class Step {
        final String id;
        final String title;
        final List<String> dependencies;
        Step(String id, String title, List<String> dependencies) {
            this.id = id; this.title = title;
            this.dependencies = Collections.unmodifiableList(dependencies);
        }
    }

    static final class Validation {
        final List<String> errors;
        Validation(List<String> errors) { this.errors = Collections.unmodifiableList(errors); }
        boolean isValid() { return errors.isEmpty(); }
    }
}
