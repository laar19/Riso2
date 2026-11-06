package com.example.service.email

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AppSetting
import com.example.data.repository.RisoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.net.ssl.SSLHandshakeException

data class RisoEmail(
    val id: String,
    val sender: String,
    val recipient: String,
    val subject: String,
    val snippet: String,
    val body: String,
    val date: String,
    val isRead: Boolean
)

data class EmailAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emailAddress: String,
    val imapServer: String,
    val imapPort: String = "993",
    val smtpServer: String = "smtp.gmail.com",
    val smtpPort: String = "587",
    val passwordVal: String,
    val isEnabled: Boolean = true
)

data class InboxSummary(
    val emails: List<RisoEmail>,
    val totalCount: Int,
    val unreadCount: Int,
    val accountEmail: String,
    val isLiveImap: Boolean,
    val errorMessage: String? = null
)

class EmailService(private val repository: RisoRepository) {

    private val TAG = "EmailService"

    private fun isCertificateException(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val name = current.javaClass.name
            val message = current.message ?: ""
            if (current is SSLHandshakeException ||
                name.contains("CertPathValidatorException") ||
                name.contains("SSLPeerUnverifiedException") ||
                message.contains("CertPathValidatorException", ignoreCase = true) ||
                message.contains("Trust anchor for certification path not found", ignoreCase = true) ||
                (message.contains("certificate", ignoreCase = true) && (message.contains("valid", ignoreCase = true) || message.contains("untrusted", ignoreCase = true)))
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    suspend fun testEmailAccountConnection(acc: EmailAccount): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (acc.emailAddress.isBlank() || acc.passwordVal.isBlank()) {
            return@withContext Pair(false, "Credenciales vacías")
        }
        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", acc.imapServer.ifBlank { "imap.gmail.com" })
                put("mail.imaps.port", acc.imapPort.ifBlank { "993" })
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
                put("mail.imaps.connectiontimeout", "8000")
                put("mail.imaps.timeout", "8000")
            }
            val session = Session.getInstance(props, null)
            val store = session.getStore("imaps")
            store.connect(acc.imapServer.ifBlank { "imap.gmail.com" }, acc.emailAddress, acc.passwordVal)
            val inbox = store.getFolder("INBOX")
            var count = 0
            var unread = 0
            try {
                inbox.open(Folder.READ_ONLY)
                count = inbox.messageCount
                val uc = inbox.unreadMessageCount
                unread = if (uc >= 0) uc else {
                    val unreadTerm = javax.mail.search.FlagTerm(Flags(Flags.Flag.SEEN), false)
                    inbox.search(unreadTerm).size
                }
                inbox.close(false)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "testEmailAccountConnection notice: ${e.message}")
                }
            }
            store.close()
            Pair(true, "Conectado ($count mensajes, $unread sin leer)")
        } catch (e: Exception) {
            val msg = if (isCertificateException(e)) {
                "El certificado del servidor no es válido"
            } else {
                e.localizedMessage ?: e.message ?: "Fallo de autenticación IMAP"
            }
            Pair(false, msg)
        }
    }

    private suspend fun getActiveCredentials(): Map<String, String> {
        val activeId = repository.getSetting("active_email_account_id") ?: ""
        val accountsJson = repository.getSetting("email_accounts_json") ?: ""
        
        if (accountsJson.isNotBlank()) {
            try {
                val array = org.json.JSONArray(accountsJson)
                if (array.length() > 0) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id == activeId || obj.optString("emailAddress") == activeId) {
                            val pass = (if (id.isNotBlank()) repository.getSecret("email_pass_$id") else null)
                                ?: obj.optString("passwordVal")
                            return mapOf(
                                "email_address" to obj.optString("emailAddress"),
                                "email_password" to pass,
                                "imap_server" to obj.optString("imapServer"),
                                "imap_port" to obj.optString("imapPort", "993"),
                                "smtp_server" to obj.optString("smtpServer"),
                                "smtp_port" to obj.optString("smtpPort", "587")
                            )
                        }
                    }
                    // Fallback to first configured account
                    val first = array.getJSONObject(0)
                    val firstId = first.optString("id")
                    val pass = (if (firstId.isNotBlank()) repository.getSecret("email_pass_$firstId") else null)
                        ?: first.optString("passwordVal")
                    return mapOf(
                        "email_address" to first.optString("emailAddress"),
                        "email_password" to pass,
                        "imap_server" to first.optString("imapServer"),
                        "imap_port" to first.optString("imapPort", "993"),
                        "smtp_server" to first.optString("smtpServer"),
                        "smtp_port" to first.optString("smtpPort", "587")
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "getActiveCredentials error", e)
                }
            }
        }
        
        // Fallback to legacy single-account settings
        return mapOf(
            "email_address" to (repository.getSetting("email_address") ?: ""),
            "email_password" to (repository.getSecret("email_password") ?: repository.getSetting("email_password") ?: ""),
            "imap_server" to (repository.getSetting("imap_server") ?: ""),
            "imap_port" to (repository.getSetting("imap_port") ?: "993"),
            "smtp_server" to (repository.getSetting("smtp_server") ?: ""),
            "smtp_port" to (repository.getSetting("smtp_port") ?: "587")
        )
    }

    // High quality mock DB to allow frictionless offline out-of-the-box sandbox testing
    private val mockEmails = mutableListOf(
        RisoEmail(
            id = "msg_101",
            sender = "soporte@cloudservice.com",
            recipient = "yo@empresa.com",
            subject = "Alerta de Servidor: Uso de CPU > 92%",
            snippet = "Hemos detectado un pico anómalo de consumo en la instancia de base de datos...",
            body = "Estimado cliente,\n\nLe informamos que el nodo db-primary-01 superó el umbral del 92% de CPU durante los últimos 15 minutos.\n\nAcción recomendada: Verificar procesos activos o escalar recursos.\n\nSoporte Técnico CloudService.",
            date = "Hoy, 10:14 AM",
            isRead = false
        ),
        RisoEmail(
            id = "msg_102",
            sender = "laura.marketing@agenciadigital.com",
            recipient = "yo@empresa.com",
            subject = "Revisión final de diseño: Campaña Q3",
            snippet = "Hola equipo, comparto el enlace de Figma con los mockups aprobados por cliente...",
            body = "Hola equipo,\n\nYa tenemos los últimos ajustes en la paleta de colores y componentes según el feedback de ayer. Por favor confirmen si pasamos a producción.\n\nSaludos,\nLaura.",
            date = "Ayer, 4:45 PM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_103",
            sender = "notificaciones@github.com",
            recipient = "yo@empresa.com",
            subject = "[GitHub] Pull Request #42 merged into main",
            snippet = "feature/agent-integration successfully merged by lead-dev...",
            body = "Your pull request #42 (Feature: Agent planning protocol) was successfully merged into main branch.\n\nAll automated CI checks passed.",
            date = "Ayer, 11:20 AM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_104",
            sender = "recursoshumanos@corporativo.com",
            recipient = "yo@empresa.com",
            subject = "Recordatorio: Cierre de Evaluaciones de Desempeño",
            snippet = "Estimado colaborador, le recordamos completar su autoevaluación antes del viernes...",
            body = "Estimado colaborador,\n\nEl periodo de evaluación semestral cierra este viernes a las 18:00hs. Por favor ingrese al portal de RRHH para completar su formulario.\n\nAtentamente,\nDirección de Talento Humano.",
            date = "08 Jun 2026, 9:00 AM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_105",
            sender = "facturacion@telecom.net",
            recipient = "yo@empresa.com",
            subject = "Factura de Servicios Digitales - Mayo 2026",
            snippet = "Adjuntamos su comprobante de pago mensual correspondiente al abono de fibra óptica...",
            body = "Estimado cliente,\n\nSe ha emitido su factura digital por el periodo Mayo 2026 por un total de $45.00.\n\nFecha de vencimiento: 20 de Junio.\n\nGracias por su confianza.",
            date = "05 Jun 2026, 6:12 PM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_106",
            sender = "newsletter@techinsider.io",
            recipient = "yo@empresa.com",
            subject = "Tech Weekly: Modelos Locales en Móviles y Edge AI",
            snippet = "Descubre las últimas técnicas de cuantización 4-bit ejecutadas en procesadores ARM...",
            body = "Hola a todos,\n\nEn esta edición exploramos cómo los pequeños modelos de lenguaje y visión están transformando la privacidad de las aplicaciones móviles sin necesidad de nube centralizada.\n\n¡Que lo disfruten!",
            date = "01 Jun 2026, 8:00 AM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_107",
            sender = "martin.finanzas@startup.org",
            recipient = "yo@empresa.com",
            subject = "Presupuesto Operativo Q3 y Licencias",
            snippet = "Hola, adjunto los montos finales para la renovación de las API keys empresariales...",
            body = "Hola Juan,\n\nAquí tienes las estimaciones finales para renovar las API en la nube para el próximo semestre. Por favor revísalo con la mesa de control.\n\nSaludos cordiales.",
            date = "12 Jun 2026, 2:30 PM",
            isRead = false
        )
    )

    // Execute SMTP send email
    suspend fun sendEmail(to: String, subject: String, body: String): Boolean = withContext(Dispatchers.IO) {
        val creds = getActiveCredentials()
        val smtpServer = creds["smtp_server"] ?: ""
        val smtpPort = creds["smtp_port"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        val isMockAccount = email.endsWith("@riso.local") || imapServerMockCheck(creds["imap_server"] ?: "") || smtpServer.contains("riso.local")
        if (smtpServer.isBlank() || email.isBlank() || password.isBlank() || isMockAccount) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "sendEmail (MOCK)")
            }
            // Mock send addition
            val newMock = RisoEmail(
                id = "msg_sent_" + System.currentTimeMillis(),
                sender = email.ifBlank { "yo" },
                recipient = to,
                subject = subject,
                snippet = if (body.length > 60) body.substring(0, 60) + "..." else body,
                body = body,
                date = "Ahora mismo",
                isRead = true
            )
            mockEmails.add(0, newMock)
            return@withContext true
        }

        try {
            val isSslPort = smtpPort.trim() == "465"
            val properties = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.host", smtpServer)
                put("mail.smtp.port", smtpPort.ifBlank { if (isSslPort) "465" else "587" })
                put("mail.smtp.ssl.checkserveridentity", "true")
                if (isSslPort) {
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                }
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
            }

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(email, password)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(email))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(body)
            }

            Transport.send(message)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "sendEmail (SMTP)")
            }
            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                if (isCertificateException(e)) {
                    Log.e(TAG, "El certificado del servidor no es válido")
                } else {
                    Log.e(TAG, "sendEmail (SMTP) error", e)
                }
            }
            false
        }
    }

    private fun imapServerMockCheck(imap: String): Boolean {
        return imap.contains("riso.local")
    }

    // Fetch inbox summary with counts and filter support
    suspend fun fetchInboxSummary(limit: Int = 10, filter: String? = null): InboxSummary = withContext(Dispatchers.IO) {
        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val imapPort = creds["imap_port"] ?: "993"
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isBlank() || email.isBlank() || password.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "fetchInboxSummary (MOCK)")
            }
            val isUnreadOnly = filter?.contains("unread", ignoreCase = true) == true
            val filtered = if (isUnreadOnly) mockEmails.filter { !it.isRead } else mockEmails
            return@withContext InboxSummary(
                emails = filtered.take(limit),
                totalCount = mockEmails.size,
                unreadCount = mockEmails.count { !it.isRead },
                accountEmail = if (email.isNotBlank()) email else "usuario@riso.local",
                isLiveImap = false
            )
        }

        try {
            return@withContext fetchEmailsFromImapDetailed(imapServer, email, password, imapPort, "INBOX", limit, filter)
        } catch (e: Exception) {
            val isCert = isCertificateException(e)
            val errDetail = if (isCert) "El certificado del servidor no es válido" else (e.localizedMessage ?: e.message ?: "Error de conexión")
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "fetchInboxSummary error: $errDetail", e)
            }
            return@withContext InboxSummary(
                emails = emptyList(),
                totalCount = 0,
                unreadCount = 0,
                accountEmail = email,
                isLiveImap = false,
                errorMessage = if (isCert) "⚠️ El certificado del servidor no es válido" else "⚠️ No se pudo conectar a IMAP ($imapServer): $errDetail. Verifica tu contraseña de aplicación y que IMAP esté habilitado."
            )
        }
    }

    // List recent emails (First of the 10 core functions)
    suspend fun listInbox(limit: Int = 10): List<RisoEmail> = withContext(Dispatchers.IO) {
        fetchInboxSummary(limit).emails
    }

    // Search emails by query (Second of core functions)
    suspend fun searchEmails(query: String, limit: Int = 10): List<RisoEmail> = withContext(Dispatchers.IO) {
        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isBlank() || email.isBlank() || password.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "searchEmails (MOCK)")
            }
            val lowercaseQuery = query.lowercase()
            return@withContext mockEmails.filter {
                it.subject.lowercase().contains(lowercaseQuery) ||
                        it.sender.lowercase().contains(lowercaseQuery) ||
                        it.body.lowercase().contains(lowercaseQuery)
            }.take(limit)
        }

        try {
            // Simplistic search for testing, search all then filter
            val list = fetchEmailsFromImap(imapServer, email, password, "INBOX", 25)
            val lowercaseQuery = query.lowercase()
            return@withContext list.filter {
                it.subject.lowercase().contains(lowercaseQuery) ||
                        it.sender.lowercase().contains(lowercaseQuery) ||
                        it.body.lowercase().contains(lowercaseQuery)
            }.take(limit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "searchEmails error", e)
            }
            val lowercaseQuery = query.lowercase()
            return@withContext mockEmails.filter {
                it.subject.lowercase().contains(lowercaseQuery) ||
                        it.sender.lowercase().contains(lowercaseQuery) ||
                        it.body.lowercase().contains(lowercaseQuery)
            }.take(limit)
        }
    }

    // Read full email (Third function)
    suspend fun readEmail(id: String): RisoEmail? = withContext(Dispatchers.IO) {
        // Look in mock first
        val mockEmail = mockEmails.find { it.id == id }
        if (mockEmail != null) {
            val index = mockEmails.indexOf(mockEmail)
            if (index != -1 && !mockEmail.isRead) {
                mockEmails[index] = mockEmail.copy(isRead = true)
            }
            return@withContext mockEmails[index]
        }

        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isBlank() || email.isBlank() || password.isBlank()) {
            return@withContext null
        }

        try {
            val properties = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", imapServer)
                put("mail.imaps.port", "993")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
                put("mail.imaps.connectiontimeout", "10000")
                put("mail.imaps.timeout", "10000")
            }
            val session = Session.getDefaultInstance(properties, null)
            val store = session.getStore("imaps")
            store.connect(imapServer, email, password)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_WRITE)

            val messageNumber = id.toIntOrNull() ?: return@withContext null
            val message = inbox.getMessage(messageNumber)

            if (message != null) {
                message.setFlag(Flags.Flag.SEEN, true)
                val bodyText = getTextFromMessage(message)
                val riso = RisoEmail(
                    id = id,
                    sender = message.from?.firstOrNull()?.toString() ?: "Desconocido",
                    recipient = email,
                    subject = message.subject ?: "(Sin Asunto)",
                    snippet = if (bodyText.length > 100) bodyText.substring(0, 100) + "..." else bodyText,
                    body = bodyText,
                    date = message.sentDate?.toString() ?: "",
                    isRead = true
                )
                inbox.close(true)
                store.close()
                return@withContext riso
            }

            inbox.close(false)
            store.close()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "readEmail error", e)
            }
            return@withContext null
        }
        null
    }

    // Reply to email (Fifth function)
    suspend fun replyToEmail(messageId: String, body: String): Boolean = withContext(Dispatchers.IO) {
        val original = mockEmails.find { it.id == messageId }
        val senderTarget = original?.sender ?: "coordinacion@universidad.edu.ar"
        val subjectTarget = original?.subject?.let { if (it.startsWith("Re:")) it else "Re: $it" } ?: "Re: Correo"
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "replyToEmail")
        }
        sendEmail(senderTarget, subjectTarget, body)
    }

    // Forward email (Sixth function)
    suspend fun forwardEmail(messageId: String, to: String, body: String): Boolean = withContext(Dispatchers.IO) {
        val original = mockEmails.find { it.id == messageId }
        val originalSubject = original?.subject ?: "Correo"
        val originalBody = original?.body ?: ""
        
        val forwardSubject = "Fwd: $originalSubject"
        val forwardBody = "$body\n\n---------- Forwarded message ----------\nFrom: ${original?.sender}\nDate: ${original?.date}\nSubject: $originalSubject\n\n$originalBody"
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "forwardEmail")
        }
        sendEmail(to, forwardSubject, forwardBody)
    }

    // Mark as read (Seventh function)
    suspend fun markAsRead(id: String): Boolean = withContext(Dispatchers.IO) {
        val mockEmail = mockEmails.find { it.id == id }
        if (mockEmail != null) {
            val index = mockEmails.indexOf(mockEmail)
            if (index != -1) {
                mockEmails[index] = mockEmail.copy(isRead = true)
            }
            return@withContext true
        }

        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isNotBlank()) {
            try {
                setImapMessageFlag(imapServer, email, password, id.toIntOrNull(), Flags.Flag.SEEN, true)
                return@withContext true
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "markAsRead error", e)
                }
            }
        }
        false
    }

    // Mark as unread (Eighth function)
    suspend fun markAsUnread(id: String): Boolean = withContext(Dispatchers.IO) {
        val mockEmail = mockEmails.find { it.id == id }
        if (mockEmail != null) {
            val index = mockEmails.indexOf(mockEmail)
            if (index != -1) {
                mockEmails[index] = mockEmail.copy(isRead = false)
            }
            return@withContext true
        }

        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isNotBlank()) {
            try {
                setImapMessageFlag(imapServer, email, password, id.toIntOrNull(), Flags.Flag.SEEN, false)
                return@withContext true
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "markAsUnread error", e)
                }
            }
        }
        false
    }

    // Archive email (Ninth function)
    suspend fun archiveEmail(id: String): Boolean = withContext(Dispatchers.IO) {
        val mockEmail = mockEmails.find { it.id == id }
        if (mockEmail != null) {
            // Remove from main list, representing archive
            mockEmails.remove(mockEmail)
            return@withContext true
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "archiveEmail")
        }
        true
    }

    // Delete email (Tenth function)
    suspend fun deleteEmail(id: String): Boolean = withContext(Dispatchers.IO) {
        val mockEmail = mockEmails.find { it.id == id }
        if (mockEmail != null) {
            mockEmails.remove(mockEmail)
            return@withContext true
        }

        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isNotBlank()) {
            try {
                setImapMessageFlag(imapServer, email, password, id.toIntOrNull(), Flags.Flag.DELETED, true)
                return@withContext true
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "deleteEmail error", e)
                }
            }
        }
        false
    }

    private fun fetchEmailsFromImapDetailed(
        host: String,
        user: String,
        pass: String,
        port: String = "993",
        folderName: String = "INBOX",
        limit: Int = 10,
        filter: String? = null
    ): InboxSummary {
        val properties = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.ifBlank { "993" })
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.ssl.checkserveridentity", "true")
            put("mail.imaps.connectiontimeout", "10000")
            put("mail.imaps.timeout", "10000")
        }

        val session = Session.getInstance(properties, null)
        val store = session.getStore("imaps")
        store.connect(host, user, pass)

        val inbox = store.getFolder(folderName)
        inbox.open(Folder.READ_ONLY)

        val totalCount = inbox.messageCount
        val rawUnread = inbox.unreadMessageCount
        val unreadCount = if (rawUnread >= 0) {
            rawUnread
        } else {
            try {
                val unreadTerm = javax.mail.search.FlagTerm(Flags(Flags.Flag.SEEN), false)
                inbox.search(unreadTerm).size
            } catch (e: Exception) {
                0
            }
        }

        val isUnreadFilter = filter?.contains("unread", ignoreCase = true) == true
        val messages = if (isUnreadFilter) {
            val unreadTerm = javax.mail.search.FlagTerm(Flags(Flags.Flag.SEEN), false)
            val unreadList = inbox.search(unreadTerm)
            val start = (unreadList.size - limit).coerceAtLeast(0)
            if (unreadList.isNotEmpty()) unreadList.slice(start until unreadList.size).toTypedArray() else emptyArray()
        } else {
            val start = (totalCount - limit + 1).coerceAtLeast(1)
            val end = totalCount
            if (totalCount > 0) inbox.getMessages(start, end) else emptyArray()
        }

        val risoList = mutableListOf<RisoEmail>()
        for (i in messages.indices.reversed()) {
            val m = messages[i]
            val fromStr = m.from?.firstOrNull()?.toString() ?: "Desconocido"
            val subj = m.subject ?: "(Sin Asunto)"
            val seen = m.isSet(Flags.Flag.SEEN)
            val preview = try {
                val text = getTextFromMessage(m)
                if (text.length > 90) text.substring(0, 90) + "..." else text
            } catch (e: Exception) {
                "(No preview)"
            }

            risoList.add(
                RisoEmail(
                    id = m.messageNumber.toString(),
                    sender = fromStr,
                    recipient = user,
                    subject = subj,
                    snippet = preview,
                    body = preview,
                    date = m.sentDate?.toString() ?: "",
                    isRead = seen
                )
            )
        }

        inbox.close(false)
        store.close()

        return InboxSummary(
            emails = risoList,
            totalCount = totalCount,
            unreadCount = unreadCount,
            accountEmail = user,
            isLiveImap = true
        )
    }

    private fun fetchEmailsFromImap(
        host: String,
        user: String,
        pass: String,
        folderName: String = "INBOX",
        limit: Int = 10
    ): List<RisoEmail> {
        return fetchEmailsFromImapDetailed(host = host, user = user, pass = pass, port = "993", folderName = folderName, limit = limit, filter = null).emails
    }

    private fun setImapMessageFlag(
        host: String,
        user: String,
        pass: String,
        messageNum: Int?,
        flag: Flags.Flag,
        setValue: Boolean
    ) {
        if (messageNum == null) return
        val properties = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", "993")
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.ssl.checkserveridentity", "true")
            put("mail.imaps.connectiontimeout", "10000")
            put("mail.imaps.timeout", "10000")
        }

        val session = Session.getDefaultInstance(properties, null)
        val store = session.getStore("imaps")
        store.connect(host, user, pass)

        val inbox = store.getFolder("INBOX")
        inbox.open(Folder.READ_WRITE)

        val message = inbox.getMessage(messageNum)
        message?.setFlag(flag, setValue)

        inbox.close(true)
        store.close()
    }

    @Throws(Exception::class)
    private fun getTextFromMessage(message: Message): String {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        }
        if (message.isMimeType("multipart/*")) {
            val mimeMultipart = message.content as Multipart
            return getTextFromMimeMultipart(mimeMultipart)
        }
        return ""
    }

    @Throws(Exception::class)
    private fun getTextFromMimeMultipart(mimeMultipart: Multipart): String {
        val count = mimeMultipart.count
        if (count == 0) return ""
        val multipartSubText = StringBuilder()
        for (i in 0 until count) {
            val bodyPart = mimeMultipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/plain")) {
                multipartSubText.append(bodyPart.content.toString())
            } else if (bodyPart.isMimeType("text/html")) {
                val html = bodyPart.content.toString()
                multipartSubText.append(android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString())
            } else if (bodyPart.content is Multipart) {
                multipartSubText.append(getTextFromMimeMultipart(bodyPart.content as Multipart))
            }
        }
        return multipartSubText.toString()
    }
}
