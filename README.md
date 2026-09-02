<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/67bfef19-987d-4774-ad22-bf376ed118e4

---

## 🚀 Guía y Lista de Verificación para Publicar en Google Play Store

Antes de publicar o enviar a revisión en **Google Play Console**, ten en cuenta los siguientes puntos críticos y disclaimers:

### 1. Documentación de Seguridad y Privacidad (Obligatorio)
- **Cuestionario de Seguridad de Datos (Data Safety):** Consulta el archivo [`DATA_SAFETY.md`](DATA_SAFETY.md) para copiar y pegar las respuestas exactas y requeridas por Google Play sobre el tratamiento de correos, cifrado TLS/SSL y eliminación de datos.
- **Política de Privacidad Pública:** Google Play exige ingresar una URL accesible en la ficha de la tienda. Puedes enlazar directamente el archivo [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) alojado en tu repositorio público de GitHub (o mediante GitHub Pages).

### 2. Disclaimers para la Ficha de la Tienda (Play Store Listing)
- **Contraseñas de Aplicación (App Passwords):**
  - Informar a los usuarios de proveedores con autenticación de dos factores (ej. Gmail, Yahoo, Outlook) que para vincular su cuenta IMAP/SMTP deben generar una *"Contraseña de aplicación"* en los ajustes de seguridad de su cuenta Google/proveedor, en lugar de su contraseña principal.
- **Arquitectura Local-First:**
  - Destacar con claridad que Riso no guarda correos ni contraseñas en servidores de la app; todo reside localmente en la base de datos segura del dispositivo.
- **Claves de API Personales (BYOK - Bring Your Own Key):**
  - Recordar que para utilizar modelos de lenguaje avanzados (Gemini, OpenAI, Anthropic), los usuarios configuran sus propias claves de API, las cuales se transmiten directamente al proveedor correspondiente sin intermediarios.

### 3. Funcionalidades de Cumplimiento de Políticas Integradas en la App
- **Derecho al Olvido (Data Deletion):** La app incluye en *Ajustes > Transparencia y Privacidad* un botón para purgar irrevocablemente todos los chats, cuentas y credenciales almacenadas.
- **Exclusión de Backups en la Nube:** Las contraseñas y base de datos local están excluidas deliberadamente de los backups automáticos de Android (`data_extraction_rules.xml` y `backup_rules.xml`) para prevenir filtraciones de credenciales.

### 4. Checklist Técnico y de Configuración en Google Play Console
- **Credenciales de Prueba para los Revisores de Google (App Access / Acceso a la aplicación):**
  - Google Play exige probar la app con una cuenta real durante la revisión. En la sección *"Contenido de la aplicación > Acceso a la aplicación"*, proporciona credenciales de una cuenta de prueba de correo (o explica cómo configurar una cuenta IMAP de prueba con su contraseña de aplicación) para que los revisores no rechacen la app por no poder avanzar.
- **Declaración de IA Generativa (AI-Generated Content Policy):**
  - Dado que Riso interactúa con LLMs (Gemini, Claude, OpenAI), debes marcar en la consola que la app contiene o interactúa con contenido generado por IA y que cuenta con mecanismos para que el usuario reporte contenido inapropiado o reinicie la conversación.
- **Generación del App Bundle (.aab) firmado para producción:**
  - Google Play ya no acepta `.apk` para lanzamientos nuevos; requiere **Android App Bundle (.aab)**.
  - Para generar el AAB firmado en Android Studio: `Build > Generate Signed Bundle / APK > Android App Bundle`.
  - Asegúrate de guardar y respaldar con seguridad tu archivo `.jks` (Keystore) y sus contraseñas; si pierdes la clave de firma, no podrás actualizar la app en la tienda.
- **Requisitos de la Ficha Gráfica:**
  - **Ícono de la app:** PNG de 512 x 512 px (máximo 1 MB, canal alfa no transparente).
  - **Gráfico de funciones (Feature Graphic):** PNG o JPEG de 1024 x 500 px.
  - **Capturas de pantalla:** Mínimo 2 capturas de teléfono (relación de aspecto 16:9 o 9:16, entre 320 px y 3840 px).
- **Clasificación de Contenido (IARC):**
  - Al completar el cuestionario de clasificación de edad, declara que la app es una herramienta de productividad / correo electrónico que permite el intercambio de mensajes entre usuarios vía correo.

---

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
