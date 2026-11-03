package org.nvaultx.api;

import java.util.UUID;

/** Read-only contextual chat metadata for loaded users. Does not alter chat formatting. Main-loop only. */
public interface Chat extends Provider {
    String prefix(UUID player);
    String suffix(UUID player);
    /** Returns the supplied fallback when the metadata key is absent. */
    String metadata(UUID player, String key, String fallback);
}
