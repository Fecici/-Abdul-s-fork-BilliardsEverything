package billiards.viewer;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;

public class Updater {
    public static void checkForUpdates(String currentVersion) throws Exception {
        String versionUrl = "https://raw.githubusercontent.com/suryansh55/billiardeverythingupdate/main/version.txt";
        String latestVersion = new String(
            java.net.URI.create(versionUrl)
                .toURL()
                .openStream()
                .readAllBytes()
        ).trim();
        System.out.println(currentVersion);

        if (!currentVersion.equals(latestVersion)) {
            System.out.println("Update available: " + latestVersion);

            URL downloadUrl = new URL("https://github.com/suryansh55/billiardeverythingupdate/releases/download/Jar/BilliardViewer-update.zip");

            Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), "BilliardViewer-update.zip");
            try (InputStream in = downloadUrl.openStream()) {
                Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Get the directory where the current JAR file is located (Contents/app)
            Path appDir = Paths.get(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            Path targetJarPath = appDir.resolve("billiard-viewer.jar");

            String osName = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (osName.contains("win")) {
                // Windows uses a .bat script and cmd
                Path updaterScriptPath = appDir.resolve("updater.bat");
                pb = new ProcessBuilder(
                    "cmd", "/c", updaterScriptPath.toString(),
                    targetJarPath.toString(),
                    tempPath.toString()
                );
            } else {
                // macOS and Linux use a .sh script and bash
                Path updaterScriptPath = appDir.resolve("updater.sh");
                pb = new ProcessBuilder(
                    "bash", updaterScriptPath.toString(),
                    targetJarPath.toString(),
                    tempPath.toString()
                );
            }

            pb.inheritIO().start().waitFor();

            // Display a pop-up message on the JavaFX thread
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Update Downloaded");
                alert.setHeaderText("New Version Available");
                alert.setContentText("A new version of the application has been downloaded. Please restart the application to apply the update.");
                alert.showAndWait();
            });

        } else {
            System.out.println("You are up to date.");
        }
    }
}