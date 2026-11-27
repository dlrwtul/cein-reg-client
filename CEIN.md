# CEIN Registration Client

Client d'enregistrement MOSIP customisé pour la CEIN (Sénégal).

## Base

| | |
|---|---|
| Amont | [mosip/registration-client](https://github.com/mosip/registration-client) |
| Version | **1.2.0.2** (dernière version stable de la ligne LTS 1.2.0.x) |
| Commit amont | `84eb683b93bbe0c34f10d714fb16ad3bcb99dae7` (« [DSD-4982] 1.2.0.2 release changes », 1 commit après le tag `v1.2.0.2`) |
| Tag local | `upstream/v1.2.0.2` = import brut de l'amont, sans customisation |
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

## Delta par rapport à MOSIP 1.2.0.2

Chaque point correspond à un commit (`git log upstream/v1.2.0.2..`).

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
via `git show upstream/v1.2.0.2:<chemin>`.

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
