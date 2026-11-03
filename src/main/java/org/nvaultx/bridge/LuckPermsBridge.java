package org.nvaultx.bridge;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.powernukkitx.plugin.Plugin;
import org.nvaultx.api.Chat;
import org.nvaultx.api.Permission;
import org.nvaultx.api.NVaultX;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Contextual queries never load users or block the main loop. Persistent edits require loaded users too. */
public final class LuckPermsBridge implements Permission, Chat {
    private final Plugin owner;
    private final LuckPerms api;
    public LuckPermsBridge(Plugin owner) { this.owner = owner; this.api = LuckPermsProvider.get(); }
    public String getName() { return "LuckPerms"; }
    public boolean isEnabled() { return owner.isEnabled(); }
    public boolean supportsGroups() { return true; }
    public boolean supportsMutations() { return true; }
    private User user(UUID uuid) {
        NVaultX.requireMainThread(owner.getServer());
        if (!isEnabled()) throw new IllegalStateException("LuckPerms disabled");
        User user = api.getUserManager().getUser(uuid);
        if (user == null) throw new IllegalStateException("LuckPerms user not loaded: " + uuid);
        return user;
    }
    private QueryOptions options(User user) {
        return api.getContextManager().getQueryOptions(user).orElseGet(
                () -> api.getContextManager().getStaticQueryOptions());
    }
    private String node(String node) {
        if (node == null || node.isBlank()) throw new IllegalArgumentException("Non-empty node required");
        return node;
    }
    public boolean has(UUID player, String node) {
        User user = user(player);
        return user.getCachedData().getPermissionData(options(user)).checkPermission(node(node)).asBoolean();
    }
    public String primaryGroup(UUID player) { return user(player).getPrimaryGroup(); }
    public List<String> groups(UUID player) {
        User user = user(player);
        return user.getInheritedGroups(options(user)).stream().map(g -> g.getName()).sorted().toList();
    }
    public String prefix(UUID player) {
        User user = user(player);
        return empty(user.getCachedData().getMetaData(options(user)).getPrefix());
    }
    public String suffix(UUID player) {
        User user = user(player);
        return empty(user.getCachedData().getMetaData(options(user)).getSuffix());
    }
    public String metadata(UUID player, String key, String fallback) {
        User user = user(player);
        String value = user.getCachedData().getMetaData(options(user)).getMetaValue(node(key));
        return value == null ? fallback : value;
    }
    public CompletionStage<Void> setPermission(UUID player, String node, boolean value) {
        User user = user(player);
        String permission = node(node);
        // Change only global, permanent explicit nodes, preserving contextual/temporary entries.
        user.data().clear(n -> n instanceof PermissionNode p && p.getPermission().equals(permission)
                && !n.hasExpiry() && n.getContexts().isEmpty());
        user.data().add(PermissionNode.builder(permission).value(value).build());
        return api.getUserManager().saveUser(user);
    }
    public CompletionStage<Void> unsetPermission(UUID player, String node) {
        User user = user(player);
        String permission = node(node);
        user.data().clear(n -> n instanceof PermissionNode p && p.getPermission().equals(permission)
                && !n.hasExpiry() && n.getContexts().isEmpty());
        return api.getUserManager().saveUser(user);
    }
    private static String empty(String value) { return value == null ? "" : value; }
}
