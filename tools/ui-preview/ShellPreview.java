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
import io.mosip.registration.cein.CeinShell;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Renders an FXML screen inside the CEIN shell. usage: ShellPreview <resCopy> <out.png> <fxml> <active> <collapsed> */
public class ShellPreview {
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
		CountDownLatch s = new CountDownLatch(1);
		Platform.startup(s::countDown);
		s.await();
		CountDownLatch d = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				CeinFonts.load();
				Parent screen = new FXMLLoader(res.resolve("fxml").resolve(a[2] + ".fxml").toUri().toURL(), bundle).load();
				Parent root = CeinShell.preview(screen, a[3], true, Boolean.parseBoolean(a[4]));
				Scene scene = new Scene(root, 1366, 768);
				scene.getStylesheets().add(res.resolve("application-cein.css").toUri().toString());
				Stage st = new Stage();
				st.setScene(scene);
				st.show();
				root.applyCss();
				root.layout();
				ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", new File(a[1]));
				System.out.println("OK " + a[2]);
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				d.countDown();
			}
		});
		d.await();
		Platform.exit();
	}
}
