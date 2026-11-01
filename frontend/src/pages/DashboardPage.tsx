
import { useEffect, useState } from 'react'

import {
    Link,
    Navigate,
    useNavigate,
    useParams,
} from 'react-router'

import {
    getCurrentUser,
    getRoleDashboard,
    logout,
    type UserResponse,
} from '../services/authService'

export default function DashboardPage() {
    const { role } = useParams()
    const navigate = useNavigate()

    const [user, setUser] = useState<UserResponse | null>(null)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let active = true

        async function loadDashboard() {
            try {
                const currentUser = await getCurrentUser()

                if (!active) return

                if (currentUser.role.toLowerCase() !== role) {
                    navigate('/', { replace: true })
                    return
                }

                const dashboard = await getRoleDashboard(currentUser.role)

                if (active) {
                    setUser(currentUser)
                    setMessage(dashboard.message)
                }

            } catch {
                if (active) {
                    navigate(`/login/${role}`, { replace: true })
                }
            } finally {
                if (active) {
                    setLoading(false)
                }
            }
        }

        void loadDashboard()

        return () => {
            active = false
        }
    }, [role, navigate])

    if (!['admin', 'teacher', 'student'].includes(role ?? '')) {
        return <Navigate to="/" replace />
    }

    async function handleLogout() {
        setError('')

        try {
            await logout()
            navigate('/', { replace: true })
        } catch (error) {
            setError(
                error instanceof Error ? error.message : 'Logout failed'
            )
        }
    }

    if (loading) {
        return (
            <main className="flex min-h-screen items-center justify-center bg-slate-950 text-white">
                Checking your session...
            </main>
        )
    }

    if (!user) return null

    return (
        <main className="min-h-screen bg-slate-950 px-6 py-10 text-white">

            <div className="mx-auto max-w-6xl">

                <header className="flex items-center justify-between border-b border-slate-800 pb-6">

                    <Link to="/" className="text-2xl font-bold">
                        Exam<span className="text-blue-400">Sphere</span>
                    </Link>

                    <button
                        onClick={handleLogout}
                        className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-400 hover:bg-red-500/10"
                    >
                        Logout
                    </button>

                </header>

                <section className="mt-12">

                    <p className="text-sm font-semibold uppercase tracking-widest text-blue-400">
                        {user.role} portal
                    </p>

                    <h1 className="mt-3 text-3xl font-bold sm:text-4xl">
                        Welcome, {user.fullName}!
                    </h1>

                    <p className="mt-3 text-slate-400">
                        {user.email}
                    </p>

                    <div className="mt-10 rounded-2xl border border-green-500/20 bg-green-500/10 p-6">

                        <h2 className="text-xl font-bold text-green-400">
                            Authentication successful
                        </h2>

                        <p className="mt-3 text-slate-300">
                            {message}
                        </p>

                    </div>

                    {error && (
                        <p role="alert" className="mt-4 text-sm text-red-400">
                            {error}
                        </p>
                    )}

                    <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6">

                        <h2 className="text-lg font-semibold">
                            Account information
                        </h2>

                        <div className="mt-5 space-y-3 text-sm text-slate-400">
                            <p>User ID: {user.id}</p>
                            <p>Name: {user.fullName}</p>
                            <p>Email: {user.email}</p>
                            <p>Role: {user.role}</p>
                        </div>

                    </div>

                </section>
            </div>
        </main>
    )
}
