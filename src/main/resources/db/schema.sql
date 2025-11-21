-- =================================================================================
-- ARCHIVO: schema.sql (Versión Definitiva 2.0)
-- PROYECTO: POS Híbrido (Librería + Centro de Copiado)
-- MOTOR: SQLite 3.x
-- DESCRIPCIÓN: Esquema relacional con soporte para productos, servicios complejos y facturación AFIP.
-- =================================================================================

-- 1. CONFIGURACIÓN INICIAL
-- Activar soporte de claves foráneas (Obligatorio en cada conexión de SQLite)
PRAGMA foreign_keys = ON;
PRAGMA encoding = "UTF-8";

BEGIN TRANSACTION;

-- =================================================================================
-- 2. TABLAS MAESTRAS (CATÁLOGOS)
-- =================================================================================

-- Tabla: Categorias
CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    activa INTEGER DEFAULT 1 CHECK (activa IN (0, 1)) -- 1=Sí, 0=No
);

-- Tabla: Clientes (Nuevo requisito)
CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    razon_social TEXT NOT NULL, -- Nombre o Razón Social
    cuit TEXT UNIQUE, -- CUIT o DNI (TEXT para preservar ceros a la izquierda)
    condicion_iva TEXT DEFAULT 'CONSUMIDOR_FINAL', -- RI, Monotributo, Exento, CF
    email TEXT,
    direccion TEXT,
    fecha_alta DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: Productos (Polimórfica: Físico vs Servicio)
CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER,
    codigo_barras TEXT UNIQUE, -- Nullable (Servicios pueden no tenerlo)
    sku_interno TEXT UNIQUE NOT NULL, -- Identificador humano (ej: FOT-A4)
    nombre TEXT NOT NULL,
    descripcion TEXT,
    tipo TEXT NOT NULL CHECK (tipo IN ('FISICO', 'SERVICIO')),
    precio_base_centavos INTEGER DEFAULT 0, -- Uso interno para físicos. Servicios usan matriz.
    stock_actual INTEGER DEFAULT 0, -- Se descuenta en físicos. Ignorar en servicios.
    stock_minimo INTEGER DEFAULT 5,
    activo INTEGER DEFAULT 1 CHECK (activo IN (0, 1)),

    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON DELETE RESTRICT -- No borrar categoría si tiene productos asignados
);

-- Índices de búsqueda rápida
CREATE INDEX IF NOT EXISTS idx_productos_barras ON productos(codigo_barras);
CREATE INDEX IF NOT EXISTS idx_productos_sku ON productos(sku_interno);

-- =================================================================================
-- 3. MÓDULO DE SERVICIOS (MATRIZ DE PRECIOS)
-- =================================================================================

-- Tabla: Matriz de Precios
-- Define el costo de servicios basados en combinaciones exactas de atributos.
CREATE TABLE IF NOT EXISTS matriz_precios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    servicio_id INTEGER NOT NULL,

    -- Dimensiones de la matriz
    tamano TEXT NOT NULL CHECK (tamano IN ('A4', 'OFICIO', 'A3', 'CARTA')),
    tipo_papel TEXT NOT NULL DEFAULT 'OBRA_80G',
    color TEXT NOT NULL CHECK (color IN ('BN', 'COLOR')),
    faz TEXT NOT NULL CHECK (faz IN ('SIMPLE', 'DOBLE')),

    -- Valor monetario
    precio_centavos INTEGER NOT NULL CHECK (precio_centavos >= 0),

    CONSTRAINT fk_matriz_servicio
        FOREIGN KEY (servicio_id)
        REFERENCES productos (id)
        ON DELETE CASCADE -- Si borro el servicio, se borra su configuración de precios
);

-- REQUISITO CRÍTICO: Índice Único Compuesto
-- Garantiza que no haya precios duplicados para la misma combinación
CREATE UNIQUE INDEX IF NOT EXISTS idx_matriz_unica
ON matriz_precios (servicio_id, tamano, tipo_papel, color, faz);

-- =================================================================================
-- 4. MÓDULO DE VENTAS Y FACTURACIÓN (FISCAL)
-- =================================================================================

-- Tabla: Ventas (Encabezado)
CREATE TABLE IF NOT EXISTS ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Datos Económicos
    total_centavos INTEGER NOT NULL DEFAULT 0,
    metodo_pago TEXT DEFAULT 'EFECTIVO',

    -- Datos del Cliente (Snapshot + Relación)
    cliente_id INTEGER, -- NULL = Consumidor Final Anónimo
    cuit_cliente TEXT,  -- Se guarda copia aquí para integridad histórica de la factura

    -- Datos Fiscales / AFIP
    requiere_factura INTEGER DEFAULT 0 CHECK (requiere_factura IN (0, 1)), -- Flag para el Worker
    estado_fiscal TEXT DEFAULT 'NO_REQUIERE'
        CHECK (estado_fiscal IN ('NO_REQUIERE', 'PENDIENTE', 'ENVIADO', 'APROBADO', 'RECHAZADO', 'ERROR')),
    cae TEXT,           -- Código de Autorización Electrónico
    vto_cae DATE,       -- Vencimiento del CAE
    punto_venta INTEGER DEFAULT 1,
    numero_factura INTEGER, -- Número oficial devuelto por AFIP

    -- Estado Interno
    estado TEXT DEFAULT 'COMPLETADA' CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'ANULADA')),

    CONSTRAINT fk_venta_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES clientes (id)
        ON DELETE SET NULL -- Si borro cliente, la venta queda (anónima o histórica)
);

-- Tabla: Detalle de Ventas (Renglones)
CREATE TABLE IF NOT EXISTS detalle_ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id INTEGER NOT NULL,
    producto_id INTEGER NOT NULL,

    -- Datos de la transacción
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario_centavos INTEGER NOT NULL, -- Snapshot del precio al momento de vender
    subtotal_centavos INTEGER NOT NULL, -- cantidad * precio_unitario

    -- Atributos específicos (ej: "Fotocopia A4 Color")
    descripcion_linea TEXT,

    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (venta_id)
        REFERENCES ventas (id)
        ON DELETE CASCADE, -- Si elimino encabezado, elimino detalles

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (producto_id)
        REFERENCES productos (id)
        ON DELETE RESTRICT -- No permitir borrar productos vendidos
);

COMMIT;

//SEED DATA


BEGIN TRANSACTION;

-- Categorías (Usamos INSERT OR IGNORE para evitar error UNIQUE si ya existen)
INSERT OR IGNORE INTO categorias (nombre) VALUES ('Papelería General');
INSERT OR IGNORE INTO categorias (nombre) VALUES ('Servicios de Impresión');

-- Cliente de Prueba
INSERT OR IGNORE INTO clientes (razon_social, cuit, condicion_iva, direccion)
VALUES ('Tech Solutions S.A.', '30112233445', 'RESPONSABLE_INSCRIPTO', 'Av. Corrientes 1234');

-- 1. PRODUCTO FÍSICO (Lápiz)
-- El conflicto se verifica contra sku_interno o codigo_barras que son UNIQUE
INSERT OR IGNORE INTO productos (categoria_id, codigo_barras, sku_interno, nombre, tipo, precio_base_centavos, stock_actual)
VALUES (1, '7791234567890', 'LAP-HB-FABER', 'Lápiz Negro HB Faber', 'FISICO', 15000, 50);

-- 2. SERVICIO (Fotocopia)
INSERT OR IGNORE INTO productos (categoria_id, sku_interno, nombre, tipo, precio_base_centavos, stock_actual)
VALUES (2, 'SERV-COPY', 'Fotocopia / Impresión', 'SERVICIO', 0, 0);

-- Llenado de MATRIZ DE PRECIOS
-- Conflictos verificados por el índice único compuesto (servicio_id, tamano, tipo_papel...)

-- A4 B/N Simple
INSERT OR IGNORE INTO matriz_precios (servicio_id, tamano, tipo_papel, color, faz, precio_centavos)
VALUES ((SELECT id FROM productos WHERE sku_interno='SERV-COPY'), 'A4', 'OBRA_80G', 'BN', 'SIMPLE', 5000);

-- A4 B/N Doble
INSERT OR IGNORE INTO matriz_precios (servicio_id, tamano, tipo_papel, color, faz, precio_centavos)
VALUES ((SELECT id FROM productos WHERE sku_interno='SERV-COPY'), 'A4', 'OBRA_80G', 'BN', 'DOBLE', 9000);

-- A4 Color Simple
INSERT OR IGNORE INTO matriz_precios (servicio_id, tamano, tipo_papel, color, faz, precio_centavos)
VALUES ((SELECT id FROM productos WHERE sku_interno='SERV-COPY'), 'A4', 'OBRA_80G', 'COLOR', 'SIMPLE', 20000);

-- Oficio B/N Simple
INSERT OR IGNORE INTO matriz_precios (servicio_id, tamano, tipo_papel, color, faz, precio_centavos)
VALUES ((SELECT id FROM productos WHERE sku_interno='SERV-COPY'), 'OFICIO', 'OBRA_80G', 'BN', 'SIMPLE', 7000);

-- =================================================================================
-- 5. CONFIGURACIÓN DEL SISTEMA
-- =================================================================================

-- Tabla: Configuración (Key-Value)
CREATE TABLE IF NOT EXISTS configuracion (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL,
    descripcion TEXT
);

-- Seed Data: Márgenes de Ganancia
INSERT OR IGNORE INTO configuracion (clave, valor, descripcion) VALUES 
('MARGEN_EFECTIVO', '1.50', 'Multiplicador para precio en efectivo (Costo * Margen)'),
('MARGEN_TRANSFERENCIA', '1.52', 'Multiplicador para precio lista/transferencia (Costo * Margen)');

COMMIT;
