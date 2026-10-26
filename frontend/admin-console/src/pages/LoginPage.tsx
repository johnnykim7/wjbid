import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, AlertDescription, Button, Card, CardContent, Input, Label, Spinner } from '@therecommerce-develop/ds-ui'
import { login } from '../api/client'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await login(email, password)
      const roles: string[] = data.roles ?? []
      if (!roles.includes('ROLE_ADMIN')) {
        setError('관리자 권한이 없습니다.')
        return
      }
      localStorage.setItem('adminToken', data.accessToken)
      navigate('/')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-primary flex items-center justify-center">
      <Card className="w-full max-w-sm shadow-2xl">
        <CardContent className="p-10">
          <div className="text-center mb-8">
            <div className="text-2xl font-bold text-primary mb-1">Bidding Agency</div>
            <div className="text-sm text-muted-foreground">Admin Console</div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1">
              <Label htmlFor="login-email">이메일</Label>
              <Input
                id="login-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="admin@example.com"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="login-password">비밀번호</Label>
              <Input
                id="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
              />
            </div>

            {error && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Button type="submit" variant="primary" disabled={loading} className="w-full">
              {loading && <Spinner />}
              로그인
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
