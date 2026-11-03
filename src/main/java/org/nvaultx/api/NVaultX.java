package org.nvaultx.api;

import org.powernukkitx.Server;
import org.powernukkitx.plugin.Plugin;
import org.powernukkitx.plugin.service.ServicePriority;
import java.util.Optional;

/** Native PNX service access. Depend on NVaultX and never bundle a second copy of this API. */
public final class NVaultX {
    private NVaultX() { }
    public static void requireMainThread(Server server) {
        if (!server.isPrimaryThread()) throw new IllegalStateException("NVaultX requires the PNX main loop");
    }
    /** Resolves on every call so a disabled provider is not retained. Empty means unavailable. */
    public static <T extends Provider> Optional<T> find(Server server, Class<T> type) {
        requireMainThread(server);
        return server.getServiceManager().getRegistrations(type).stream()
                .filter(r -> r.getPlugin() == null || r.getPlugin().isEnabled())
                .map(r -> r.getProvider()).filter(Provider::isEnabled).findFirst();
    }
    /** Returns false if PNX rejects registration (notably a priority already occupied). Check the result. */
    public static <T extends Provider> boolean register(Server server, Class<T> type, T provider,
                                                       Plugin owner, ServicePriority priority) {
        requireMainThread(server);
        if (owner == null || !owner.isEnabled()) throw new IllegalArgumentException("Enabled owner required");
        if (!provider.isEnabled()) throw new IllegalArgumentException("Enabled provider required");
        return server.getServiceManager().register(type, provider, owner, priority);
    }
    /** Removes only this API's services; leaves unrelated services of the owner intact. */
    public static void unregister(Server server, Plugin owner) {
        requireMainThread(server);
        remove(server, owner, Economy.class);
        remove(server, owner, Permission.class);
        remove(server, owner, Chat.class);
    }
    private static <T extends Provider> void remove(Server server, Plugin owner, Class<T> type) {
        for (var registration : server.getServiceManager().getRegistrations(type)) {
            if (registration.getPlugin() == owner)
                server.getServiceManager().cancel(type, registration.getProvider());
        }
    }
}
