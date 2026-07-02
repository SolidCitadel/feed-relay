package com.feedrelay.ingestion.internal.adapter.out.ical

import com.feedrelay.ingestion.api.ItemKind
import com.feedrelay.ingestion.api.SourceItem
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.property.DateProperty
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal

/**
 * ics 문서를 SourceItem으로 정규화하는 파서 — RFC 5545 지식만 갖는다 (벤더 지식 금지, ADR-0012).
 *
 * - kind는 컴포넌트 유래 사실: VEVENT→EVENT(startAt/endAt), VTODO→TASK(dueAt).
 * - UID 없는 컴포넌트는 멱등성 추적이 불가능하므로 경고 후 제외한다.
 * - 종일(DATE) 값은 UTC 자정으로, 타임존 없는 부동 시각은 UTC로 해석한다.
 */
class IcalFeedParser {

    private val log = LoggerFactory.getLogger(javaClass)

    fun parse(ics: String): List<SourceItem> {
        val calendar = CalendarBuilder().build(StringReader(ics))
        return calendar.getComponents<CalendarComponent>()
            .filter { it.name == Component.VEVENT || it.name == Component.VTODO }
            .mapNotNull { toSourceItem(it) }
    }

    private fun toSourceItem(component: Component): SourceItem? {
        val properties = component.getProperties<Property>()
        val byName = properties.groupBy { it.name.uppercase() }
        fun value(name: String): String? = byName[name]?.firstOrNull()?.value
        fun instant(name: String): Instant? =
            (byName[name]?.firstOrNull() as? DateProperty<*>)?.date?.toInstant()

        val uid = value(Property.UID)
        if (uid.isNullOrBlank()) {
            log.warn("UID 없는 {} 컴포넌트 제외 — 멱등성 추적 불가", component.name)
            return null
        }

        val kind = if (component.name == Component.VTODO) ItemKind.TASK else ItemKind.EVENT
        return SourceItem(
            sourceUid = uid,
            kind = kind,
            title = value(Property.SUMMARY) ?: "",
            description = value(Property.DESCRIPTION),
            url = value(Property.URL),
            dueAt = if (kind == ItemKind.TASK) instant(Property.DUE) else null,
            startAt = instant(Property.DTSTART),
            endAt = instant(Property.DTEND),
            raw = buildMap {
                for (property in properties) putIfAbsent(property.name.lowercase(), property.value)
            },
        )
    }

    private fun Temporal.toInstant(): Instant = when (this) {
        is Instant -> this
        is OffsetDateTime -> toInstant()
        is ZonedDateTime -> toInstant()
        is LocalDateTime -> atZone(ZoneOffset.UTC).toInstant()
        is LocalDate -> atStartOfDay(ZoneOffset.UTC).toInstant()
        else -> error("지원하지 않는 시간 타입: ${this::class.qualifiedName}")
    }
}
