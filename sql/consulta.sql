-- Parte 2 – SQL (20%)
-- Base de datos: BTG
--
-- Consulta: Obtener los nombres de los clientes que tienen inscrito algún producto
-- disponible solo en las sucursales que visitan.
--
-- Interpretación: un cliente califica si tiene al menos un producto inscrito cuya
-- disponibilidad se limita exclusivamente a sucursales que ese cliente visita.
-- Es decir, no existe ninguna sucursal que ofrezca el producto y que el cliente NO visite.

SELECT DISTINCT c.nombre
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
      AND d.idSucursal NOT IN (
          SELECT v.idSucursal
          FROM Visitan v
          WHERE v.idCliente = c.id
      )
);
