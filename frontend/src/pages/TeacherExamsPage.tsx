import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import {
    createTeacherExam,
    closeTeacherExam,
    getCurrentUser,
    getTeacherExams,
    logout,
    publishTeacherExam,
    publishTeacherExamResults,
    type CreateExamPayload,
    type TeacherExam,
} from '../services/authService'

const emptyForm: CreateExamPayload = {
    title: '',
    description: '',
    subject: '',
    instructions: '',
    durationMinutes: 60,
    startsAt: '',
    endsAt: '',
}

function formatDate(value: string) {
    return new Date(value).toLocaleString()
}

export default function TeacherExamsPage() {
    const navigate = useNavigate()
    const [exams, setExams] = useState<TeacherExam[]>([])
    const [form, setForm] = useState(emptyForm)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [publishingExamId, setPublishingExamId] = useState<number | null>(null)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    useEffect(() => {
        let active = true
        async function load() {
            try {
                const user = await getCurrentUser()
                if (user.role !== 'TEACHER') {
                    navigate('/', { replace: true })
                    return
                }
                const result = await getTeacherExams()
                if (active) setExams(result)
            } catch {
                if (active) navigate('/login/teacher', { replace: true })
            } finally {
                if (active) setLoading(false)
            }
        }
        void load()
        return () => { active = false }
    }, [navigate])

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setSuccess('')
        setSaving(true)
        try {
            const created = await createTeacherExam({
                ...form,
                durationMinutes: Number(form.durationMinutes),
                startsAt: new Date(form.startsAt).toISOString(),
                endsAt: new Date(form.endsAt).toISOString(),
            })
            setExams((current) => [created, ...current])
            setForm(emptyForm)
            setSuccess('Examination created as a draft.')
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to create examination')
        } finally {
            setSaving(false)
        }
    }

    async function handleLogout() {
        await logout()
        navigate('/', { replace: true })
    }

    async function changeExamStatus(
        exam: TeacherExam,
        action: 'publish' | 'close'
    ) {
        const prompt = action === 'publish'
            ? 'Publish this examination? Questions cannot be edited after publication.'
            : 'Close this examination? New attempts and answer changes will be blocked. Students with existing attempts can still submit saved answers.'
        if (!window.confirm(prompt)) return

        setError('')
        setSuccess('')
        try {
            if (action === 'publish') {
                await publishTeacherExam(exam.id)
            } else {
                await closeTeacherExam(exam.id)
            }
            const refreshed = await getTeacherExams()
            setExams(refreshed)
            setSuccess(
                action === 'publish'
                    ? 'Examination published successfully.'
                    : 'Examination closed successfully.'
            )
        } catch (reason) {
            setError(
                reason instanceof Error
                    ? reason.message
                    : 'Unable to update examination status'
            )
        }
    }

    async function publishResults(exam: TeacherExam) {
        if (publishingExamId !== null) return
        if (!window.confirm('Publish results for this examination? Students will then be able to view their results.')) return

        setError('')
        setSuccess('')
        setPublishingExamId(exam.id)
        try {
            await publishTeacherExamResults(exam.id)
            setExams(await getTeacherExams())
            setSuccess('Examination results published successfully.')
        } catch (reason) {
            setError(
                reason instanceof Error
                    ? reason.message
                    : 'Unable to publish examination results'
            )
        } finally {
            setPublishingExamId(null)
        }
    }

    if (loading) {
        return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">Loading examinations...</main>
    }

    return (
        <main className="min-h-screen bg-slate-950 px-6 py-8 text-white">
            <div className="mx-auto max-w-6xl">
                <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-6">
                    <Link to="/" className="text-2xl font-bold">Exam<span className="text-blue-400">Sphere</span></Link>
                    <div className="flex gap-3">
                        <Link to="/dashboard/teacher" className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">Dashboard</Link>
                        <button onClick={() => void handleLogout()} className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-400 hover:bg-red-500/10">Logout</button>
                    </div>
                </header>

                <section className="mt-10">
                    <p className="text-sm font-semibold uppercase tracking-widest text-blue-400">Teacher portal</p>
                    <h1 className="mt-2 text-3xl font-bold">My Examinations</h1>
                    <p className="mt-2 text-slate-400">Create examinations and manage their questions.</p>
                </section>

                <section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6">
                    <h2 className="text-xl font-semibold">Create examination</h2>
                    <form onSubmit={(event) => void handleSubmit(event)} className="mt-5 grid gap-4 md:grid-cols-2">
                        <input required maxLength={200} placeholder="Title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} className="input" />
                        <input required maxLength={120} placeholder="Subject" value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} className="input" />
                        <textarea maxLength={10000} placeholder="Description (optional)" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} className="input min-h-24 md:col-span-2" />
                        <textarea maxLength={10000} placeholder="Instructions (optional)" value={form.instructions} onChange={(event) => setForm({ ...form, instructions: event.target.value })} className="input min-h-24 md:col-span-2" />
                        <label className="text-sm text-slate-400">Duration (minutes)<input required type="number" min="1" max="600" value={form.durationMinutes} onChange={(event) => setForm({ ...form, durationMinutes: Number(event.target.value) })} className="input mt-2" /></label>
                        <label className="text-sm text-slate-400">Starts at<input required type="datetime-local" value={form.startsAt} onChange={(event) => setForm({ ...form, startsAt: event.target.value })} className="input mt-2" /></label>
                        <label className="text-sm text-slate-400">Ends at<input required type="datetime-local" value={form.endsAt} onChange={(event) => setForm({ ...form, endsAt: event.target.value })} className="input mt-2" /></label>
                        <div className="md:col-span-2">
                            <button disabled={saving} className="rounded-lg bg-blue-600 px-5 py-3 font-semibold hover:bg-blue-500 disabled:opacity-50">{saving ? 'Creating...' : 'Create draft'}</button>
                        </div>
                    </form>
                    {success && <p className="mt-4 text-sm text-green-400">{success}</p>}
                    {error && <p role="alert" className="mt-4 text-sm text-red-400">{error}</p>}
                </section>

                <section className="mt-8">
                    <h2 className="text-2xl font-semibold">Your examinations</h2>
                    {exams.length === 0 && <p className="mt-5 rounded-xl border border-slate-800 bg-slate-900 p-6 text-slate-400">No examinations yet.</p>}
                    <div className="mt-5 grid gap-5 lg:grid-cols-2">
                        {exams.map((exam) => (
                            <article key={exam.id} className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
                                <div className="flex items-start justify-between gap-4">
                                    <div><h3 className="text-xl font-semibold">{exam.title}</h3><p className="mt-1 text-blue-400">{exam.subject}</p></div>
                                    <span className="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-300">{exam.status}</span>
                                </div>
                                <div className="mt-5 grid grid-cols-2 gap-3 text-sm text-slate-400">
                                    <p>Duration: <span className="text-slate-200">{exam.durationMinutes} min</span></p>
                                    <p>Total marks: <span className="text-slate-200">{exam.totalMarks}</span></p>
                                    <p className="col-span-2">Starts: <span className="text-slate-200">{formatDate(exam.startsAt)}</span></p>
                                    <p className="col-span-2">Ends: <span className="text-slate-200">{formatDate(exam.endsAt)}</span></p>
                                </div>
                                <div className="mt-6 flex flex-wrap gap-3">
                                    {exam.status === 'DRAFT' ? (
                                        <>
                                            <Link to={`/teacher/exams/${exam.id}/questions`} className="rounded-lg border border-purple-500/50 px-4 py-2 text-sm text-purple-300 hover:bg-purple-500/10">Manage Questions</Link>
                                            <button onClick={() => void changeExamStatus(exam, 'publish')} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500">Publish</button>
                                        </>
                                    ) : exam.status === 'PUBLISHED' ? (
                                        <button onClick={() => void changeExamStatus(exam, 'close')} className="rounded-lg border border-amber-500/50 px-4 py-2 text-sm text-amber-300 hover:bg-amber-500/10">Close Examination</button>
                                    ) : (
                                        <>
                                            {!exam.resultsPublishedAt ? (
                                                <button
                                                    disabled={publishingExamId !== null}
                                                    onClick={() => void publishResults(exam)}
                                                    className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    {publishingExamId === exam.id ? 'Publishing...' : 'Publish Results'}
                                                </button>
                                            ) : (
                                                <span className="rounded-lg border border-green-500/50 px-4 py-2 text-sm text-green-300">
                                                    Results published
                                                </span>
                                            )}
                                            <span className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-500">Editing disabled</span>
                                        </>
                                    )}
                                </div>
                                {exam.resultsPublishedAt && (
                                    <p className="mt-4 text-sm text-green-400">
                                        Results published: {formatDate(exam.resultsPublishedAt)}
                                    </p>
                                )}
                            </article>
                        ))}
                    </div>
                </section>
            </div>
        </main>
    )
}
