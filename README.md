# Power Grid Economy

Prepaid electricity economy for Minecraft 1.21.1 NeoForge.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.x
- Create: Power Grid 0.6.1 NeoForge
- Create: Numismatics 1.1.0 NeoForge
- Java 21

## Core behavior

The **Medidor eléctrico prepago** sits between a Power Grid source and a customer's circuit. It has four electrical terminals:

- Input +
- Input -
- Output +
- Output -

The owner can crouch-right-click to edit the meter. If an ID card from Create: Numismatics is inserted, only the UUID bound to that ID card can edit the meter. With no ID card inserted, anyone can configure it.

The owner creates one active plan per meter:

- **Tiempo:** price per day. The buyer chooses how many days to purchase.
- **Energía:** price per MWh. The buyer chooses how many MWh to purchase.

The buyer pays through a bound Create: Numismatics bank/authorized card. The seller receives the payment in their Numismatics account when available.

Energy plans are consumed from actual simulated Power Grid electrical power. When the balance reaches zero, both electrical paths are opened automatically. Buying another plan reconnects the meter.

## Build

Open a terminal in the repository root and run:

```bat
gradlew.bat build
```

The compiled jar is produced under `build/libs/`.

> This project is designed for the 1.21.1 Power Grid API. Power Grid is required at runtime; the dependency is pinned to the 0.6.1 release used by this project.
