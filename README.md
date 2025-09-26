<div align="center">

# Notagus 📱

**Gestor personal de tareas, notas, listas y calendario para Android**  
Proyecto propio en **Java + Room**, usado a diario como herramienta de organización.

[![Made with Java](https://img.shields.io/badge/Made%20with-Java-orange.svg)](#)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84.svg)](#)
[![Gradle KTS](https://img.shields.io/badge/Build-Gradle%20KTS-02303A.svg)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Last commit](https://img.shields.io/github/last-commit/Agustinpujol12/Notagus.svg)](#)

</div>

---

## ✨ Funcionalidades

- ✅ **Tareas**: crear, editar, fijar, completar; subtareas; recordatorios.
- 📝 **Notas**: editor con título y cuerpo; pin/unpin.
- 📅 **Calendario**: vista mensual; tareas vinculadas por fecha.
- 📋 **Listas**: listas personalizadas con ítems chequeables.
- 🎨 **Temas**: claro, oscuro y rosa; header y FAB adaptados.
- 💾 **Persistencia**: Room con migraciones versionadas.
- 🧩 **Widget**: de tareas para la pantalla de inicio.

---

## 🖼️ Capturas (opcional)

> Colocá tus imágenes en `docs/` y descomentá estas líneas.

<!--
<p align="center">
  <img src="docs/screenshot_main.png" alt="Home" width="280"/>
  <img src="docs/screenshot_tasks.png" alt="Tareas" width="280"/>
  <img src="docs/screenshot_calendar.png" alt="Calendario" width="280"/>
</p>
-->

---

## 🛠️ Stack técnico

| Área | Tecnología |
|---|---|
| Lenguaje | **Java (Android)** |
| SDK | `compileSdk = 36`, `minSdk = 24` |
| Persistencia | **Room (SQLite)**, DAOs, **Singleton** de acceso |
| UI | **RecyclerView + Adapters**, Material Components |
| Notificaciones | NotificationManager + Channels |
| Build | **Gradle (KTS)**, wrapper incluido |

---

## 🚀 Instalación

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/Agustinpujol12/Notagus.git
