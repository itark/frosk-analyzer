package nu.itark.frosk.service;

/** Outcome of {@link RiskManagementService#checkEntry}. {@code reason} is null when {@code allowed}. */
public record RiskCheckResult(boolean allowed, String reason) {

    public static RiskCheckResult allow() {
        return new RiskCheckResult(true, null);
    }

    public static RiskCheckResult blocked(String reason) {
        return new RiskCheckResult(false, reason);
    }
}
