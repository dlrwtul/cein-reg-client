# CEIN Registration Client

Client d'enregistrement MOSIP customisé pour la CEIN (Sénégal).

## Base

| | |
|---|---|
| Amont | [mosip/registration-client](https://github.com/mosip/registration-client) |
| Version | **1.2.0.2** (dernière version stable de la ligne LTS 1.2.0.x) |
| Commit amont | `84eb683b93bbe0c34f10d714fb16ad3bcb99dae7` (« [DSD-4982] 1.2.0.2 release changes », 1 commit après le tag `v1.2.0.2`) |
| Import amont | commit `4f8bfb2` « Import upstream mosip/registration-client v1.2.0.2 » = amont brut, sans customisation (tag local `upstream/v1.2.0.2`, non poussé) |
| JDK de build | 11 (le JAR de référence a été compilé avec 11.0.25) |

Pourquoi pas une version plus récente : depuis 1.2.0.2, la branche `release-1.2.0.x`
n'a reçu que du Helm et de la CI, sans aucun changement Java. La ligne 1.3.0 n'existe
qu'en bêta, elle passe à Java 21 et exige un backend MOSIP 1.3. Le client doit rester
aligné sur la version du serveur MOSIP déployé.

Le dossier `design/` de l'amont (maquettes Adobe XD et PDF, environ 89 Mo) n'est pas importé.

## Origine de la customisation

Elle a été reconstruite à partir du JAR `registration-client-1.2.0.2.jar` livré
(compilé le 2025-08-26, `Built-By: mayoro`). Ce JAR a été comparé au JAR officiel
`io.mosip.registration:registration-client:1.2.0.2` publié sur Maven Central :

* ressources (FXML, properties, images) : comparaison binaire puis copie à l'identique ;
* classes : décompilation des deux JAR (CFR 0.152), diff, puis report à la main
  dans les sources amont.

Aucun autre fichier ne diffère. Les autres modules (`registration-services`,
`registration-api`, etc.) sont ceux de la version officielle 1.2.0.2.

## Customisation d'origine (JAR du 2025-08-26)

Ce qui a été reporté tel quel depuis le JAR livré, un commit par point
(`4f8bfb2..02964d3`). La section « Revue » plus bas décrit ce qui a changé ensuite.

### 1. Français forcé comme langue unique
- `controller/Initialization.java` : au démarrage, positionne des propriétés système
  `mosip.registration.*language*` sur `fra` et affiche les propriétés mosip/language sur stdout.
- `controller/auth/LoginController.java` : la liste des langues de connexion est remplacée
  par « Français » (`fra`) seul, la combobox est masquée.
- `controller/reg/LanguageSelectionController.java` : la pop-up de choix des langues
  d'enregistrement est court-circuitée, `fra` est sélectionné et l'enregistrement démarre directement.
- `util/control/impl/TextFieldFxControl.java` : positionne `user.language=fr`,
  `user.country=FR` et `javafx.input.method=french` à chaque création de champ texte.

### 2. Onboarding opérateur
- `controller/reg/HomeController.java` : si l'utilisateur n'a pas le rôle `DEFAULT`, les
  indicateurs de session `isOnboardUser` et `updateOnboard` sont forcés à `false` et
  l'écran d'onboarding n'est pas affiché.

### 3. Clavier virtuel
- `controller/VirtualKeyboard.java` : les touches du clavier physique ne sont plus
  re-mappées via `keyboard_<lang>.properties`. La méthode inutilisée `focusListener()` est supprimée.

### 4. Bouton « Parcourir » dans la pop-up de scan de documents
- `fxml/Scan.fxml` : bouton `browseBtn` ajouté au-dessus de « Sauvegarder ».
- `controller/device/ScanPopUpViewController.java` : `browseFile()` ouvre un sélecteur de
  fichiers (png, jpg, jpeg, bmp, gif), convertit l'image en RGB sur fond blanc et l'ajoute
  aux pages scannées. `getImage(BufferedImage)` est surchargée pour passer par `SwingFXUtils`.
- `images/browse.png`.

### 5. Branding Sénégal
- `images/logo-final.png`, `images/logo-final-small.png`, `images/LoginBackGround.png`.
- `fxml/RegistrationLogin.fxml` : logo agrandi (90x60) et titre « REPUBLIQUE DU SÉNÉGAL »
  dans l'en-tête (fichier également ré-indenté).

### 6. Traductions
- `labels_fr.properties`, `messages_fr.properties` : environ 150 clés traduites ou corrigées
  dans chaque fichier, ré-encodés de ISO-8859-1 vers UTF-8.
- `labels_en.properties` : `submit=Soumettre`.

### Écarts volontaires avec le JAR livré
Certaines images présentes dans le JAR livré ne sont référencées nulle part et ne sont pas reprises :
`browse2.png`, `LoginBackGroundOri.png`, `logo-final_ori.png` et `logo-final-small_ori.png`.
Les trois `*Ori*/*_ori*` sont des copies des images MOSIP d'origine, toujours disponibles
via `git show 4f8bfb2:<chemin>`.

## Revue (peigne) : état actuel

| Sujet | Avant (JAR livré) | Maintenant |
|---|---|---|
| Français forcé | Propriétés système `mosip.registration.language.*` que MOSIP ne lit nulle part, liste de langues reconstruite à la main, pop-up court-circuitée de l'intérieur, `labels_en` patché (`submit=Soumettre`) parce que l'écran de login était chargé avant le forçage | `CeinSettings.getForcedLanguage()` : la langue forcée devient la langue de l'application dès le chargement des bundles, le login filtre la liste standard, `PacketHandlerController` enchaîne sans pop-up. `Initialization`, `LanguageSelectionController`, `TextFieldFxControl` et `labels_en` sont revenus à l'identique de l'amont |
| Onboarding | Contournement systématique, log « Mayoro » | Désactivable par configuration, ne s'applique que si l'onboarding était demandé, WARN avec l'identifiant de l'utilisateur |
| Textes en dur | « REPUBLIQUE DU SÉNÉGAL », « Parcourir », messages du scan, styles inline | Clés `countryTitle`, `browse*` dans `labels_fr/en/ar`, classes CSS `ceinCountryTitle` / `ceinHeaderSection` |
| Parcourir | Images seulement, aucune limite, textes en dur, couleur du message sans effet (`-fx-text-fill` sur un `Text`) | PDF (toutes les pages, 200 dpi), PNG/JPG/BMP, taille max, réduction des grandes images, i18n, bouton masquable |
| Reçu | A4 sur l'imprimante par défaut | Impression thermique 58/80 mm (voir ci-dessous) |
| Debug | `System.out` au démarrage, au login et à la sélection de langue | Supprimés |

### Langue : la configuration serveur reste la bonne pratique

Le forçage côté client garantit l'interface en français, mais le plus propre est de
configurer aussi le serveur (`registration-default.properties`) :

```properties
mosip.mandatory-languages=fra
mosip.optional-languages=
```

La langue forcée doit faire partie de ces deux listes. Sinon ses bundles ne sont pas
chargés, le forçage est ignoré et un WARN est écrit dans les logs. Si le serveur exige
d'autres langues obligatoires (par exemple `eng`), le client ne saisit que le français,
comme c'était déjà le cas avec le JAR livré : vérifiez que le registration-processor
accepte ces paquets.

## Configuration CEIN

Toutes les clés se règlent dans `registration-default.properties` sur le config server.
Elles sont synchronisées vers le poste et peuvent être surchargées localement. La valeur par
défaut s'applique quand la clé est absente.

| Clé | Défaut | Effet |
|---|---|---|
| `mosip.registration.cein.forced-language` | `fra` | Langue unique de l'interface et des enregistrements. Vide = comportement MOSIP standard |
| `mosip.registration.cein.skip-onboarding-for-non-default-users` | `true` | Les opérateurs sans rôle `DEFAULT` ne passent pas par l'onboarding (pas de capture biométrique opérateur ni de rattachement au poste au premier login) |
| `mosip.registration.cein.doc-browse.enabled` | `true` | Affiche le bouton « Parcourir » dans la pop-up de scan |
| `mosip.registration.cein.doc-browse.max-size-kb` | `5120` | Taille maximale du fichier importé |
| `mosip.registration.cein.doc-browse.max-dimension-px` | `2480` | Plus grand côté d'une image importée, réduite au-delà (2480 px = A4 à 300 dpi) |
| `mosip.registration.cein.receipt.paper` | `80` | `80` ou `58` = rouleau thermique de cette largeur en mm, `A4` = impression MOSIP standard |
| `mosip.registration.cein.receipt.printable-width-mm` | `72` (rouleau 80), `48` (rouleau 58) | Largeur réellement imprimable de la tête |
| `mosip.registration.cein.receipt.printer-name` | vide | Partie du nom de l'imprimante du reçu (par exemple `TM-T20`). Vide = imprimante par défaut |
| `mosip.registration.cein.window.min-width` / `min-height` | `1280` / `720` | Taille minimale de la fenêtre. Le client s'ouvre maximisé et reste redimensionnable (avant : taille de l'écran, non redimensionnable) |

### Impression thermique du reçu

1. Imprimante : celle désignée par `printer-name`, sinon l'imprimante par défaut.
2. Papier : parmi les formats exposés par le pilote, celui dont la largeur est la plus proche
   du rouleau (± 6 mm), le plus long en priorité pour ne pas couper le reçu en pages. Marges minimales.
3. Mise en page : une feuille de style réservée à l'impression (`@media print`) est injectée dans
   le reçu. Le gabarit HTML, conçu pour l'A4, se réajuste à la largeur imprimable : tableaux
   fluides, images réduites, police compacte. L'aperçu à l'écran ne change pas.
4. Si le pilote n'expose aucun format rouleau, son format par défaut est utilisé et le contenu
   reste limité à la largeur imprimable.

Le gabarit du reçu (`reg-ack-template`) vient de la master data du serveur. Pour un rendu
optimal, on peut créer un gabarit dédié au thermique : une seule colonne, QR code centré,
pas de photo pleine largeur.

## Environnements et JAR à tester

Le client installé sur les postes est celui téléchargé depuis le serveur MOSIP
(zip 1.2.0.2). Pour appliquer la customisation, on remplace **deux JAR** dans son
dossier `lib/` :

| JAR | Contenu |
|---|---|
| `registration-client-1.2.0.2.jar` | Compilé depuis ce dépôt : toute la customisation CEIN |
| `registration-services-1.2.0.2.jar` | JAR officiel MOSIP 1.2.0.2, classes inchangées, avec la configuration de l'environnement : `props/mosip-application.properties` et les clés de `spring.properties` |

### Configuration par environnement

```
config/
  common/spring.properties.overrides            # clés communes (langue fra…)
  environments/sandbox/mosip-application.properties
  environments/_modele/mosip-application.properties   # à copier pour un nouvel environnement
```

`mosip-application.properties` porte l'hôte du serveur (`mosip.hostname`), l'URL de mise à jour
et les réglages CEIN par défaut du poste. Ces deux fichiers sont chargés au démarrage
comme valeurs par défaut ; les paramètres synchronisés depuis le serveur passent par-dessus.

`environment=LOCAL` est nécessaire tant que des JAR sont remplacés à la main : en dehors de
`LOCAL`, le client vérifie la signature et le checksum de chaque JAR par rapport au manifeste
du serveur, et re-télécharge ceux qui ne correspondent pas.

Clés présentes dans l'ancien JAR mais lues nulle part dans MOSIP 1.2.0.2, donc retirées :
`mosip.registration.language.primary`, `.secondary`, `mosip.registration.languages`,
`mosip.registration.mdm_enabled_langs`, `mosip.primary.language`,
`mosip.registration.default.language`, `mosip.registration.ui.language`,
`mosip.registration.location.lang`, `mosip.registration.master.lang`,
`mosip.registration.multilang.enabled`, `mosip.registration.language.fallback.enabled`,
`mosip.mdm.enabled`, `mosip.registration.device.biometrics.enabled`,
`mosip.registration.mds.fingerprint.capture.enable.flag`,
`mosip.registration.device.fingerprint.provider`. La langue est gérée par
`mosip.mandatory-languages=fra` et `mosip.registration.cein.forced-language=fra`.

### Générer les JAR

```bash
export JAVA_HOME=/chemin/vers/jdk-11
tools/build-dist.sh sandbox
```

Le résultat est dans `dist/sandbox/` : les deux JAR et `SHA256SUMS`. Copiez-les dans `lib/`
du client (en remplaçant les existants), puis lancez `run.bat`.

## Build

```bash
export JAVA_HOME=/chemin/vers/jdk-11
cd registration
mvn -f registration-client/pom.xml -DskipTests package
# -> registration/registration-client/target/registration-client-1.2.0.2.jar
```

Les dépendances (`registration-services 1.2.0.2`, `kernel-core 1.2.0.1`, OpenJFX 11.0.2…)
sont résolues depuis Maven Central.

Pour vérifier qu'un JAR compilé correspond au JAR de référence :

```bash
tools/compare-jars.sh <jar-référence> registration/registration-client/target/registration-client-1.2.0.2.jar
```
