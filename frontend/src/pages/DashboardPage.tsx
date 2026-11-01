
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
    getAdminDashboardSummary,
    logout,
    type UserResponse,
    type AdminDashboardSummaryResponse,
} from '../services/authService'

export default function DashboardPage() {
    const { role } = useParams()
    const navigate = useNavigate()

    const [user, setUser] = useState<UserResponse | null>(null)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(true)

    // Admin dashboard statistics
    const [summary, setSummary] =
        useState<AdminDashboardSummaryResponse | null>(null)

    const [summaryLoading, setSummaryLoading] = useState(false)
    const [summaryError, setSummaryError] = useState('')

    // Load authenticated user and dashboard data
    useEffect(() => {
        let active = true

        async function loadDashboard() {
            try {
                const currentUser = await getCurrentUser()

                if (!active) return

                // Verify that the logged-in user's role
                // matches the requested dashboard.
                if (currentUser.role.toLowerCase() !== role) {
                    navigate('/', { replace: true })
                    return
                }

                const dashboard = await getRoleDashboard(
                    currentUser.role
                )

                if (!active) return

                setUser(currentUser)
                setMessage(dashboard.message)

                // Load live statistics only for Admin users.
                if (currentUser.role === 'ADMIN') {
                    setSummaryLoading(true)

                    try {
                        const statistics =
                            await getAdminDashboardSummary()

                        if (active) {
                            setSummary(statistics)
                            setSummaryError('')
                        }
                    } catch (error) {
                        if (active) {
                            setSummaryError(
                                error instanceof Error
                                    ? error.message
                                    : 'Unable to load dashboard statistics'
                            )
                        }
                    } finally {
                        if (active) {
                            setSummaryLoading(false)
                        }
                    }
                }
            } catch {
                if (active) {
                    navigate(`/login/${role}`, {
                        replace: true,
                    })
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

    // Refresh Admin statistics
    async function refreshAdminSummary() {
        setSummaryLoading(true)
        setSummaryError('')

        try {
            const statistics = await getAdminDashboardSummary()
            setSummary(statistics)
        } catch (error) {
            setSummaryError(
                error instanceof Error
                    ? error.message
                    : 'Unable to refresh dashboard statistics'
            )
        } finally {
            setSummaryLoading(false)
        }
    }

    // Logout
    async function handleLogout() {
        setError('')

        try {
            await logout()
            navigate('/', { replace: true })
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Logout failed'
            )
        }
    }

    // Validate requested role
    if (!['admin', 'teacher', 'student'].includes(role ?? '')) {
        return <Navigate to="/" replace />
    }

    if (loading) {
        return (
            <main className="flex min-h-screen items-center justify-center bg-slate-950 text-white">
                Checking your session...
            </main>
        )
    }

    if (!user) return null

    const statisticsCards = summary
        ? [
            {
                title: 'Total Users',
                value: summary.totalUsers,
                description: 'All registered accounts',
                color: 'text-blue-400',
            },
            {
                title: 'Students',
                value: summary.totalStudents,
                description: 'Registered students',
                color: 'text-cyan-400',
            },
            {
                title: 'Teachers',
                value: summary.totalTeachers,
                description: 'Registered teachers',
                color: 'text-purple-400',
            },
            {
                title: 'Administrators',
                value: summary.totalAdmins,
                description: 'Admin accounts',
                color: 'text-amber-400',
            },
            {
                title: 'Active Users',
                value: summary.activeUsers,
                description: 'Enabled accounts',
                color: 'text-green-400',
            },
            {
                title: 'Disabled Users',
                value: summary.disabledUsers,
                description: 'Disabled accounts',
                color: 'text-red-400',
            },
        ]
        : []

    return (
        <main className="min-h-screen bg-slate-950 px-6 py-10 text-white">

            <div className="mx-auto max-w-6xl">

                {/* Header */}

                <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-6">

                    <Link to="/" className="text-2xl font-bold">
                        Exam<span className="text-blue-400">Sphere</span>
                    </Link>

                    <div className="flex items-center gap-3">

                        {user.role === 'ADMIN' && (
                            <Link
                                to="/admin/users"
                                className="rounded-lg border border-blue-500/40 px-4 py-2 text-sm text-blue-400 hover:bg-blue-500/10"
                            >
                                Manage Users
                            </Link>
                        )}

                        <button
                            onClick={handleLogout}
                            className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-400 hover:bg-red-500/10"
                        >
                            Logout
                        </button>

                    </div>

                </header>


                {/* Welcome section */}
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

                    {/* Authentication status */}
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

                </section>

                {/* ADMIN DASHBOARD */}
                {user.role === 'ADMIN' && (

                    <section className="mt-12">

                        <div className="flex flex-wrap items-center justify-between gap-4">

                            <div>
                                <h2 className="text-2xl font-bold">
                                    Admin Overview
                                </h2>

                                <p className="mt-2 text-sm text-slate-400">
                                    Live statistics from your ExamSphere database
                                </p>
                            </div>

                            <button
                                onClick={() => void refreshAdminSummary()}
                                disabled={summaryLoading}
                                className="rounded-lg border border-blue-500/40 px-4 py-2 text-sm text-blue-400 hover:bg-blue-500/10 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {summaryLoading ? 'Refreshing...' : 'Refresh Statistics'}
                            </button>

                        </div>

                        {/* Loading state */}
                        {summaryLoading && !summary && (
                            <p className="mt-8 text-slate-400">
                                Loading dashboard statistics...
                            </p>
                        )}

                        {/* Statistics error */}
                        {summaryError && (
                            <p
                                role="alert"
                                className="mt-6 rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-400"
                            >
                                {summaryError}
                            </p>
                        )}

                        {/* Statistics cards */}
                        {summary && (

                            <div className="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">

                                {statisticsCards.map((card) => (

                                    <div
                                        key={card.title}
                                        className="rounded-2xl border border-slate-800 bg-slate-900 p-6 transition hover:border-slate-600"
                                    >

                                        <p className="text-sm font-medium text-slate-400">
                                            {card.title}
                                        </p>

                                        <h3 className={`mt-4 text-4xl font-bold ${card.color}`}>
                                            {card.value}
                                        </h3>

                                        <p className="mt-3 text-sm text-slate-500">
                                            {card.description}
                                        </p>

                                    </div>

                                ))}

                            </div>

                        )}

                    </section>

                )}

                {/* Account information */}
                <section className="mt-10 rounded-2xl border border-slate-800 bg-slate-900 p-6">

                    <h2 className="text-lg font-semibold">
                        Account information
                    </h2>

                    <div className="mt-5 space-y-3 text-sm text-slate-400">

                        <p>User ID: {user.id}</p>

                        <p>Name: {user.fullName}</p>

                        <p>Email: {user.email}</p>

                        <p>Role: {user.role}</p>

                    </div>

                </section>

            </div>

        </main>
    )
}
