package com.velnix.tv.data.iptv

/**
 * Minimal M3U playlist parser.
 * Supports EXTINF display name and common tvg/group attributes.
 * Network fetching and credential handling belong in a repository layer.
 */
data class IptvChannel(
    val id: String?,
    val name: String,
    val logoUrl: String?,
    val group: String?,
    val streamUrl: String
)

object M3uParser {
    fun parse(content: String): List<IptvChannel> {
        val lines = content.lineSequence().map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val result = mutableListOf<IptvChannel>()
        var pendingInfo: String? = null

        for (line in lines) {
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingInfo = line
                continue
            }
            if (line.startsWith("#")) continue
            val info = pendingInfo ?: continue
            val name = info.substringAfterLast(',', "").trim().ifBlank { "Unnamed channel" }
            result += IptvChannel(
                id = attribute(info, "tvg-id"),
                name = name,
                logoUrl = attribute(info, "tvg-logo"),
                group = attribute(info, "group-title"),
                streamUrl = line
            )
            pendingInfo = null
        }
        return result
    }

    private fun attribute(line: String, key: String): String? {
        val regex = Regex("""(?:^|\\s)${Regex.escape(key)}="([^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
