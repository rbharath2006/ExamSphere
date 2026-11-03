import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
    getCurrentUser,
    getTeacherExams,
    getTeacherQuestions,
    saveTeacherQuestion,
    deleteTeacherQuestion,
    type QuestionPayload,
    type TeacherExam,
    type TeacherQuestion,
} from '../services/authService'

const mcqOptions = () => [
    { optionText: '', isCorrect: true },
    { optionText: '', isCorrect: false },
    { optionText: '', isCorrect: false },
    { optionText: '', isCorrect: false },
]

const trueFalseOptions = () => [
    { optionText: 'True', isCorrect: true },
    { optionText: 'False', isCorrect: false },
]

export default function TeacherQuestionsPage() {
    const { examId } = useParams()
    const navigate = useNavigate()
    const id = Number(examId)
    const [exam, setExam] = useState<TeacherExam | null>(null)
    const [questions, setQuestions] = useState<TeacherQuestion[]>([])
    const [editingId, setEditingId] = useState<number | undefined>()
    const [form, setForm] = useState<QuestionPayload>({
        questionText: '',
        questionType: 'SINGLE_CHOICE',
        marks: 1,
        negativeMarks: 0,
        options: mcqOptions(),
    })
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')

    useEffect(() => {
        let active = true
        async function load() {
            try {
                const user = await getCurrentUser()
                if (user.role !== 'TEACHER' || !Number.isInteger(id)) {
                    navigate('/', { replace: true })
                    return
                }
                const [examList, questionList] = await Promise.all([
                    getTeacherExams(),
                    getTeacherQuestions(id),
                ])
                const currentExam = examList.find((item) => item.id === id)
                if (!currentExam) {
                    navigate('/teacher/exams', { replace: true })
                    return
                }
                if (active) {
                    setExam(currentExam)
                    setQuestions(questionList)
                }
            } catch (reason) {
                if (active) setError(reason instanceof Error ? reason.message : 'Unable to load questions')
            } finally {
                if (active) setLoading(false)
            }
        }
        void load()
        return () => { active = false }
    }, [id, navigate])

    function changeType(type: QuestionPayload['questionType']) {
        setForm({ ...form, questionType: type, options: type === 'TRUE_FALSE' ? trueFalseOptions() : mcqOptions() })
    }

    function updateOption(index: number, field: 'optionText' | 'isCorrect', value: string | boolean) {
        const options = form.options.map((option, optionIndex) => {
            if (optionIndex !== index) return { ...option, isCorrect: field === 'isCorrect' ? false : option.isCorrect }
            return { ...option, [field]: value }
        })
        setForm({ ...form, options })
    }

    function editQuestion(question: TeacherQuestion) {
        setEditingId(question.id)
        setForm({
            questionText: question.questionText,
            questionType: question.questionType,
            marks: question.marks,
            negativeMarks: question.negativeMarks,
            options: question.options.map(({ optionText, isCorrect }) => ({ optionText, isCorrect })),
        })
        setMessage('')
    }

    function resetForm() {
        setEditingId(undefined)
        setForm({ questionText: '', questionType: 'SINGLE_CHOICE', marks: 1, negativeMarks: 0, options: mcqOptions() })
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setMessage('')
        if (form.options.filter((option) => option.isCorrect).length !== 1) {
            setError('Select exactly one correct answer.')
            return
        }
        setSaving(true)
        try {
            const saved = await saveTeacherQuestion(id, {
                ...form,
                marks: Number(form.marks),
                negativeMarks: Number(form.negativeMarks),
            }, editingId)
            const nextQuestions = editingId
                ? questions.map((item) => item.id === saved.id ? saved : item)
                : [...questions, saved]
            setQuestions(nextQuestions)
            if (exam) {
                setExam({
                    ...exam,
                    totalMarks: nextQuestions.reduce(
                        (total, item) => total + Number(item.marks),
                        0
                    ),
                })
            }
            resetForm()
            setMessage(editingId ? 'Question updated.' : 'Question added.')
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to save question')
        } finally {
            setSaving(false)
        }
    }

    async function remove(question: TeacherQuestion) {
        if (!window.confirm(`Delete question ${question.displayOrder}?`)) return
        try {
            await deleteTeacherQuestion(id, question.id)
            setQuestions((current) => current.filter((item) => item.id !== question.id))
            if (exam) {
                setExam({
                    ...exam,
                    totalMarks: Number(exam.totalMarks) - Number(question.marks),
                })
            }
            setMessage('Question deleted.')
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Unable to delete question')
        }
    }

    if (loading) return <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">Loading questions...</main>
    if (!exam) return null

    return (
        <main className="min-h-screen bg-slate-950 px-6 py-8 text-white">
            <div className="mx-auto max-w-6xl">
                <header className="flex items-center justify-between border-b border-slate-800 pb-6">
                    <Link to="/teacher/exams" className="text-2xl font-bold">Exam<span className="text-blue-400">Sphere</span></Link>
                    <Link to="/teacher/exams" className="text-sm text-blue-400 hover:text-blue-300">Back to examinations</Link>
                </header>
                <section className="mt-10">
                    <p className="text-sm uppercase tracking-widest text-purple-400">Question management</p>
                    <h1 className="mt-2 text-3xl font-bold">{exam.title}</h1>
                    <p className="mt-2 text-slate-400">Total marks: <span className="text-white">{exam.totalMarks}</span></p>
                </section>
                <section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6">
                    <h2 className="text-xl font-semibold">{editingId ? 'Edit question' : 'Add question'}</h2>
                    <form onSubmit={(event) => void submit(event)} className="mt-5 space-y-4">
                        <textarea required maxLength={5000} placeholder="Question text" value={form.questionText} onChange={(event) => setForm({ ...form, questionText: event.target.value })} className="input min-h-28" />
                        <div className="grid gap-4 sm:grid-cols-3">
                            <label className="text-sm text-slate-400">Type<select value={form.questionType} onChange={(event) => changeType(event.target.value as QuestionPayload['questionType'])} className="input mt-2"><option value="SINGLE_CHOICE">Single choice</option><option value="TRUE_FALSE">True / False</option></select></label>
                            <label className="text-sm text-slate-400">Marks<input required type="number" min="0.01" step="0.01" value={form.marks} onChange={(event) => setForm({ ...form, marks: Number(event.target.value) })} className="input mt-2" /></label>
                            <label className="text-sm text-slate-400">Negative marks<input type="number" min="0" step="0.01" value={form.negativeMarks} onChange={(event) => setForm({ ...form, negativeMarks: Number(event.target.value) })} className="input mt-2" /></label>
                        </div>
                        <div className="grid gap-3 sm:grid-cols-2">
                            {form.options.map((option, index) => (
                                <label key={index} className="flex items-center gap-3 rounded-lg border border-slate-700 p-3">
                                    <input type="radio" name="correct-answer" checked={option.isCorrect} onChange={() => updateOption(index, 'isCorrect', true)} />
                                    <input required maxLength={1000} value={option.optionText} readOnly={form.questionType === 'TRUE_FALSE'} onChange={(event) => updateOption(index, 'optionText', event.target.value)} className="input" />
                                </label>
                            ))}
                        </div>
                        <div className="flex gap-3">
                            <button disabled={saving} className="rounded-lg bg-blue-600 px-5 py-3 font-semibold hover:bg-blue-500 disabled:opacity-50">{saving ? 'Saving...' : editingId ? 'Update question' : 'Add question'}</button>
                            {editingId && <button type="button" onClick={resetForm} className="rounded-lg border border-slate-700 px-5 py-3 text-slate-300 hover:bg-slate-800">Cancel</button>}
                        </div>
                    </form>
                    {message && <p className="mt-4 text-sm text-green-400">{message}</p>}
                    {error && <p role="alert" className="mt-4 text-sm text-red-400">{error}</p>}
                </section>
                <section className="mt-8 space-y-4">
                    {questions.map((question) => (
                        <article key={question.id} className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
                            <div className="flex flex-wrap items-start justify-between gap-4">
                                <div><p className="text-xs uppercase tracking-widest text-slate-500">Question {question.displayOrder}</p><h3 className="mt-2 text-lg font-semibold">{question.questionText}</h3></div>
                                <p className="text-sm text-slate-400">{question.marks} marks · −{question.negativeMarks}</p>
                            </div>
                            <div className="mt-4 grid gap-2 sm:grid-cols-2">{question.options.map((option) => <p key={option.id ?? option.optionText} className="rounded-lg bg-slate-950 px-3 py-2 text-sm text-slate-300">{option.optionText}</p>)}</div>
                            <div className="mt-5 flex gap-3"><button onClick={() => editQuestion(question)} className="rounded-lg border border-blue-500/40 px-4 py-2 text-sm text-blue-300 hover:bg-blue-500/10">Edit</button><button onClick={() => void remove(question)} className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-300 hover:bg-red-500/10">Delete</button></div>
                        </article>
                    ))}
                    {questions.length === 0 && <p className="rounded-xl border border-slate-800 bg-slate-900 p-6 text-slate-400">No questions yet.</p>}
                </section>
            </div>
        </main>
    )
}
