package org.nvaultx.bridge;

import org.powernukkitx.plugin.Plugin;
import org.nvaultx.api.Economy;
import org.nvaultx.api.NVaultX;
import deepslatedev.statboard.StatBoard;

/**
 * StatBoard 1.0 has a final economy class and no public injection point.
 * Its existing FrenchEconomy support shares the same wallets. This is a read-only compatibility check,
 * not a claim that StatBoard consumes arbitrary NVaultX providers. No third-party fields are modified.
 */
public final class StatBoardCompatibility {
    private StatBoardCompatibility() { }
    public static String status(Plugin plugin) {
        NVaultX.requireMainThread(plugin.getServer());
        if (!plugin.isEnabled()) return "desactive";
        try {
            var field = StatBoard.class.getDeclaredField("economy");
            field.setAccessible(true);
            var economy = (deepslatedev.statboard.Economy) field.get(plugin);
            if (economy == null) return "initialisation en cours";
            String backend = economy.name();
            String selected = NVaultX.find(plugin.getServer(), Economy.class).map(Economy::getName).orElse("aucun");
            return backend.equals("FrenchEconomy") && selected.equals("FrenchEconomy")
                    ? "compatible : memes comptes FrenchEconomy (soldes entiers dans StatBoard)"
                    : "fournisseur StatBoard=" + backend + ", NVaultX=" + selected + " ; comptes partages non confirmes";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return "version StatBoard non reconnue : " + e.getClass().getSimpleName();
        }
    }
}
