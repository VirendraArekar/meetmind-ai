import { AppProviders } from './app/providers';
import { ErrorBoundary } from './components/common/ErrorBoundary';
import { ToastProvider } from './components/common/ToastContext';
import { AppRouter } from './routes/AppRouter';

export function App() {
  return (
    <ErrorBoundary>
      <AppProviders>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </AppProviders>
    </ErrorBoundary>
  );
}

export default App;
