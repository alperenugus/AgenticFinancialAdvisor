import { useState, useEffect } from 'react';
import { MessageSquare, Briefcase, User, TrendingUp, AlertTriangle, Sparkles, LogOut } from 'lucide-react';
import { useAuth } from './contexts/AuthContext';
import LoginPage from './components/LoginPage';
import ChatComponent from './components/ChatComponent';
import PortfolioView from './components/PortfolioView';
import UserProfileForm from './components/UserProfileForm';
import RecommendationCard from './components/RecommendationCard';
import { advisorAPI } from './services/api';

function App() {
  const { user, loading, isAuthenticated, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('chat');
  const [recommendations, setRecommendations] = useState([]);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      loadRecommendations();
    }
  }, [isAuthenticated]);

  const loadRecommendations = async () => {
    try {
      setLoadingRecommendations(true);
      const response = await advisorAPI.getRecommendations();
      setRecommendations(response.data || []);
    } catch (error) {
      console.error('Error loading recommendations:', error);
    } finally {
      setLoadingRecommendations(false);
    }
  };

  // Show login page if not authenticated
  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-slate-50 dark:from-slate-900 dark:via-slate-900 dark:to-slate-800 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400 font-medium">Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const tabs = [
    { id: 'chat', label: 'AI Advisor', icon: MessageSquare, description: 'Get personalized advice' },
    { id: 'portfolio', label: 'Portfolio', icon: Briefcase, description: 'Manage investments' },
    { id: 'recommendations', label: 'Recommendations', icon: TrendingUp, description: 'View insights' },
    { id: 'profile', label: 'Profile', icon: User, description: 'Your preferences' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-slate-50 dark:from-slate-900 dark:via-slate-900 dark:to-slate-800">
      {/* Professional Header */}
      <header className="bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl border-b border-gray-200/50 dark:border-gray-700/50 sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-20">
            <div className="flex items-center gap-4">
              <div className="relative">
                <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-primary-700 rounded-2xl blur opacity-75"></div>
                <div className="relative p-3 bg-gradient-to-br from-primary-600 to-primary-700 rounded-2xl shadow-lg">
                  <Sparkles className="w-7 h-7 text-white" />
                </div>
              </div>
              <div>
                <h1 className="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
                  Financial Advisor AI
                </h1>
                <p className="text-xs text-gray-500 dark:text-gray-400 font-medium">Powered by Groq</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              {user?.pictureUrl && (
                <img 
                  src={user.pictureUrl} 
                  alt={user.name}
                  className="w-10 h-10 rounded-full border-2 border-primary-200 dark:border-primary-800"
                />
              )}
              <div className="hidden md:flex items-center gap-3 px-4 py-2 bg-gray-100 dark:bg-gray-800 rounded-xl">
                <div className="w-2 h-2 bg-success-500 rounded-full animate-pulse"></div>
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {user?.name || user?.email}
                </span>
              </div>
              <button
                onClick={logout}
                className="btn-ghost flex items-center gap-2 text-sm"
                title="Sign out"
              >
                <LogOut className="w-4 h-4" />
                <span className="hidden md:inline">Sign Out</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Professional Disclaimer Banner */}
      <div className="bg-gradient-to-r from-amber-50 via-yellow-50 to-amber-50 dark:from-amber-900/20 dark:via-yellow-900/20 dark:to-amber-900/20 border-b border-amber-200/50 dark:border-amber-800/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-amber-800 dark:text-amber-200 leading-relaxed">
              <strong className="font-semibold">Disclaimer:</strong> This is a demonstration system for educational purposes only. 
              Not a licensed financial advisor. Consult a professional for actual investment decisions. 
              Past performance does not guarantee future results. Investments carry risk of loss.
            </div>
          </div>
        </div>
      </div>

      {/* Modern Navigation Tabs */}
      <nav className="bg-white/60 dark:bg-gray-900/60 backdrop-blur-sm border-b border-gray-200/50 dark:border-gray-700/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-1 overflow-x-auto">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`group relative flex items-center gap-3 px-6 py-4 border-b-2 font-semibold text-sm transition-all duration-200 min-w-fit ${
                    isActive
                      ? 'border-primary-600 text-primary-600 dark:text-primary-400'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
                  }`}
                >
                  <Icon className={`w-5 h-5 transition-transform duration-200 ${isActive ? 'scale-110' : ''}`} />
                  <span>{tab.label}</span>
                  {isActive && (
                    <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary-600 to-primary-400 rounded-full"></div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="min-h-[calc(100vh-280px)]">
          {activeTab === 'chat' && (
            <div className="card-elevated h-[calc(100vh-300px)] min-h-[600px] p-0 overflow-hidden">
              <ChatComponent />
            </div>
          )}

          {activeTab === 'portfolio' && (
            <PortfolioView />
          )}

          {activeTab === 'recommendations' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Investment Recommendations</h2>
                  <p className="text-gray-600 dark:text-gray-400">AI-powered insights tailored to your profile</p>
                </div>
                <button
                  onClick={loadRecommendations}
                  disabled={loadingRecommendations}
                  className="btn-secondary flex items-center gap-2"
                >
                  {loadingRecommendations ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-700"></div>
                      Loading...
                    </>
                  ) : (
                    <>
                      <TrendingUp className="w-4 h-4" />
                      Refresh
                    </>
                  )}
                </button>
              </div>

              {loadingRecommendations ? (
                <div className="flex items-center justify-center h-64">
                  <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-600 mx-auto mb-4"></div>
                    <p className="text-gray-600 dark:text-gray-400 font-medium">Loading recommendations...</p>
                  </div>
                </div>
              ) : recommendations.length === 0 ? (
                <div className="card text-center py-16">
                  <div className="w-20 h-20 bg-gradient-to-br from-primary-100 to-primary-200 dark:from-primary-900 dark:to-primary-800 rounded-2xl flex items-center justify-center mx-auto mb-6">
                    <TrendingUp className="w-10 h-10 text-primary-600 dark:text-primary-400" />
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">No recommendations yet</h3>
                  <p className="text-gray-600 dark:text-gray-400 mb-6 max-w-md mx-auto">
                    Start a conversation with your AI advisor to get personalized investment recommendations based on your profile and goals.
                  </p>
                  <button
                    onClick={() => setActiveTab('chat')}
                    className="btn-primary inline-flex items-center gap-2"
                  >
                    <MessageSquare className="w-4 h-4" />
                    Start Chat
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {recommendations.map((rec) => (
                    <RecommendationCard key={rec.id} recommendation={rec} />
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'profile' && (
            <UserProfileForm
              onSave={() => {
                loadRecommendations();
              }}
            />
          )}
        </div>
      </main>

      {/* Professional Footer */}
      <footer className="bg-white/60 dark:bg-gray-900/60 backdrop-blur-sm border-t border-gray-200/50 dark:border-gray-700/50 mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">
              Financial Advisor AI - Powered by <span className="text-primary-600 dark:text-primary-400 font-semibold">Groq</span> & LangChain4j
            </p>
            <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-500">
              <div className="w-2 h-2 bg-success-500 rounded-full"></div>
              <span>System Operational</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
