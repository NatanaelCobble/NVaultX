package org.nvaultx.api;

/**
 * Global, name-keyed economy bridge. Names must match the economy's stored account.
 * All calls run on the PNX main loop. No implicit accounts, transfers or banks.
 * Successful mutations reflect the provider's live state, not a disk commit.
 */
public interface Economy extends Provider {
    String currencyName();
    int fractionalDigits();
    String format(double amount);
    boolean hasAccount(String playerName);
    /** Throws if the provider/account is unavailable; never substitutes a zero balance. */
    double getBalance(String playerName);
    EconomyResponse deposit(String playerName, double amount);
    EconomyResponse withdraw(String playerName, double amount);
}
