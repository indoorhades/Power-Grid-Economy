# POWER GRID ECONOMY — CONTINUIDAD DEL PROYECTO

Este archivo es la memoria permanente del proyecto. **Cada nueva edición importante, cambio de código, corrección o nuevo requisito debe añadirse aquí como una entrada independiente**, en orden cronológico. Los prompts están redactados para poder copiarlos en otro chat y continuar sin perder contexto.

---

# 1. PROMPT INICIAL — PROYECTO ORIGINAL

Quiero desarrollar un mod para Minecraft 1.21.1 con NeoForge que se llame **Power Grid Economy**.

El mod debe integrarse con:

- Create 6.0.10
- Create: Power Grid 0.6.1 para NeoForge 1.21.1
- Create: Numismatics 1.1.0 para NeoForge 1.21.1
- Architectury API 13.0.8

Quiero crear un bloque eléctrico que funcione como un punto de suministro/venta de electricidad.

El bloque tendrá cuatro terminales eléctricos de cobre en sus lados:

- entrada positiva
- entrada negativa
- salida positiva
- salida negativa

Los cuatro terminales son conexiones eléctricas reales. No son decoración.

El bloque debe funcionar como un interruptor físico de la red eléctrica: cuando el servicio está activo deja pasar la electricidad desde la entrada hasta la salida; cuando el servicio se termina o se agota, debe abrir las conexiones y cortar físicamente el suministro.

El bloque debe permitir guardar un propietario mediante una tarjeta **Create: Numismatics ID Card**. Si hay una tarjeta insertada, solamente el jugador cuya identidad coincida con ella puede modificar, configurar o romper el bloque cuando esté agachado/sneaking y haga clic. Si no hay tarjeta, cualquier jugador que pueda interactuar con el bloque podrá entrar al editor.

El propietario debe poder configurar planes de servicio.

## Plan por tiempo

El propietario establece un precio por día.

El comprador selecciona una cantidad de días, por ejemplo 10 días.

El precio final debe ser:

`precio_por_día × días_comprados`

Después del pago, el suministro queda activo durante el tiempo comprado. Cuando el tiempo termina, el bloque corta físicamente la electricidad.

## Plan por energía

El propietario establece un precio por unidad de energía.

Por ejemplo:

`1 MWh = X monedas`

El comprador selecciona una cantidad de energía, por ejemplo 10 MWh.

El precio final debe ser:

`precio_por_unidad × energía_comprada`

Después del pago, el bloque entrega la energía comprada y debe descontar el consumo real de electricidad que atraviese el servicio usando la API real de Create: Power Grid.

Cuando la energía restante llega a cero, el bloque debe cortar físicamente la electricidad.

Si el comprador vuelve a comprar energía, el servicio debe reconectarse.

## Dinero

Los pagos deben utilizar las APIs reales de Create: Numismatics y su sistema de cuentas/monedas. El dinero debe llegar al propietario correspondiente cuando corresponda.

No quiero un sistema de dinero falso si Numismatics permite utilizar su sistema real.

## Persistencia

Toda la información importante debe guardarse en el BlockEntity/NBT y sobrevivir al reinicio del mundo/servidor:

- propietario
- identidad de la ID Card
- tipo de plan
- precio
- comprador
- tiempo comprado/restante
- energía comprada/restante
- consumo acumulado
- estado del servicio

## GUI

Quiero posteriormente una interfaz gráfica completa para configurar y comprar planes.

Debe diferenciar claramente las funciones del propietario y del comprador.

## Roadmap

0.1 — bloque eléctrico, BlockEntity, propietario, compra básica, contador y persistencia.

0.2 — GUI completa.

0.3 — paquetes/planes de servicio.

0.4 — integración real con Numismatics.

0.5 — múltiples clientes/usuarios.

0.6 — empresas/proveedores.

0.7 — estadísticas, medidores y consumo histórico.

No inventar APIs de Power Grid o Numismatics. Antes de usar una clase o método de esos mods hay que comprobar que realmente existe para las versiones indicadas.

El proyecto debe mantenerse compatible con Minecraft 1.21.1 + NeoForge 21.1.x + Java 21.

---

# 2. EDICIÓN — DEPENDENCIAS Y GRADLE

Configurar el proyecto para Minecraft 1.21.1, NeoForge 21.1.250 y Java 21.

Usar:

- Create 6.0.10-280
- Ponder 1.0.82
- Flywheel 1.0.6
- Registrate MC1.21-1.3.0+67
- Architectury 13.0.8
- Power Grid 0.6.1
- Numismatics 1.1.0

Debido a dependencias transitivas que no son necesarias para compilar este mod, Create debe declararse con `slim` y sin transitivas:

```gradle
implementation("com.simibubi.create:create-${minecraft_version}:${create_version}:slim") {
    transitive = false
}
```

No añadir JEI, FTB Chunks, FTB Teams, FTB Library, JourneyMap, Curios o CC:Tweaked solamente por dependencias transitivas de Create si no son utilizadas directamente por el mod.

---

# 3. EDICIÓN — TRABAJO DIRECTO EN GITHUB

El repositorio del proyecto es:

`indoorhades/Power-Grid-Economy`

Trabajar directamente sobre la rama principal cuando sea posible.

Antes de modificar un archivo existente, leer su contenido actual y utilizar su SHA actual para evitar sobrescribir cambios recientes.

Después de cada modificación importante, registrar el cambio en `CONTINUIDAD_POWER_GRID_ECONOMY.md`.

---

# 4. EDICIÓN — CREACIÓN DEL BLOQUE

Crear el bloque `electrical_service_block` con su BlockItem y BlockEntity.

La clase principal debe ser `ElectricalServiceBlock` y extender `BaseEntityBlock`.

El BlockEntity debe ser `ElectricalServiceBlockEntity`.

El bloque debe poder mostrar inicialmente su estado de servicio al hacer clic.

La implementación inicial puede utilizar un estado de servicio básico mientras se integra la API real de Power Grid.

---

# 5. EDICIÓN — MODELO BLOCKBENCH

El bloque debe utilizar la geometría procedente del modelo exportado desde Blockbench 5.1.6.

El modelo original contiene 9 cubos:

1. cuerpo principal
2. terminal superior delantero
3. terminal inferior delantero
4. terminal inferior trasero
5. terminal superior trasero
6. puente central
7. pieza pequeña inferior 1
8. pieza pequeña inferior 2
9. pieza pequeña inferior 3

La geometría debe conservar la intención del modelo original y utilizar la textura del bloque.

---

# 6. EDICIÓN — LOS CUATRO PINES SON ELÉCTRICOS

Los cuatro terminales laterales de cobre no son solamente parte visual del modelo.

Deben representar conexiones reales del sistema eléctrico:

- positivo de entrada
- negativo de entrada
- positivo de salida
- negativo de salida

El objetivo final es que Power Grid pueda conectarse físicamente al bloque y que el bloque pueda abrir/cerrar el circuito.

No implementar todavía una API inventada. Investigar el código fuente/API real de Power Grid 0.6.1 antes de escribir la integración.

---

# 7. EDICIÓN — TEXTURA REAL

Utilizar la textura real proporcionada para el bloque:

`assets/powergrideconomy/textures/block/texture.png`

La textura debe estar físicamente dentro del repositorio, no solamente referenciada en documentación.

El modelo debe utilizar:

`powergrideconomy:block/texture`

para la textura principal y de partículas.

---

# 8. EDICIÓN — PROPIETARIO MEDIANTE NUMISMATICS ID CARD

Añadir al BlockEntity la información necesaria para almacenar la identidad del propietario obtenida mediante una Create: Numismatics ID Card.

Reglas:

- Sin ID Card: cualquier jugador con permiso normal de interacción puede abrir el editor.
- Con ID Card: solamente la identidad correspondiente puede editar/configurar el bloque.
- Para editar/configurar o romper el bloque protegido, el propietario debe estar agachado/sneaking y hacer clic.
- No permitir que otro jugador modifique la configuración protegida.

La identidad debe persistir en NBT.

Antes de implementar la extracción de identidad, comprobar la API real de Numismatics 1.1.0.

---

# 9. EDICIÓN — PLAN POR TIEMPO

Implementar un plan de electricidad basado en tiempo.

El propietario establece `precioPorDia`.

El comprador selecciona `diasComprados`.

Calcular:

`precioTotal = precioPorDia * diasComprados`

Al completarse el pago, guardar el tiempo comprado y activar el servicio.

El tiempo restante debe disminuir con el tiempo real del servidor y persistir.

Cuando llegue a cero:

- marcar servicio como agotado
- abrir físicamente el circuito eléctrico
- impedir que continúe el suministro

Una nueva compra debe volver a activar el servicio.

---

# 10. EDICIÓN — PLAN POR ENERGÍA

Implementar un plan basado en consumo energético real.

El propietario establece `precioPorUnidadEnergia`.

El comprador selecciona `energiaComprada`.

Calcular:

`precioTotal = precioPorUnidadEnergia * energiaComprada`

Guardar el saldo energético restante.

El saldo debe disminuir mediante el consumo real que atraviesa el bloque, no mediante un contador ficticio de ticks.

Cuando el saldo llegue a cero, abrir el circuito y cortar el suministro.

Una nueva compra debe añadir energía al saldo y permitir la reconexión.

---

# 11. EDICIÓN — CONSUMO REAL DE POWER GRID

Investigar primero el código fuente de Create: Power Grid 0.6.1 para Minecraft 1.21.1.

Identificar las clases/interfaces reales utilizadas para:

- nodos eléctricos
- conexiones
- potencia/energía
- transferencia
- consumo
- estado de conexión
- carga eléctrica

No inventar nombres de clases, métodos o paquetes.

Una vez identificada la API real, implementar el bloque como nodo eléctrico compatible con Power Grid.

El consumo contabilizado por Power Grid debe ser la fuente de verdad para el plan energético.

---

# 12. EDICIÓN — INTERRUPTOR FÍSICO

El bloque debe tener dos estados eléctricos:

`CLOSED/ACTIVE` — las entradas están conectadas con las salidas.

`OPEN/CUT` — las entradas y salidas están eléctricamente separadas.

El estado debe cambiar automáticamente según:

- servicio comprado
- tiempo restante
- energía restante
- condiciones del sistema eléctrico

No basta con cambiar una textura o mostrar un mensaje. El circuito real debe quedar abierto cuando el servicio está cortado.

---

# 13. EDICIÓN — PAGOS CON NUMISMATICS

Integrar los pagos con la API real de Create: Numismatics 1.1.0.

El comprador debe pagar el precio calculado.

El dinero debe transferirse utilizando las cuentas/monedas reales de Numismatics.

No crear una economía paralela.

Si el plan pertenece a un propietario, el pago debe llegar a la cuenta correspondiente del propietario.

Validar saldo antes de confirmar la compra.

La compra debe ser atómica: si el pago falla, no debe añadirse tiempo ni energía.

---

# 14. EDICIÓN — PERSISTENCIA

Guardar mediante BlockEntity/NBT toda la información necesaria para reconstruir el servicio después de cerrar y volver a abrir el mundo.

Como mínimo:

- owner identity
- ID Card data/identity
- plan type
- price
- buyer identity
- purchased time
- remaining time
- purchased energy
- remaining energy
- total consumption
- service enabled/open state

Evitar perder saldos o reiniciar el servicio al reiniciar el servidor.

---

# 15. EDICIÓN — GUI

Crear una GUI para el bloque.

Debe permitir:

### Propietario

- insertar/configurar ID Card
- crear plan
- seleccionar plan por tiempo o energía
- establecer precio
- consultar ventas
- consultar consumo
- consultar estado

### Comprador

- ver el plan disponible
- seleccionar cantidad
- ver precio total
- confirmar compra
- consultar tiempo/energía restante

La GUI debe respetar las reglas de propietario y comprador.

---

# 16. ROADMAP DE DESARROLLO

## 0.1 — Núcleo eléctrico

- bloque
- BlockEntity
- cuatro terminales
- identidad
- planes básicos
- persistencia
- estado activo/cortado
- integración real con Power Grid

## 0.2 — GUI

- editor propietario
- comprador
- precios
- selección de cantidades

## 0.3 — Paquetes

- paquetes configurables
- diferentes servicios
- límites

## 0.4 — Economía

- Numismatics real
- pagos
- cuentas del propietario

## 0.5 — Clientes

- varios compradores
- contratos
- historial

## 0.6 — Empresas

- proveedores
- múltiples bloques
- administración

## 0.7 — Estadísticas

- consumo histórico
- medidores
- facturación
- estadísticas de uso

---

# 17. ESTADO ACTUAL DEL PROYECTO

Minecraft objetivo: **1.21.1**.

NeoForge: **21.1.250**.

Java: **21**.

Mod ID: `powergrideconomy`.

Versión actual del mod: `1.0.0`.

Repositorio: `indoorhades/Power-Grid-Economy`.

Ya existe:

- `ElectricalServiceBlock.java`
- `ElectricalServiceBlockEntity.java`
- registro del bloque
- registro del BlockEntity
- BlockItem
- blockstate
- modelo del bloque
- modelo del ítem
- textura física `texture.png`
- dependencias base de Create/Power Grid/Numismatics/Architectury

La clase `ElectricalServiceBlock` ya tiene ticker de servidor y muestra el estado del servicio. El contador actual de ticks es solamente una base temporal; **no debe confundirse con consumo real de Power Grid**.

El modelo actual utiliza los 9 elementos de la geometría adaptada del modelo de Blockbench y la textura `powergrideconomy:block/texture`.

---

# 18. CAMBIOS IMPORTANTES YA REALIZADOS

### Dependencias

Se corrigió la resolución excesiva de dependencias de Create utilizando `slim` y `transitive = false`.

### Bloque

Se creó `ElectricalServiceBlock` como `BaseEntityBlock`.

### BlockEntity

Se creó `ElectricalServiceBlockEntity` como contenedor persistente inicial.

### Error de compilación

Se corrigió el conflicto de variable `level` dentro del ticker cambiando el parámetro de lambda a `tickLevel`.

### Modelo

Se adaptó el modelo de Blockbench a JSON de modelo de bloque.

### Pines

Se estableció que los cuatro pines de cobre representan las cuatro conexiones eléctricas reales previstas.

### Textura

La textura `texture.png` fue colocada físicamente en:

`powergrideconomy-template-1.21.1/src/main/resources/assets/powergrideconomy/textures/block/texture.png`

---

# 19. REGLA PARA TODAS LAS FUTURAS EDICIONES

**Cada vez que se modifique el código, se añada una función, se corrija un error o se cambie un requisito importante, añadir inmediatamente una nueva sección aquí.**

Formato obligatorio:

```md
# EDICIÓN N — TÍTULO

## Prompt
[Prompt reutilizable que describe exactamente lo solicitado]

## Cambios realizados
- archivo X
- archivo Y
- comportamiento añadido

## Estado
- hecho
- pendiente
- problemas conocidos
```

No borrar las entradas anteriores. Este documento es un historial acumulativo.

---

# 20. PROMPT MAESTRO PARA CONTINUAR EN OTRO CHAT

Estoy continuando el desarrollo de mi mod **Power Grid Economy** para Minecraft 1.21.1 con NeoForge 21.1.x y Java 21.

Repositorio GitHub:

`indoorhades/Power-Grid-Economy`

Lee primero `CONTINUIDAD_POWER_GRID_ECONOMY.md` y utiliza ese archivo como memoria del proyecto.

El mod debe integrarse con Create 6.0.10, Create: Power Grid 0.6.1, Create: Numismatics 1.1.0 y Architectury 13.0.8.

El objetivo principal es un bloque de servicio eléctrico con cuatro terminales reales de Power Grid: positivo/negativo de entrada y positivo/negativo de salida. El bloque funciona como interruptor físico: permite el paso eléctrico mientras existe un servicio comprado y abre el circuito cuando el servicio termina.

Debe existir propiedad mediante Create: Numismatics ID Card, planes por tiempo y por energía, pagos reales mediante Numismatics, consumo real medido mediante Power Grid, persistencia en BlockEntity/NBT y GUI.

**Regla fundamental:** antes de implementar cualquier integración con Power Grid o Numismatics, comprobar las APIs reales de las versiones exactas instaladas. No inventar clases, métodos o paquetes.

Trabaja directamente sobre el repositorio cuando sea posible. Antes de editar un archivo existente, léelo y conserva sus cambios actuales. Después de cada cambio importante, actualiza también `CONTINUIDAD_POWER_GRID_ECONOMY.md` con una nueva entrada individual que contenga el prompt reutilizable, cambios realizados y estado.

No reinicies ni reemplaces funcionalidades que ya estén hechas sin comprobar primero el estado actual del repositorio.

Continúa desde el siguiente elemento pendiente del roadmap y prioriza primero una implementación compilable y verificable antes de añadir funciones grandes.
