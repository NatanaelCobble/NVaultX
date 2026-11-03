package org.nvaultx;

import org.powernukkitx.plugin.PluginDescription;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Runs without the plugin on the server classpath, matching PNX's plugin loader boundary. */
public final class ManifestTest {
    public static void main(String[] args) throws Exception {
        Map<String, Object> data = new Yaml().load(Files.readString(Path.of(args[0])));
        // Permission constructors require a running server. Check their YAML shape separately here.
        Object permissions = data.remove("permissions");
        if (!(permissions instanceof Map<?, ?> map) || !map.containsKey("nvaultx.admin"))
            throw new AssertionError("Missing admin permission");
        PluginDescription description = new PluginDescription(data);
        if (!description.getCompatibleAPIs().contains("3.0.6")
                || !description.getCommands().containsKey("nvaultx")
                || !description.getMain().equals("org.nvaultx.NVaultPlugin"))
            throw new AssertionError("Invalid plugin manifest");
        System.out.println("PASS: manifest fields parsed by PNX; permission YAML verified separately");
    }
}
