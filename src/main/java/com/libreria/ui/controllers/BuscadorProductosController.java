package com.libreria.ui.controllers;

import com.libreria.core.models.Producto;
import com.libreria.core.services.ConfiguracionService;
import com.libreria.core.services.ProductoService;
import com.libreria.ui.utils.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class BuscadorProductosController {

    private static final Logger logger = LoggerFactory.getLogger(BuscadorProductosController.class);

    // Dependencies
    private ProductoService productoService;
    private ConfiguracionService configService;
    private ExecutorService executor;
    private Consumer<Producto> onProductoSeleccionado;

    // UI
    @FXML
    private TextField txtSku;
    @FXML
    private TableView<Producto> tablaBusqueda;
    @FXML
    private TableColumn<Producto, String> colBusquedaNombre;
    @FXML
    private TableColumn<Producto, String> colBusquedaCosto;
    @FXML
    private TableColumn<Producto, String> colBusquedaEfectivo;
    @FXML
    private TableColumn<Producto, String> colBusquedaTransferencia;

    // State
    private final ObservableList<Producto> masterData = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredData;
    private SortedList<Producto> sortedData;

    public void init(ProductoService productoService, ConfiguracionService configService, ExecutorService executor) {
        this.productoService = productoService;
        this.configService = configService;
        this.executor = executor;
        cargarProductosEnMemoria();
    }

    public void setOnProductoSeleccionado(Consumer<Producto> callback) {
        this.onProductoSeleccionado = callback;
    }

    @FXML
    public void initialize() {
        configurarTablaBusqueda();
        configurarListeners();
    }

    private void configurarListeners() {
        txtSku.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredData != null) {
                filteredData.setPredicate(crearPredicado(newValue));
            }
        });

        txtSku.setOnKeyPressed(this::manejarEnterSku);

    }

    private void configurarTablaBusqueda() {
        colBusquedaNombre.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nombre()));
        colBusquedaCosto.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatearMoneda(cellData.getValue().getCosto())));
        colBusquedaEfectivo.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatearMoneda(cellData.getValue().getPrecioEfectivo(configService.getMargenEfectivo()))));
        colBusquedaTransferencia.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatearMoneda(
                        cellData.getValue().getPrecioTransferencia(configService.getMargenTransferencia()))));

        filteredData = new FilteredList<>(masterData, p -> !p.esServicio());
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaBusqueda.comparatorProperty());
        tablaBusqueda.setItems(sortedData);

        tablaBusqueda.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Producto selected = tablaBusqueda.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    notificarSeleccion(selected);
                }
            }
        });

        tablaBusqueda.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Producto selected = tablaBusqueda.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    notificarSeleccion(selected);
                }
            }
        });
    }

    @FXML
    private void onAgregarFisico() {
        String sku = txtSku.getText().trim();
        if (sku.isEmpty())
            return;

        txtSku.setDisable(true);

        javafx.concurrent.Task<Optional<Producto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<Producto> call() throws Exception {
                return productoService.buscarPorSku(sku);
            }
        };

        task.setOnSucceeded(e -> {
            txtSku.setDisable(false);
            txtSku.clear();
            txtSku.requestFocus();

            Optional<Producto> optProd = task.getValue();
            if (optProd.isPresent()) {
                Producto prod = optProd.get();
                if (prod.esServicio()) {
                    DialogUtils.showWarning("Producto Incorrecto", "El ítem es un SERVICIO. Úselo en el panel derecho.",
                            "");
                    return;
                }
                notificarSeleccion(prod);
            } else {
                DialogUtils.showWarning("No encontrado", "No existe producto con SKU: " + sku, "");
            }
        });

        task.setOnFailed(e -> {
            txtSku.setDisable(false);
            txtSku.requestFocus();
            logger.error("Error buscando producto", task.getException());
            DialogUtils.showError("Error de Base de Datos", "No se pudo consultar el producto.", "");
        });

        executor.submit(task);
    }

    private void notificarSeleccion(Producto prod) {
        if (onProductoSeleccionado != null) {
            onProductoSeleccionado.accept(prod);
        }
    }

    private void cargarProductosEnMemoria() {
        javafx.concurrent.Task<List<Producto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Producto> call() throws Exception {
                return productoService.listarProductosActivos();
            }
        };

        task.setOnSucceeded(e -> masterData.setAll(task.getValue()));
        task.setOnFailed(e -> {
            logger.error("Error cargando productos", task.getException());
            DialogUtils.showError("Error", "No se pudieron cargar los productos.", "");
        });

        executor.submit(task);
    }

    private java.util.function.Predicate<Producto> crearPredicado(String searchText) {
        return producto -> {
            if (producto.esServicio())
                return false;
            if (searchText == null || searchText.isEmpty())
                return true;

            String lowerQuery = removerAcentos(searchText.toLowerCase());

            return removerAcentos(producto.nombre().toLowerCase()).contains(lowerQuery) ||
                    removerAcentos(producto.skuInterno().toLowerCase()).contains(lowerQuery) ||
                    (producto.codigoBarras() != null && producto.codigoBarras().toLowerCase().contains(lowerQuery));
        };
    }

    private String removerAcentos(String input) {
        if (input == null)
            return "";
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void manejarEnterSku(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String query = txtSku.getText().trim();
            if (query.isEmpty())
                return;

            Optional<Producto> exactMatch = masterData.stream()
                    .filter(p -> query.equalsIgnoreCase(p.skuInterno()) || query.equalsIgnoreCase(p.codigoBarras()))
                    .findFirst();

            if (exactMatch.isPresent()) {
                Producto prod = exactMatch.get();
                if (prod.esServicio()) {
                    DialogUtils.showWarning("Producto Incorrecto", "El ítem es un SERVICIO.", "");
                    return;
                }
                notificarSeleccion(prod);
                txtSku.clear();
                return;
            }

            if (filteredData.size() == 1) {
                notificarSeleccion(filteredData.get(0));
                txtSku.clear();
                return;
            }

            if (!filteredData.isEmpty()) {
                tablaBusqueda.requestFocus();
                tablaBusqueda.getSelectionModel().selectFirst();
            }
        }
    }

    private String formatearMoneda(int centavos) {
        return String.format("$ %,.2f", centavos / 100.0);
    }
}
