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

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Renders FXML screens of the registration client off-screen (no controller, no Spring) with a
 * given stylesheet, to check the visual design. Event handlers and fx:controller are stripped.
 *
 * usage: java FxmlPreview <resourcesCopyDir> <css> <outDir> <width> <height> <fxml>...
 *   resourcesCopyDir: a COPY of src/main/resources (FXML files are rewritten in place)
 */
public class FxmlPreview {

	public static void main(String[] args) throws Exception {
		Path res = Path.of(args[0]);
		String css = args[1];
		File out = new File(args[2]);
		int w = Integer.parseInt(args[3]);
		int h = Integer.parseInt(args[4]);
		out.mkdirs();

		File fonts = res.resolve("fonts").toFile();
		if (fonts.isDirectory()) {
			for (File f : fonts.listFiles((d, n) -> n.endsWith(".ttf"))) {
				Font.loadFont(f.toURI().toString(), 12);
			}
		}
		ResourceBundle bundle;
		try (InputStream in = Files.newInputStream(res.resolve("labels_fr.properties"))) {
			bundle = new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
		}

		// strip controllers and handlers from every FXML (fx:include'd files too)
		try (var files = Files.list(res.resolve("fxml"))) {
			for (Path f : (Iterable<Path>) files::iterator) {
				if (!f.toString().endsWith(".fxml")) continue;
				Files.writeString(f, Files.readString(f, StandardCharsets.UTF_8)
						.replaceAll("fx:controller=\"[^\"]*\"", "")
						.replaceAll("\\son[A-Z][A-Za-z]*=\"#[^\"]*\"", ""), StandardCharsets.UTF_8);
			}
		}

		CountDownLatch started = new CountDownLatch(1);
		Platform.startup(started::countDown);
		started.await();

		for (int i = 5; i < args.length; i++) {
			String name = args[i];
			Path fxml = res.resolve("fxml").resolve(name + ".fxml");
			String text = Files.readString(fxml, StandardCharsets.UTF_8)
					.replaceAll("fx:controller=\"[^\"]*\"", "")
					.replaceAll("\\son[A-Z][A-Za-z]*=\"#[^\"]*\"", "");
			Files.writeString(fxml, text, StandardCharsets.UTF_8);

			CountDownLatch done = new CountDownLatch(1);
			Platform.runLater(() -> {
				try {
					FXMLLoader loader = new FXMLLoader(fxml.toUri().toURL(), bundle);
					Parent root = loader.load();
					if (Boolean.getBoolean("preview.demo")) {
						root = PreviewDemo.fill(name, root, bundle, loader.getNamespace());
					}
					Scene scene = new Scene(root, w, h);
					scene.getStylesheets().add(res.resolve(css).toUri().toString());
					Stage stage = new Stage();
					stage.setScene(scene);
					stage.show();
					root.applyCss();
					root.layout();
					WritableImage img = scene.snapshot(null);
					ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(out, name + ".png"));
					stage.close();
					System.out.println("OK   " + name);
				} catch (Throwable t) {
					System.out.println("FAIL " + name + " : " + t);
				} finally {
					done.countDown();
				}
			});
			done.await();
		}
		Platform.exit();
	}
}
