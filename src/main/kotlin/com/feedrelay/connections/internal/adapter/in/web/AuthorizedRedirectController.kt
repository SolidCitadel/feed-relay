package com.feedrelay.connections.internal.adapter.`in`.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * 위임 콜백 처리 후 프레임워크가 콜백 URI(파라미터 제거)로 재리다이렉트한다
 * (OAuth2AuthorizationCodeGrantFilter) — 그 착지를 SPA 대시보드로 돌려보낸다.
 */
@Controller
class AuthorizedRedirectController {

    @GetMapping("/authorized/google-tasks")
    fun done(): String = "redirect:/dashboard"
}
