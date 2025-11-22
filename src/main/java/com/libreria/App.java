package com.libreria;

import atlantafx.base.theme.PrimerLight;
import com.libreria.data.config.DatabaseManager;
import com.libreria.ui.utils.DialogUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Inicializar Base de Datos (Antes de levantar la UI)
        try {
            DatabaseManager.initDb();
        } catch (Exception e) {
            DialogUtils.showError("Error Crítico de Base de Datos",
                    "No se pudo conectar a la base de datos.", e.getMessage());
            return; // Detener inicio
        }

        // Cargar Tema
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Cargar MainLayout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setTitle("POS Librería");
        stage.setMaximized(true); // Iniciar maximizado
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}