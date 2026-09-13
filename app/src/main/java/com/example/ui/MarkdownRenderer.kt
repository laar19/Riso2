package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clean and robust Markdown renderer for Riso chat messages.
 * Formats bold, italic, inline code, code blocks, blockquotes, headings, lists,
 * and sanitizes raw DSML or XML artifacts.
 */
@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isUserMessage: Boolean = false
) {
    val sanitized = sanitizeMarkdownText(text)
    val blocks = parseMarkdownBlocks(sanitized)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.content,
                        isUserMessage = isUserMessage
                    )
                }
                is MarkdownBlock.Blockquote -> {
                    BlockquoteItem(
                        quote = block.content,
                        textColor = textColor,
                        isUserMessage = isUserMessage
                    )
                }
                is MarkdownBlock.Heading -> {
                    val (fSize, fWeight) = when (block.level) {
                        1 -> Pair(16.sp, FontWeight.ExtraBold)
                        2 -> Pair(15.sp, FontWeight.Bold)
                        else -> Pair(14.sp, FontWeight.SemiBold)
                    }
                    Text(
                        text = parseInlineMarkdown(block.content, textColor, isUserMessage),
                        fontSize = fSize,
                        fontWeight = fWeight,
                        color = textColor,
                        lineHeight = (fSize.value + 4).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.orderedPrefix != null) "${block.orderedPrefix} " else "• ",
                            fontWeight = FontWeight.Bold,
                            color = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.content, textColor, isUserMessage),
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.content, textColor, isUserMessage),
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 18.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String,
    isUserMessage: Boolean
) {
    val context = LocalContext.current
    val containerBg = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language.isNotBlank()) language.uppercase() else "CÓDIGO",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        try {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (clip != null) {
                                clip.setPrimaryClip(ClipData.newPlainText("Código", code))
                                Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MarkdownRenderer", "Error copying code", e)
                        }
                    },
                    modifier = Modifier.size(22.dp)
                ) {
                    Text("📋", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = if (isUserMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BlockquoteItem(
    quote: String,
    textColor: Color,
    isUserMessage: Boolean
) {
    val barColor = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
    val quoteBg = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(quoteBg)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parseInlineMarkdown(quote, textColor, isUserMessage),
            fontSize = 12.5.sp,
            fontStyle = FontStyle.Italic,
            color = textColor.copy(alpha = 0.9f),
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Strips raw DSML tags or dangling tool calls that should never be shown in chat.
 */
fun sanitizeMarkdownText(raw: String): String {
    if (raw.isBlank()) return ""
    // Remove DSML tags like < | DSML | calls> ... </ | DSML | calls> or variants
    val cleaned = raw.replace(Regex("<\\s*[|｜]\\s*DSML\\s*[|｜]\\s*calls>.*?</\\s*[|｜]\\s*DSML\\s*[|｜]\\s*calls>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<\\s*[|｜]\\s*DSML\\s*[|｜][^>]*>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("</\\s*[|｜]\\s*DSML\\s*[|｜][^>]*>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<\\s*[|｜]\\s*invoke[^>]*>.*?</\\s*[|｜]\\s*invoke>", RegexOption.DOT_MATCHES_ALL), "")
        .trim()
    return cleaned
}

sealed class MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock()
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class Blockquote(val content: String) : MarkdownBlock()
    data class ListItem(val content: String, val orderedPrefix: String? = null) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Fenced Code Block: ```lang
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language = lang, content = codeLines.joinToString("\n")))
            i++
            continue
        }

        // 2. Horizontal Divider: --- or ***
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 3. Headings: #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headingText = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level = level.coerceIn(1, 3), content = headingText))
            i++
            continue
        }

        // 4. Blockquote: > text
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            var currentQuote = trimmed.removePrefix(">").trim()
            quoteLines.add(currentQuote)
            i++
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString(" ")))
            continue
        }

        // 5. Unordered List items: - item, * item, • item
        val isUnordered = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")
        if (isUnordered) {
            val itemContent = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.ListItem(content = itemContent))
            i++
            continue
        }

        // 6. Ordered List items: 1. item, 2. item
        val orderedMatch = Regex("^([0-9]+)[.)]\\s+(.*)").find(trimmed)
        if (orderedMatch != null) {
            val num = orderedMatch.groupValues[1]
            val itemContent = orderedMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItem(content = itemContent, orderedPrefix = "$num."))
            i++
            continue
        }

        // 7. Regular paragraph / empty line
        if (trimmed.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }

    return blocks
}

/**
 * Parses inline formatting: **bold**, *italic*, `code`, ~~strikethrough~~.
 */
fun parseInlineMarkdown(
    text: String,
    defaultColor: Color,
    isUserMessage: Boolean
): AnnotatedString {
    val codeBg = if (isUserMessage) {
        Color.White.copy(alpha = 0.2f)
    } else {
        Color(0xFF6750A4).copy(alpha = 0.1f)
    }

    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Bold: **text** or __text__
            if ((text.startsWith("**", cursor) || text.startsWith("__", cursor)) && cursor + 2 < len) {
                val marker = text.substring(cursor, cursor + 2)
                val end = text.indexOf(marker, cursor + 2)
                if (end != -1) {
                    val inner = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Inline Code: `code`
            if (text[cursor] == '`' && cursor + 1 < len) {
                val end = text.indexOf('`', cursor + 1)
                if (end != -1) {
                    val inner = text.substring(cursor + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = codeBg
                        )
                    )
                    append(" $inner ")
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Italic: *text* or _text_ (single asterisk/underscore)
            if ((text[cursor] == '*' || text[cursor] == '_') && cursor + 1 < len && text.getOrNull(cursor + 1) != text[cursor]) {
                val marker = text[cursor]
                val end = text.indexOf(marker, cursor + 1)
                if (end != -1) {
                    val inner = text.substring(cursor + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (text.startsWith("~~", cursor) && cursor + 2 < len) {
                val end = text.indexOf("~~", cursor + 2)
                if (end != -1) {
                    val inner = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Regular char
            append(text[cursor])
            cursor++
        }
    }
}
