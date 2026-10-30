export const roles = [
    {
        id: 'admin',
        title: 'Administrator',
        description: 'Manage users, teachers and examinations.',
        icon: '🛡️',
    },
    {
        id: 'teacher',
        title: 'Teacher',
        description: 'Create examinations and manage questions.',
        icon: '👨‍🏫',
    },
    {
        id: 'student',
        title: 'Student',
        description: 'Attend examinations and view results.',
        icon: '🎓',
    },
] as const

export type Role = (typeof roles)[number]