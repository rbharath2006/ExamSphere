import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { getCurrentUser, getStudentExams, startStudentExam, type StudentExam } from '../services/authService'

export default function ExamInstructionsPage() {
    const { examId } = useParams()
    const navigate = useNavigate()
    const [exam, setExam] = useState<StudentExam | null>(null)
    const [error, setError] = useState('')
    useEffect(() => {
        void (async () => {
            try {
                const user = await getCurrentUser()
                if (user.role !== 'STUDENT') { navigate('/', { replace: true }); return }
                const found = (await getStudentExams()).find((item) => item.id === Number(examId))
                if (found) setExam(found); else navigate('/student/exams', { replace: true })
            } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load exam') }
        })()
    }, [examId, navigate])
    async function start() {
        if (!exam) return
        if (exam.attempted && exam.attemptId) {
            navigate(`/student/attempts/${exam.attemptId}`)
            return
        }
        if (!window.confirm('Start this examination now? Your time is enforced by the server.')) return
        try {
            const attempt = await startStudentExam(exam.id)
            navigate(`/student/attempts/${attempt.attemptId}`)
        } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to start examination') }
    }
    if (!exam) return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">Loading instructions...</main>
    return <main className="min-h-screen bg-slate-950 px-6 py-10 text-white"><div className="mx-auto max-w-3xl"><Link to="/student/exams" className="text-blue-400">← Examinations</Link><section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-8"><p className="text-sm uppercase tracking-widest text-cyan-400">{exam.subject}</p><h1 className="mt-3 text-3xl font-bold">{exam.title}</h1><div className="mt-6 grid gap-3 text-slate-300 sm:grid-cols-3"><p>Duration: {exam.durationMinutes} min</p><p>Total: {exam.totalMarks} marks</p><p>Ends: {new Date(exam.endsAt).toLocaleString()}</p></div><h2 className="mt-8 text-xl font-semibold">Instructions</h2><p className="mt-3 whitespace-pre-wrap text-slate-400">{exam.instructions || 'No special instructions.'}</p>{!exam.available && !exam.attempted && <p className="mt-5 text-amber-300">This examination is outside its scheduled time window.</p>}{error && <p role="alert" className="mt-5 text-red-400">{error}</p>}<button disabled={!exam.available && !exam.attempted} onClick={() => void start()} className="mt-8 rounded-lg bg-blue-600 px-6 py-3 font-semibold hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{exam.attempted ? 'Resume examination' : 'Start examination'}</button></section></div></main>
}
