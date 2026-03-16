-- ============================================================
-- Base de datos BTG - Schema y datos de prueba
-- ============================================================

CREATE TABLE Cliente (
    id      SERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    ciudad    VARCHAR(100) NOT NULL
);

CREATE TABLE Sucursal (
    id     SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL
);

CREATE TABLE Producto (
    id            SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    tipoProducto  VARCHAR(50)  NOT NULL
);

CREATE TABLE Inscripcion (
    idProducto INT NOT NULL REFERENCES Producto(id),
    idCliente  INT NOT NULL REFERENCES Cliente(id),
    PRIMARY KEY (idProducto, idCliente)
);

CREATE TABLE Disponibilidad (
    idSucursal INT NOT NULL REFERENCES Sucursal(id),
    idProducto INT NOT NULL REFERENCES Producto(id),
    PRIMARY KEY (idSucursal, idProducto)
);

CREATE TABLE Visitan (
    idSucursal  INT  NOT NULL REFERENCES Sucursal(id),
    idCliente   INT  NOT NULL REFERENCES Cliente(id),
    fechaVisita DATE NOT NULL,
    PRIMARY KEY (idSucursal, idCliente)
);

-- ============================================================
-- Datos de prueba
-- ============================================================

-- Sucursales
INSERT INTO Sucursal (id, nombre, ciudad) VALUES
    (1, 'Sucursal Norte',  'Bogotá'),
    (2, 'Sucursal Sur',    'Bogotá'),
    (3, 'Sucursal Centro', 'Medellín'),
    (4, 'Sucursal Oeste',  'Cali');

-- Productos
INSERT INTO Producto (id, nombre, tipoProducto) VALUES
    (1, 'CDT Premium',      'Inversión'),
    (2, 'Cuenta Ahorro',    'Ahorro'),
    (3, 'Fondo Renta Fija', 'Fondo'),
    (4, 'Tarjeta Crédito',  'Crédito'),
    (5, 'Seguro Vida',      'Seguro');

-- Disponibilidad de productos en sucursales
-- Producto 1 (CDT Premium):      solo en sucursal 1
-- Producto 2 (Cuenta Ahorro):    en sucursales 1 y 2
-- Producto 3 (Fondo Renta Fija): en sucursales 1, 2 y 3
-- Producto 4 (Tarjeta Crédito):  solo en sucursal 3
-- Producto 5 (Seguro Vida):      en sucursales 3 y 4
INSERT INTO Disponibilidad (idSucursal, idProducto) VALUES
    (1, 1),
    (1, 2), (2, 2),
    (1, 3), (2, 3), (3, 3),
    (3, 4),
    (3, 5), (4, 5);

-- Clientes
INSERT INTO Cliente (id, nombre, apellidos, ciudad) VALUES
    (1, 'Carlos',   'García López',    'Bogotá'),
    (2, 'María',    'Rodríguez Pérez', 'Medellín'),
    (3, 'Andrés',   'Martínez Ruiz',   'Bogotá'),
    (4, 'Laura',    'Fernández Díaz',  'Cali'),
    (5, 'Pedro',    'Sánchez Torres',  'Bogotá'),
    (6, 'Ana',      'López Moreno',    'Medellín');

-- Visitas de clientes a sucursales
-- Carlos (1):  visita sucursales 1 y 2
-- María (2):   visita sucursal 3
-- Andrés (3):  visita sucursal 1 (solo)
-- Laura (4):   visita sucursales 3 y 4
-- Pedro (5):   visita sucursal 2 (solo)
-- Ana (6):     no visita ninguna sucursal
INSERT INTO Visitan (idSucursal, idCliente, fechaVisita) VALUES
    (1, 1, '2025-01-15'), (2, 1, '2025-02-20'),
    (3, 2, '2025-03-10'),
    (1, 3, '2025-01-05'),
    (3, 4, '2025-04-01'), (4, 4, '2025-04-15'),
    (2, 5, '2025-05-01');

-- Inscripciones (aquí están los casos interesantes)
--
-- CASO 1 - Carlos (1) inscrito en Producto 1 (CDT Premium)
--   CDT Premium solo disponible en sucursal 1. Carlos visita sucursales 1 y 2.
--   → CUMPLE: todas las sucursales del producto (1) están en sus visitas (1,2)
--
-- CASO 2 - Carlos (1) inscrito en Producto 3 (Fondo Renta Fija)
--   Fondo Renta Fija disponible en sucursales 1, 2 y 3. Carlos visita 1 y 2.
--   → NO CUMPLE: sucursal 3 ofrece el producto pero Carlos no la visita
--
-- CASO 3 - María (2) inscrito en Producto 4 (Tarjeta Crédito)
--   Tarjeta Crédito solo en sucursal 3. María visita sucursal 3.
--   → CUMPLE: match exacto
--
-- CASO 4 - María (2) inscrito en Producto 5 (Seguro Vida)
--   Seguro Vida en sucursales 3 y 4. María visita solo sucursal 3.
--   → NO CUMPLE: sucursal 4 ofrece el producto pero María no la visita
--
-- CASO 5 - Andrés (3) inscrito en Producto 2 (Cuenta Ahorro)
--   Cuenta Ahorro en sucursales 1 y 2. Andrés visita solo sucursal 1.
--   → NO CUMPLE: sucursal 2 ofrece el producto pero Andrés no la visita
--
-- CASO 6 - Laura (4) inscrito en Producto 5 (Seguro Vida)
--   Seguro Vida en sucursales 3 y 4. Laura visita sucursales 3 y 4.
--   → CUMPLE: match exacto
--
-- CASO 7 - Pedro (5) inscrito en Producto 1 (CDT Premium)
--   CDT Premium solo en sucursal 1. Pedro visita solo sucursal 2.
--   → NO CUMPLE: sucursal 1 ofrece el producto pero Pedro no la visita
--
-- CASO 8 - Ana (6) inscrito en Producto 2 (Cuenta Ahorro)
--   Cuenta Ahorro en sucursales 1 y 2. Ana no visita ninguna sucursal.
--   → NO CUMPLE: no visita ninguna sucursal
--
-- RESULTADO ESPERADO: Carlos, María, Laura

INSERT INTO Inscripcion (idProducto, idCliente) VALUES
    (1, 1), (3, 1),   -- Carlos: CDT Premium (cumple) + Fondo Renta Fija (no cumple)
    (4, 2), (5, 2),   -- María:  Tarjeta Crédito (cumple) + Seguro Vida (no cumple)
    (2, 3),            -- Andrés: Cuenta Ahorro (no cumple)
    (5, 4),            -- Laura:  Seguro Vida (cumple)
    (1, 5),            -- Pedro:  CDT Premium (no cumple, no visita suc 1)
    (2, 6);            -- Ana:    Cuenta Ahorro (no cumple, no visita nada)
