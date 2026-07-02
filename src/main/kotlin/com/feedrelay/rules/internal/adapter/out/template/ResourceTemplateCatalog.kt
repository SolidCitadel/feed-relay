package com.feedrelay.rules.internal.adapter.out.template

import com.feedrelay.rules.api.RuleSetDefinition
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Template(배포되는 읽기 전용 규칙 원본, §12) 로더 — 리소스 파일 `rule-templates/<key>.json`.
 * 템플릿은 DB가 아니라 코드의 일부다 — 선택 시 rule_sets로 복제된다 (§8.3).
 */
@Component
class ResourceTemplateCatalog(private val objectMapper: ObjectMapper) {

    fun load(key: String): RuleSetDefinition {
        val resource = ClassPathResource("rule-templates/$key.json")
        require(resource.exists()) { "알 수 없는 템플릿: $key" }
        return resource.inputStream.use { objectMapper.readValue(it, RuleSetDefinition::class.java) }
    }
}
