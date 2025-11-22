
package com.libreria.ui.controllers;

import com.libreria.core.models.Producto;
import com.libreria.core.models.dto.ItemCarrito;
import com.libreria.core.services.ConfiguracionService;
import com.libreria.data.config.AppContainer;
import javafx.fxml.FXML;

public class VentasController {

    // --- SUB-CONTROLLERS (Injected by FXMLLoader) ---
    // Naming convention: [fx:id] + Controller
    @FXML
    private BuscadorProductosController buscadorProductosController;
    @FXML
    private PanelServiciosController panelServiciosController;
    @FXML
    private CarritoController carritoController;

    // --- DEPENDENCIES ---
    private final AppContainer container;
    private final ConfiguracionService configService;

    public VentasController() {
        this.container = AppContainer.getInstance();
        this.configService = container.getConfiguracionService();
    }

    @FXML
    public void initialize() {
        // 1. Initialize Sub-Controllers with Dependencies
        buscadorProductosController.init(container.getProductoService(), configService, container.getExecutor());
        panelServiciosController.init(container.getPrecioService(), container.getExecutor());
        carritoController.init(container.getVentaService(), container.getExecutor());

        // 2. Wire Events

        // Event: Product Selected -> Add to Cart
        buscadorProductosController.setOnProductoSeleccionado(this::agregarProductoAlCarrito);

        // Event: Service Created -> Add to Cart
        panelServiciosController.setOnServicioCreado(item -> carritoController.agregarItem(item));
    }

    private void agregarProductoAlCarrito(Producto prod) {
        // Calculate both prices based on current margins
        int precioEfectivo = prod.getPrecioEfectivo(configService.getMargenEfectivo());
        int precioTransferencia = prod.getPrecioTransferencia(configService.getMargenTransferencia());

        ItemCarrito item = new ItemCarrito(
                prod.id(),
                prod.nombre(),
                1,
                precioTransferencia,
                precioEfectivo,
                true // Es físico
        );
        carritoController.agregarItem(item);
    }
}
