# NVaultX

Pont natif entre les plugins **PowerNukkitX**, inspiré du principe de Vault : une API commune pour l'économie, les permissions et les métadonnées de chat.

## Installation

1. Utiliser **PowerNukkitX 3.0.6**. Le cœur NVaultX fonctionne sous Java 21 ou plus récent ; **ShopUI 2.0.1 publié nécessite Java 25**.
2. Placer `NVaultX-1.0.0.jar` dans `plugins/`.
3. Installer les fournisseurs souhaités : **FrenchEconomy 1.0** pour l'économie, **LuckPerms PNX 5.4.0** pour les groupes et préfixes.
4. Ajouter éventuellement **ShopUI 2.0.1** et **StatBoard 1.0.0**.
5. Redémarrer le serveur et exécuter `/nvaultx` depuis la console ou avec `nvaultx.admin` (op par défaut).

Ne pas installer le JAR `NVaultX-API` dans `plugins/` : il sert uniquement à compiler d'autres plugins.

Si le système d'événements de PNX produit une erreur d'accès à `ClassLoader.defineClass`, les tests ont utilisé ces options de lancement Java :

```text
java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED -jar powernukkitx.jar
```

## Compatibilité

| Plugin | Intégration |
| --- | --- |
| FrenchEconomy 1.0 | Soldes, existence de compte, dépôts et retraits. Les données restent dans FrenchEconomy. |
| ShopUI 2.0.1 | Fournisseur injecté par son API publique ; achats et ventes utilisent le fournisseur économique sélectionné par NVaultX. |
| StatBoard 1.0.0 | Son connecteur FrenchEconomy existant utilise les mêmes portefeuilles. `/nvaultx` vérifie le fournisseur réellement choisi. |
| LuckPerms PNX 5.4.0 | Permissions contextuelles, groupe principal, groupes hérités, préfixes/suffixes et métadonnées ; modification des permissions globales permanentes. |
| Permissions natives PNX | Vérification des droits des joueurs connectés lorsqu'aucun fournisseur de priorité supérieure n'est actif. |

StatBoard **ne consomme pas automatiquement tous les fournisseurs NVaultX** : son intégration actuelle passe par FrenchEconomy. Si LlamaEconomy ou EconomyAPI est aussi installé, StatBoard peut le choisir en priorité ; la commande indique alors que le partage des comptes n'est pas confirmé. Pas de migration automatique de soldes.

ShopUI et StatBoard représentent leurs montants en unités entières. FrenchEconomy conserve les décimales. La monnaie du connecteur FrenchEconomy est `$`, avec deux décimales.

## API pour les développeurs

Déclarer `depend: [NVaultX]` dans le `plugin.yml` du consommateur. Compiler avec `NVaultX-API-1.0.0.jar` et PNX, **sans embarquer ces classes dans le JAR du consommateur**.

```java
import org.nvaultx.api.Economy;
import org.nvaultx.api.NVaultX;

// Depuis la boucle principale PNX :
var service = NVaultX.find(getServer(), Economy.class);
if (service.isEmpty()) {
    // Désactiver proprement la fonctionnalité qui exige une économie.
    return;
}
var result = service.get().withdraw(player.getName(), 25.0);
if (result.successful()) {
    // Effectuer la prestation demandée.
}
// UNKNOWN : résultat incertain, intervention requise, aucune répétition automatique.
```

Les fournisseurs personnalisés implémentent `Economy`, `Permission` ou `Chat`, puis appellent :

```java
boolean accepted = NVaultX.register(getServer(), Economy.class, myProvider,
        this, org.powernukkitx.plugin.service.ServicePriority.HIGHER);
```

Vérifier `accepted`. Le registre de PNX 3.0.6 refuse notamment un deuxième fournisseur de même priorité. Le connecteur FrenchEconomy et LuckPerms utilisent `NORMAL`, le repli natif `LOWEST`. Résoudre le service au moment de l'utilisation ; ne pas conserver indéfiniment un fournisseur désactivé. PNX retire les services appartenant à un plugin désactivé.

Toutes les opérations de l'API s'appellent depuis la boucle principale PNX. Les changements de permissions retournent un `CompletionStage` : ne pas bloquer avec `join()`/`get()`. Leurs continuations peuvent être exécutées hors de la boucle principale ; y reprogrammer tout accès au jeu. LuckPerms exige un utilisateur déjà chargé. Les changements affectent d'abord son état en mémoire ; un échec de sauvegarde n'annule pas automatiquement cet état.

## Limites de cette version

- API native indépendante : aucun chargement des plugins Bukkit/Spigot et aucune compatibilité binaire avec VaultAPI.
- Les comptes économiques sont identifiés par leur nom exact, comme dans FrenchEconomy. Aucun compte n'est créé implicitement par NVaultX.
- Montants finis, positifs ou nuls, deux décimales maximum ; montants et soldes plafonnés à 10^12 dans le connecteur. Des soldes d'une précision supérieure sont refusés, hormis de très petits résidus de calcul flottant.
- Pas de banque, de transfert atomique entre comptes, de création de groupes ni d'édition des métadonnées de chat.
- `SUCCESS` confirme l'état du portefeuille en mémoire, pas une écriture durable sur disque. La persistance appartient à FrenchEconomy.
- `UNKNOWN` signifie qu'une mutation a pu avoir lieu : contrôler les comptes manuellement avant toute nouvelle tentative. Les échanges inventaire/argent de ShopUI restent soumis aux garanties de ShopUI ; NVaultX ne les rend pas atomiques.
- LlamaEconomy n'est pas raccordé : son API publique de mutation asynchrone ne renvoie pas de confirmation de transaction exploitable par cette API synchrone.

## Compilation sous Windows

Installer un **JDK 25** pour compiler contre le JAR publié de ShopUI. Le bytecode NVaultX et l'API ciblent Java 21.

```powershell
./download-dependencies.ps1
./build.ps1 -JavaHome 'C:/chemin/vers/jdk-25'
```

Les dépendances officielles sont téléchargées dans `deps/` et vérifiées par SHA-256. Elles ne sont pas embarquées dans NVaultX. Le script compile, exécute les tests et produit le plugin et le JAR API à la racine.

## Vérifications

Voir [TESTING.md](TESTING.md) : tests de contrats automatisés et intégration sur un serveur local PNX. Les interfaces et échanges d'inventaire avec un véritable client Bedrock restent à tester en jeu.

## Distribution

Projet indépendant, non affilié à Vault, Bukkit, Spigot, Mojang ou aux auteurs des plugins raccordés. Sources NVaultX originales sous licence MIT. Les plugins tiers conservent leurs propres licences et distributions.
