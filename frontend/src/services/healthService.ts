
export interface HealthResponse {
    status: string
    application: string
    message: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
    const response = await fetch(
        'http://localhost:8080/api/health'
    )

    if (!response.ok) {
        throw new Error(`Backend error: ${response.status}`)
    }

    return response.json() as Promise<HealthResponse>
}
