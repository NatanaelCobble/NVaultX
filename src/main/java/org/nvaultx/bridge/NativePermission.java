package org.nvaultx.bridge;

import org.powernukkitx.plugin.Plugin;
import org.nvaultx.api.Permission;
import org.nvaultx.api.NVaultX;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Native effective permissions include attachments from installed permission managers. */
public final class NativePermission implements Permission {
    private final Plugin owner;
    public NativePermission(Plugin owner) { this.owner = owner; }
    public String getName() { return "PowerNukkitX (native)"; }
    public boolean isEnabled() { return owner.isEnabled(); }
    public boolean supportsGroups() { return false; }
    public boolean supportsMutations() { return false; }
    public boolean has(UUID player, String node) {
        NVaultX.requireMainThread(owner.getServer());
        if (!isEnabled()) throw new IllegalStateException("Provider disabled");
        if (node == null || node.isBlank()) throw new IllegalArgumentException("Permission required");
        return owner.getServer().getPlayer(player).orElseThrow(
                () -> new IllegalStateException("Native permissions require an online player")).hasPermission(node);
    }
    public String primaryGroup(UUID player) { throw new UnsupportedOperationException("Native PNX has no groups"); }
    public List<String> groups(UUID player) { throw new UnsupportedOperationException("Native PNX has no groups"); }
    public CompletionStage<Void> setPermission(UUID player, String node, boolean value) { return unsupported(); }
    public CompletionStage<Void> unsetPermission(UUID player, String node) { return unsupported(); }
    private CompletionStage<Void> unsupported() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Install LuckPerms for persistent changes"));
    }
}
