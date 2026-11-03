import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { getCurrentUser, getStudentExams, logout, type StudentExam } from '../services/authService'

const date = (value: string) => new Date(value).toLocaleString()

export default function StudentExamsPage() {
    const navigate = useNavigate()
    const [exams, setExams] = useState<StudentExam[]>([])
    const [error, setError] = useState('')
    useEffect(() => {
        let active = true
        void (async () => {
            try {
                const user = await getCurrentUser()
                if (user.role !== 'STUDENT') { navigate('/', { replace: true }); return }
                const result = await getStudentExams()
                if (active) setExams(result)
            } catch (reason) {
                if (active) setError(reason instanceof Error ? reason.message : 'Unable to load examinations')
            }
        })()
        return () => { active = false }
    }, [navigate])
    async function signOut() { await logout(); navigate('/', { replace: true }) }
    const render = (exam: StudentExam) => (
        <article key={exam.id} className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="flex justify-between gap-4"><div><h2 className="text-xl font-semibold">{exam.title}</h2><p className="mt-1 text-cyan-400">{exam.subject}</p></div><span className="rounded-full bg-blue-500/10 px-3 py-1 text-xs text-blue-300">{exam.status === 'CLOSED' ? 'Closed · attempt available' : exam.attempted ? 'Attempt started' : 'Available'}</span></div>
            <div className="mt-5 grid gap-2 text-sm text-slate-400 sm:grid-cols-2"><p>Duration: <b className="text-slate-200">{exam.durationMinutes} minutes</b></p><p>Total marks: <b className="text-slate-200">{exam.totalMarks}</b></p><p>Starts: <b className="text-slate-200">{date(exam.startsAt)}</b></p><p>Ends: <b className="text-slate-200">{date(exam.endsAt)}</b></p></div>
            <Link to={`/student/exams/${exam.id}`} className="mt-5 inline-block rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500">{exam.attempted ? 'Resume / view exam' : 'View instructions'}</Link>
        </article>
    )
    return <main className="min-h-screen bg-slate-950 px-6 py-8 text-white"><div className="mx-auto max-w-6xl">
        <header className="flex flex-wrap justify-between gap-4 border-b border-slate-800 pb-6"><Link to="/" className="text-2xl font-bold">Exam<span className="text-blue-400">Sphere</span></Link><div className="flex gap-3"><Link to="/dashboard/student" className="rounded-lg border border-slate-700 px-4 py-2 text-sm">Dashboard</Link><button onClick={() => void signOut()} className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-400">Logout</button></div></header>
        <h1 className="mt-10 text-3xl font-bold">Available Examinations</h1>{error && <p role="alert" className="mt-4 text-red-400">{error}</p>}
        <h2 className="mt-8 text-xl font-semibold">Upcoming</h2><div className="mt-4 grid gap-5 lg:grid-cols-2">{exams.filter((exam) => exam.upcoming).map(render)}{!exams.some((exam) => exam.upcoming) && <p className="text-slate-500">No upcoming examinations.</p>}</div>
        <h2 className="mt-10 text-xl font-semibold">Currently available</h2><div className="mt-4 grid gap-5 lg:grid-cols-2">{exams.filter((exam) => exam.available).map(render)}{!exams.some((exam) => exam.available) && <p className="text-slate-500">No examinations are currently available.</p>}</div>
        <h2 className="mt-10 text-xl font-semibold">Past examinations</h2><div className="mt-4 grid gap-5 lg:grid-cols-2">{exams.filter((exam) => !exam.upcoming && !exam.available).map(render)}{!exams.some((exam) => !exam.upcoming && !exam.available) && <p className="text-slate-500">No past examinations.</p>}</div>
    </div></main>
}
