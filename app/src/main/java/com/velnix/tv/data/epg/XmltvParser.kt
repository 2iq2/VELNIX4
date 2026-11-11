package com.velnix.tv.data.epg

import android.util.Xml
import com.velnix.tv.domain.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parses the common XMLTV programme subset (channel, start, stop, title, desc).
 * XMLTV timestamps without an explicit zone are interpreted as UTC.
 */
object XmltvParser {
    fun parse(xml: String): List<EpgProgram> {
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        val programmes = mutableListOf<EpgProgram>()

        var channelId: String? = null
        var start: String? = null
        var stop: String? = null
        var title: String? = null
        var description: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channelId = parser.getAttributeValue(null, "channel")
                        start = parser.getAttributeValue(null, "start")
                        stop = parser.getAttributeValue(null, "stop")
                        title = null
                        description = null
                    }
                    "title" -> if (channelId != null) title = parser.nextText().trim()
                    "desc" -> if (channelId != null) description = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    val id = channelId?.takeIf { it.isNotBlank() }
                    val name = title?.takeIf { it.isNotBlank() }
                    val startMillis = start?.let(::parseXmltvTime)
                    val endMillis = stop?.let(::parseXmltvTime)
                    if (id != null && name != null && startMillis != null &&
                        endMillis != null && endMillis > startMillis
                    ) {
                        programmes += EpgProgram(
                            channelId = id,
                            title = name,
                            description = description?.takeIf { it.isNotBlank() },
                            startUtcMillis = startMillis,
                            endUtcMillis = endMillis
                        )
                    }
                    channelId = null
                    start = null
                    stop = null
                    title = null
                    description = null
                }
            }
            event = parser.next()
        }
        return programmes
    }

    private fun parseXmltvTime(value: String): Long? {
        val input = value.trim()
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val date = formatter.parse(input, position)
            if (date != null && position.index == input.length) return date.time
        }
        return null
    }
}
