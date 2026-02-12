import { useState, useEffect } from 'react';
import { MessageSquare, Briefcase, User, TrendingUp, AlertTriangle } from 'lucide-react';
import ChatComponent from './components/ChatComponent';
import PortfolioView from './components/PortfolioView';
import UserProfileForm from './components/UserProfileForm';
import RecommendationCard from './components/RecommendationCard';
import { advisorAPI } from './services/api';

function App() {
  const [activeTab, setActiveTab] = useState('chat');
  const [userId] = useState(() => {
    // Get or create user ID from localStorage
    let id = localStorage.getItem('userId');
    if (!id) {
      id = `user-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      localStorage.setItem('userId', id);
    }
    return id;
  });
  const [recommendations, setRecommendations] = useState([]);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);

  useEffect(() => {
    loadRecommendations();
  }, [userId]);

  const loadRecommendations = async () => {
    try {
      setLoadingRecommendations(true);
      const response = await advisorAPI.getRecommendations(userId);
      setRecommendations(response.data || []);
    } catch (error) {
      console.error('Error loading recommendations:', error);
    } finally {
      setLoadingRecommendations(false);
    }
  };

  const tabs = [
    { id: 'chat', label: 'Chat', icon: MessageSquare },
    { id: 'portfolio', label: 'Portfolio', icon: Briefcase },
    { id: 'recommendations', label: 'Recommendations', icon: TrendingUp },
    { id: 'profile', label: 'Profile', icon: User },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <header className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-primary-600 rounded-lg">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">
                Agentic Financial Advisor
              </h1>
            </div>
            <div className="text-sm text-gray-500 dark:text-gray-400">
              User: {userId.substring(0, 20)}...
            </div>
          </div>
        </div>
      </header>

      {/* Disclaimer */}
      <div className="bg-yellow-50 dark:bg-yellow-900/20 border-b border-yellow-200 dark:border-yellow-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
          <div className="flex items-start gap-2">
            <AlertTriangle className="w-5 h-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-yellow-800 dark:text-yellow-200">
              <strong>Disclaimer:</strong> This is a demonstration system for educational purposes only. 
              Not a licensed financial advisor. Consult a professional for actual investment decisions. 
              Past performance does not guarantee future results. Investments carry risk of loss.
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium text-sm transition-colors ${
                    activeTab === tab.id
                      ? 'border-primary-600 text-primary-600 dark:text-primary-400'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {tab.label}
                </button>
              );
            })}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="h-[calc(100vh-280px)] min-h-[600px]">
          {activeTab === 'chat' && (
            <div className="card h-full p-0">
              <ChatComponent userId={userId} />
            </div>
          )}

          {activeTab === 'portfolio' && (
            <PortfolioView userId={userId} />
          )}

          {activeTab === 'recommendations' && (
            <div>
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold">Investment Recommendations</h2>
                <button
                  onClick={loadRecommendations}
                  disabled={loadingRecommendations}
                  className="btn-secondary"
                >
                  {loadingRecommendations ? 'Loading...' : 'Refresh'}
                </button>
              </div>

              {loadingRecommendations ? (
                <div className="flex items-center justify-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                </div>
              ) : recommendations.length === 0 ? (
                <div className="card text-center py-12">
                  <p className="text-gray-500 dark:text-gray-400">
                    No recommendations yet. Start a chat to get personalized investment advice!
                  </p>
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
              userId={userId}
              onSave={() => {
                // Optionally reload recommendations after profile update
                loadRecommendations();
              }}
            />
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <p className="text-center text-sm text-gray-500 dark:text-gray-400">
            Agentic Financial Advisor - Powered by Ollama & LangChain4j
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;

