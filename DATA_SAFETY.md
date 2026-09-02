# Guía Oficial para el Cuestionario de Seguridad de los Datos (Data Safety Section) - Google Play Console

Esta guía contiene las respuestas exactas, precisas y legales que debes marcar en **Google Play Console** para que tu app **Riso** sea aprobada sin observaciones ni suspensiones.

---

## 1. Recopilación y Uso Compartido de Datos (Data Collection & Sharing)

### ¿Tu aplicación recopila o comparte datos de usuarios requeridos de algún tipo?
- **Respuesta:** **Sí**
- *Justificación*: La aplicación procesa correos electrónicos e interactúa con APIs de IA externas (Google Gemini, OpenAI, Claude) o servidores de correo (IMAP/SMTP) según la configuración del usuario.

### ¿Todos los datos de usuarios recopilados por la app se cifran en tránsito?
- **Respuesta:** **Sí**
- *Justificación*: Todas las conexiones de red (HTTPS con TLS 1.3 / SSL para IMAP y SMTP) están cifradas punto a punto.

### ¿Proporcionas un mecanismo para que los usuarios soliciten la eliminación de sus datos?
- **Respuesta:** **Sí**
- *Justificación*: Dentro de la aplicación existe la opción de borrado permanente e irrevocable en **Ajustes > Transparencia y Privacidad > Eliminar Todos Mis Datos de la App**.

---

## 2. Tipos de Datos Específicos (Data Types)

### A. Información Personal (Personal Info)
1. **Dirección de correo electrónico (Email address)**:
   - **¿Se recopila?**: Sí.
   - **¿Se comparte?**: No (no se comparte con intermediarios ni anunciantes).
   - **¿Es efímero?**: No, se guarda localmente en el dispositivo para gestionar la bandeja de entrada del usuario.
   - **¿Es obligatorio u opcional?**: Opcional (el usuario decide vincular cuentas de correo).
   - **Fines de uso**:
     - *Funcionalidad de la aplicación (App functionality)*: Marcado.
     - *Gestión de cuentas (Account management)*: Marcado.

### B. Mensajes (Messages)
1. **Correos electrónicos (Emails)**:
   - **¿Se recopila?**: Sí (se leen cabeceras y contenido para mostrar la bandeja y permitir la asistencia por IA).
   - **¿Se comparte?**: No con terceros con fines publicitarios. Se transmite de forma directa y exclusiva a los endpoints de la API del modelo seleccionado por el usuario (ej. Google Gemini API) o servidores IMAP/SMTP.
   - **Fines de uso**: *Funcionalidad de la aplicación (App functionality)*.

### C. Archivos y Documentos (Files and Docs) / Fotos y Videos (Photos and Videos)
1. **Fotos / Imágenes (Photos)**:
   - **¿Se recopila?**: Sí, cuando el usuario decide adjuntar una imagen o captura para análisis multimodal.
   - **¿Se comparte?**: No.
   - **Fines de uso**: *Funcionalidad de la aplicación (App functionality)*.

### D. Audio (Audio files)
1. **Grabaciones de voz (Voice or sound recordings)**:
   - **¿Se recopila?**: Procesamiento local / efímero para reconocimiento de voz y comandos.
   - **¿Es efímero?**: Sí.
   - **Fines de uso**: *Funcionalidad de la aplicación (App functionality)*.

### E. Información de la App y Rendimiento (App info and performance)
- **Registros de fallos (Crash logs)**: No (no se recopilan mediante SDKs de terceros ni se envían a servidores propios).
- **Diagnósticos**: No.

### F. Identificadores de Dispositivo u Otros (Device or other IDs)
- **¿Recopila ID de publicidad (AD_ID)?**: **No**. La aplicación no incluye publicidad ni rastreadores publicitarios.

---

## 3. Prácticas de Seguridad Declaradas
- **Cifrado en tránsito**: Sí (HTTPS / TLS / SSL).
- **Solicitud de eliminación de datos**: Sí (mediante el botón en la app y por correo al desarrollador).
- **Cumplimiento de la Política de Familias**: N/A (la app no está dirigida específicamente a menores de 13 años).

---

## 4. Declaración de Permisos en Primer Plano / Servicios (Foreground Services)
- La aplicación **no** utiliza servicios en segundo plano persistentes sin consentimiento (`FOREGROUND_SERVICE_SPECIAL_USE`). Todas las peticiones a APIs de IA y lectura de correos son disparadas por interacción directa del usuario en primer plano.
