import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/**
 * Sample data for FxmlPreview (-Dpreview.demo=true): fills the nodes that the controllers populate
 * at run time, so that the rendering shows a screen in use. Mirrors what the controllers do
 * (style classes, texts); it is only a preview aid.
 */
final class PreviewDemo {

	private PreviewDemo() {
	}

	private static java.util.Map<String, Object> ns;

	static Parent fill(String name, Parent root, ResourceBundle b, java.util.Map<String, Object> namespace) {
		ns = namespace;
		switch (name) {
			case "BiometricsCapture":
				return biometrics(root, b);
			case "Scan":
				scan(root, b);
				return root;
			case "OperatorAuthentication": {
				((Label) find(root, "ceinAuthOperatorId")).setText("operateur.dk01");
				((Label) find(root, "ceinAuthOperatorName")).setText("Awa Ndiaye");
				Label st = find(root, "ceinAuthOperatorStatus");
				st.setText(b.getString("ceinAuthDone_PWD"));
				st.getStyleClass().add("ceinAuthStatusOk");
				((Node) find(root, "ceinAuthOperatorNumber")).setVisible(false);
				Node check = find(root, "ceinAuthOperatorCheck");
				check.setVisible(true);
				check.setManaged(true);
				((Node) find(root, "ceinAuthOperatorBadge")).getStyleClass().add("ceinAuthBadgeDone");
				VBox panes = find(root, "ceinAuthPanes");
				((Pane) panes.getParent()).getChildren().remove(panes);
				((VBox) find(root, "ceinAuthSupervisorSlot")).getChildren().add(panes);
				Node sup = find(root, "ceinAuthSupervisorCard");
				sup.setVisible(true);
				sup.getStyleClass().add("ceinAuthCurrent");
				((Node) find(root, "ceinAuthSupervisorBadge")).getStyleClass().add("ceinAuthBadgeCurrent");
				((Node) find(root, "pwdBasedLogin")).setVisible(true);
				((Label) find(root, "pwdLabel")).setText(b.getString("reviewerPwdAuth"));
				((javafx.scene.control.TextField) find(root, "username")).setText("superviseur.dk");
				((javafx.scene.control.TextField) find(root, "password")).setText("motdepasse");
				((Node) find(root, "backButton")).setVisible(false);
				return root;
			}
			case "RegistrationLogin": {
				Label st = find(root, "ceinStationChip"); st.setText(MessageFormat.format(b.getString("ceinStationChip"), "DK-PLT-014")); st.setVisible(true);
				Label ce = find(root, "ceinCenterChip"); ce.setText("Centre Dakar Plateau"); ce.setVisible(true);
				((Label) find(root, "versionValueLabel")).setText("1.2.0.2-cein");
				return root;
			}
			case "RegistrationOfficerLayout":
				home(root, b);
				return root;
			case "AckReceipt":
				((Label) find(root, "ceinAckRid")).setText("10001100010002 20261127104230");
				((Label) find(root, "ceinAckApplicant")).setText("Aminata DIALLO");
				((Label) find(root, "ceinAckPrintNote")).setText(MessageFormat.format(b.getString("ceinAckPrintNoteRoll"), 80));
				((Label) find(root, "ceinAckPreviewTitle")).setText(b.getString("ceinAckPreview") + " \u00B7 80 mm");
				return root;
			default:
				return root;
		}
	}

	private static <T extends Node> T find(Parent root, String id) {
		@SuppressWarnings("unchecked")
		T node = (T) (ns != null && ns.containsKey(id) ? ns.get(id) : root.lookup("#" + id));
		return node;
	}

	private static Parent biometrics(Parent root, ResourceBundle b) {
		((Label) find(root, "biometricType")).setText(b.getString("FINGERPRINT_SLAB_LEFT"));
		((Label) find(root, "ceinBioInstruction")).setText(b.getString("ceinBioHint_FINGERPRINT_SLAB_LEFT"));
		((Label) find(root, "ceinBioQualityTitle")).setText(MessageFormat.format(b.getString("ceinQualityAttempt"), 2));
		((Label) find(root, "qualityText")).setText("72%");
		ProgressBar bar = find(root, "bioProgress");
		bar.setProgress(0.72);
		bar.getStyleClass().add("progress-barGreen");
		((Label) find(root, "thresholdLabel")).setText(MessageFormat.format(b.getString("ceinThresholdRequired"), 60));
		Label verdict = find(root, "ceinBioVerdict");
		verdict.setText(b.getString("ceinAboveThreshold"));
		verdict.getStyleClass().add("labelGreen");
		((Label) find(root, "ceinBioAttemptsTitle")).setText(MessageFormat.format(b.getString("ceinAttemptsUsed"), 2, 3));
		HBox attempts = find(root, "bioRetryBox");
		String[][] data = { { "52", "qualityLabelRed" }, { "72", "qualityLabelGreen" }, { "—", "qualityLabelGrey" } };
		for (int i = 0; i < data.length; i++) {
			Label l = new Label(MessageFormat.format(b.getString("ceinAttemptN"), i + 1) + "\n" + data[i][0]);
			l.getStyleClass().setAll(data[i][1]);
			if (i == 1) l.getStyleClass().add("qualityLabelBlueBorder");
			l.setMaxWidth(Double.MAX_VALUE);
			l.setTextAlignment(TextAlignment.CENTER);
			HBox.setHgrow(l, Priority.ALWAYS);
			attempts.getChildren().add(l);
		}
		GridPane exceptions = find(root, "checkBoxPane");
		Label t = new Label(b.getString("exceptionCheckBoxPaneLabel"));
		t.getStyleClass().add("demoGraphicFieldLabel");
		HBox fingers = new HBox(10);
		for (int i = 0; i < 4; i++) {
			Rectangle r = new Rectangle(34, 70 + (i == 1 || i == 2 ? 20 : 0), Color.web(i == 3 ? "#FFFFFF" : "#2E3B47"));
			r.setArcWidth(30);
			r.setArcHeight(30);
			if (i == 3) {
				r.setStroke(Color.web("#B3261E"));
				r.getStrokeDashArray().addAll(4.0, 4.0);
			}
			fingers.getChildren().add(r);
		}
		fingers.setAlignment(Pos.BOTTOM_LEFT);
		exceptions.add(new VBox(6, t, fingers), 0, 0);
		Rectangle print = new Rectangle(200, 230, Color.web("#C4CED6"));
		print.setArcWidth(40);
		print.setArcHeight(40);
		((Pane) find(root, "biometricPane")).getChildren().add(print);
		((Label) find(root, "qualityScore")).setText("72%");
		((Label) find(root, "sdkQualityText")).setText("70%");

		// modality list as built by BiometricFxControl
		VBox list = new VBox();
		list.getStyleClass().addAll("ceinCard", "ceinBioModalities");
		String[][] rows = { { "FACE", "ceinBioStatusCaptured", "81", "ok" }, { "FINGERPRINT_SLAB_LEFT", "ceinBioStatusCapturedExceptions", "1", "" },
				{ "FINGERPRINT_SLAB_RIGHT", "ceinBioStatusTodo", "", "" }, { "FINGERPRINT_SLAB_THUMBS", "ceinBioStatusTodo", "", "" },
				{ "IRIS_DOUBLE", "ceinBioStatusTodo", "", "" }, { "EXCEPTION_PHOTO", "ceinBioStatusPhotoRequired", "", "warn" } };
		for (String[] row : rows) {
			Label title = new Label(b.getString(row[0]));
			title.getStyleClass().add("ceinModalityTitle");
			Label status = new Label(MessageFormat.format(b.getString(row[1]), row[2].isEmpty() ? (Object) "" : (Object) Integer.valueOf(row[2])));
			status.getStyleClass().add("ceinModalityStatus");
			if (row[3].equals("ok")) status.getStyleClass().add("ceinModalityOk");
			if (row[3].equals("warn")) status.getStyleClass().add("ceinModalityWarn");
			Button button = new Button();
			button.setGraphic(new VBox(2, title, status));
			button.setMaxWidth(Double.MAX_VALUE);
			button.getStyleClass().addAll("modalityButtons", "ceinModality");
			if (row[0].equals("FINGERPRINT_SLAB_LEFT")) button.getStyleClass().add("ceinModalitySelected");
			HBox h = new HBox(button);
			HBox.setHgrow(button, Priority.ALWAYS);
			list.getChildren().add(h);
		}
		Label note = new Label(b.getString("ceinBioModalitiesNote"));
		note.setWrapText(true);
		note.getStyleClass().add("ceinBioModalitiesNote");
		list.getChildren().add(note);

		GridPane field = new GridPane();
		field.setHgap(20);
		field.setPadding(new Insets(24, 32, 24, 32));
		field.setStyle("-fx-background-color: #F4F6F5;");
		ColumnConstraints c1 = new ColumnConstraints(220, 260, 280);
		ColumnConstraints c2 = new ColumnConstraints();
		c2.setHgrow(Priority.ALWAYS);
		field.getColumnConstraints().addAll(c1, c2);
		field.add(list, 0, 0);
		field.add(root, 1, 0);
		GridPane.setValignment(list, javafx.geometry.VPos.TOP);
		GridPane.setValignment(root, javafx.geometry.VPos.TOP);
		return field;
	}

	private static void scan(Parent root, ResourceBundle b) {
		((Label) find(root, "ceinScanTitle")).setText("Preuve d'identité — Extrait de naissance");
		((Label) find(root, "ceinScanSubtitle")).setText(MessageFormat.format(b.getString("ceinScanPageCount"), 2, 2));
		((Label) find(root, "ceinScanHint")).setText(MessageFormat.format(b.getString("ceinScanHint"), 5));
		@SuppressWarnings("unchecked")
		ComboBox<String> source = find(root, "ceinSource");
		source.getItems().setAll(b.getString("ceinScanSourceScanner"), b.getString("ceinScanSourceCamera"));
		source.getSelectionModel().select(0);
		HBox thumbs = find(root, "ceinThumbs");
		for (int i = 1; i <= 2; i++) {
			Rectangle page = new Rectangle(38, 50, Color.web("#EEF1F3"));
			Label n = new Label(String.valueOf(i));
			n.getStyleClass().add("ceinScanThumbNumber");
			VBox box = new VBox(2, page, n);
			box.setAlignment(Pos.BOTTOM_CENTER);
			box.getStyleClass().add("ceinScanThumb");
			if (i == 2) box.getStyleClass().add("ceinScanThumbSelected");
			thumbs.getChildren().add(box);
		}
		Rectangle doc = new Rectangle(300, 410, Color.WHITE);
		((javafx.scene.Group) find(root, "imageGroup")).getChildren().setAll(doc);
		((javafx.scene.text.Text) find(root, "scanningMsg")).setText("Fichier chargé : extrait-naissance.pdf (2 pages)");
	}

	private static void setText(Parent root, String id, String text) {
		Node n = root.lookup("#" + id);
		if (n instanceof Label) ((Label) n).setText(text);
	}

	private static void home(Parent root, ResourceBundle b) {
		Node content = root.lookup("#homeContent");
		if (content != null) content.setVisible(true);
		setText(root, "ceinPageTitle", b.getString("ceinHomeTitle"));
		setText(root, "ceinLastSync", MessageFormat.format(b.getString("ceinLastSyncShort"), "08:12"));
		setText(root, "registrationOfficerName", "Awa Ndiaye");
		setText(root, "ceinUserInitials", "AN");
		root.applyCss();
		setText(root, "ceinGreeting", MessageFormat.format(b.getString("ceinGreeting"), "Awa"));
		setText(root, "lastSyncTime", "aujourd'hui 08:12");
		setText(root, "versionValueLabel", "1.2.0.2-cein");
		setText(root, "ceinUploadCount", "12");
		setText(root, "ceinApprovalCount", "3");
		setText(root, "ceinReRegCount", "1");
		GridPane tiles = (GridPane) root.lookup("#registrationGridPane");
		if (tiles == null) return;
		String[][] data = { { "Nouvel enregistrement", "NEW" }, { "Mise à jour UIN", "UPDATE" }, { "Carte perdue", "LOST" }, { "Correction biométrique", "CORRECTION" } };
		for (int i = 0; i < 4; i++) {
			ColumnConstraints c = new ColumnConstraints();
			c.setPercentWidth(25);
			tiles.getColumnConstraints().add(c);
		}
		String[] icons = { "M13 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M2 21c0-3.9 3.1-7 7-7s7 3.1 7 7M19 8v6M16 11h6",
				"M3 5h18v14H3z M11 11a2 2 0 1 1-4 0 2 2 0 0 1 4 0 M6 16c.6-1.4 1.7-2 3-2s2.4.6 3 2M15 10h3M15 13h3",
				"M18 11a7 7 0 1 1-14 0 7 7 0 0 1 14 0 M21 21l-4.3-4.3",
				"M12 11v2.5c0 3-1 5.5-2.5 7.5M8 5.6A7 7 0 0 1 19 11v2M5 9a7 7 0 0 0-.4 2.3v2M15.5 13c0 3-.6 5.5-1.8 7.6M8.6 11a3.5 3.5 0 0 1 6.9 0" };
		for (int i = 0; i < data.length; i++) {
			GridPane tile = new GridPane();
			tile.getStyleClass().addAll("operationalPaneDetailsSync", "ceinProcessTile");
			if (i == 0) tile.getStyleClass().add("ceinProcessTilePrimary");
			tile.setMaxWidth(Double.MAX_VALUE);
			javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
			icon.setContent(icons[i]);
			icon.getStyleClass().addAll("ceinIconStroke", "ceinProcessIcon");
			Label t = new Label(data[i][0]);
			t.getStyleClass().addAll("operationalTitle", "ceinProcessTileTitle");
			Label c = new Label(b.getString("ceinProcessHint_" + data[i][1]));
			c.getStyleClass().add("ceinProcessTileCaption");
			VBox v = new VBox(14, new javafx.scene.layout.StackPane(icon), new VBox(4, t, c));
			((javafx.scene.layout.StackPane) v.getChildren().get(0)).setAlignment(Pos.TOP_LEFT);
			tile.add(v, 0, 0);
			GridPane.setHgrow(v, Priority.ALWAYS);
			tiles.add(tile, i, 0);
		}
	}
}
