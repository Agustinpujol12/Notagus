<p align="center">
  <img src="docs/ic_launcher_round.webp" alt="Notagus logo" width="160"/>
</p>

<h1 align="center">Notagus</h1>

<p align="center">
  Gestor personal de <b>tareas, notas, listas y calendario</b> para Android.<br/>
  Proyecto nativo en <b>Java</b> con <b>Room (SQLite)</b>, <b>RecyclerView</b>, <b>Material Design</b> y <b>notificaciones</b>.
</p>

<p align="center">
  <a href="https://github.com/Agustinpujol12/Notagus/releases">
    <img src="https://img.shields.io/badge/Android-API%2024%2B-3DDC84" alt="Android API"/>
  </a>
  <img src="https://img.shields.io/badge/Made%20with-Java-orange" alt="Java"/>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License: MIT"/>
  </a>
  <img src="https://img.shields.io/github/last-commit/Agustinpujol12/Notagus" alt="Last commit"/>
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

---

## ⬇️ Descarga

- [APK más reciente (Releases)](../../releases)

---

## 🖼️ Capturas de pantalla

<p align="center">
  <img src="docs/home.jpeg" alt="Pantalla principal" width="250" style="margin:15px"/>
  <img src="docs/calendar.jpeg" alt="Calendario" width="250" style="margin:15px"/>
  <img src="docs/note.jpeg" alt="Notas" width="250" style="margin:15px"/>
</p>

<p align="center">
  <img src="docs/newtask.jpeg" alt="Nueva tarea" width="250" style="margin:15px"/>
  <img src="docs/editnote.jpeg" alt="Editar nota" width="250" style="margin:15px"/>
  <img src="docs/settings.jpeg" alt="Configuración" width="250" style="margin:15px"/>
</p>

---

## 🔒 Permisos & Privacidad

- 🔔 **Notificaciones**: se utilizan únicamente para recordatorios de tareas.  
- 💾 **Almacenamiento local**: todos los datos se guardan en el dispositivo mediante Room (SQLite).  
- 🚫 **Sin conexión a servidores externos**: los datos son privados y permanecen solo en tu dispositivo.  
