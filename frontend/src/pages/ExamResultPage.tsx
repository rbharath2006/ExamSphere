import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { getStudentResult, type StudentResult } from '../services/authService'

export default function ExamResultPage() {
    const { attemptId } = useParams()
    const [result, setResult] = useState<StudentResult | null>(null)
    const [error, setError] = useState('')
    useEffect(() => { void getStudentResult(Number(attemptId)).then(setResult).catch((reason) => setError(reason instanceof Error ? reason.message : 'Unable to load result')) }, [attemptId])
    if (!result) return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">{error || 'Loading result...'}</main>
    return <main className="min-h-screen bg-slate-950 px-6 py-10 text-white"><div className="mx-auto max-w-2xl"><section className="rounded-2xl border border-slate-800 bg-slate-900 p-8 text-center"><p className="text-sm uppercase tracking-widest text-green-400">Examination result</p><h1 className="mt-3 text-3xl font-bold">{result.title}</h1><p className="mt-8 text-6xl font-bold text-cyan-300">{result.obtainedMarks}<span className="text-2xl text-slate-500"> / {result.totalMarks}</span></p><p className="mt-3 text-slate-400">{result.percentage}% · {result.status.replace('_', ' ')}</p><p className="mt-4 text-sm text-slate-500">Submitted: {new Date(result.submittedAt).toLocaleString()}</p><Link to="/student/exams" className="mt-8 inline-block rounded-lg bg-blue-600 px-5 py-3">Back to examinations</Link></section></div></main>
}
