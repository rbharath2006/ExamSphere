
import {
    Navigate,
    Route,
    Routes,
} from 'react-router'
import AdminUsersPage from './pages/AdminUsersPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'

export default function App() {
    return (
        <Routes>

            <Route path="/" element={<HomePage />} />

            <Route
                path="/login/:role"
                element={<LoginPage />}
            />

            <Route
                path="/dashboard/:role"
                element={<DashboardPage />}
            />

            <Route
                path="/admin/users"
                element={<AdminUsersPage />}
            />

            <Route
                path="*"
                element={<Navigate to="/" replace />}
            />

        </Routes>
    )
}
