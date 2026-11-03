package org.nvaultx.api;

/** UNKNOWN means the mutation may have occurred: never retry automatically. NaN means balance unavailable. */
public record EconomyResponse(Status status, double amount, double balance, String message) {
    public enum Status { SUCCESS, FAILURE, NOT_SUPPORTED, UNKNOWN }
    public boolean successful() { return status == Status.SUCCESS; }
}
