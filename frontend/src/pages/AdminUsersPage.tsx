import { useEffect, useState, type FormEvent } from 'react'

import {
    Link,
    useNavigate,
} from 'react-router'

import {
    getCurrentUser,
    getAdminUsers,
    updateAdminUserStatus,
    logout,
    type AdminManagedUser,
    type AdminUserPageResponse,
    type ManagedUserRole,
} from '../services/authService'

const PAGE_SIZE = 10

type RoleFilter = 'ALL' | ManagedUserRole

interface PendingStatusChange {
    user: AdminManagedUser
    enabled: boolean
}

export default function AdminUsersPage() {
    const navigate = useNavigate()

    // Authentication
    const [authLoading, setAuthLoading] = useState(true)
    const [authorized, setAuthorized] = useState(false)
    const [adminName, setAdminName] = useState('')

    // User listing
    const [users, setUsers] =
        useState<AdminUserPageResponse | null>(null)

    const [listLoading, setListLoading] = useState(false)
    const [error, setError] = useState('')

    // Search and filtering
    const [searchInput, setSearchInput] = useState('')
    const [appliedSearch, setAppliedSearch] = useState('')
    const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL')
    const [page, setPage] = useState(0)

    // Account status management
    const [pendingChange, setPendingChange] =
        useState<PendingStatusChange | null>(null)

    const [updatingId, setUpdatingId] = useState<number | null>(null)
    const [actionMessage, setActionMessage] = useState('')

    // Verify that the current user is an Admin.
    useEffect(() => {
        let active = true

        async function verifyAdmin() {
            try {
                const currentUser = await getCurrentUser()

                if (!active) return

                if (currentUser.role !== 'ADMIN') {
                    navigate(
                        `/dashboard/${currentUser.role.toLowerCase()}`,
                        { replace: true }
                    )
                    return
                }

                setAdminName(currentUser.fullName)
                setAuthorized(true)

            } catch {
                if (active) {
                    navigate('/login/admin', { replace: true })
                }
            } finally {
                if (active) {
                    setAuthLoading(false)
                }
            }
        }

        void verifyAdmin()

        return () => {
            active = false
        }
    }, [navigate])

    // Load users whenever search, filter or page changes.
    useEffect(() => {
        if (!authorized) return

        let active = true

        async function loadUsers() {
            setListLoading(true)
            setError('')

            try {
                const result = await getAdminUsers({
                    role: roleFilter === 'ALL' ? undefined : roleFilter,
                    search: appliedSearch,
                    page,
                    size: PAGE_SIZE,
                })

                if (active) {
                    setUsers(result)
                }

            } catch (error) {
                if (active) {
                    setError(
                        error instanceof Error
                            ? error.message
                            : 'Unable to load users'
                    )
                }

            } finally {
                if (active) {
                    setListLoading(false)
                }
            }
        }

        void loadUsers()

        return () => {
            active = false
        }
    }, [authorized, roleFilter, appliedSearch, page])

    // Search submission.
    function handleSearch(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()

        setPage(0)
        setAppliedSearch(searchInput.trim())
        setActionMessage('')
    }

    // Clear search.
    function handleClearSearch() {
        setSearchInput('')
        setAppliedSearch('')
        setPage(0)
        setActionMessage('')
    }

    // Ask for confirmation before changing account status.
    function requestStatusChange(user: AdminManagedUser) {
        setPendingChange({
            user,
            enabled: !user.enabled,
        })

        setActionMessage('')
        setError('')
    }

    // Confirm and perform the account status update.
    async function confirmStatusChange() {
        if (!pendingChange) return

        const { user, enabled } = pendingChange

        setUpdatingId(user.id)
        setError('')
        setActionMessage('')

        try {
            const updatedUser = await updateAdminUserStatus(
                user.id,
                enabled
            )

            setUsers((previous) => {
                if (!previous) return previous

                return {
                    ...previous,
                    content: previous.content.map((item) =>
                        item.id === updatedUser.id
                            ? updatedUser
                            : item
                    ),
                }
            })

            setActionMessage(
                `${updatedUser.fullName}'s account has been ${
                    updatedUser.enabled ? 'enabled' : 'disabled'
                }.`
            )

            setPendingChange(null)

        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Unable to update account status'
            )

        } finally {
            setUpdatingId(null)
        }
    }

    // Logout.
    async function handleLogout() {
        setError('')

        try {
            await logout()
            navigate('/', { replace: true })

        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Logout failed'
            )
        }
    }

    if (authLoading) {
        return (
            <main className="flex min-h-screen items-center justify-center bg-slate-950 text-white">
                Verifying Admin access...
            </main>
        )
    }

    if (!authorized) return null

    return (
        <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-6 lg:px-8">

            <div className="mx-auto max-w-7xl">

                {/* Header */}
                <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-6">

                    <Link to="/" className="text-2xl font-bold">
                        Exam<span className="text-blue-400">Sphere</span>
                    </Link>

                    <div className="flex flex-wrap items-center gap-3">

                        <Link
                            to="/dashboard/admin"
                            className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
                        >
                            Back to Dashboard
                        </Link>

                        <button
                            onClick={handleLogout}
                            className="rounded-lg border border-red-500/40 px-4 py-2 text-sm text-red-400 hover:bg-red-500/10"
                        >
                            Logout
                        </button>

                    </div>

                </header>

                {/* Page title */}
                <section className="mt-10">

                    <p className="text-sm font-semibold uppercase tracking-widest text-blue-400">
                        Admin Portal
                    </p>

                    <h1 className="mt-3 text-3xl font-bold sm:text-4xl">
                        User Management
                    </h1>

                    <p className="mt-3 text-slate-400">
                        Welcome, {adminName}. Manage Student and Teacher accounts.
                    </p>

                </section>

                {/* Summary */}
                <section className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-3">

                    <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
                        <p className="text-sm text-slate-400">
                            Total managed accounts
                        </p>

                        <p className="mt-2 text-3xl font-bold text-blue-400">
                            {users?.totalElements ?? '—'}
                        </p>
                    </div>

                    <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
                        <p className="text-sm text-slate-400">
                            Current filter
                        </p>

                        <p className="mt-2 text-xl font-bold text-purple-400">
                            {roleFilter === 'ALL'
                                ? 'All Users'
                                : roleFilter === 'STUDENT'
                                    ? 'Students'
                                    : 'Teachers'}
                        </p>
                    </div>

                    <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
                        <p className="text-sm text-slate-400">
                            Accounts on this page
                        </p>

                        <p className="mt-2 text-3xl font-bold text-green-400">
                            {users?.content.length ?? '—'}
                        </p>
                    </div>

                </section>

                {/* Search and filters */}
                <section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-5">

                    <form
                        onSubmit={handleSearch}
                        className="flex flex-col gap-4 lg:flex-row"
                    >

                        <div className="flex-1">
                            <label
                                htmlFor="userSearch"
                                className="mb-2 block text-sm font-medium text-slate-300"
                            >
                                Search users
                            </label>

                            <input
                                id="userSearch"
                                type="search"
                                value={searchInput}
                                onChange={(event) =>
                                    setSearchInput(event.target.value)
                                }
                                placeholder="Search by name or email..."
                                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-blue-500"
                            />
                        </div>

                        <div className="lg:w-56">
                            <label
                                htmlFor="roleFilter"
                                className="mb-2 block text-sm font-medium text-slate-300"
                            >
                                Filter by role
                            </label>

                            <select
                                id="roleFilter"
                                value={roleFilter}
                                onChange={(event) => {
                                    setRoleFilter(event.target.value as RoleFilter)
                                    setPage(0)
                                    setActionMessage('')
                                }}
                                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-blue-500"
                            >
                                <option value="ALL">All Users</option>
                                <option value="STUDENT">Students</option>
                                <option value="TEACHER">Teachers</option>
                            </select>
                        </div>

                        <div className="flex items-end gap-2">

                            <button
                                type="submit"
                                className="rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500"
                            >
                                Search
                            </button>

                            <button
                                type="button"
                                onClick={handleClearSearch}
                                className="rounded-lg border border-slate-700 px-5 py-3 text-sm text-slate-300 hover:bg-slate-800"
                            >
                                Clear
                            </button>

                        </div>

                    </form>

                </section>

                {/* Error message */}
                {error && (
                    <p
                        role="alert"
                        className="mt-5 rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-400"
                    >
                        {error}
                    </p>
                )}

                {/* Action success message */}
                {actionMessage && (
                    <p
                        role="status"
                        className="mt-5 rounded-lg border border-green-500/30 bg-green-500/10 p-4 text-sm text-green-400"
                    >
                        {actionMessage}
                    </p>
                )}

                {/* User table */}
                <section className="mt-8 overflow-hidden rounded-2xl border border-slate-800 bg-slate-900">

                    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 p-5">

                        <h2 className="text-lg font-semibold">
                            Students and Teachers
                        </h2>

                        {listLoading && (
                            <span className="text-sm text-blue-400">
                                Loading users...
                            </span>
                        )}

                    </div>

                    {listLoading && !users ? (
                        <div className="p-10 text-center text-slate-400">
                            Loading user accounts...
                        </div>
                    ) : users && users.content.length > 0 ? (

                        <div className="overflow-x-auto">

                            <table className="w-full min-w-[760px] text-left text-sm">

                                <thead className="bg-slate-950 text-xs uppercase tracking-wider text-slate-400">

                                <tr>
                                    <th className="px-6 py-4">User</th>
                                    <th className="px-6 py-4">Email</th>
                                    <th className="px-6 py-4">Role</th>
                                    <th className="px-6 py-4">Joined</th>
                                    <th className="px-6 py-4">Status</th>
                                    <th className="px-6 py-4">Action</th>
                                </tr>

                                </thead>

                                <tbody className="divide-y divide-slate-800">

                                {users.content.map((managedUser) => (

                                    <tr
                                        key={managedUser.id}
                                        className="transition hover:bg-slate-800/40"
                                    >

                                        <td className="px-6 py-4 font-medium text-white">
                                            {managedUser.fullName}
                                            <p className="mt-1 text-xs text-slate-500">
                                                ID: {managedUser.id}
                                            </p>
                                        </td>

                                        <td className="px-6 py-4 text-slate-300">
                                            {managedUser.email}
                                        </td>

                                        <td className="px-6 py-4">

                                                <span
                                                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                                        managedUser.role === 'STUDENT'
                                                            ? 'bg-cyan-500/10 text-cyan-400'
                                                            : 'bg-purple-500/10 text-purple-400'
                                                    }`}
                                                >
                                                    {managedUser.role}
                                                </span>

                                        </td>

                                        <td className="px-6 py-4 text-slate-400">
                                            {new Date(
                                                managedUser.createdAt
                                            ).toLocaleDateString()}
                                        </td>

                                        <td className="px-6 py-4">

                                                <span
                                                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                                        managedUser.enabled
                                                            ? 'bg-green-500/10 text-green-400'
                                                            : 'bg-red-500/10 text-red-400'
                                                    }`}
                                                >
                                                    {managedUser.enabled
                                                        ? 'Active'
                                                        : 'Disabled'}
                                                </span>

                                        </td>

                                        <td className="px-6 py-4">

                                            <button
                                                onClick={() =>
                                                    requestStatusChange(managedUser)
                                                }
                                                disabled={updatingId === managedUser.id}
                                                className={`rounded-lg px-3 py-2 text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${
                                                    managedUser.enabled
                                                        ? 'border border-red-500/30 text-red-400 hover:bg-red-500/10'
                                                        : 'border border-green-500/30 text-green-400 hover:bg-green-500/10'
                                                }`}
                                            >
                                                {updatingId === managedUser.id
                                                    ? 'Updating...'
                                                    : managedUser.enabled
                                                        ? 'Disable'
                                                        : 'Enable'}
                                            </button>

                                        </td>

                                    </tr>

                                ))}

                                </tbody>

                            </table>

                        </div>

                    ) : !listLoading ? (

                        <div className="p-10 text-center">

                            <p className="text-lg font-semibold text-slate-300">
                                No users found
                            </p>

                            <p className="mt-2 text-sm text-slate-500">
                                Try another search term or change the role filter.
                            </p>

                        </div>

                    ) : null}

                    {/* Pagination */}
                    {users && (

                        <div className="flex flex-wrap items-center justify-between gap-4 border-t border-slate-800 p-5">

                            <p className="text-sm text-slate-400">
                                Showing {users.content.length} of {users.totalElements} accounts
                            </p>

                            <div className="flex items-center gap-3">

                                <button
                                    onClick={() => setPage((current) => current - 1)}
                                    disabled={users.first || listLoading}
                                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                    Previous
                                </button>

                                <span className="text-sm text-slate-400">
                                    Page {users.page + 1} of {Math.max(users.totalPages, 1)}
                                </span>

                                <button
                                    onClick={() => setPage((current) => current + 1)}
                                    disabled={users.last || listLoading}
                                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                    Next
                                </button>

                            </div>

                        </div>

                    )}

                </section>

            </div>

            {/* Account status confirmation */}
            {pendingChange && (

                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">

                    <div
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="statusDialogTitle"
                        className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl"
                    >

                        <h2
                            id="statusDialogTitle"
                            className="text-xl font-bold"
                        >
                            Confirm account status change
                        </h2>

                        <p className="mt-4 text-sm leading-6 text-slate-300">
                            Are you sure you want to{' '}
                            <strong>
                                {pendingChange.enabled ? 'enable' : 'disable'}
                            </strong>{' '}
                            the account belonging to{' '}
                            <strong>{pendingChange.user.fullName}</strong>?
                        </p>

                        {!pendingChange.enabled && (
                            <p className="mt-3 text-sm text-amber-400">
                                The user will be prevented from logging in while the account is disabled.
                            </p>
                        )}

                        <div className="mt-7 flex justify-end gap-3">

                            <button
                                onClick={() => setPendingChange(null)}
                                disabled={updatingId !== null}
                                className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
                            >
                                Cancel
                            </button>

                            <button
                                onClick={() => void confirmStatusChange()}
                                disabled={updatingId !== null}
                                className={`rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-50 ${
                                    pendingChange.enabled
                                        ? 'bg-green-600 hover:bg-green-500'
                                        : 'bg-red-600 hover:bg-red-500'
                                }`}
                            >
                                {updatingId !== null
                                    ? 'Updating...'
                                    : pendingChange.enabled
                                        ? 'Confirm Enable'
                                        : 'Confirm Disable'}
                            </button>

                        </div>

                    </div>

                </div>

            )}

        </main>
    )
}
