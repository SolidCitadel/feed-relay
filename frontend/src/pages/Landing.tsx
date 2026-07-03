import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { fetchMe } from '../api'

export default function Landing() {
  const navigate = useNavigate()
  const [checking, setChecking] = useState(true)

  // 이미 로그인된 세션이면 대시보드로
  useEffect(() => {
    fetchMe()
      .then((me) => {
        if (me) navigate('/dashboard', { replace: true })
        else setChecking(false)
      })
      .catch(() => setChecking(false))
  }, [navigate])

  if (checking) return null

  return (
    <main className="page">
      <h1>FeedRelay</h1>
      <p>LMS 과제 피드를 과목별로 분류된 체크 가능한 할 일로.</p>
      <a className="button" href="/oauth2/authorization/google">
        Google로 로그인
      </a>
    </main>
  )
}
