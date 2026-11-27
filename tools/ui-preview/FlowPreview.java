import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import io.mosip.registration.cein.CeinFonts;
import io.mosip.registration.cein.CeinFormLayout;
import io.mosip.registration.cein.CeinShell;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * Renders the registration flow (Registration.fxml: top bar + GenericRegistrationLayout) with sample
 * screens built like GenericController / the field controls. usage: FlowPreview <resCopy> <outDir>
 */
public class FlowPreview {
	static ResourceBundle b;

	static VBox field(String label, String value, boolean error, String message) {
		Label l = new Label(label + " * "); l.getStyleClass().add("demoGraphicFieldLabel");
		TextField t = new TextField(value); t.getStyleClass().add("demoGraphicTextField");
		VBox v = new VBox(6, l, t);
		if (error) {
			t.getStyleClass().add("demoGraphicTextFieldFocused");
			Label m = new Label(message); m.getStyleClass().add("demoGraphicFieldMessageLabel");
			v.getChildren().add(m);
		}
		return v;
	}
	static VBox combo(String label, String value) {
		Label l = new Label(label + " * "); l.getStyleClass().add("demoGraphicFieldLabel");
		ComboBox<String> c = new ComboBox<>(); c.getItems().add(value); c.getSelectionModel().select(0); c.setMaxWidth(Double.MAX_VALUE);
		return new VBox(6, l, c);
	}
	static Button button(String text, String style, String svg) {
		Button bt = new Button(text); bt.getStyleClass().addAll(style, "ceinDocButton");
		SVGPath p = new SVGPath(); p.setContent(svg); p.getStyleClass().add("ceinIconStroke");
		if (style.equals("ceinDanger")) p.getStyleClass().add("ceinIconDanger");
		bt.setGraphic(p); bt.setGraphicTextGap(8); bt.setMinWidth(Region.USE_PREF_SIZE); return bt;
	}
	static GridPane docRow(String category, String type, String ref, String info, String source) {
		GridPane row = new GridPane(); row.setHgap(16); row.getStyleClass().addAll("ceinCard", "ceinDocRow"); row.setMaxWidth(Double.MAX_VALUE);
		for (double w : new double[] { 22, 27, 20, 31 }) { ColumnConstraints c = new ColumnConstraints(); c.setPercentWidth(w); row.getColumnConstraints().add(c); }
		row.getColumnConstraints().get(3).setHalignment(javafx.geometry.HPos.RIGHT);
		Label cat = new Label(category + " * "); cat.getStyleClass().addAll("demoGraphicFieldLabel", "ceinDocCategory"); cat.setWrapText(true);
		ComboBox<String> combo = new ComboBox<>(); combo.setMaxWidth(Double.MAX_VALUE); combo.getStyleClass().add("demoGraphicTextField");
		if (type != null) { combo.getItems().add(type); combo.getSelectionModel().select(0); } else combo.setPromptText(category);
		TextField refField = new TextField(ref == null ? "" : ref); refField.setPromptText(b.getString("REF_NUMBER")); refField.getStyleClass().add("demoGraphicTextField");
		HBox file;
		if (info != null) {
			SVGPath fi = new SVGPath(); fi.setContent("M14 3H6v18h12V7z M14 3v4h4"); fi.getStyleClass().add("ceinIconStroke");
			StackPane icon = new StackPane(fi); icon.getStyleClass().add("ceinDocFileIcon");
			Label i1 = new Label(info); i1.getStyleClass().add("ceinDocInfo");
			Label i2 = new Label(source); i2.getStyleClass().add("ceinDocSource");
			VBox infoBox = new VBox(2, i1, i2); HBox.setHgrow(infoBox, Priority.ALWAYS);
			file = new HBox(12, icon, infoBox,
					button(b.getString("ceinDocView"), "ceinSecondary", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0"),
					button(null, "ceinDanger", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"));
		} else {
			file = new HBox(8,
					button(b.getString("ceinScanCapture"), "ceinOutline", "M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16"),
					button(b.getString("ceinDocImport"), "ceinSecondary", "M12 16V4M7 9l5-5 5 5M4 16v4h16v-4"));
			file.setAlignment(Pos.CENTER_RIGHT);
		}
		file.setAlignment(Pos.CENTER_RIGHT);
		row.add(cat, 0, 0); row.add(combo, 1, 0); row.add(refField, 2, 0); row.add(file, 3, 0);
		return row;
	}
	static FlowPane row(VBox card) {
		FlowPane r = CeinFormLayout.row(new FlowPane()); r.prefWidthProperty().bind(card.widthProperty().subtract(48)); return r;
	}

	public static void main(String[] a) throws Exception {
		Path res = Path.of(a[0]);
		File outDir = new File(a[1]); outDir.mkdirs();
		try (var files = Files.list(res.resolve("fxml"))) {
			for (Path f : (Iterable<Path>) files::iterator) {
				Files.writeString(f, Files.readString(f, StandardCharsets.UTF_8).replaceAll("fx:controller=\"[^\"]*\"", "")
						.replaceAll("\\son[A-Z][A-Za-z]*=\"#[^\"]*\"", ""), StandardCharsets.UTF_8);
			}
		}
		try (InputStream in = Files.newInputStream(res.resolve("labels_fr.properties"))) {
			b = new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		CountDownLatch s = new CountDownLatch(1); Platform.startup(s::countDown); s.await();
		for (int step : new int[] { 1, 2 }) {
			CountDownLatch d = new CountDownLatch(1);
			Platform.runLater(() -> {
				try {
					CeinFonts.load();
					Parent page = new FXMLLoader(res.resolve("fxml/Registration.fxml").toUri().toURL(), b).load();
					GridPane flow = (GridPane) page.lookup("#genericScreen");
					Node nav = flow.lookup("#navigationAnchorPane"); nav.setVisible(false); nav.setManaged(false);
					((Button) flow.lookup("#authenticate")).setVisible(false);
					CeinShell.setTitle(page, "Nouvel enregistrement");
					((Label) page.lookup("#ceinLastSync")).setText("· dernière synchro 08:12");
					AnchorPane content = (AnchorPane) flow.lookup("#anchorPane");
					TabPane tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
					for (String n : new String[] { "Consentement", "Informations", "Documents", "Biométrie", "Aperçu", "Validation" }) {
						Tab tab = new Tab(n); CeinFormLayout.numberTab(tab, tabs.getTabs().size() + 1); tabs.getTabs().add(tab);
					}
					CeinFormLayout.trackSteps(tabs);
					AnchorPane.setTopAnchor(tabs, 0.0); AnchorPane.setLeftAnchor(tabs, 0.0); AnchorPane.setRightAnchor(tabs, 0.0); AnchorPane.setBottomAnchor(tabs, 0.0);
					content.getChildren().add(tabs);

					GridPane screen = new GridPane(); screen.getStyleClass().add("ceinFormScreen");
					ColumnConstraints cc = new ColumnConstraints(); cc.setHgrow(Priority.ALWAYS); screen.getColumnConstraints().add(cc);
					VBox column = new VBox(16); column.setFillWidth(true); GridPane.setHgrow(column, Priority.ALWAYS);
					if (step == 1) {
						column.getChildren().add(CeinFormLayout.screenHeader("Informations du demandeur", "Saisissez les informations figurant sur la pièce d'identité."));
						VBox pre = CeinFormLayout.card(null);
						Label pl = new Label(b.getString("ceinPreRegLabel")); pl.getStyleClass().addAll("demoGraphicFieldLabel", "ceinFieldLabel");
						TextField pf = new TextField(); pf.setPromptText(b.getString("ceinPreRegPrompt")); pf.getStyleClass().add("demoGraphicTextField");
						VBox fb = new VBox(6, pl, pf); fb.setPrefWidth(420);
						Button fetch = button(b.getString("ceinPreRegFetch"), "ceinOutline", "M12 3v12M7 10l5 5 5-5M4 19h16"); fetch.getStyleClass().add("ceinPreRegButton");
						Label hint = new Label(b.getString("ceinPreRegFormHint")); hint.setWrapText(true); hint.getStyleClass().add("ceinMuted"); HBox.setHgrow(hint, Priority.ALWAYS);
						HBox preRow = new HBox(12, fb, fetch, hint); preRow.setAlignment(Pos.BOTTOM_LEFT); preRow.getStyleClass().add("ceinPreReg");
						pre.getChildren().add(preRow);
						VBox identity = CeinFormLayout.card("Identité");
						FlowPane r1 = row(identity); r1.getChildren().addAll(field("Nom", "DIALLO", false, null), field("Prénom(s)", "Aminata", false, null));
						FlowPane r2 = row(identity); r2.getChildren().add(field("NIN", "198501234567", true, "Le NIN doit comporter exactement 13 chiffres."));
						identity.getChildren().addAll(r1, r2);
						VBox address = CeinFormLayout.card("Adresse");
						FlowPane r3 = row(address); r3.getChildren().addAll(combo("Région", "Dakar"), combo("Département", "Dakar"), combo("Commune", "Médina"));
						address.getChildren().add(r3);
						column.getChildren().addAll(pre, identity, address);
					} else {
						column.getChildren().add(CeinFormLayout.screenHeader("Pièces justificatives", "Une ligne par pièce demandée."));
						VBox docs = CeinFormLayout.card(null); docs.getStyleClass().add("ceinDocCard");
						docs.getChildren().add(CeinFormLayout.documentHeader(b));
						FlowPane r = row(docs);
						for (GridPane g : new GridPane[] { docRow("Preuve d'identité", "Extrait de naissance", "TH-1991-04412", "2 pages · PDF", "TH-1991-04412"),
								docRow("Preuve d'adresse", "Certificat de résidence", "DK-2025-18877", "1 page · PDF", "DK-2025-18877"),
								docRow("Preuve de date de naissance", null, null, null, null) }) {
							g.prefWidthProperty().bind(r.widthProperty()); r.getChildren().add(g);
						}
						docs.getChildren().add(r);
						column.getChildren().add(docs);
					}
					screen.add(column, 0, 0);
					ScrollPane sp = new ScrollPane(screen); sp.setFitToWidth(true); sp.getStyleClass().add("ceinFormScroll");
					tabs.getTabs().get(step).setContent(sp); tabs.getSelectionModel().select(step);

					Parent root = CeinShell.preview(page, "home", true, false);
					Scene scene = new Scene(root, 1366, 768);
					scene.getStylesheets().add(res.resolve("application-cein.css").toUri().toString());
					Stage st = new Stage(); st.setScene(scene); st.show();
					root.applyCss();
					for (String[] kv : new String[][] { { "registrationOfficerName", "Awa Ndiaye" }, { "ceinUserInitials", "AN" } }) {
						Node n = root.lookup("#" + kv[0]);
						if (n instanceof Label) ((Label) n).setText(kv[1]);
					}
					for (int i = 0; i < 3; i++) { root.applyCss(); root.layout(); }
					ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", new File(outDir, "flow" + step + ".png"));
					st.close();
					System.out.println("OK flow" + step);
				} catch (Throwable t) { t.printStackTrace(); } finally { d.countDown(); }
			});
			d.await();
		}
		Platform.exit();
	}
}
