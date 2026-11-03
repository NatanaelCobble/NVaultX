package org.nvaultx.api;

/** A live service supplied by an enabled plugin. Calls are main-loop only. */
public interface Provider {
    String getName();
    boolean isEnabled();
}
