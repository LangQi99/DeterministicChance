package io.github.langqi99.deterministicchance.compat.jei;

import java.util.Objects;
import java.util.Optional;

/**
 * Tri-state result for a JEI recipe transfer.
 *
 * <p>An adapter that recognizes a probabilistic recipe must never silently
 * fall back to AE2's ordinary transfer when it cannot prove an exact batch.
 * That fallback would encode the viewer's one-run average as if it were a
 * guaranteed machine output.</p>
 */
public record JeiBatchDecision(Status status, JeiBatchPlan plan, String reason) {
    public enum Status {
        NOT_APPLICABLE,
        EXACT_PLAN,
        RECOGNIZED_BUT_UNSUPPORTED
    }

    public JeiBatchDecision {
        Objects.requireNonNull(status, "status");
        reason = reason == null ? "" : reason;
        if ((status == Status.EXACT_PLAN) != (plan != null)) {
            throw new IllegalArgumentException("only an exact decision may contain a plan");
        }
    }

    public static JeiBatchDecision notApplicable() {
        return new JeiBatchDecision(Status.NOT_APPLICABLE, null, "");
    }

    public static JeiBatchDecision exact(JeiBatchPlan plan) {
        return new JeiBatchDecision(Status.EXACT_PLAN, Objects.requireNonNull(plan, "plan"), "");
    }

    public static JeiBatchDecision unsupported(String reason) {
        return new JeiBatchDecision(
                Status.RECOGNIZED_BUT_UNSUPPORTED,
                null,
                reason == null || reason.isBlank() ? "no exact finite batch is available" : reason);
    }

    public Optional<JeiBatchPlan> exactPlan() {
        return Optional.ofNullable(plan);
    }
}
