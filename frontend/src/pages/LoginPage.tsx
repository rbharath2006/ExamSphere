
import { useState, type FormEvent } from 'react'

import {
    Link,
    Navigate,
    useNavigate,
    useParams,
} from 'react-router'

import { roles } from '../types/roles'
import { login, logout } from '../services/authService'

export default function LoginPage() {
    const { role } = useParams()
    const navigate = useNavigate()

    const selectedRole = roles.find(
        (item) => item.id === role
    )

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const [showPassword, setShowPassword] = useState(false)

    if (!selectedRole) {
        return <Navigate to="/" replace />
    }
    const selectedRoleId = selectedRole.id

    async function handleLogin(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()

        setError('')
        setLoading(true)

        try {
            const user = await login(email, password)

            if (user.role.toLowerCase() !== selectedRoleId) {
                await logout()

                setError(
                    `This account belongs to the ${user.role.toLowerCase()} portal.`
                )

                return
            }

            navigate(`/dashboard/${user.role.toLowerCase()}`, {
                replace: true,
            })

        } catch (error: unknown) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Unable to sign in'
            )
        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="flex min-h-screen items-center justify-center bg-slate-950 px-5 py-12 text-white">

            <div className="w-full max-w-md">

                <Link
                    to="/"
                    className="mb-8 inline-flex text-sm text-slate-400 hover:text-white"
                >
                    ← Back to portals
                </Link>

                <div className="rounded-3xl border border-white/10 bg-slate-900 p-8 shadow-2xl">

                    <div className="text-5xl">
                        {selectedRole.icon}
                    </div>

                    <h1 className="mt-5 text-3xl font-bold">
                        {selectedRole.title} Login
                    </h1>

                    <p className="mt-3 text-sm text-slate-400">
                        Sign in using your registered account.
                    </p>

                    <form
                        onSubmit={handleLogin}
                        className="mt-8 space-y-5"
                    >

                        <div>
                            <label
                                htmlFor="email"
                                className="mb-2 block text-sm font-medium"
                            >
                                Email address
                            </label>

                            <input
                                id="email"
                                type="email"
                                autoComplete="username"
                                required
                                value={email}
                                onChange={(event) => setEmail(event.target.value)}
                                placeholder="Enter your email"
                                className="w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-blue-500"
                            />
                        </div>

                        <div>
                            <label
                                htmlFor="password"
                                className="mb-2 block text-sm font-medium"
                            >
                                Password
                            </label>

                            <div className="flex rounded-xl border border-white/10 bg-slate-950 focus-within:border-blue-500">

                                <input
                                    id="password"
                                    type={showPassword ? 'text' : 'password'}
                                    autoComplete="current-password"
                                    required
                                    value={password}
                                    onChange={(event) => setPassword(event.target.value)}
                                    placeholder="Enter your password"
                                    className="w-full rounded-xl bg-transparent px-4 py-3 outline-none"
                                />

                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="px-4 text-slate-400"
                                    aria-label="Toggle password visibility"
                                >
                                    {showPassword ? 'Hide' : 'Show'}
                                </button>

                            </div>
                        </div>

                        {error && (
                            <div
                                role="alert"
                                className="rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-300"
                            >
                                {error}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full rounded-xl bg-blue-600 py-3.5 font-semibold transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {loading ? 'Signing in...' : 'Sign In'}
                        </button>

                    </form>

                    <p className="mt-6 text-center text-xs text-slate-500">
                        Secure authentication powered by Spring Security.
                    </p>

                </div>
            </div>
        </main>
    )
}
