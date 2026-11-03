
import { API_BASE_URL } from './apiConfig'

export interface HealthResponse {
    status: string
    application: string
    message: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
    const response = await fetch(
        `${API_BASE_URL}/api/health`
    )

    if (!response.ok) {
        throw new Error(`Backend error: ${response.status}`)
    }

    return response.json() as Promise<HealthResponse>
}
