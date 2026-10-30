import { useState, type FormEvent } from 'react'
import {
    Link,
    Navigate,
    useParams,
} from 'react-router'

import { roles } from '../types/roles'

export default function LoginPage() {
    const { role } = useParams()

    const selectedRole = roles.find(
        (item) => item.id === role
    )

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [message, setMessage] = useState('')

    if (!selectedRole) {
        return <Navigate to="/" replace />
    }

    function handleLogin(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()

        setMessage(
            'Login interface ready. Backend authentication will be implemented later.'
        )
    }

    return (
        <main className="flex min-h-screen items-center justify-center bg-slate-950 px-5 text-white">

            <div className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-8">

                <Link
                    to="/"
                    className="mb-8 block text-sm text-blue-400"
                >
                    ← Back to portals
                </Link>

                <div className="text-5xl">
                    {selectedRole.icon}
                </div>

                <h1 className="mt-5 text-3xl font-bold">
                    {selectedRole.title} Login
                </h1>

                <p className="mt-2 text-slate-400">
                    Sign in to access your examination portal.
                </p>

                <form onSubmit={handleLogin} className="mt-8 space-y-5">

                    <div>
                        <label htmlFor="email" className="mb-2 block text-sm">
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
                            className="w-full rounded-lg border border-slate-700 bg-slate-950 p-3 outline-none focus:border-blue-500"
                        />
                    </div>

                    <div>
                        <label htmlFor="password" className="mb-2 block text-sm">
                            Password
                        </label>

                        <input
                            id="password"
                            type="password"
                            autoComplete="current-password"
                            required
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            className="w-full rounded-lg border border-slate-700 bg-slate-950 p-3 outline-none focus:border-blue-500"
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full rounded-lg bg-blue-600 py-3 font-semibold hover:bg-blue-500"
                    >
                        Sign In
                    </button>

                </form>

                {message && (
                    <p role="status" className="mt-5 rounded-lg bg-blue-500/10 p-3 text-sm text-blue-300">
                        {message}
                    </p>
                )}

                <p className="mt-6 text-center text-xs text-slate-500">
                    Development preview. Authentication is not connected yet.
                </p>

            </div>

        </main>
    )
}