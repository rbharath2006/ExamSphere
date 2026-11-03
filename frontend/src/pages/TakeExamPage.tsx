import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
    getStudentAttempt,
    saveStudentAnswer,
    submitStudentAttempt,
    type StudentAttempt,
} from '../services/authService'

export default function TakeExamPage() {
    const { attemptId } = useParams()
    const navigate = useNavigate()
    const [attempt, setAttempt] = useState<StudentAttempt | null>(null)
    const [current, setCurrent] = useState(0)
    const [remaining, setRemaining] = useState(0)
    const [error, setError] = useState('')
    const [saving, setSaving] = useState(false)
    const autoSubmitStarted = useRef(false)

    useEffect(() => {
        void getStudentAttempt(Number(attemptId)).then((value) => {
            if (value.status !== 'IN_PROGRESS') {
                navigate(`/student/attempts/${value.attemptId}/result`)
                return
            }
            setAttempt(value)
            setRemaining(Math.max(0, new Date(value.expiresAt).getTime() - Date.now()))
        }).catch((reason) => setError(
            reason instanceof Error ? reason.message : 'Unable to load attempt'
        ))
    }, [attemptId, navigate])

    useEffect(() => {
        if (!attempt || attempt.status !== 'IN_PROGRESS') return
        const timer = window.setInterval(() => setRemaining(
            Math.max(0, new Date(attempt.expiresAt).getTime() - Date.now())
        ), 1000)
        return () => window.clearInterval(timer)
    }, [attempt])

    useEffect(() => {
        if (!attempt || remaining > 0 || attempt.status !== 'IN_PROGRESS'
            || autoSubmitStarted.current) return
        autoSubmitStarted.current = true
        void submitStudentAttempt(attempt.attemptId)
            .then(() => navigate(`/student/attempts/${attempt.attemptId}/result`))
            .catch((reason) => {
                setError(reason instanceof Error ? reason.message : 'Automatic submission failed')
                autoSubmitStarted.current = false
            })
    }, [attempt, remaining, navigate])

    const question = attempt?.questions[current]
    const answered = useMemo(
        () => attempt?.questions.filter((item) => item.selectedOptionId !== null).length ?? 0,
        [attempt]
    )

    async function choose(optionId: number) {
        if (!attempt || !question || saving || !attempt.answersAllowed) return
        setSaving(true)
        setError('')
        try {
            await saveStudentAnswer(attempt.attemptId, question.id, optionId)
            setAttempt({
                ...attempt,
                questions: attempt.questions.map((item) =>
                    item.id === question.id
                        ? { ...item, selectedOptionId: optionId }
                        : item
                ),
            })
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to save answer')
        } finally {
            setSaving(false)
        }
    }

    async function submit() {
        if (!attempt || !window.confirm('Submit your examination? This cannot be undone.')) return
        try {
            await submitStudentAttempt(attempt.attemptId)
            navigate(`/student/attempts/${attempt.attemptId}/result`)
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to submit')
        }
    }

    if (!attempt || !question) {
        return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">{error || 'Loading examination...'}</main>
    }

    const seconds = Math.floor(remaining / 1000)
    return (
        <main className="min-h-screen bg-slate-950 px-4 py-6 text-white">
            <div className="mx-auto max-w-6xl">
                <header className="flex flex-wrap justify-between gap-4 border-b border-slate-800 pb-5">
                    <div>
                        <h1 className="text-xl font-bold">{attempt.title}</h1>
                        {!attempt.answersAllowed && (
                            <p className="mt-2 text-sm text-amber-300">
                                This examination is closed. Saved answers are read-only; you may still submit them.
                            </p>
                        )}
                    </div>
                    <div className="text-right">
                        <p className="text-sm text-slate-400">{answered}/{attempt.questions.length} answered</p>
                        <p className="font-mono text-lg text-cyan-300">
                            {Math.floor(seconds / 60).toString().padStart(2, '0')}:{(seconds % 60).toString().padStart(2, '0')}
                        </p>
                    </div>
                </header>
                <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_260px]">
                    <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
                        <p className="text-sm text-slate-500">Question {question.questionNumber} · {question.marks} marks · −{question.negativeMarks}</p>
                        <h2 className="mt-4 text-2xl font-semibold">{question.questionText}</h2>
                        <div className="mt-8 space-y-3">
                            {question.options.map((option) => (
                                <button
                                    key={option.id}
                                    disabled={!attempt.answersAllowed}
                                    onClick={() => void choose(option.id)}
                                    className={`block w-full rounded-lg border p-4 text-left disabled:cursor-not-allowed disabled:opacity-60 ${question.selectedOptionId === option.id ? 'border-blue-400 bg-blue-500/10' : 'border-slate-700 hover:border-slate-500'}`}
                                >
                                    {option.optionText}
                                </button>
                            ))}
                        </div>
                        <div className="mt-8 flex justify-between">
                            <button disabled={current === 0} onClick={() => setCurrent(current - 1)} className="rounded-lg border border-slate-700 px-4 py-2 disabled:opacity-40">Previous</button>
                            {current === attempt.questions.length - 1
                                ? <button onClick={() => void submit()} className="rounded-lg bg-green-600 px-4 py-2 font-semibold">Submit saved answers</button>
                                : <button onClick={() => setCurrent(current + 1)} className="rounded-lg bg-blue-600 px-4 py-2">Next</button>}
                        </div>
                        {error && <p role="alert" className="mt-4 text-red-400">{error}</p>}
                    </section>
                    <aside className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
                        <h2 className="font-semibold">Questions</h2>
                        <div className="mt-4 grid grid-cols-5 gap-2">
                            {attempt.questions.map((item, index) => (
                                <button key={item.id} onClick={() => setCurrent(index)} className={`rounded p-2 text-sm ${index === current ? 'bg-blue-600' : item.selectedOptionId !== null ? 'bg-slate-700' : 'bg-slate-800'}`}>
                                    {index + 1}
                                </button>
                            ))}
                        </div>
                    </aside>
                </div>
            </div>
        </main>
    )
}
