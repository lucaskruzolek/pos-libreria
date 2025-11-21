package com.libreria.ui.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Platform;

public class DialogUtils {

    private DialogUtils() {
    }

    public static void showError(String title, String header, String content) {
        showAlert(AlertType.ERROR, title, header, content);
    }

    public static void showWarning(String title, String header, String content) {
        showAlert(AlertType.WARNING, title, header, content);
    }

    public static void showInfo(String title, String header, String content) {
        showAlert(AlertType.INFORMATION, title, header, content);
    }

    private static void showAlert(AlertType type, String title, String header, String content) {
        // Ensure UI operations run on JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            createAndShowAlert(type, title, header, content);
        } else {
            Platform.runLater(() -> createAndShowAlert(type, title, header, content));
        }
    }

    private static void createAndShowAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
