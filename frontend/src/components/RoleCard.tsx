import { Link } from 'react-router'
import type { Role } from '../types/roles'

interface RoleCardProps {
    role: Role
}

export default function RoleCard({ role }: RoleCardProps) {
    return (
        <article className="rounded-2xl border border-slate-700 bg-slate-900 p-8 transition hover:-translate-y-1 hover:border-blue-500">

            <div className="text-5xl">
                {role.icon}
            </div>

            <h3 className="mt-6 text-xl font-bold text-white">
                {role.title}
            </h3>

            <p className="mt-3 min-h-12 leading-6 text-slate-400">
                {role.description}
            </p>

            <Link
                to={`/login/${role.id}`}
                className="mt-6 block w-full rounded-lg bg-blue-600 py-3 text-center font-semibold text-white transition hover:bg-blue-500"
            >
                Continue →
            </Link>

        </article>
    )
}