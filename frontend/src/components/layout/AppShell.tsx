import { useState, type FormEvent, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  Brain,
  LayoutDashboard,
  Mic,
  FileAudio,
  CheckSquare,
  MessageSquare,
  BarChart3,
  Users,
  Settings,
  Sun,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  LogOut,
  User as UserIcon,
  Menu,
  X,
} from 'lucide-react';
import type { RootState } from '../../app/store';
import { toggleSidebar, toggleTheme } from '../../app/uiSlice';
import { clearCredentials } from '../../app/authSlice';
import { authApi } from '../../lib/api';

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/meetings', label: 'Meetings', icon: FileAudio },
  { to: '/record', label: 'Live Recording', icon: Mic },
  { to: '/action-items', label: 'Action Items', icon: CheckSquare },
  { to: '/chat', label: 'AI Chat', icon: MessageSquare },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/team', label: 'Team', icon: Users },
  { to: '/settings', label: 'Settings', icon: Settings },
];

export function AppShell({ children }: { children: ReactNode }) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { theme, sidebarOpen } = useSelector((state: RootState) => state.ui);
  const { user, refreshToken } = useSelector((state: RootState) => state.auth);
  const [searchQuery, setSearchQuery] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/meetings?q=${encodeURIComponent(searchQuery.trim())}`);
      setMobileMenuOpen(false);
    }
  };

  const handleLogout = async () => {
    try {
      await authApi.logout(refreshToken);
    } catch {
      // ignore network errors during logout
    }
    dispatch(clearCredentials());
    navigate('/login');
  };

  return (
    <div className="flex min-h-screen overflow-x-hidden bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-xs md:hidden"
          onClick={() => setMobileMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        aria-label="Primary navigation"
        className={`fixed inset-y-0 left-0 z-40 flex flex-col border-r border-slate-200 bg-white transition-all duration-200 dark:border-slate-800 dark:bg-slate-900 md:static ${
          mobileMenuOpen ? 'translate-x-0 w-64' : '-translate-x-full md:translate-x-0'
        } ${sidebarOpen ? 'md:w-60' : 'md:w-16'}`}
      >
        <div className="flex h-14 items-center justify-between border-b border-slate-200 px-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-600 text-white">
              <Brain className="h-4 w-4" />
            </div>
            {(sidebarOpen || mobileMenuOpen) && (
              <span className="truncate text-sm font-semibold tracking-tight">MeetMind AI</span>
            )}
          </div>
          <button
            type="button"
            onClick={() => dispatch(toggleSidebar())}
            aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
            className="hidden rounded-md p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100 md:inline-flex"
          >
            {sidebarOpen ? <PanelLeftClose className="h-4 w-4" /> : <PanelLeftOpen className="h-4 w-4" />}
          </button>
          <button
            type="button"
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Close navigation menu"
            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 md:hidden"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-2">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800/70 dark:hover:text-slate-100'
                }`
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              {(sidebarOpen || mobileMenuOpen) && <span className="truncate">{label}</span>}
            </NavLink>
          ))}
        </nav>

        {(sidebarOpen || mobileMenuOpen) && user && (
          <div className="border-t border-slate-200 p-3 dark:border-slate-800">
            <NavLink
              to="/profile"
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center gap-2.5 rounded-lg p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300">
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs font-medium">{user.fullName}</p>
                <p className="truncate text-[11px] text-slate-500 dark:text-slate-400">
                  {user.defaultOrganizationName || user.email}
                </p>
              </div>
            </NavLink>
          </div>
        )}
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between gap-2 border-b border-slate-200 bg-white px-3 sm:gap-4 sm:px-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex min-w-0 flex-1 items-center gap-2">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              aria-label="Open navigation menu"
              className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-300 md:hidden"
            >
              <Menu className="h-4 w-4" />
            </button>

            <form onSubmit={handleSearch} className="relative max-w-md flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search meetings..."
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-1.5 pl-9 pr-3 text-sm outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-800"
              />
            </form>
          </div>

          <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
            <button
              type="button"
              onClick={() => dispatch(toggleTheme())}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 sm:px-3 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
            >
              {theme === 'dark' ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
              <span className="hidden sm:inline">{theme === 'dark' ? 'Light' : 'Dark'}</span>
            </button>
            <NavLink
              to="/profile"
              aria-label="User profile"
              className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              <UserIcon className="h-4 w-4" />
            </NavLink>
            <button
              type="button"
              onClick={handleLogout}
              aria-label="Log out"
              className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-rose-50 hover:text-rose-600 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-rose-950/40"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
