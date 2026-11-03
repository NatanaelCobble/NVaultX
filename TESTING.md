# Vérifications NVaultX 1.0.0

Exécutées le 3 novembre 2026. Code, tests et documentation produits avec l'assistance de Codex (GPT-6).

## Tests automatisés

`build.ps1` compile le projet avec le JDK 25 et `--release 21`, puis vérifie :

- 35 assertions sur les montants, soldes, retraits insuffisants, limites, résidus flottants, refus de mutations et résultats incertains ;
- sélection par priorité et retrait des fournisseurs avec le **véritable registre NKServiceManager de PNX 3.0.6** ;
- lecture des champs du manifeste par le parseur PNX. Les permissions YAML sont vérifiées séparément, car leur construction exige un serveur actif.

## Intégration exécutée sur un serveur local

Serveur PNX 3.0.6 lié à 127.0.0.1, fermé après chaque exécution. Les comptes utilisés étaient des comptes de test, sans client Bedrock connecté.

Configurations vérifiées :

1. Cœur du pont sans fournisseurs externes, sous Java 21 avant renommage en NVaultX : chargement et vérification du fournisseur natif.
2. FrenchEconomy 1.0 et LuckPerms PNX 5.4.0 : chargement et raccordement des trois services.
3. NVaultX final avec FrenchEconomy 1.0, LuckPerms PNX 5.4.0, ShopUI 2.0.1 et StatBoard 1.0.0 sous Java 25 : chargement des cinq plugins et commande `/nvaultx`.

Un plugin de sonde a appelé les API réellement chargées :

- dépôt de 25,50 sur un solde de 100, retrait de 20, rejet d'un retrait de 106 et de NaN ;
- retrait de 15 et dépôt de 4 via le fournisseur **ShopUI**, vérification du portefeuille FrenchEconomy obtenu ;
- lecture de ce même portefeuille par **l'instance économique de StatBoard** : 94,50 dans FrenchEconomy, 94 unités dans StatBoard ; retrait et ajout via StatBoard vérifiés dans FrenchEconomy ;
- lecture des groupes et métadonnées LuckPerms, ajout puis suppression d'une permission globale avec attente de la sauvegarde ;
- désactivation de FrenchEconomy : service retiré ; réactivation : service retrouvé ;
- arrêt propre du serveur sans erreur de chargement NVaultX.

La sonde et les données du serveur local ne font pas partie du plugin distribué.

## Vérification en jeu encore nécessaire

Avec un véritable client Bedrock :

1. Se connecter, comparer le solde FrenchEconomy et les soldes affichés par ShopUI/StatBoard. Les deux derniers utilisent des unités entières.
2. Acheter puis vendre un objet dans ShopUI : vérifier argent, quantité d'objets et absence de double opération après une double confirmation.
3. Réessayer un achat sans argent et avec inventaire plein ; vérifier les messages et les remboursements prévus par ShopUI.
4. Contrôler les classements et hologrammes StatBoard après leur délai d'actualisation.
5. Tester les permissions, préfixes et suffixes avec plusieurs groupes et mondes LuckPerms.
6. Redémarrer puis comparer les soldes persistés par FrenchEconomy.

Ces étapes n'ont pas été exécutées en jeu ; les tests d'API ne remplacent pas la validation des formulaires et inventaires.
