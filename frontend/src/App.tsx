
import {
    Navigate,
    Route,
    Routes,
} from 'react-router'
import AdminUsersPage from './pages/AdminUsersPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import TeacherExamsPage from './pages/TeacherExamsPage'
import TeacherQuestionsPage from './pages/TeacherQuestionsPage'
import StudentExamsPage from './pages/StudentExamsPage'
import ExamInstructionsPage from './pages/ExamInstructionsPage'
import TakeExamPage from './pages/TakeExamPage'
import ExamResultPage from './pages/ExamResultPage'

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
                path="/teacher/exams"
                element={<TeacherExamsPage />}
            />

            <Route
                path="/teacher/exams/:examId/questions"
                element={<TeacherQuestionsPage />}
            />

            <Route path="/student/exams" element={<StudentExamsPage />} />
            <Route path="/student/exams/:examId" element={<ExamInstructionsPage />} />
            <Route path="/student/attempts/:attemptId" element={<TakeExamPage />} />
            <Route path="/student/attempts/:attemptId/result" element={<ExamResultPage />} />

            <Route
                path="*"
                element={<Navigate to="/" replace />}
            />

        </Routes>
    )
}
