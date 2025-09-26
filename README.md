<p align="center">
  <img src="docs/logo.png" alt="Notagus logo" width="96" height="96"/>
</p>

<h1 align="center">Notagus</h1>

<p align="center">
  Gestor personal de <b>tareas, notas, listas y calendario</b> para Android.<br/>
  Proyecto nativo en <b>Java</b> con <b>Room (SQLite)</b>, <b>RecyclerView</b>, <b>Material Design</b> y <b>notificaciones</b>.
</p>

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

## 🧰 Herramientas y stack (nivel “complejo”)

- **Lenguaje / Plataforma**
  - Java (Android), Android SDK (`compileSdk = 36`, `minSdk = 24`)
- **Arquitectura y Persistencia**
  - Room (SQLite) con DAOs, entidades y migraciones
  - `DatabaseClient` como **Singleton** de acceso a `AppDatabase`
- **UI / UX**
  - RecyclerView + ViewHolders/Adapters
  - Fragments (Tasks, Notes, Calendar)
  - Material Components (temas: claro/oscuro/rosa), FAB, BottomNav
- **Notificaciones / Sistema**
  - NotificationManager + canales; compatibilidad API 24+
  - App Widget de tareas (provider + views + actualizaciones)
- **Build / Tooling**
  - Gradle (KTS), wrapper incluido
  - `.gitignore` para Android/Gradle
- **Pruebas**
  - `test/` (unit) y `androidTest/` (instrumentadas)

---

## 🚀 Instalación

1. Clonar el repositorio
git clone https://github.com/Agustinpujol12/Notagus.git
2. Abrir en Android Studio
Esperar la indexación inicial.
3. Sincronizar dependencias con Gradle
(Android Studio lo propone automáticamente).
4. Ejecutar en un dispositivo o emulador con Android 7.0 (API 24) o superior.

