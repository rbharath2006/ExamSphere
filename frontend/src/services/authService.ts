
const API_BASE_URL = 'http://localhost:8080'

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface UserResponse {
    id: number
    fullName: string
    email: string
    role: UserRole
    createdAt: string
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
        throw new Error(await getErrorMessage(response))
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
        throw new Error(await getErrorMessage(response))
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
