package com.libreria.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainLayoutController {

    private static final Logger logger = LoggerFactory.getLogger(MainLayoutController.class);

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnVentas;

    @FXML
    private Button btnProductos;

    // Cache for loaded views
    private final Map<String, Parent> viewCache = new HashMap<>();

    @FXML
    public void initialize() {
        // Load default view (Ventas)
        mostrarVentas();
    }

    @FXML
    private void mostrarVentas() {
        loadView("ventas", "/fxml/ventasView.fxml");
        setActiveButton(btnVentas);
    }

    @FXML
    private void mostrarProductos() {
        // Placeholder for Productos view.
        // Since we don't have a productos.fxml yet, we might want to show a temporary
        // placeholder or just log it.
        // For now, assuming we might want to load a future view or just show a "Coming
        // Soon" label.
        // But per requirements, I will implement the structure.
        // If the file doesn't exist, it will throw an error, so I'll handle it
        // gracefully or create a dummy if needed.
        // The user asked for "Productos / Admin" button.

        // For this task, I'll create a simple placeholder if the file is missing,
        // or just try to load it if the user intends to create it later.
        // Given the prompt, I'll assume I should just set up the mechanism.
        // However, to avoid runtime errors if the user clicks it, I will check or
        // catch.

        // Actually, the user said "future module", so I will just log for now or show a
        // simple label in code if file missing.
        // But to be robust, I will try to load a "productos.fxml" if it existed, or
        // just a placeholder.

        // Let's just implement the logic to load it, and if it fails (because file
        // missing), we catch and show error.
        // Or better, I'll create a simple "Under Construction" view programmatically if
        // the file is missing.

        // Wait, the user didn't ask me to create productos.fxml.
        // I will just implement the method to try to load it, but maybe use a
        // placeholder text for now.

        if (!viewCache.containsKey("productos")) {
            javafx.scene.control.Label placeholder = new javafx.scene.control.Label(
                    "Módulo de Productos / Admin (Próximamente)");
            placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: -color-fg-default;");
            viewCache.put("productos", new javafx.scene.layout.StackPane(placeholder));
        }

        contentArea.getChildren().clear();
        contentArea.getChildren().add(viewCache.get("productos"));
        setActiveButton(btnProductos);
    }

    private void loadView(String key, String fxmlPath) {
        if (viewCache.containsKey(key)) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(viewCache.get(key));
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            viewCache.put(key, view);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            logger.error("Error loading view: " + fxmlPath, e);
            // Show error in UI
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("Error cargando la vista.");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorLabel);
        }
    }

    private void setActiveButton(Button activeButton) {
        btnVentas.getStyleClass().remove("accent");
        btnProductos.getStyleClass().remove("accent");

        activeButton.getStyleClass().add("accent");
    }
}
