# NVaultX

NVaultX provides a common API for economy, permissions and chat metadata on PowerNukkitX. Server plugins discover providers through the native PNX service registry.

## Installation

1. Use PowerNukkitX 3.0.6 with Java 21 or newer. The published ShopUI 2.0.1 JAR requires Java 25.
2. Download `NVaultX-1.0.0.jar` from the [release](https://github.com/NatanaelCobble/NVaultX/releases/tag/v1.0.0) and place it in `plugins/`.
3. Install FrenchEconomy 1.0 for economy features and LuckPerms PNX 5.4.0 for group and chat metadata features as needed. ShopUI 2.0.1 and StatBoard 1.0.0 are optional integrations.
4. Restart and run `/nvaultx` from the console or with the `nvaultx.admin` permission, which defaults to operators.

Keep the `NVaultX-API` JAR on the developer's compile classpath. Only the main NVaultX JAR goes in the server's plugin directory.

If PNX reports an access error for `ClassLoader.defineClass`, use these Java options:

```text
java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED -jar powernukkitx.jar
```

## Compatibility

| Plugin | Integration |
| --- | --- |
| FrenchEconomy 1.0 | Account checks, balances, deposits and withdrawals. FrenchEconomy stores the balances. |
| ShopUI 2.0.1 | NVaultX registers a provider through ShopUI's public API. Purchases and sales use the active NVaultX economy provider. |
| StatBoard 1.0.0 | Its existing FrenchEconomy connector reads the same player wallets. If StatBoard selects LlamaEconomy or EconomyAPI instead, verify provider selection with `/nvaultx`. |
| LuckPerms PNX 5.4.0 | Contextual permissions, primary and inherited groups, prefixes, suffixes, metadata and permanent global permission edits. |
| Native PNX permissions | Fallback permission checks for online players. |

ShopUI and StatBoard represent amounts as whole units. The FrenchEconomy adapter uses dollars with two decimal places. Existing balances stay with their provider.

LlamaEconomy 1.0.2 has no NVaultX adapter. Its public write methods schedule the storage operation asynchronously and return no result. NVaultX needs a confirmed outcome to authorize purchases and faction bank transfers. [LlamaEconomy API source](https://github.com/PowerNukkitX-Bundle/LlamaEconomy/blob/v1.0.2/src/main/java/net/lldv/llamaeconomy/components/api/API.java)

## Developer API

Declare `depend: [NVaultX]` in the consumer's `plugin.yml`. Compile against `NVaultX-API-1.0.0.jar` and PNX as provided dependencies. Resolve services on the PNX main thread at the point of use.

```java
import org.nvaultx.api.Economy;
import org.nvaultx.api.NVaultX;

var service = NVaultX.find(getServer(), Economy.class);
if (service.isEmpty()) {
    return;
}
var result = service.get().withdraw(player.getName(), 25.0);
if (result.successful()) {
    // Fulfill the purchase.
}
```

Custom providers implement `Economy`, `Permission` or `Chat` and register with:

```java
boolean accepted = NVaultX.register(getServer(), Economy.class, myProvider,
        this, org.powernukkitx.plugin.service.ServicePriority.HIGHER);
```

Check the return value. The PNX 3.0.6 registry can reject a second provider at the same priority. FrenchEconomy and LuckPerms register at `NORMAL`, while the native permission fallback uses `LOWEST`. Disabled plugins lose their service registrations.

Permission mutations return a `CompletionStage`. Use a continuation and schedule any subsequent game access on the PNX main thread. LuckPerms requires an already loaded user. A failed save can leave a change applied in memory.

## Economy contract

- Use the provider's exact player account name. Account creation belongs to the economy plugin.
- The FrenchEconomy adapter accepts finite, nonnegative amounts with at most two decimal places and caps amounts and balances at 10^12. It tolerates minor floating-point residue.
- `SUCCESS` confirms the provider's in-memory result. Durable persistence belongs to the provider.
- `UNKNOWN` means the operation may have happened. Reconcile the account before retrying. ShopUI controls inventory delivery independently from the provider's money operation.
- The API covers account operations, permission checks and edits, and read-only chat metadata. Bank accounts and atomic transfers between accounts are outside its scope.

## Build and test

Use JDK 25 to compile against the published ShopUI JAR. NVaultX and its API target Java 21.

```powershell
./download-dependencies.ps1
./build.ps1 -JavaHome 'C:/path/to/jdk-25'
```

The download script verifies dependency SHA-256 hashes. The build script runs the tests and produces the plugin and API JARs at the project root. See [TESTING.md](TESTING.md) for automated and local server checks. In-game rendering and inventory exchanges with a real Bedrock client still need verification.

NVaultX is an independent PowerNukkitX project under the MIT license. Its API targets native PNX plugins. Third-party plugins retain their own licenses and distribution channels.
