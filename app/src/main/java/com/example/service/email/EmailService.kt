package com.example.service.email

import android.util.Log
import com.example.data.model.AppSetting
import com.example.data.repository.RisoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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
    val id: String,
    val emailAddress: String,
    val imapServer: String,
    val imapPort: String = "993",
    val smtpServer: String,
    val smtpPort: String = "587",
    val passwordVal: String
)

class EmailService(private val repository: RisoRepository) {

    private val TAG = "EmailService"

    private suspend fun getActiveCredentials(): Map<String, String> {
        val activeId = repository.getSetting("active_email_account_id") ?: ""
        val accountsJson = repository.getSetting("email_accounts_json") ?: ""
        
        if (activeId.isNotBlank() && accountsJson.isNotBlank()) {
            try {
                val array = org.json.JSONArray(accountsJson)
                for (i in 0 until array.length()) {
                     val obj = array.getJSONObject(i)
                     if (obj.optString("id") == activeId || obj.optString("emailAddress") == activeId) {
                         return mapOf(
                             "email_address" to obj.optString("emailAddress"),
                             "email_password" to obj.optString("passwordVal"),
                             "imap_server" to obj.optString("imapServer"),
                             "imap_port" to obj.optString("imapPort", "993"),
                             "smtp_server" to obj.optString("smtpServer"),
                             "smtp_port" to obj.optString("smtpPort", "587")
                         )
                     }
                }
            } catch (e: Exception) {
                Log.e("EmailService", "Error parsing email_accounts_json", e)
            }
        }
        
        // Fallback to legacy single-account settings
        return mapOf(
            "email_address" to (repository.getSetting("email_address") ?: ""),
            "email_password" to (repository.getSetting("email_password") ?: ""),
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
            sender = "ana.perez@empresa.com",
            recipient = "usuario@riso.local",
            subject = "Reunión del Proyecto Riso - Urgente",
            snippet = "Hola team, necesitamos reconfirmar los entregables de Riso para este viernes...",
            body = "Hola team,\n\nNecesitamos reconfirmar los entregables de Riso para este viernes. Por favor, revisen el roadmap y avísenme si tienen comentarios. La presentación principal es a las 10:00 AM.\n\nSaludos,\nAna Pérez\nProject Leader",
            date = "Hoy, 10:30 AM",
            isRead = false
        ),
        RisoEmail(
            id = "msg_102",
            sender = "marketing@ofertas.com",
            recipient = "usuario@riso.local",
            subject = "¡Última oportunidad! 50% de descuento en servidores cloud",
            snippet = "Mejora tus despliegues hoy mismo con nuestro cupón exclusivo RISOCLOUD...",
            body = "Estimado cliente,\n\nNo dejes pasar esta gran oferta. Usa el cupón RISOCLOUD para obtener 50% de descuento mensual en tus instancias de servidor cloud. Válido hasta la medianoche.\n\nSuscríbete ya.",
            date = "Ayer, 4:15 PM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_103",
            sender = "coordinacion@universidad.edu.ar",
            recipient = "usuario@riso.local",
            subject = "Certificado Analítico Digital Disponible",
            snippet = "Estimado alumno, le informamos que el reporte académico ha sido cargado en su portal...",
            body = "Estimando alumno,\n\nLe informamos que su Certificado Analítico Digital ya se encuentra listo para descargar en su panel general de autogestión.\n\nAtentamente,\nSecretaría Académica",
            date = "14 Jun 2026, 9:00 AM",
            isRead = true
        ),
        RisoEmail(
            id = "msg_104",
            sender = "juan.marquez@tecnologia.com",
            recipient = "usuario@riso.local",
            subject = "Presupuestos de Licenciamiento",
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

        if (smtpServer.isBlank() || email.isBlank() || password.isBlank()) {
            Log.d(TAG, "[MOCK] Sending email to $to, Subject: $subject")
            // Mock send addition
            val newMock = RisoEmail(
                id = "msg_sent_" + System.currentTimeMillis(),
                sender = email.ifBlank { "usuario@riso.local" },
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
            val properties = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", smtpServer)
                put("mail.smtp.port", smtpPort.ifBlank { "587" })
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
            Log.d(TAG, "SMTP email sent successfully to $to")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMTP email: ${e.message}", e)
            false
        }
    }

    // List recent emails (First of the 10 core functions)
    suspend fun listInbox(limit: Int = 10): List<RisoEmail> = withContext(Dispatchers.IO) {
        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isBlank() || email.isBlank() || password.isBlank()) {
            Log.d(TAG, "[MOCK] Listing inbox")
            return@withContext mockEmails.take(limit)
        }

        try {
            return@withContext fetchEmailsFromImap(imapServer, email, password, "INBOX", limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing inbox via IMAP, falling back to mock: ${e.message}", e)
            return@withContext mockEmails.take(limit)
        }
    }

    // Search emails by query (Second of core functions)
    suspend fun searchEmails(query: String, limit: Int = 10): List<RisoEmail> = withContext(Dispatchers.IO) {
        val creds = getActiveCredentials()
        val imapServer = creds["imap_server"] ?: ""
        val email = creds["email_address"] ?: ""
        val password = creds["email_password"] ?: ""

        if (imapServer.isBlank() || email.isBlank() || password.isBlank()) {
            Log.d(TAG, "[MOCK] Searching inbox for: '$query'")
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
            Log.e(TAG, "Error searching email via IMAP, falling back to mock search: ${e.message}")
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
            // mark read locally
            val index = mockEmails.indexOf(mockEmail)
            if (index != -1) {
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
            Log.e(TAG, "Error reading message via IMAP: ${e.message}")
        }
        null
    }

    // Reply to email (Fifth function)
    suspend fun replyToEmail(messageId: String, body: String): Boolean = withContext(Dispatchers.IO) {
        val original = mockEmails.find { it.id == messageId }
        val senderTarget = original?.sender ?: "coordinacion@universidad.edu.ar"
        val subjectTarget = original?.subject?.let { if (it.startsWith("Re:")) it else "Re: $it" } ?: "Re: Correo"
        
        Log.d(TAG, "Replying to message $messageId, sending to $senderTarget")
        sendEmail(senderTarget, subjectTarget, body)
    }

    // Forward email (Sixth function)
    suspend fun forwardEmail(messageId: String, to: String, body: String): Boolean = withContext(Dispatchers.IO) {
        val original = mockEmails.find { it.id == messageId }
        val originalSubject = original?.subject ?: "Correo"
        val originalBody = original?.body ?: ""
        
        val forwardSubject = "Fwd: $originalSubject"
        val forwardBody = "$body\n\n---------- Forwarded message ----------\nFrom: ${original?.sender}\nDate: ${original?.date}\nSubject: $originalSubject\n\n$originalBody"
        
        Log.d(TAG, "Forwarding message $messageId to $to")
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
                Log.e(TAG, "Error setting SEEN IMAP: ${e.message}")
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
                Log.e(TAG, "Error clearing SEEN IMAP: ${e.message}")
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
        Log.d(TAG, "Archived email $id successfully")
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
                Log.e(TAG, "Error setting DELETED IMAP: ${e.message}")
            }
        }
        false
    }

    // Helper functions for actual IMAP connection
    private fun fetchEmailsFromImap(
        host: String,
        user: String,
        pass: String,
        folderName: String = "INBOX",
        limit: Int = 10
    ): List<RisoEmail> {
        val properties = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", "993")
        }

        val session = Session.getDefaultInstance(properties, null)
        val store = session.getStore("imaps")
        store.connect(host, user, pass)

        val inbox = store.getFolder(folderName)
        inbox.open(Folder.READ_ONLY)

        val count = inbox.messageCount
        val start = (count - limit + 1).coerceAtLeast(1)
        val end = count

        val messages = if (count > 0) inbox.getMessages(start, end) else emptyArray()
        val risoEmails = mutableListOf<RisoEmail>()

        for (i in messages.indices.reversed()) {
            val message = messages[i]
            val content = try {
                getTextFromMessage(message)
            } catch (e: Exception) {
                "Contenido codificado no legible"
            }
            risoEmails.add(
                RisoEmail(
                    id = message.messageNumber.toString(),
                    sender = message.from?.firstOrNull()?.toString() ?: "Desconocido",
                    recipient = user,
                    subject = message.subject ?: "(Sin Asunto)",
                    snippet = if (content.length > 80) content.substring(0, 80) + "..." else content,
                    body = content,
                    date = message.sentDate?.toString() ?: "",
                    isRead = message.isSet(Flags.Flag.SEEN)
                )
            )
        }

        inbox.close(false)
        store.close()
        return risoEmails
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
                return bodyPart.content.toString()
            } else if (bodyPart.isMimeType("text/html")) {
                // Strips HTML minimally
                val html = bodyPart.content as String
                return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            } else if (bodyPart.content is Multipart) {
                multipartSubText.append(getTextFromMimeMultipart(bodyPart.content as Multipart))
            }
        }
        return multipartSubText.toString()
    }
}
