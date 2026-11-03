import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
    ApiRequestError,
    getStudentResult,
    type StudentResult,
} from '../services/authService'

export default function ExamResultPage() {
    const { attemptId } = useParams()
    const [result, setResult] = useState<StudentResult | null>(null)
    const [error, setError] = useState('')
    const [pending, setPending] = useState(false)
    const [loading, setLoading] = useState(true)

    const loadResult = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            setResult(await getStudentResult(Number(attemptId)))
            setPending(false)
        } catch (reason) {
            if (reason instanceof ApiRequestError && reason.status === 409
                && reason.message === 'Exam results have not been published yet') {
                setResult(null)
                setPending(true)
            } else {
                setPending(false)
                setError(reason instanceof Error ? reason.message : 'Unable to load result')
            }
        } finally {
            setLoading(false)
        }
    }, [attemptId])

    useEffect(() => {
        const timer = window.setTimeout(() => void loadResult(), 0)
        return () => window.clearTimeout(timer)
    }, [loadResult])

    if (loading) {
        return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">Loading result...</main>
    }
    if (pending) {
        return (
            <main className="min-h-screen bg-slate-950 px-6 py-10 text-white">
                <div className="mx-auto max-w-2xl">
                    <section className="rounded-2xl border border-slate-800 bg-slate-900 p-8 text-center">
                        <p className="text-sm uppercase tracking-widest text-amber-400">Results pending</p>
                        <h1 className="mt-3 text-3xl font-bold">Your examination was submitted</h1>
                        <p className="mt-5 text-slate-300">Your examination has been submitted successfully. Your results are awaiting publication by your teacher.</p>
                        <div className="mt-8 flex justify-center gap-3">
                            <button onClick={() => void loadResult()} className="rounded-lg bg-blue-600 px-5 py-3 font-semibold hover:bg-blue-500">Refresh results</button>
                            <Link to="/student/exams" className="rounded-lg border border-slate-700 px-5 py-3 text-slate-300">Back to examinations</Link>
                        </div>
                    </section>
                </div>
            </main>
        )
    }
    if (error || !result) {
        return <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6 text-center text-red-400">{error || 'Unable to load result'}</main>
    }
    return <main className="min-h-screen bg-slate-950 px-6 py-10 text-white"><div className="mx-auto max-w-2xl"><section className="rounded-2xl border border-slate-800 bg-slate-900 p-8 text-center"><p className="text-sm uppercase tracking-widest text-green-400">Examination result</p><h1 className="mt-3 text-3xl font-bold">{result.title}</h1><p className="mt-8 text-6xl font-bold text-cyan-300">{result.obtainedMarks}<span className="text-2xl text-slate-500"> / {result.totalMarks}</span></p><p className="mt-3 text-slate-400">{result.percentage}% · {result.status.replace('_', ' ')}</p><p className="mt-4 text-sm text-slate-500">Submitted: {new Date(result.submittedAt).toLocaleString()}</p><Link to="/student/exams" className="mt-8 inline-block rounded-lg bg-blue-600 px-5 py-3">Back to examinations</Link></section></div></main>
}
