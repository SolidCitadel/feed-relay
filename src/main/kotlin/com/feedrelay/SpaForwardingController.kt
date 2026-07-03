package com.feedrelay

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/** SPA 라우트 딥링크·새로고침을 index.html로 포워딩 — 클라이언트 라우팅은 react-router 소관 */
@Controller
class SpaForwardingController {

    @GetMapping("/dashboard")
    fun dashboard(): String = "forward:/index.html"
}
