package io.mosip.registration.preloader;

import io.mosip.registration.cein.CeinFonts;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.update.ClientSetupValidator;
import javafx.application.Preloader;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientPreLoader extends Preloader {

    private static final Logger logger = LoggerFactory.getLogger(ClientPreLoader.class);

    public static boolean errorsFound = false;
    public static boolean restartRequired = false;
    private Stage preloaderStage;
    private ProgressBar progressBar = new ProgressBar();
    private TextArea textArea = new TextArea();
    private Label label = new Label("Démarrage en cours…");
    private Button stopClient = new Button("Arrêter le client");
    private Button hidePreLoader = new Button("Fermer");

    // CEIN: readable start-up steps (the raw technical log stays available below)
    private final VBox steps = new VBox(8);

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        CeinFonts.load();

        // ---- header: flag + title + version
        Label title = new Label("Démarrage du client d'enregistrement");
        title.getStyleClass().add("ceinPreloaderTitle");
        String implementationVersion = ClientPreLoader.class.getPackage() == null ? null
                : ClientPreLoader.class.getPackage().getImplementationVersion();
        Label version = new Label("CEIN · version " + (implementationVersion == null ? "1.2.0.2" : implementationVersion));
        version.getStyleClass().add("ceinMuted");
        VBox titles = new VBox(2, title, version);
        HBox header = new HBox(14, flag(), titles);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- current status + progress
        label.getStyleClass().add("ceinPreloaderStatus");
        label.setWrapText(true);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);

        // ---- technical log, collapsed
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(140);
        textArea.getStyleClass().add("ceinPreloaderLog");
        TitledPane log = new TitledPane("Afficher le journal technique", textArea);
        log.setExpanded(false);
        log.setAnimated(false);
        log.getStyleClass().add("ceinPreloaderLogPane");

        // ---- actions
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        stopClient.getStyleClass().add("ceinDanger");
        hidePreLoader.getStyleClass().add("ceinPrimary");
        HBox buttons = new HBox(12, spacer, stopClient, hidePreLoader);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        stopClient.setVisible(false);
        stopClient.setManaged(false);
        stopClient.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                textArea.appendText("Exiting from application...\n");
                System.exit(0);
            }
        });

        hidePreLoader.setVisible(false);
        hidePreLoader.setManaged(false);
        hidePreLoader.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                preloaderStage.hide();
            }
        });

        // ---- status line: current step and "Étape n sur 7" (design "Demarrage")
        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        stepCounter.getStyleClass().add("ceinMuted");
        HBox status = new HBox(12, label, statusSpacer, stepCounter);
        status.setAlignment(Pos.CENTER_LEFT);
        VBox progress = new VBox(8, status, progressBar);
        for (String name : STAGES) {
            Label stage = new Label(name);
            stage.getStyleClass().addAll("ceinStep", "ceinStepPending");
            Label dot = new Label("\u00B7");
            dot.setMinWidth(16);
            dot.setAlignment(Pos.CENTER);
            stage.setGraphic(dot);
            stage.setGraphicTextGap(10);
            stageLabels.add(stage);
            steps.getChildren().add(stage);
        }
        stepCounter.setText("");

        VBox card = new VBox(20, header, progress, steps, log, buttons);
        card.getStyleClass().addAll("ceinCard", "ceinPreloaderCard");
        card.setMaxWidth(640);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane root = new StackPane();
        root.getStyleClass().add("ceinPreloaderRoot");
        try {
            ImageView background = new ImageView(new Image(getClass().getResource("/images/LoginBackGround.png").toExternalForm()));
            background.setOpacity(0.22);
            background.setPreserveRatio(false);
            background.fitWidthProperty().bind(root.widthProperty());
            background.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(background);
        } catch (Exception e) {
            logger.warn("Preloader background not found");
        }
        root.getChildren().add(card);
        StackPane.setMargin(card, new Insets(24));

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/application-cein.css").toExternalForm());
        } catch (Exception e) {
            logger.warn("CEIN stylesheet not found for the preloader");
        }
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setWidth(820);
        primaryStage.setHeight(660);
        primaryStage.setResizable(true);
        primaryStage.setTitle("Client d'enregistrement - démarrage");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResource(RegistrationConstants.LOGO).toExternalForm()));
        primaryStage.show();
    }

    /** Senegal flag drawn with shapes (no image needed). */
    private static HBox flag() {
        HBox flag = new HBox();
        Region g = new Region(); g.setStyle("-fx-background-color: #00853F;");
        Region y = new Region(); y.setStyle("-fx-background-color: #FDEF42;");
        Region r = new Region(); r.setStyle("-fx-background-color: #E31B23;");
        for (Region band : new Region[] { g, y, r }) {
            band.setPrefSize(16, 32);
            band.setMinSize(16, 32);
        }
        StackPane middle = new StackPane(y, star());
        flag.getChildren().addAll(g, middle, r);
        flag.setMinSize(48, 32);
        flag.setMaxSize(48, 32);
        return flag;
    }

    private static javafx.scene.shape.SVGPath star() {
        javafx.scene.shape.SVGPath star = new javafx.scene.shape.SVGPath();
        star.setContent("M8 2 L9.8 7.4 L15.5 7.4 L10.9 10.8 L12.6 16.2 L8 12.9 L3.4 16.2 L5.1 10.8 L0.5 7.4 L6.2 7.4 Z");
        star.setStyle("-fx-fill: #00853F;");
        star.setScaleX(0.8);
        star.setScaleY(0.8);
        return star;
    }

    /** The 7 start-up stages shown to the operator (design "Demarrage"). */
    private static final String[] STAGES = {
            "Vérification de l'intégrité du client",
            "Base de données locale",
            "Langues et préférences locales",
            "Connexion au serveur",
            "Périphériques biométriques",
            "Synchronisation des paramètres",
            "Démarrage des tâches planifiées" };
    private final java.util.List<Label> stageLabels = new java.util.ArrayList<>();
    private final Label stepCounter = new Label();
    private int currentStage = -1;

    /** Moves the stage list forward for a known technical message (unknown messages only go to the log). */
    private void addStep(String message) {
        int stage = stageOf(message);
        if (stage < 0) {
            return;
        }
        String detail = detailOf(message);
        if (detail != null) {
            stageLabels.get(stage).setText(STAGES[stage] + " : " + detail);
        }
        if (stage < currentStage) {
            return;
        }
        for (int i = 0; i < stage; i++) {
            setStageDone(i);
        }
        currentStage = stage;
        Label current = stageLabels.get(stage);
        current.getStyleClass().removeAll("ceinStepPending", "ceinStepDone");
        if (!current.getStyleClass().contains("ceinStepCurrent")) {
            current.getStyleClass().add("ceinStepCurrent");
        }
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(16, 16);
        spinner.setMaxSize(16, 16);
        current.setGraphic(spinner);
        label.setText(STAGES[stage] + "…");
        stepCounter.setText("Étape " + (stage + 1) + " sur " + STAGES.length);
        progressBar.setProgress((double) stage / STAGES.length);
    }

    private void setStageDone(int index) {
        Label stage = stageLabels.get(index);
        if (stage.getStyleClass().contains("ceinStepDone")) {
            return;
        }
        stage.getStyleClass().removeAll("ceinStepPending", "ceinStepCurrent");
        stage.getStyleClass().add("ceinStepDone");
        javafx.scene.shape.SVGPath check = new javafx.scene.shape.SVGPath();
        check.setContent("M2 8 L6 12 L14 3");
        check.setStyle("-fx-fill: transparent; -fx-stroke: #006B33; -fx-stroke-width: 2.4; -fx-stroke-line-cap: round; -fx-stroke-line-join: round;");
        StackPane box = new StackPane(check);
        box.setPrefSize(16, 16);
        box.setMinSize(16, 16);
        stage.setGraphic(box);
    }

    private void markCurrentStepDone() {
        for (int i = 0; i < stageLabels.size(); i++) {
            setStageDone(i);
        }
        stepCounter.setText("");
    }

    private static int stageOf(String message) {
        if (message == null) return -1;
        String m = message.trim();
        if (m.startsWith("Started to validate the build setup") || m.startsWith("Client integrity check successful")) return 0;
        if (m.startsWith("Creating application context") || m.startsWith("Checking for any DB upgrades")
                || m.startsWith("Nothing to be upgraded")) return 1;
        if (m.startsWith("Language setup complete") || m.startsWith("Properties with local preferences")) return 2;
        if (m.startsWith("Checking server connectivity") || m.startsWith("Machine is ONLINE") || m.startsWith("Machine is OFFLINE")) return 3;
        if (m.startsWith("Biometric device scanning started")) return 4;
        if (m.startsWith("Client settings startup sync started") || m.startsWith("Warning : ** NO VALID AUTH-TOKEN")) return 5;
        if (m.startsWith("Job scheduler started")) return 6;
        return -1;
    }

    private static String detailOf(String message) {
        String m = message.trim();
        if (m.startsWith("Machine is ONLINE")) return "en ligne";
        if (m.startsWith("Machine is OFFLINE")) return "hors ligne";
        if (m.startsWith("Nothing to be upgraded")) return "aucune mise à niveau";
        if (m.startsWith("Warning : ** NO VALID AUTH-TOKEN")) return "reportée à la connexion";
        if (m.startsWith("Biometric device scanning started")) return "ports 4501 à 4600";
        return null;
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        return true;
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if(info instanceof ClientPreLoaderErrorNotification) {
            errorsFound = true;
            Throwable t = ((ClientPreLoaderErrorNotification)info).getCause();
            textArea.appendText(t.getMessage()+"\n");
            label.setText("Le démarrage a échoué : " + t.getMessage());
            label.getStyleClass().add("ceinError");
            hidePreLoader.setVisible(false);
            hidePreLoader.setManaged(false);
            stopClient.setVisible(true);
            stopClient.setManaged(true);
            logger.error(t.getMessage(), t);
            return;
        }

        if(info instanceof ClientPreLoaderNotification) {
            String message = ((ClientPreLoaderNotification)info).getMessage();
            textArea.appendText(message);
            textArea.appendText("\n");
            addStep(message);
            return;
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification) {
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_LOAD) {
            textArea.appendText("Started to validate the build setup...\n");
            addStep("Started to validate the build setup");
            try {
                progressBar.setProgress(0.1);
                ClientSetupValidator clientSetupValidator = new ClientSetupValidator();
                clientSetupValidator.validateBuildSetup();
                if(clientSetupValidator.isPatch_downloaded()) {
                    restartRequired = true;
                    throw new RegBaseCheckedException("","New patches downloaded, Kindly restart the client");
                }
                if(clientSetupValidator.isUnknown_jars_found()) {
                    restartRequired = true;
                    throw new RegBaseCheckedException("","Unrecognized jars found in the classpath, Kindly restart the client");
                }
                else
                    errorsFound = clientSetupValidator.isValidationFailed();

            } catch (RegBaseCheckedException e) {
                errorsFound = true;
                notifyPreloader(new ClientPreLoaderErrorNotification(e));
            }
            textArea.appendText("Build setup validation completed with status : " + (errorsFound ? "FAILURE" : "SUCCESS") + "\n");
        }
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_INIT) {
            if(!errorsFound) {
                textArea.appendText("Started to initialize application...\n");
                progressBar.setProgress(0.3);
            }
        }
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_START) {
            if(restartRequired) {
                label.setText("Redémarrage nécessaire : des mises à jour du client ont été téléchargées. Fermez puis relancez le client.");
                stopClient.setText("Quitter");
                stopClient.setVisible(true);
                stopClient.setManaged(true);
            }
            else if(errorsFound) {
                label.setText("Le démarrage a échoué. Consultez le journal technique.");
                if (!label.getStyleClass().contains("ceinError")) label.getStyleClass().add("ceinError");
                stopClient.setVisible(true);
                stopClient.setManaged(true);
            }
            else {
                textArea.appendText("Setting up application stage...\n");
                progressBar.setProgress(100);
                markCurrentStepDone();
                label.setText("Client démarré.");
                textArea.appendText("Registration client started :)\n");
                hidePreLoader.setVisible(true);
                hidePreLoader.setManaged(true);
                stopClient.setVisible(true);
                stopClient.setManaged(true);
            }
        }
    }
}
