import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  jobTitle?: string;
  avatarUrl?: string;
  emailVerified: boolean;
  roles: string[];
  defaultOrganizationId?: string;
  defaultOrganizationName?: string;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
}

const STORAGE_KEY = 'meetmind-auth';

function loadInitialAuth(): AuthState {
  if (typeof window === 'undefined') {
    return { accessToken: null, refreshToken: null, user: null };
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw) {
      return JSON.parse(raw) as AuthState;
    }
  } catch {
    // ignore corrupted storage
  }
  return { accessToken: null, refreshToken: null, user: null };
}

const authSlice = createSlice({
  name: 'auth',
  initialState: loadInitialAuth(),
  reducers: {
    setCredentials(state, action: PayloadAction<AuthState>) {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.user = action.payload.user;
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(action.payload));
      }
    },
    updateUser(state, action: PayloadAction<UserSummary>) {
      state.user = action.payload;
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({
            accessToken: state.accessToken,
            refreshToken: state.refreshToken,
            user: action.payload,
          })
        );
      }
    },
    clearCredentials(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      if (typeof window !== 'undefined') {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    },
  },
});

export const { setCredentials, updateUser, clearCredentials } = authSlice.actions;
export default authSlice.reducer;
