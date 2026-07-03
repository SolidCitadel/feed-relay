export type Me = {
  email: string
  displayName: string | null
}

/** 세션 주체 프로필 — 미인증(401)은 null */
export async function fetchMe(): Promise<Me | null> {
  const res = await fetch('/api/me')
  if (res.status === 401) return null
  if (!res.ok) throw new Error(`/api/me 실패: ${res.status}`)
  return res.json()
}

/** XSRF-TOKEN 쿠키 원문 — 변경 요청의 X-XSRF-TOKEN 헤더로 회신 (§8.5 CSRF) */
function xsrfToken(): string {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : ''
}

export async function logout(): Promise<void> {
  const res = await fetch('/logout', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': xsrfToken() },
  })
  if (!res.ok) throw new Error(`/logout 실패: ${res.status}`)
}
