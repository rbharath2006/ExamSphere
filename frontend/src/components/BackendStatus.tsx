
import { useEffect, useState } from 'react'
import { getBackendHealth } from '../services/healthService'

type ConnectionState = 'checking' | 'connected' | 'unavailable'

export default function BackendStatus() {
    const [status, setStatus] = useState<ConnectionState>('checking')
    const [message, setMessage] = useState('Checking backend connection...')

    useEffect(() => {
        let active = true

        getBackendHealth()
            .then((data) => {
                if (active) {
                    setStatus('connected')
                    setMessage(data.message)
                }
            })
            .catch((error: unknown) => {
                if (active) {
                    setStatus('unavailable')
                    setMessage(
                        error instanceof Error ? error.message : 'Connection failed'
                    )
                }
            })

        return () => {
            active = false
        }
    }, [])

    const styles = {
        checking: 'bg-yellow-500/10 text-yellow-400',
        connected: 'bg-green-500/10 text-green-400',
        unavailable: 'bg-red-500/10 text-red-400',
    }

    return (
        <div className={`mt-6 rounded-xl p-4 text-sm ${styles[status]}`}>
            <p className="font-semibold">
                Backend: {status.toUpperCase()}
            </p>
            <p className="mt-1">{message}</p>
        </div>
    )
}
