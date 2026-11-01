import { Link } from 'react-router'
import RoleCard from '../components/RoleCard'
import { roles } from '../types/roles'
import BackendStatus from '../components/BackendStatus'

export default function HomePage() {
    return (
        <main className="min-h-screen bg-slate-950 text-white">
            <BackendStatus />
            <header className="mx-auto flex max-w-7xl items-center justify-between px-6 py-6">

                <Link to="/" className="text-2xl font-bold">
                    Exam<span className="text-blue-400">Sphere</span>
                </Link>

                <Link
                    to="/login/student"
                    className="rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold hover:bg-blue-500"
                >
                    Sign in
                </Link>

            </header>

            <section className="mx-auto max-w-7xl px-6 py-24 text-center">

                <p className="font-semibold tracking-widest text-blue-400 uppercase">
                    Welcome to ExamSphere
                </p>

                <h1 className="mt-6 text-4xl font-bold leading-tight sm:text-6xl">
                    Examinations,
                    <span className="block text-blue-400">
            without the chaos.
          </span>
                </h1>

                <p className="mx-auto mt-6 max-w-2xl text-lg leading-8 text-slate-400">
                    Create, conduct and manage online examinations
                    through one unified platform designed for
                    administrators, teachers and students.
                </p>

                <h2 className="mt-20 text-2xl font-bold">
                    Choose Your Portal
                </h2>

                <div className="mt-10 grid gap-6 md:grid-cols-3">

                    {roles.map((role) => (
                        <RoleCard
                            key={role.id}
                            role={role}
                        />
                    ))}

                </div>

            </section>

            <footer className="border-t border-slate-800 px-6 py-8 text-center text-sm text-slate-500">
                ExamSphere © 2026 | Online Examination System
            </footer>

        </main>
    )
}