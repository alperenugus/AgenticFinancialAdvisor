import { useState } from "react";
import { LineChart, Mail, Lock, User as UserIcon, Loader2, AlertCircle } from "lucide-react";
import { useAuth } from "../contexts/useAuth";

const LoginPage = () => {
  const { loginWithGoogle, loginWithPassword, register } = useAuth();

  const [mode, setMode] = useState("login"); // 'login' | 'register'
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isRegister = mode === "register";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (isRegister && name.trim().length === 0) {
      setError("Please enter your name.");
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      setError("Please enter a valid email address.");
      return;
    }
    if (isRegister && password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setSubmitting(true);
    try {
      if (isRegister) {
        await register(name.trim(), email.trim(), password);
      } else {
        await loginWithPassword(email.trim(), password);
      }
      // On success, AuthContext flips isAuthenticated and App re-renders the app shell.
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        (isRegister ? "Could not create your account." : "Invalid email or password.");
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const switchMode = (next) => {
    setMode(next);
    setError("");
    setPassword("");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full">
        {/* Logo and Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center mb-6">
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-primary-700 rounded-3xl blur-xl opacity-50"></div>
              <div className="relative p-4 bg-gradient-to-br from-primary-600 to-primary-700 rounded-3xl shadow-2xl">
                <LineChart className="w-12 h-12 text-white" />
              </div>
            </div>
          </div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent mb-2">
            Financial Advisor AI
          </h1>
          <p className="text-gray-600 font-medium">
            Sign in to access your personalized investment advisor
          </p>
        </div>

        {/* Auth Card */}
        <div className="card-elevated p-8">
          {/* Tabs */}
          <div className="flex bg-gray-100 rounded-xl p-1 mb-6">
            <button
              type="button"
              onClick={() => switchMode("login")}
              className={`flex-1 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                !isRegister ? "bg-white text-primary-700 shadow-sm" : "text-gray-500 hover:text-gray-700"
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => switchMode("register")}
              className={`flex-1 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                isRegister ? "bg-white text-primary-700 shadow-sm" : "text-gray-500 hover:text-gray-700"
              }`}
            >
              Create Account
            </button>
          </div>

          {error && (
            <div className="mb-4 flex items-start gap-2 rounded-lg bg-red-50 border border-red-200 p-3 text-sm text-red-700">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <div className="relative">
                  <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    id="name" type="text" value={name} onChange={(e) => setName(e.target.value)}
                    autoComplete="name" placeholder="Jane Investor"
                    className="w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  />
                </div>
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email" placeholder="you@example.com"
                  className="w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                  autoComplete={isRegister ? "new-password" : "current-password"}
                  placeholder={isRegister ? "At least 8 characters" : "Your password"}
                  className="w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
              </div>
            </div>

            <button
              type="submit" disabled={submitting}
              className="w-full btn-primary flex items-center justify-center gap-2 py-3 disabled:opacity-60"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>{isRegister ? "Create Account" : "Sign In"}</span>
            </button>
          </form>

          {/* Divider */}
          <div className="flex items-center gap-3 my-6">
            <div className="flex-1 h-px bg-gray-200"></div>
            <span className="text-xs text-gray-400 font-medium">OR</span>
            <div className="flex-1 h-px bg-gray-200"></div>
          </div>

          <button
            onClick={loginWithGoogle}
            className="w-full btn-secondary flex items-center justify-center gap-3 py-3"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            <span>Continue with Google</span>
          </button>
        </div>

        {/* Footer */}
        <p className="text-center text-sm text-gray-500 mt-6">
          By continuing, you agree to our Terms of Service and Privacy Policy
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
