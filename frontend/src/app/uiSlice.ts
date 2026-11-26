import { createSlice } from '@reduxjs/toolkit';

export type ThemeMode = 'light' | 'dark';

interface UiState {
  theme: ThemeMode;
  sidebarOpen: boolean;
}

const getInitialTheme = (): ThemeMode => {
  if (typeof window !== 'undefined') {
    const saved = window.localStorage.getItem('meetmind-theme');
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
  }
  return 'dark';
};

const initialState: UiState = {
  theme: getInitialTheme(),
  sidebarOpen: true,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleTheme(state) {
      state.theme = state.theme === 'dark' ? 'light' : 'dark';
      if (typeof window !== 'undefined') {
        window.localStorage.setItem('meetmind-theme', state.theme);
      }
    },
    toggleSidebar(state) {
      state.sidebarOpen = !state.sidebarOpen;
    },
  },
});

export const { toggleTheme, toggleSidebar } = uiSlice.actions;
export default uiSlice.reducer;
