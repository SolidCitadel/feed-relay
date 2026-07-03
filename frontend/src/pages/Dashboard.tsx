import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { fetchMe, logout, type Me } from '../api'

export default function Dashboard() {
  const navigate = useNavigate()
  const [me, setMe] = useState<Me | null>(null)

  useEffect(() => {
    fetchMe()
      .then((user) => {
        if (user) setMe(user)
        else navigate('/', { replace: true })
      })
      .catch(() => navigate('/', { replace: true }))
  }, [navigate])

  if (!me) return null

  const onLogout = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <main className="page">
      <h1>대시보드</h1>
      <p>
        {me.displayName ?? me.email} <span className="muted">({me.email})</span>
      </p>
      <button type="button" onClick={onLogout}>
        로그아웃
      </button>
    </main>
  )
}
