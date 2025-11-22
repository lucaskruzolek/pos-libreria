package com.libreria.ui.controllers;

import com.libreria.core.exceptions.PrecioNoConfiguradoException;
import com.libreria.core.models.dto.ItemCarrito;
import com.libreria.core.models.dto.PrecioRequest;
import com.libreria.core.models.enums.Color;
import com.libreria.core.models.enums.Faz;
import com.libreria.core.models.enums.Tamano;
import com.libreria.core.models.enums.TipoPapel;
import com.libreria.core.services.PrecioCalculatorService;
import com.libreria.ui.utils.DialogUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class PanelServiciosController {

    private static final Logger logger = LoggerFactory.getLogger(PanelServiciosController.class);

    // Dependencies
    private PrecioCalculatorService precioService;
    private ExecutorService executor;
    private Consumer<ItemCarrito> onServicioCreado;

    // UI
    @FXML
    private ComboBox<Tamano> comboTamano;
    @FXML
    private ComboBox<TipoPapel> comboPapel;
    @FXML
    private ComboBox<Color> comboColor;
    @FXML
    private ComboBox<Faz> comboFaz;
    @FXML
    private Spinner<Integer> spinnerCantidad;

    public void init(PrecioCalculatorService precioService, ExecutorService executor) {
        this.precioService = precioService;
        this.executor = executor;
    }

    public void setOnServicioCreado(Consumer<ItemCarrito> callback) {
        this.onServicioCreado = callback;
    }

    @FXML
    public void initialize() {
        comboTamano.setItems(FXCollections.observableArrayList(Tamano.values()));
        comboPapel.setItems(FXCollections.observableArrayList(TipoPapel.values()));
        comboColor.setItems(FXCollections.observableArrayList(Color.values()));
        comboFaz.setItems(FXCollections.observableArrayList(Faz.values()));

        comboTamano.getSelectionModel().select(Tamano.A4);
        comboPapel.getSelectionModel().select(TipoPapel.OBRA_80G);
        comboColor.getSelectionModel().select(Color.BN);
        comboFaz.getSelectionModel().select(Faz.SIMPLE);

        spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
    }

    @FXML
    private void onAgregarServicio() {
        Tamano t = comboTamano.getValue();
        TipoPapel p = comboPapel.getValue();
        Color c = comboColor.getValue();
        Faz f = comboFaz.getValue();
        int cantidad = spinnerCantidad.getValue();

        spinnerCantidad.setDisable(true);

        javafx.concurrent.Task<com.libreria.core.models.dto.PrecioResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.libreria.core.models.dto.PrecioResult call() throws Exception {
                var request = new PrecioRequest(t, p, c, f);
                return precioService.calcularPrecioBase(request);
            }
        };

        task.setOnSucceeded(e -> {
            spinnerCantidad.setDisable(false);
            var resultado = task.getValue();

            String descripcion = String.format("Copia %s %s %s %s", t, p, c, f);
            ItemCarrito item = new ItemCarrito(
                    9999, // ID ficticio
                    descripcion,
                    cantidad,
                    resultado.precioCentavos(), // Precio Lista
                    resultado.precioCentavos(), // Precio Efectivo (igual para servicios)
                    false);

            if (onServicioCreado != null) {
                onServicioCreado.accept(item);
            }
        });

        task.setOnFailed(e -> {
            spinnerCantidad.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof PrecioNoConfiguradoException) {
                logger.warn("Matriz incompleta: {}", ex.getMessage());
                DialogUtils.showWarning("Precio No Configurado", ex.getMessage(), "");
            } else {
                logger.error("Error SQL al agregar servicio", ex);
                DialogUtils.showError("Error de Sistema", "Fallo al consultar base de datos.", "");
            }
        });

        executor.submit(task);
    }
}
