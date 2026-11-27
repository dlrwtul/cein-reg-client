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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Renders the registration flow layout with sample cards/rows built like GenericController. */
public class FlowPreview {
	static VBox field(String label, String value, boolean error, String message) {
		Label l = new Label(label + " *"); l.getStyleClass().add("demoGraphicFieldLabel");
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
		Label l = new Label(label + " *"); l.getStyleClass().add("demoGraphicFieldLabel");
		ComboBox<String> c = new ComboBox<>(); c.getItems().add(value); c.getSelectionModel().select(0); c.setMaxWidth(Double.MAX_VALUE);
		return new VBox(6, l, c);
	}
	public static void main(String[] a) throws Exception {
		Path res = Path.of(a[0]);
		try (var files = Files.list(res.resolve("fxml"))) {
			for (Path f : (Iterable<Path>) files::iterator) {
				Files.writeString(f, Files.readString(f, StandardCharsets.UTF_8).replaceAll("fx:controller=\"[^\"]*\"", "")
						.replaceAll("\\son[A-Z][A-Za-z]*=\"#[^\"]*\"", ""), StandardCharsets.UTF_8);
			}
		}
		ResourceBundle bundle;
		try (InputStream in = Files.newInputStream(res.resolve("labels_fr.properties"))) {
			bundle = new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		CountDownLatch s = new CountDownLatch(1); Platform.startup(s::countDown); s.await();
		CountDownLatch d = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				CeinFonts.load();
				GridPane flow = new FXMLLoader(res.resolve("fxml/GenericRegistrationLayout.fxml").toUri().toURL(), bundle).load();
				AnchorPane nav = (AnchorPane) flow.lookup("#navigationAnchorPane");
				Label title = new Label("Nouvel enregistrement"); title.getStyleClass().add("navigationLabel");
				nav.getChildren().add(title); AnchorPane.setTopAnchor(title, 5.0); AnchorPane.setLeftAnchor(title, 10.0);
				((Button) flow.lookup("#authenticate")).setVisible(false);
				AnchorPane content = (AnchorPane) flow.lookup("#anchorPane");
				TabPane tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
				for (String n : new String[] { "Consentement", "Informations", "Documents", "Biométrie", "Aperçu", "Validation" }) {
					Tab tab = new Tab(n); CeinFormLayout.numberTab(tab, tabs.getTabs().size() + 1); tabs.getTabs().add(tab);
				}
				AnchorPane.setTopAnchor(tabs, 0.0); AnchorPane.setLeftAnchor(tabs, 0.0); AnchorPane.setRightAnchor(tabs, 0.0); AnchorPane.setBottomAnchor(tabs, 0.0);
				content.getChildren().add(tabs);

				GridPane screen = new GridPane(); screen.getStyleClass().add("ceinFormScreen");
				ColumnConstraints cc = new ColumnConstraints(); cc.setHgrow(Priority.ALWAYS); screen.getColumnConstraints().add(cc);
				VBox column = new VBox(20); column.setFillWidth(true); GridPane.setHgrow(column, Priority.ALWAYS);
				VBox identity = CeinFormLayout.card("Identité"); CeinFormLayout.watchCard(identity);
				FlowPane r1 = CeinFormLayout.row(new FlowPane()); r1.prefWidthProperty().bind(identity.widthProperty().subtract(48));
				r1.getChildren().addAll(field("Nom", "DIALLO", false, null), field("Prénom(s)", "Aminata", false, null));
				FlowPane r2 = CeinFormLayout.row(new FlowPane()); r2.prefWidthProperty().bind(identity.widthProperty().subtract(48));
				r2.getChildren().add(field("NIN", "198501234567", true, "Le NIN doit comporter exactement 13 chiffres."));
				identity.getChildren().addAll(r1, r2);
				VBox address = CeinFormLayout.card("Adresse"); CeinFormLayout.watchCard(address);
				FlowPane r3 = CeinFormLayout.row(new FlowPane()); r3.prefWidthProperty().bind(address.widthProperty().subtract(48));
				r3.getChildren().addAll(combo("Région", "Dakar"), combo("Département", "Dakar"), combo("Commune", "Médina"));
				address.getChildren().add(r3);
				column.getChildren().addAll(identity, address);
				screen.add(column, 0, 0);
				ScrollPane sp = new ScrollPane(screen); sp.setFitToWidth(true); sp.getStyleClass().add("ceinFormScroll");
				tabs.getTabs().get(1).setContent(sp); tabs.getSelectionModel().select(1);

				Parent root = CeinShell.preview(flow, "home", true, false);
				Scene scene = new Scene(root, 1366, 768);
				scene.getStylesheets().add(res.resolve("application-cein.css").toUri().toString());
				Stage st = new Stage(); st.setScene(scene); st.show();
				for (int i = 0; i < 3; i++) { root.applyCss(); root.layout(); }
				ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", new File(a[1]));
				System.out.println("OK flow");
			} catch (Throwable t) { t.printStackTrace(); } finally { d.countDown(); }
		});
		d.await(); Platform.exit();
	}
}
