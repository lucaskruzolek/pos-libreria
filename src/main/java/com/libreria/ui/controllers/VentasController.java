package com.libreria.ui.controllers;

import com.libreria.core.exceptions.PrecioNoConfiguradoException;
import com.libreria.core.models.Venta;
import com.libreria.core.models.dto.ItemCarrito;
import com.libreria.core.models.dto.PrecioRequest;
import com.libreria.core.models.enums.*;
import com.libreria.core.services.ProductoService;
import com.libreria.core.services.ConfiguracionService;
import com.libreria.data.config.AppContainer;
import com.libreria.core.models.Producto;
import com.libreria.core.services.PrecioCalculatorService;
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
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import java.util.List;
import java.util.Optional;

public class VentasController {

    private static final Logger logger = LoggerFactory.getLogger(VentasController.class);

    // --- SERVICIOS ---
    private final VentaService ventaService;
    private final PrecioCalculatorService precioService;
    private final ProductoService productoService;
    private final ConfiguracionService configService;
    private final java.util.concurrent.ExecutorService executor;

    // --- UI COMPONENTS (FXML) ---
    @FXML
    private TextField txtSku;
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

    @FXML
    private TableView<Producto> tablaBusqueda;
    // @FXML private TableColumn<Producto, String> colBusquedaSku; // REMOVED
    @FXML
    private TableColumn<Producto, String> colBusquedaNombre;
    @FXML
    private TableColumn<Producto, String> colBusquedaCosto;
    @FXML
    private TableColumn<Producto, String> colBusquedaEfectivo;
    @FXML
    private TableColumn<Producto, String> colBusquedaTransferencia;

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
    private ToggleSwitch toggleFactura;
    @FXML
    private ToggleSwitch toggleDescuento;
    @FXML
    private Label lblTotal;

    // --- ESTADO ---
    private final ObservableList<ItemCarrito> itemsCarrito = FXCollections.observableArrayList();
    private final ObservableList<Producto> masterData = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredData;
    private SortedList<Producto> sortedData;

    // Constructor por defecto (JavaFX lo necesita)
    public VentasController() {
        AppContainer container = AppContainer.getInstance();

        this.ventaService = container.getVentaService();
        this.precioService = container.getPrecioService();
        this.productoService = container.getProductoService();
        this.configService = container.getConfiguracionService();
        this.executor = container.getExecutor();
    }

    @FXML
    public void initialize() {
        configurarControlesServicio();
        configurarTabla();
        configurarTablaBusqueda();
        configurarListeners();
        cargarProductosEnMemoria();
    }

    private void configurarListeners() {
        // Listener para actualizar el TOTAL cuando cambia la lista
        itemsCarrito.addListener((ListChangeListener<ItemCarrito>) c -> actualizarTotal());

        // Reactive Search Logic
        txtSku.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredData != null) {
                filteredData.setPredicate(crearPredicado(newValue));
            }
        });

        txtSku.setOnKeyPressed(this::manejarEnterSku);

        // Listener para Descuento Efectivo
        toggleDescuento.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recalcularPreciosCarrito(newVal);
        });
    }

    private void configurarControlesServicio() {
        comboTamano.setItems(FXCollections.observableArrayList(Tamano.values()));
        comboPapel.setItems(FXCollections.observableArrayList(TipoPapel.values()));
        comboColor.setItems(FXCollections.observableArrayList(Color.values()));
        comboFaz.setItems(FXCollections.observableArrayList(Faz.values()));

        // Valores por defecto para agilizar
        comboTamano.getSelectionModel().select(Tamano.A4);
        comboPapel.getSelectionModel().select(TipoPapel.OBRA_80G);
        comboColor.getSelectionModel().select(Color.BN);
        comboFaz.getSelectionModel().select(Faz.SIMPLE);

        // Configurar Spinner (Min 1, Max 10000, Default 1)
        spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
    }

    private void configurarTabla() {
        tablaCarrito.setItems(itemsCarrito);

        colProducto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreProducto()));
        colCantidad.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().cantidad()));

        // Formateo de moneda (Centavos -> $)
        colPrecio.setCellValueFactory(
                d -> new SimpleStringProperty(formatearMoneda(d.getValue().precioUnitarioCentavos())));
        colSubtotal
                .setCellValueFactory(d -> new SimpleStringProperty(formatearMoneda(d.getValue().calcularSubtotal())));

        // Botón de Eliminar
        colAccion.setCellFactory(new Callback<>() {
            @Override
            public TableCell<ItemCarrito, Void> call(final TableColumn<ItemCarrito, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("X");

                    {
                        btn.getStyleClass().add("danger"); // Estilo rojo de AtlantaFX
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

    // --- ACCIONES (HANDLERS) ---

    @FXML
    private void onAgregarFisico() {
        String sku = txtSku.getText().trim();
        if (sku.isEmpty())
            return;

        // Bloquear UI
        txtSku.setDisable(true);

        // Crear Tarea en Background
        javafx.concurrent.Task<Optional<Producto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<Producto> call() throws Exception {
                return productoService.buscarPorSku(sku);
            }
        };

        task.setOnSucceeded(e -> {
            // Desbloquear UI
            txtSku.setDisable(false);
            txtSku.clear();
            txtSku.requestFocus();

            Optional<Producto> optProd = task.getValue();
            if (optProd.isPresent()) {
                Producto prod = optProd.get();
                if (prod.esServicio()) {
                    DialogUtils.showWarning("Producto Incorrecto",
                            "El ítem es un SERVICIO. Úselo en el panel derecho.", "");
                    return;
                }
                agregarProductoAlCarrito(prod);
            } else {
                logger.info("Intento de búsqueda fallido: SKU {}", sku);
                DialogUtils.showWarning("No encontrado", "No existe producto con SKU: " + sku, "");
            }
        });

        task.setOnFailed(e -> {
            txtSku.setDisable(false);
            txtSku.requestFocus();
            Throwable error = task.getException();
            logger.error("Error de DB buscando producto", error);
            DialogUtils.showError("Error de Base de Datos", "No se pudo consultar el producto.", "");
        });

        // Ejecutar en Thread Pool
        executor.submit(task);
    }

    private void agregarProductoAlCarrito(Producto prod) {
        // Buscamos si ya existe en el carrito para sumar cantidad en vez de duplicar
        // fila
        Optional<ItemCarrito> existente = itemsCarrito.stream()
                .filter(i -> i.productoId().equals(prod.id()))
                .findFirst();

        if (existente.isPresent()) {
            // Lógica de actualización (Remove + Add actualizado)
            ItemCarrito itemOld = existente.get();
            ItemCarrito itemNew = new ItemCarrito(
                    itemOld.productoId(),
                    itemOld.nombreProducto(),
                    itemOld.cantidad() + 1, // Sumamos 1
                    itemOld.precioUnitarioCentavos(),
                    itemOld.esProductoFisico());
            int index = itemsCarrito.indexOf(itemOld);
            itemsCarrito.set(index, itemNew);
        } else {
            // Nuevo Item
            int precio = toggleDescuento.isSelected() ? prod.getPrecioEfectivo(configService.getMargenEfectivo())
                    : prod.getPrecioTransferencia(configService.getMargenTransferencia());
            itemsCarrito.add(new ItemCarrito(
                    prod.id(),
                    prod.nombre(),
                    1,
                    precio,
                    true // Es físico, descontará stock
            ));
        }
    }

    @FXML
    private void onAgregarServicio() {
        // 1. Obtener parámetros UI (En hilo UI)
        Tamano t = comboTamano.getValue();
        TipoPapel p = comboPapel.getValue();
        Color c = comboColor.getValue();
        Faz f = comboFaz.getValue();
        int cantidad = spinnerCantidad.getValue();

        // Bloquear
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

            // 3. Crear Item Carrito
            String descripcion = String.format("Copia %s %s %s %s", t, p, c, f);
            ItemCarrito item = new ItemCarrito(
                    9999, // ID ficticio de servicio
                    descripcion,
                    cantidad,
                    resultado.precioCentavos(),
                    false // No es físico
            );
            itemsCarrito.add(item);
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

    @FXML
    private void onCobrar() {
        if (itemsCarrito.isEmpty()) {
            DialogUtils.showWarning("Carrito Vacío", "Agrega ítems antes de cobrar.", "");
            return;
        }

        // Snapshot de datos para el hilo
        List<ItemCarrito> itemsSnapshot = itemsCarrito.stream().toList();
        boolean requiereFactura = toggleFactura.isSelected();
        MetodoPago metodoPago = toggleDescuento.isSelected() ? MetodoPago.EFECTIVO : MetodoPago.TRANSFERENCIA;

        // Bloquear todo
        tablaCarrito.setDisable(true);

        javafx.concurrent.Task<Venta> task = new javafx.concurrent.Task<>() {
            @Override
            protected Venta call() throws Exception {
                return ventaService.realizarVenta(
                        itemsSnapshot,
                        metodoPago,
                        null, // Cliente anónimo
                        requiereFactura);
            }
        };

        task.setOnSucceeded(e -> {
            tablaCarrito.setDisable(false);
            Venta venta = task.getValue();

            logger.info("Venta registrada ID: {} | Total: ${}", venta.id(), venta.totalCentavos() / 100.0);
            DialogUtils.showInfo("Venta Exitosa",
                    "Ticket #" + venta.id() + " registrado.", "Total: " + formatearMoneda(venta.totalCentavos()));

            // Reset UI
            itemsCarrito.clear();
            toggleFactura.setSelected(false);
            // toggleDescuento.setSelected(false); // Mantener selección o resetear? Mejor
            // mantener.
            txtSku.requestFocus();
        });

        task.setOnFailed(e -> {
            tablaCarrito.setDisable(false);
            Throwable ex = task.getException();
            logger.error("Fallo crítico de persistencia en venta", ex);
            DialogUtils.showError("Error Crítico",
                    "No se pudo guardar la venta: " + ex.getMessage(), "");
        });

        executor.submit(task);
    }

    // --- HELPERS ---

    private void actualizarTotal() {
        int totalCentavos = itemsCarrito.stream().mapToInt(ItemCarrito::calcularSubtotal).sum();
        lblTotal.setText(formatearMoneda(totalCentavos));
    }

    private String formatearMoneda(int centavos) {
        return String.format("$ %,.2f", centavos / 100.0);
    }

    // --- REACTIVE SEARCH METHODS ---

    private void configurarTablaBusqueda() {
        // colBusquedaSku.setCellValueFactory(cellData -> new
        // SimpleStringProperty(cellData.getValue().skuInterno())); // REMOVED
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
                    agregarProductoAlCarrito(selected);
                }
            }
        });
    }

    private void cargarProductosEnMemoria() {
        javafx.concurrent.Task<List<Producto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Producto> call() throws Exception {
                return productoService.listarProductosActivos();
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
        });

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

            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = searchText.toLowerCase();

            if (producto.nombre().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (producto.skuInterno().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (producto.codigoBarras() != null && producto.codigoBarras().contains(lowerCaseFilter)) {
                return true;
            }
            return false;
        };
    }

    private void manejarEnterSku(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String query = txtSku.getText().trim();
            if (query.isEmpty())
                return;

            // 1. Exact Match (Scanner behavior)
            Optional<Producto> exactMatch = masterData.stream()
                    .filter(p -> query.equalsIgnoreCase(p.skuInterno()) || query.equalsIgnoreCase(p.codigoBarras()))
                    .findFirst();

            if (exactMatch.isPresent()) {
                Producto prod = exactMatch.get();
                if (prod.esServicio()) {
                    DialogUtils.showWarning("Producto Incorrecto", "El ítem es un SERVICIO.", "");
                    return;
                }
                agregarProductoAlCarrito(prod);
                txtSku.clear();
                return;
            }

            // 2. Single Result in Filtered List
            if (filteredData.size() == 1) {
                agregarProductoAlCarrito(filteredData.get(0));
                txtSku.clear();
                return;
            }

            // 3. Multiple Results -> Focus Table
            if (!filteredData.isEmpty()) {
                tablaBusqueda.requestFocus();
                tablaBusqueda.getSelectionModel().selectFirst();
            }
        }
    }

    private void recalcularPreciosCarrito(boolean usarEfectivo) {
        for (int i = 0; i < itemsCarrito.size(); i++) {
            ItemCarrito item = itemsCarrito.get(i);
            if (item.esProductoFisico()) {
                // Buscar producto en masterData para obtener precios actualizados
                Optional<Producto> prodOpt = masterData.stream()
                        .filter(p -> p.id().equals(item.productoId()))
                        .findFirst();

                if (prodOpt.isPresent()) {
                    Producto prod = prodOpt.get();
                    int nuevoPrecio = usarEfectivo ? prod.getPrecioEfectivo(configService.getMargenEfectivo())
                            : prod.getPrecioTransferencia(configService.getMargenTransferencia());

                    // Solo actualizar si el precio es diferente
                    if (item.precioUnitarioCentavos() != nuevoPrecio) {
                        ItemCarrito newItem = new ItemCarrito(
                                item.productoId(),
                                item.nombreProducto(),
                                item.cantidad(),
                                nuevoPrecio,
                                item.esProductoFisico());
                        itemsCarrito.set(i, newItem);
                    }
                }
            }
        }
        // El listener de la lista se encargará de actualizar el total visual
    }
}
