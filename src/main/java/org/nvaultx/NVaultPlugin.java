package org.nvaultx;

import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.plugin.PluginDisableEvent;
import org.powernukkitx.event.plugin.PluginEnableEvent;
import org.powernukkitx.plugin.Plugin;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.plugin.service.ServicePriority;
import org.nvaultx.api.*;
import org.nvaultx.bridge.*;
import java.util.HashSet;
import java.util.Set;

/** Independent native Vault-style bridge for PNX; not a Bukkit compatibility layer. */
public final class NVaultPlugin extends PluginBase implements Listener {
    private final Set<Plugin> hooked = new HashSet<>();
    private ShopUIBridge shopUI;

    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        install(Permission.class, new NativePermission(this), this, ServicePriority.LOWEST);
        for (Plugin plugin : getServer().getPluginManager().getPlugins().values()) hook(plugin);
        getLogger().info("NVaultX actif. /nvaultx pour voir les fournisseurs.");
    }
    @Override public void onDisable() {
        if (shopUI != null) { shopUI.restore(); shopUI = null; }
        NVaultX.unregister(getServer(), this);
        // Cancel only adapters created by us; other provider registrations remain owned by their plugins.
        for (Plugin plugin : Set.copyOf(hooked)) removeAdapters(plugin);
        hooked.clear();
    }
    @EventHandler public void enabled(PluginEnableEvent event) { hook(event.getPlugin()); }
    @EventHandler public void disabled(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("ShopUI") && shopUI != null) {
            shopUI.restore(); shopUI = null;
        }
        NVaultX.unregister(getServer(), event.getPlugin());
        hooked.remove(event.getPlugin());
    }
    private void hook(Plugin plugin) {
        if (!plugin.isEnabled() || hooked.contains(plugin)) return;
        try {
            switch (plugin.getName()) {
                case "ShopUI" -> {
                    shopUI = new ShopUIBridge(this);
                    shopUI.install();
                    hooked.add(plugin);
                    getLogger().info("ShopUI connecte a l'economie NVaultX.");
                }
                case "StatBoard" -> {
                    getLogger().info("StatBoard : " + StatBoardCompatibility.status(plugin));
                }
                case "FrenchEconomy" -> {
                    install(Economy.class, new FrenchEconomyBridge(plugin), plugin, ServicePriority.NORMAL);
                    hooked.add(plugin);
                }
                case "LuckPerms" -> {
                    LuckPermsBridge bridge = new LuckPermsBridge(plugin);
                    install(Permission.class, bridge, plugin, ServicePriority.NORMAL);
                    install(Chat.class, bridge, plugin, ServicePriority.NORMAL);
                    hooked.add(plugin);
                }
                default -> { }
            }
        } catch (LinkageError | RuntimeException e) {
            removeAdapters(plugin);
            getLogger().warning("Connecteur " + plugin.getName() + " indisponible : " + e.getClass().getSimpleName()
                    + " : " + e.getMessage());
        }
    }
    private <T extends Provider> void install(Class<T> type, T provider, Plugin owner, ServicePriority priority) {
        if (!NVaultX.register(getServer(), type, provider, owner, priority))
            getLogger().warning("Service " + type.getSimpleName() + " : priorite " + priority + " deja occupee.");
    }
    private void removeAdapters(Plugin owner) {
        removeAdapter(owner, Economy.class);
        removeAdapter(owner, Permission.class);
        removeAdapter(owner, Chat.class);
    }
    private <T extends Provider> void removeAdapter(Plugin owner, Class<T> type) {
        for (var r : getServer().getServiceManager().getRegistrations(type)) {
            if (r.getPlugin() == owner && (r.getProvider() instanceof FrenchEconomyBridge
                    || r.getProvider() instanceof LuckPermsBridge))
                getServer().getServiceManager().cancel(type, r.getProvider());
        }
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nvaultx.admin")) { sender.sendMessage("Permission insuffisante."); return true; }
        sender.sendMessage("NVaultX 1.0.0 - PowerNukkitX 3.0.6 / Java 21");
        status(sender, "Economie", Economy.class);
        status(sender, "Permissions", Permission.class);
        status(sender, "Chat (metadonnees)", Chat.class);
        Plugin shop = getServer().getPluginManager().getPlugin("ShopUI");
        sender.sendMessage("ShopUI : " + (shop != null && shop.isEnabled() && shopUI != null ? "connecte" : "absent/desactive"));
        Plugin statboard = getServer().getPluginManager().getPlugin("StatBoard");
        sender.sendMessage("StatBoard : " + (statboard == null ? "absent" : StatBoardCompatibility.status(statboard)));
        return true;
    }
    private <T extends Provider> void status(CommandSender sender, String name, Class<T> type) {
        sender.sendMessage(name + " : " + NVaultX.find(getServer(), type).map(Provider::getName).orElse("aucun fournisseur"));
    }
}
