
const API_BASE_URL = 'http://localhost:8080'

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface UserResponse {
    id: number
    fullName: string
    email: string
    role: UserRole
    createdAt: string
}

export class ApiRequestError extends Error {
    readonly status: number

    constructor(message: string, status: number) {
        super(message)
        this.name = 'ApiRequestError'
        this.status = status
    }
}

async function getCsrfHeaders(): Promise<Record<string, string>> {
    const response = await fetch(`${API_BASE_URL}/api/csrf`, {
        method: 'GET',
        credentials: 'include',
    })

    if (!response.ok) {
        throw new Error('Could not obtain CSRF token')
    }

    // Consume the response before reading the cookie.
    await response.json()

    const cookie = document.cookie
        .split(';')
        .map((item) => item.trim())
        .find((item) => item.startsWith('XSRF-TOKEN='))

    if (!cookie) {
        throw new Error(
            'CSRF cookie not found. Check localhost and backend CORS settings.'
        )
    }

    const token = decodeURIComponent(
        cookie.substring('XSRF-TOKEN='.length)
    )

    return {
        'X-XSRF-TOKEN': token,
    }
}

async function getErrorMessage(response: Response): Promise<string> {
    try {
        const data = await response.json() as {
            error?: string
            message?: string
        }

        return data.message ?? data.error ?? `Request failed (${response.status})`
    } catch {
        return `Request failed (${response.status})`
    }
}

export async function login(
    email: string,
    password: string
): Promise<UserResponse> {

    const headers = await getCsrfHeaders()

    const formData = new URLSearchParams()
    formData.set('email', email.trim())
    formData.set('password', password)

    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            ...headers,
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: formData.toString(),
    })

    if (!response.ok) {
        throw new ApiRequestError(
            await getErrorMessage(response),
            response.status
        )
    }

    // Obtain a fresh CSRF token after authentication.
    await getCsrfHeaders()

    return getCurrentUser()
}

export async function getCurrentUser(): Promise<UserResponse> {
    const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        method: 'GET',
        credentials: 'include',
    })

    if (!response.ok) {
        throw new ApiRequestError(
            await getErrorMessage(response),
            response.status
        )
    }

    return response.json() as Promise<UserResponse>
}

export async function logout(): Promise<void> {
    const headers = await getCsrfHeaders()

    const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers,
    })

    if (!response.ok) {
        throw new Error(await getErrorMessage(response))
    }
}

export async function getRoleDashboard(role: UserRole) {
    const response = await fetch(
        `${API_BASE_URL}/api/${role.toLowerCase()}/dashboard`,
        {
            method: 'GET',
            credentials: 'include',
        }
    )

    if (!response.ok) {
        throw new Error(await getErrorMessage(response))
    }

    return response.json() as Promise<{
        role: UserRole
        message: string
    }>
}

export interface AdminDashboardSummaryResponse {
    totalUsers: number
    totalStudents: number
    totalTeachers: number
    totalAdmins: number
    activeUsers: number
    disabledUsers: number
}

export async function getAdminDashboardSummary(): Promise<AdminDashboardSummaryResponse> {
    const response = await fetch(
        'http://localhost:8080/api/admin/dashboard/summary',
        {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        }
    )

    if (!response.ok) {
        let message = `Failed to load dashboard statistics (${response.status})`

        try {
            const errorBody = await response.json()
            message = errorBody.message ?? errorBody.error ?? message
        } catch {
            // Keep the fallback error message.
        }

        throw new Error(message)
    }

    return response.json() as Promise<AdminDashboardSummaryResponse>
}

export type ManagedUserRole = 'STUDENT' | 'TEACHER'

export interface AdminManagedUser {
    id: number
    fullName: string
    email: string
    role: ManagedUserRole
    enabled: boolean
    createdAt: string
}

export interface AdminUserPageResponse {
    content: AdminManagedUser[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

/**
 * Fetch Students and Teachers from the Admin user management API.
 */
export async function getAdminUsers(options: {
    role?: ManagedUserRole
    search?: string
    page?: number
    size?: number
} = {}): Promise<AdminUserPageResponse> {

    const params = new URLSearchParams()

    if (options.role) {
        params.set('role', options.role)
    }

    if (options.search?.trim()) {
        params.set('search', options.search.trim())
    }

    params.set('page', String(options.page ?? 0))
    params.set('size', String(options.size ?? 10))

    const response = await fetch(
        `${API_BASE_URL}/api/admin/users?${params.toString()}`,
        {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        }
    )

    if (!response.ok) {
        throw new Error(await getErrorMessage(response))
    }

    return response.json() as Promise<AdminUserPageResponse>
}

/**
 * Enable or disable a Student or Teacher account.
 */
export async function updateAdminUserStatus(
    userId: number,
    enabled: boolean
): Promise<AdminManagedUser> {

    const csrfHeaders = await getCsrfHeaders()

    const response = await fetch(
        `${API_BASE_URL}/api/admin/users/${userId}/status`,
        {
            method: 'PATCH',
            credentials: 'include',
            headers: {
                ...csrfHeaders,
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify({ enabled }),
        }
    )

    if (!response.ok) {
        throw new Error(await getErrorMessage(response))
    }

    return response.json() as Promise<AdminManagedUser>
}

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'

export interface TeacherExam {
    id: number
    title: string
    description: string | null
    subject: string
    instructions: string | null
    durationMinutes: number
    startsAt: string
    endsAt: string
    status: ExamStatus
    totalMarks: number
    resultsPublishedAt: string | null
}

export interface QuestionOption {
    id?: number
    optionText: string
    displayOrder?: number
    isCorrect: boolean
}

export interface TeacherQuestion {
    id: number
    examId: number
    questionText: string
    questionType: 'SINGLE_CHOICE' | 'TRUE_FALSE'
    marks: number
    negativeMarks: number
    displayOrder: number
    options: QuestionOption[]
}

async function teacherRequest<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const method = options.method ?? 'GET'
    const csrfHeaders = method === 'GET'
        ? {}
        : await getCsrfHeaders()

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        method,
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            ...csrfHeaders,
            ...options.headers,
        },
    })

    if (!response.ok) {
        throw new ApiRequestError(
            await getErrorMessage(response),
            response.status
        )
    }

    if (response.status === 204) return undefined as T
    return response.json() as Promise<T>
}

export async function getTeacherExams(): Promise<TeacherExam[]> {
    return teacherRequest<TeacherExam[]>('/api/teacher/exams')
}

export async function publishTeacherExam(examId: number): Promise<TeacherExam> {
    return teacherRequest<TeacherExam>(
        `/api/teacher/exams/${examId}/publish`,
        { method: 'POST' }
    )
}

export async function closeTeacherExam(examId: number): Promise<TeacherExam> {
    return teacherRequest<TeacherExam>(
        `/api/teacher/exams/${examId}/close`,
        { method: 'POST' }
    )
}

export async function publishTeacherExamResults(examId: number): Promise<TeacherExam> {
    return teacherRequest<TeacherExam>(
        `/api/teacher/exams/${examId}/results/publish`,
        { method: 'POST' }
    )
}

export interface CreateExamPayload {
    title: string
    description: string
    subject: string
    instructions: string
    durationMinutes: number
    startsAt: string
    endsAt: string
}

export async function createTeacherExam(
    payload: CreateExamPayload
): Promise<TeacherExam> {
    return teacherRequest<TeacherExam>('/api/teacher/exams', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })
}

export async function getTeacherQuestions(
    examId: number
): Promise<TeacherQuestion[]> {
    return teacherRequest<TeacherQuestion[]>(
        `/api/teacher/exams/${examId}/questions`
    )
}

export interface QuestionPayload {
    questionText: string
    questionType: TeacherQuestion['questionType']
    marks: number
    negativeMarks: number
    options: QuestionOption[]
}

export async function saveTeacherQuestion(
    examId: number,
    payload: QuestionPayload,
    questionId?: number
): Promise<TeacherQuestion> {
    const path = questionId
        ? `/api/teacher/exams/${examId}/questions/${questionId}`
        : `/api/teacher/exams/${examId}/questions`

    return teacherRequest<TeacherQuestion>(path, {
        method: questionId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })
}

export async function deleteTeacherQuestion(
    examId: number,
    questionId: number
): Promise<void> {
    await teacherRequest<void>(
        `/api/teacher/exams/${examId}/questions/${questionId}`,
        { method: 'DELETE' }
    )
}

export interface StudentExam {
    id: number
    title: string
    subject: string
    instructions: string | null
    durationMinutes: number
    startsAt: string
    endsAt: string
    status: ExamStatus
    totalMarks: number
    upcoming: boolean
    available: boolean
    attempted: boolean
    attemptId: number | null
}

export interface StudentOption {
    id: number
    optionText: string
}

export interface StudentQuestion {
    id: number
    questionNumber: number
    questionText: string
    marks: number
    negativeMarks: number
    options: StudentOption[]
    selectedOptionId: number | null
}

export interface StudentAttempt {
    attemptId: number
    examId: number
    title: string
    durationMinutes: number
    startedAt: string
    expiresAt: string
    status: 'IN_PROGRESS' | 'SUBMITTED' | 'AUTO_SUBMITTED'
    answersAllowed: boolean
    questions: StudentQuestion[]
}

export interface StudentResult {
    attemptId: number
    title: string
    totalMarks: number
    obtainedMarks: number
    percentage: number
    status: StudentAttempt['status']
    submittedAt: string
}

export async function getStudentExams(): Promise<StudentExam[]> {
    return teacherRequest<StudentExam[]>('/api/student/exams')
}

export async function startStudentExam(examId: number): Promise<StudentAttempt> {
    return teacherRequest<StudentAttempt>(
        `/api/student/exams/${examId}/attempts/start`, { method: 'POST' }
    )
}

export async function getStudentAttempt(attemptId: number): Promise<StudentAttempt> {
    return teacherRequest<StudentAttempt>(`/api/student/attempts/${attemptId}`)
}

export async function saveStudentAnswer(
    attemptId: number, questionId: number, selectedOptionId: number
): Promise<void> {
    await teacherRequest<void>(`/api/student/attempts/${attemptId}/answers`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId, selectedOptionId }),
    })
}

export async function submitStudentAttempt(attemptId: number): Promise<StudentResult> {
    return teacherRequest<StudentResult>(
        `/api/student/attempts/${attemptId}/submit`, { method: 'POST' }
    )
}

export async function getStudentResult(attemptId: number): Promise<StudentResult> {
    return teacherRequest<StudentResult>(`/api/student/attempts/${attemptId}/result`)
}
