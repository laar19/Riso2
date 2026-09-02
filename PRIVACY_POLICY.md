# Política de Privacidad de Riso (Privacy Policy)

**Última actualización:** Septiembre de 2026

La presente Política de Privacidad describe la forma en que **Riso** (en adelante, "la Aplicación", "nosotros" o "nuestro") trata la información y los datos del usuario cuando utiliza la aplicación móvil.

---

## 1. Principio Fundamental: Arquitectura Local-First
Riso está construida bajo una filosofía de máxima privacidad y soberanía de datos para el usuario:
- **Sin servidores intermedios:** La Aplicación no mantiene servidores propios, bases de datos en la nube de desarrollo ni sistemas de telemetría o analítica de comportamiento.
- **Almacenamiento en el dispositivo:** Toda la información referente a tus cuentas de correo (direcciones, credenciales IMAP/SMTP cifradas localmente), historial de chats con la IA, notas y configuraciones se almacena estrictamente en la memoria interna y en la base de datos local SQLite/Room del dispositivo del usuario.

---

## 2. Información que Procesa la Aplicación y su Finalidad

### A. Cuentas de Correo Electrónico (IMAP y SMTP)
- **Datos procesados:** Dirección de correo electrónico, servidor de entrada (IMAP), servidor de salida (SMTP), puerto y contraseña de aplicación o token de autenticación.
- **Finalidad:** Permitir la lectura de correos de tu bandeja de entrada, la redacción y el envío de mensajes a solicitud explícita tuya.
- **Destino:** Las credenciales se utilizan exclusivamente para establecer conexiones directas y cifradas (SSL/TLS) entre tu dispositivo móvil y los servidores oficiales de tu proveedor de correo (ej. Gmail, Outlook, Yahoo, servidores privados). No se transmiten jamás a terceros.

### B. Consultas al Asistente y Claves de API (LLM)
- **Datos procesados:** Mensajes de texto, prompts, imágenes adjuntas voluntariamente y claves de API personales (Google Gemini, OpenAI, Anthropic Claude, Brave Search).
- **Finalidad:** Procesar las instrucciones, redactar respuestas, consultar la web en tiempo real o resumir mensajes mediante los modelos de lenguaje.
- **Destino:** Las consultas y claves viajan directamente y de forma encriptada vía HTTPS a los endpoints oficiales del proveedor de IA que tú elijas. Riso no intercepta ni almacena tus claves en ningún servidor externo.

### C. Audio y Voz
- **Datos procesados:** Muestras de audio capturadas al mantener presionado el botón de micrófono.
- **Finalidad:** Transcribir comandos de voz a texto para interactuar con la aplicación. El procesamiento se realiza localmente o a través del transcriptor del sistema operativo Android.

---

## 3. Ausencia de Publicidad y Venta de Datos
- **Cero Venta de Datos:** No vendemos, no alquilamos ni comercializamos ningún tipo de información personal, contenido de correos o credenciales de nuestros usuarios.
- **Sin SDKs Publicitarios:** La aplicación no incluye bibliotecas de publicidad de terceros (Google AdMob, Unity Ads, Facebook Audience Network, etc.), ni rastreadores de huella digital ni analítica invasiva.

---

## 4. Retención y Eliminación de Datos (Derecho al Olvido)
El usuario mantiene en todo momento el control absoluto sobre su información:
1. **Eliminación Selectiva:** Puedes eliminar sesiones individuales de chat o desvincular cuentas de correo en cualquier momento desde la interfaz.
2. **Purga Total ("Eliminar Todos Mis Datos"):** En la pantalla de **Ajustes > Transparencia y Privacidad**, la app ofrece la función **"Eliminar Todos Mis Datos de la App"**, la cual borra irreversiblemente todas las bases de datos locales, historial, perfiles y claves guardadas.
3. **Desinstalación:** Al desinstalar la aplicación del dispositivo, Android elimina automáticamente todos los datos locales asociados a la app gracias a las directivas de exclusión de backup configuradas.

---

## 5. Contacto del Desarrollador
Si tienes dudas, consultas o requieres asistencia sobre la privacidad y el tratamiento de datos de Riso, puedes comunicarte a través del correo de soporte proporcionado en la ficha de Google Play Store:
- **Correo Electrónico:** taomei2019@gmail.com
