# EDICIONES RECIENTES — POWER GRID ECONOMY

Este archivo complementa `CONTINUIDAD_POWER_GRID_ECONOMY.md` y se debe mantener junto al historial principal cuando se hagan nuevas modificaciones.

## EDICIÓN 21 — NÚCLEO DE CONTRATOS Y PERSISTENCIA

### Prompt
Implementar el núcleo persistente de contratos del bloque: propietario, comprador, ID Card vinculada, tipo de plan, precio, tiempo restante, energía restante, consumo acumulado y estado del servicio. El plan por tiempo debe descontar ticks reales del servidor. El plan por energía debe aceptar consumo real de Power Grid mediante un método de integración.

### Cambios
- `ElectricalServiceBlockEntity.java` recibió `PlanType.NONE/TIME/ENERGY`.
- Se añadió persistencia NBT.
- Se añadió propietario/comprador mediante UUID.
- Se añadió contador de tiempo.
- Se añadió saldo energético en Wh.
- Se añadió consumo acumulado.
- Se añadió `recordEnergyConsumed(long)`.

## EDICIÓN 22 — INTERRUPTOR ELÉCTRICO REAL

### Prompt
Integrar el bloque con la API real de Create: Power Grid. Crear cuatro terminales y dos conductores conmutables: positivo entrada→salida y negativo entrada→salida. El estado del servicio debe abrir/cerrar físicamente ambos conductores.

### Cambios
- `ElectricalServiceBlockEntity` implementa `IElectricEntity`.
- Se crean 4 terminales Power Grid.
- Se utilizan dos `SwitchedWire`.
- Terminales: 0 positivo entrada, 1 negativo entrada, 2 positivo salida, 3 negativo salida.
- El servicio activa/desactiva ambos interruptores simultáneamente.
- Se añadió medición de potencia mediante voltaje × corriente y conversión por tick a Wh para el plan energético.

La API usada (`IElectricEntity.CircuitBuilder.setTerminalCount`, `terminalNode`, `connectSwitch` y `SwitchedWire.setState`) fue comprobada contra el código fuente público de Power Grid. No se está usando una API inventada.

## EDICIÓN 23 — NUMISMATICS REAL

### Prompt
Usar la API real de Create: Numismatics para la ID Card y para los pagos. La identidad del propietario debe obtenerse desde la ID Card real. Las compras deben descontar dinero del comprador y depositarlo en la cuenta del propietario antes de activar el servicio.

### Cambios
- Se usa `IDCardItem.get(ItemStack)` para obtener la identidad de la tarjeta.
- La tarjeta solo puede vincularse si corresponde al jugador que la usa.
- Se añadió compra de tiempo mediante cuenta real de Numismatics.
- Se añadió compra de energía mediante cuenta real de Numismatics.
- El pago se valida antes de activar el servicio.
- El dinero se deposita en la cuenta del propietario.

## ESTADO ACTUAL

- Bloque: hecho.
- Modelo: hecho.
- Textura física: hecha.
- 4 terminales reales Power Grid: implementados en el BlockEntity.
- Interruptor positivo/negativo: implementado.
- Plan por tiempo: implementado.
- Plan por energía: implementado.
- Persistencia: implementada.
- ID Card real: integrada.
- Pago real Numismatics: integrado a nivel de BlockEntity.
- GUI: pendiente.
- Pruebas de compilación/ejecución en Minecraft: pendientes de ejecutar en el entorno local.
- Integración final de interfaz de usuario y registro de menú: pendiente.

## SIGUIENTE PASO OBLIGATORIO

Crear la GUI del bloque con dos vistas:

1. Propietario: vincular ID Card, elegir plan, establecer precio y consultar estado.
2. Comprador: seleccionar días o Wh, calcular precio y ejecutar compra.

Después ejecutar `gradlew clean build` y corregir cualquier incompatibilidad de versión que aparezca.
