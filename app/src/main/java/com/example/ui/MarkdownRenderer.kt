package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Clean, responsive, and robust Markdown renderer for Riso chat messages.
 * Formats headings, paragraphs, lists, code blocks, inline code, blockquotes, tables,
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.content
                    )
                }
                is MarkdownBlock.Blockquote -> {
                    BlockquoteItem(
                        quote = block.content,
                        textColor = textColor,
                        isUserMessage = isUserMessage
                    )
                }
                is MarkdownBlock.Table -> {
                    MarkdownTableItem(
                        headers = block.headers,
                        rows = block.rows,
                        textColor = textColor,
                        isUserMessage = isUserMessage
                    )
                }
                is MarkdownBlock.Heading -> {
                    val (fSize, fWeight) = when (block.level) {
                        1 -> Pair(17.sp, FontWeight.ExtraBold)
                        2 -> Pair(15.sp, FontWeight.Bold)
                        else -> Pair(14.sp, FontWeight.SemiBold)
                    }
                    Text(
                        text = parseInlineMarkdown(block.content, textColor, isUserMessage),
                        fontSize = fSize,
                        fontWeight = fWeight,
                        color = textColor,
                        lineHeight = (fSize.value + 5).sp,
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
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.content, textColor, isUserMessage),
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = 20.sp,
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
                        fontSize = 14.sp,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language.isNotBlank()) language.uppercase() else "CÓDIGO",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("📋", fontSize = 14.sp)
                }
            }

            // Code content with horizontal scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
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
    val barColor = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
    val quoteBg = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(quoteBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(20.dp)
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parseInlineMarkdown(quote, textColor, isUserMessage),
            fontSize = 13.5.sp,
            fontStyle = FontStyle.Italic,
            color = textColor.copy(alpha = 0.95f),
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarkdownTableItem(
    headers: List<String>,
    rows: List<List<String>>,
    textColor: Color,
    isUserMessage: Boolean
) {
    val borderColor = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier
                        .background(
                            if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 6.dp)
                ) {
                    headers.forEach { header ->
                        Text(
                            text = header.trim(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier
                                .widthIn(min = 90.dp)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                HorizontalDivider(color = borderColor, modifier = Modifier.padding(vertical = 2.dp))

                // Data rows
                rows.forEachIndexed { index, row ->
                    val rowBg = if (index % 2 == 1) {
                        if (isUserMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.06f)
                        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.35f)
                    } else Color.Transparent

                    Row(
                        modifier = Modifier
                            .background(rowBg, RoundedCornerShape(2.dp))
                            .padding(vertical = 5.dp)
                    ) {
                        row.forEachIndexed { colIdx, cell ->
                            Text(
                                text = parseInlineMarkdown(cell.trim(), textColor, isUserMessage),
                                fontSize = 12.sp,
                                color = textColor,
                                modifier = Modifier
                                    .widthIn(min = 90.dp)
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Strips raw DSML tags or dangling tool calls that should never be shown in chat.
 */
fun sanitizeMarkdownText(raw: String): String {
    if (raw.isBlank()) return ""
    return raw.replace(Regex("<\\s*[|｜]\\s*DSML\\s*[|｜]\\s*calls>.*?</\\s*[|｜]\\s*DSML\\s*[|｜]\\s*calls>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<\\s*[|｜]\\s*DSML\\s*[|｜][^>]*>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("</\\s*[|｜]\\s*DSML\\s*[|｜][^>]*>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<\\s*[|｜]\\s*invoke[^>]*>.*?</\\s*[|｜]\\s*invoke>", RegexOption.DOT_MATCHES_ALL), "")
        .trim()
}

sealed class MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock()
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class Blockquote(val content: String) : MarkdownBlock()
    data class ListItem(val content: String, val orderedPrefix: String? = null) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
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

        // 2. Table: line has '|' and next line has '|' and '---'
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size) {
            val nextTrimmed = lines[i + 1].trim()
            if (nextTrimmed.startsWith("|") && nextTrimmed.contains("---")) {
                val headers = trimmed.split("|").filter { it.isNotBlank() }
                i += 2 // skip header and separator
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val cells = lines[i].trim().split("|").filter { it.isNotBlank() }
                    rows.add(cells)
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers = headers, rows = rows))
                continue
            }
        }

        // 3. Horizontal Divider: --- or ***
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 4. Headings: #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headingText = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level = level.coerceIn(1, 3), content = headingText))
            i++
            continue
        }

        // 5. Blockquote: > text
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

        // 6. Unordered List items: - item, * item, • item
        val isUnordered = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")
        if (isUnordered) {
            val itemContent = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.ListItem(content = itemContent))
            i++
            continue
        }

        // 7. Ordered List items: 1. item, 2. item
        val orderedMatch = Regex("^([0-9]+)[.)]\\s+(.*)").find(trimmed)
        if (orderedMatch != null) {
            val num = orderedMatch.groupValues[1]
            val itemContent = orderedMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItem(content = itemContent, orderedPrefix = "$num."))
            i++
            continue
        }

        // 8. Regular paragraph / empty line
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
        Color(0xFF6750A4).copy(alpha = 0.12f)
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
                    append(inner)
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
