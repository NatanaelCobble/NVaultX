package org.nvaultx.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Permission queries use current contexts and require a loaded user (online for native PNX).
 * An unavailable user throws; it is never treated as a denied permission or a default group.
 * Invoke on the main loop. Mutations complete asynchronously; do not block with join/get.
 */
public interface Permission extends Provider {
    boolean supportsGroups();
    boolean supportsMutations();
    boolean has(UUID player, String node);
    String primaryGroup(UUID player);
    List<String> groups(UUID player);
    /** Sets/removes a permanent global explicit permission, not an inherited grant. */
    CompletionStage<Void> setPermission(UUID player, String node, boolean value);
    CompletionStage<Void> unsetPermission(UUID player, String node);
}
