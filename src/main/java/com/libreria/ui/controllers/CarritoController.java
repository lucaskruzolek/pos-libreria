package com.libreria.ui.controllers;

import com.libreria.core.models.Venta;
import com.libreria.core.models.dto.ItemCarrito;
import com.libreria.core.models.enums.MetodoPago;
import com.libreria.core.services.VentaService;
import com.libreria.ui.utils.DialogUtils;
import atlantafx.base.controls.ToggleSwitch;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class CarritoController {

    private static final Logger logger = LoggerFactory.getLogger(CarritoController.class);

    // Dependencies
    private VentaService ventaService;
    private ExecutorService executor;

    // UI
    @FXML
    private TableView<ItemCarrito> tablaCarrito;
    @FXML
    private TableColumn<ItemCarrito, String> colProducto;
    @FXML
    private TableColumn<ItemCarrito, Integer> colCantidad;
    @FXML
    private TableColumn<ItemCarrito, String> colPrecio;
    @FXML
    private TableColumn<ItemCarrito, String> colSubtotal;
    @FXML
    private TableColumn<ItemCarrito, Void> colAccion;
    @FXML
    private Label lblTotal;
    @FXML
    private ToggleSwitch toggleFactura;
    @FXML
    private ToggleSwitch toggleDescuento;

    // State
    private final ObservableList<ItemCarrito> itemsCarrito = FXCollections.observableArrayList();
    private boolean descuentoActivo = false;

    public void init(VentaService ventaService, ExecutorService executor) {
        this.ventaService = ventaService;
        this.executor = executor;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        itemsCarrito.addListener((ListChangeListener<ItemCarrito>) c -> actualizarTotal());

        toggleDescuento.selectedProperty().addListener((obs, oldVal, newVal) -> {
            descuentoActivo = newVal;
            tablaCarrito.refresh(); // Refresh table to show new prices
            actualizarTotal();
        });
    }

    private void configurarTabla() {
        tablaCarrito.setItems(itemsCarrito);

        colProducto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreProducto()));
        colCantidad.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().cantidad()));
        colPrecio.setCellValueFactory(
                d -> new SimpleStringProperty(formatearMoneda(d.getValue().getPrecioUnitario(descuentoActivo))));
        colSubtotal
                .setCellValueFactory(
                        d -> new SimpleStringProperty(formatearMoneda(d.getValue().calcularSubtotal(descuentoActivo))));

        colAccion.setCellFactory(new Callback<>() {
            @Override
            public TableCell<ItemCarrito, Void> call(final TableColumn<ItemCarrito, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("X");
                    {
                        btn.getStyleClass().add("danger");
                        btn.setOnAction(event -> {
                            ItemCarrito item = getTableView().getItems().get(getIndex());
                            itemsCarrito.remove(item);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty)
                            setGraphic(null);
                        else
                            setGraphic(btn);
                    }
                };
            }
        });
    }

    public void agregarItem(ItemCarrito newItem) {
        // Check for duplicates
        for (int i = 0; i < itemsCarrito.size(); i++) {
            ItemCarrito existing = itemsCarrito.get(i);
            if (existing.productoId().equals(newItem.productoId())
                    && existing.esProductoFisico() == newItem.esProductoFisico()) {
                // Update quantity
                ItemCarrito updated = new ItemCarrito(
                        existing.productoId(),
                        existing.nombreProducto(),
                        existing.cantidad() + newItem.cantidad(),
                        existing.precioListaCentavos(),
                        existing.precioEfectivoCentavos(),
                        existing.esProductoFisico());
                itemsCarrito.set(i, updated);
                return;
            }
        }
        itemsCarrito.add(newItem);
    }

    public ObservableList<ItemCarrito> getItems() {
        return itemsCarrito;
    }

    public void setItems(List<ItemCarrito> items) {
        itemsCarrito.setAll(items);
    }

    @FXML
    private void onCobrar() {
        if (itemsCarrito.isEmpty()) {
            DialogUtils.showWarning("Carrito Vacío", "Agrega ítems antes de cobrar.", "");
            return;
        }

        List<ItemCarrito> itemsSnapshot = itemsCarrito.stream().toList();
        boolean requiereFactura = toggleFactura.isSelected();
        MetodoPago metodoPago = descuentoActivo ? MetodoPago.EFECTIVO : MetodoPago.TRANSFERENCIA;

        tablaCarrito.setDisable(true);

        javafx.concurrent.Task<Venta> task = new javafx.concurrent.Task<>() {
            @Override
            protected Venta call() throws Exception {
                return ventaService.realizarVenta(itemsSnapshot, metodoPago, null, requiereFactura);
            }
        };

        task.setOnSucceeded(e -> {
            tablaCarrito.setDisable(false);
            Venta venta = task.getValue();
            logger.info("Venta registrada ID: {}", venta.id());
            DialogUtils.showInfo("Venta Exitosa", "Ticket #" + venta.id() + " registrado.",
                    "Total: " + formatearMoneda(venta.totalCentavos()));
            itemsCarrito.clear();
            toggleFactura.setSelected(false);
        });

        task.setOnFailed(e -> {
            tablaCarrito.setDisable(false);
            logger.error("Fallo venta", task.getException());
            DialogUtils.showError("Error Crítico", "No se pudo guardar la venta: " + task.getException().getMessage(),
                    "");
        });

        executor.submit(task);
    }

    private void actualizarTotal() {
        int totalCentavos = itemsCarrito.stream().mapToInt(item -> item.calcularSubtotal(descuentoActivo)).sum();
        lblTotal.setText(formatearMoneda(totalCentavos));
    }

    private String formatearMoneda(int centavos) {
        return String.format("$ %,.2f", centavos / 100.0);
    }

    public void setDescuentoActivo(boolean activo) {
        this.descuentoActivo = activo;
    }

    public boolean isDescuentoActivo() {
        return descuentoActivo;
    }
}
