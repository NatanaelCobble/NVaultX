package org.nvaultx.bridge;

import org.powernukkitx.Player;
import org.powernukkitx.plugin.Plugin;
import org.powernukkitx.shopui.ShopUI;
import org.powernukkitx.shopui.economy.EconomyProvider;
import org.nvaultx.api.Economy;
import org.nvaultx.api.EconomyResponse;
import org.nvaultx.api.NVaultX;

/** ShopUI's public extension point uses whole units; actual balances stay in the selected economy. */
public final class ShopUIBridge implements EconomyProvider {
    private final Plugin owner;
    private final EconomyProvider previous;
    public ShopUIBridge(Plugin owner) {
        this.owner = owner;
        this.previous = ShopUI.getEconomyProvider();
    }
    public void install() { ShopUI.setEconomyProvider(this); }
    public void restore() {
        if (ShopUI.getEconomyProvider() == this) ShopUI.setEconomyProvider(previous);
    }
    private Economy economy() {
        NVaultX.requireMainThread(owner.getServer());
        if (!owner.isEnabled()) throw new IllegalStateException("NVaultX disabled");
        return NVaultX.find(owner.getServer(), Economy.class).orElseThrow(
                () -> new IllegalStateException("NVaultX: aucun fournisseur d'economie actif"));
    }
    public long getMoney(Player player) {
        double balance = economy().getBalance(player.getName());
        if (!Double.isFinite(balance) || balance < 0 || balance > 1_000_000_000_000d)
            throw new IllegalStateException("Solde hors limites ShopUI/NVaultX");
        return (long) Math.floor(balance);
    }
    public void addMoney(String player, long amount) { mutate(player, amount, false); }
    public void reduceMoney(String player, long amount) { mutate(player, amount, true); }
    private void mutate(String player, long amount, boolean withdraw) {
        if (amount < 0 || amount > 1_000_000_000_000L) throw new IllegalArgumentException("Montant ShopUI hors limites");
        Economy economy = economy();
        EconomyResponse response = withdraw ? economy.withdraw(player, amount) : economy.deposit(player, amount);
        if (!response.successful()) {
            owner.getLogger().warning("ShopUI " + player + ": " + response.status() + " " + response.message());
            throw new IllegalStateException("NVaultX: " + response.status() + " " + response.message());
        }
    }
}
