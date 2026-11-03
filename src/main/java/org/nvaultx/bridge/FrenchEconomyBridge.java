package org.nvaultx.bridge;

import org.powernukkitx.plugin.Plugin;
import org.nvaultx.api.Economy;
import org.nvaultx.api.EconomyResponse;
import org.nvaultx.api.NVaultX;
import psycofeu.economy.FrenchEconomy;
import psycofeu.economy.managers.Wallet;
import java.util.Locale;

/** Uses the published FrenchEconomy 1.0 API; account names and persistence remain provider-owned. */
public final class FrenchEconomyBridge implements Economy {
    private final Plugin owner;
    public FrenchEconomyBridge(Plugin owner) { this.owner = owner; }
    public String getName() { return "FrenchEconomy"; }
    public boolean isEnabled() { return owner.isEnabled(); }
    public String currencyName() { return "$"; }
    public int fractionalDigits() { return 2; }
    public String format(double amount) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Finite amount required");
        return String.format(Locale.FRANCE, "%.2f $", amount);
    }
    private void check() {
        NVaultX.requireMainThread(owner.getServer());
        if (!isEnabled()) throw new IllegalStateException("FrenchEconomy disabled");
    }
    private void name(String player) {
        if (player == null || player.isBlank()) throw new IllegalArgumentException("Account name required");
    }
    public boolean hasAccount(String player) { check(); name(player); return FrenchEconomy.getWalletManager().exists(player); }
    private Wallet wallet(String player) {
        check(); name(player);
        Wallet wallet = FrenchEconomy.getWalletManager().getWallet(player);
        if (wallet == null) throw new IllegalArgumentException("Compte inexistant : " + player);
        return wallet;
    }
    public double getBalance(String player) { return wallet(player).getMoney(); }
    public EconomyResponse deposit(String player, double amount) { return mutate(player, amount, false); }
    public EconomyResponse withdraw(String player, double amount) { return mutate(player, amount, true); }
    private EconomyResponse mutate(String player, double amount, boolean withdraw) {
        check();
        final Wallet wallet;
        try { wallet = wallet(player); }
        catch (IllegalArgumentException e) {
            return new EconomyResponse(EconomyResponse.Status.FAILURE, amount, Double.NaN, e.getMessage());
        }
        return MoneyOperations.apply(new MoneyOperations.Account() {
            public double balance() { return wallet.getMoney(); }
            public void deposit(double value) { wallet.addMoney(value); }
            public void withdraw(double value) { wallet.reduceMoney(value); }
        }, amount, withdraw);
    }
}
