import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import io.mosip.registration.preloader.ClientPreLoader;
import io.mosip.registration.preloader.ClientPreLoaderNotification;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;

/** Renders the start-up window (ClientPreLoader) with a few real notifications. usage: PreloaderPreview <out.png> */
public class PreloaderPreview {
	public static void main(String[] args) throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		Platform.startup(started::countDown);
		started.await();
		CountDownLatch done = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				ClientPreLoader p = new ClientPreLoader();
				Stage stage = new Stage();
				p.start(stage);
				for (String m : new String[] { "Client integrity check successful.", "Creating application context...",
						"Created application context.", "Language setup complete.",
						"Properties with local preferences loaded.", "Checking server connectivity...", "Machine is ONLINE",
						"Biometric device scanning started..." }) {
					p.handleApplicationNotification(new ClientPreLoaderNotification(m));
				}
				javafx.scene.Parent root = stage.getScene().getRoot();
				stage.getScene().setRoot(new javafx.scene.Group());
				javafx.scene.Scene sized = new javafx.scene.Scene(root, 820, 660);
				sized.getStylesheets().add(ClientPreLoader.class.getResource("/application-cein.css").toExternalForm());
				root.applyCss();
				root.layout();
				ImageIO.write(SwingFXUtils.fromFXImage(sized.snapshot(null), null), "png", new File(args[0]));
				System.out.println("OK preloader");
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				done.countDown();
			}
		});
		done.await();
		Platform.exit();
	}
}
