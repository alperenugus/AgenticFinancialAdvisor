import { TrendingUp, TrendingDown, Minus, AlertCircle, Calendar, Target, Sparkles, BarChart3 } from 'lucide-react';

const RecommendationCard = ({ recommendation }) => {
  if (!recommendation) return null;

  const getActionIcon = () => {
    switch (recommendation.action) {
      case 'BUY':
        return <TrendingUp className="w-6 h-6 text-success-600 dark:text-success-400" />;
      case 'SELL':
        return <TrendingDown className="w-6 h-6 text-danger-600 dark:text-danger-400" />;
      case 'HOLD':
        return <Minus className="w-6 h-6 text-yellow-600 dark:text-yellow-400" />;
      default:
        return null;
    }
  };

  const getActionColor = () => {
    switch (recommendation.action) {
      case 'BUY':
        return 'bg-gradient-to-br from-success-50 to-success-100 dark:from-success-900/50 dark:to-success-800/50 text-success-700 dark:text-success-300 border-success-200 dark:border-success-800';
      case 'SELL':
        return 'bg-gradient-to-br from-danger-50 to-danger-100 dark:from-danger-900/50 dark:to-danger-800/50 text-danger-700 dark:text-danger-300 border-danger-200 dark:border-danger-800';
      case 'HOLD':
        return 'bg-gradient-to-br from-yellow-50 to-yellow-100 dark:from-yellow-900/50 dark:to-yellow-800/50 text-yellow-700 dark:text-yellow-300 border-yellow-200 dark:border-yellow-800';
      default:
        return 'bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800 text-gray-700 dark:text-gray-300 border-gray-200 dark:border-gray-600';
    }
  };

  const getRiskColor = () => {
    switch (recommendation.riskLevel) {
      case 'LOW':
        return 'badge-success';
      case 'MEDIUM':
        return 'badge-warning';
      case 'HIGH':
        return 'badge-danger';
      default:
        return 'badge-primary';
    }
  };

  const confidence = (recommendation.confidence || 0) * 100;
  const targetPrice = recommendation.targetPrice ? parseFloat(recommendation.targetPrice) : null;

  return (
    <div className="card-elevated hover:shadow-large transition-all duration-300 group">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div className="flex items-center gap-4">
          <div className={`p-3 rounded-xl border-2 ${getActionColor()}`}>
            {getActionIcon()}
          </div>
          <div>
            <h3 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">{recommendation.symbol}</h3>
            <span className={`badge ${getActionColor().includes('success') ? 'badge-success' : getActionColor().includes('danger') ? 'badge-danger' : 'badge-warning'}`}>
              {recommendation.action}
            </span>
          </div>
        </div>
        <div className="text-right">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-4 h-4 text-primary-600 dark:text-primary-400" />
            <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Confidence</span>
          </div>
          <div className="text-2xl font-bold text-gray-900 dark:text-white">{confidence.toFixed(0)}%</div>
          <div className="w-24 h-2 bg-gray-200 dark:bg-gray-700 rounded-full mt-2 overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-primary-500 to-primary-600 rounded-full transition-all duration-500"
              style={{ width: `${confidence}%` }}
            ></div>
          </div>
        </div>
      </div>

      {/* Reasoning */}
      {recommendation.reasoning && (
        <div className="mb-6 p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800 rounded-xl border border-gray-200 dark:border-gray-600">
          <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-wrap font-medium">
            {recommendation.reasoning}
          </p>
        </div>
      )}

      {/* Key Metrics Grid */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        {recommendation.riskLevel && (
          <div className="p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800 rounded-xl border border-gray-200 dark:border-gray-600">
            <div className="flex items-center gap-2 mb-2">
              <AlertCircle className="w-4 h-4 text-gray-500 dark:text-gray-400" />
              <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Risk Level</span>
            </div>
            <div className={`font-bold text-lg ${getRiskColor().replace('badge-', 'text-').replace('-', '-')}`}>
              {recommendation.riskLevel}
            </div>
          </div>
        )}

        {recommendation.timeHorizon && (
          <div className="p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-700 dark:to-gray-800 rounded-xl border border-gray-200 dark:border-gray-600">
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="w-4 h-4 text-gray-500 dark:text-gray-400" />
              <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Time Horizon</span>
            </div>
            <div className="font-bold text-lg text-gray-900 dark:text-white">{recommendation.timeHorizon}</div>
          </div>
        )}

        {targetPrice && (
          <div className="col-span-2 p-4 bg-gradient-to-br from-primary-50 to-primary-100 dark:from-primary-900/30 dark:to-primary-800/30 rounded-xl border border-primary-200 dark:border-primary-800">
            <div className="flex items-center gap-2 mb-2">
              <Target className="w-4 h-4 text-primary-600 dark:text-primary-400" />
              <span className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-wide">Target Price</span>
            </div>
            <div className="font-bold text-2xl text-primary-700 dark:text-primary-300">${targetPrice.toFixed(2)}</div>
          </div>
        )}
      </div>

      {/* Detailed Analysis */}
      {(recommendation.marketAnalysis || recommendation.riskAssessment || recommendation.researchSummary) && (
        <div className="border-t border-gray-200 dark:border-gray-700 pt-6 mt-6 space-y-4">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="w-5 h-5 text-primary-600 dark:text-primary-400" />
            <h4 className="font-bold text-gray-900 dark:text-white">Detailed Analysis</h4>
          </div>
          
          {recommendation.marketAnalysis && (
            <div className="p-4 bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl border border-blue-200 dark:border-blue-800">
              <div className="text-sm font-bold text-blue-700 dark:text-blue-300 mb-2">
                Market Analysis
              </div>
              <p className="text-sm text-blue-600 dark:text-blue-400 leading-relaxed">
                {recommendation.marketAnalysis}
              </p>
            </div>
          )}

          {recommendation.riskAssessment && (
            <div className="p-4 bg-gradient-to-br from-amber-50 to-yellow-50 dark:from-amber-900/20 dark:to-yellow-900/20 rounded-xl border border-amber-200 dark:border-amber-800">
              <div className="text-sm font-bold text-amber-700 dark:text-amber-300 mb-2">
                Risk Assessment
              </div>
              <p className="text-sm text-amber-600 dark:text-amber-400 leading-relaxed">
                {recommendation.riskAssessment}
              </p>
            </div>
          )}

          {recommendation.researchSummary && (
            <div className="p-4 bg-gradient-to-br from-purple-50 to-pink-50 dark:from-purple-900/20 dark:to-pink-900/20 rounded-xl border border-purple-200 dark:border-purple-800">
              <div className="text-sm font-bold text-purple-700 dark:text-purple-300 mb-2">
                Research Summary
              </div>
              <p className="text-sm text-purple-600 dark:text-purple-400 leading-relaxed">
                {recommendation.researchSummary}
              </p>
            </div>
          )}
        </div>
      )}

      {/* Footer */}
      {recommendation.createdAt && (
        <div className="text-xs text-gray-500 dark:text-gray-400 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <span>Created: {new Date(recommendation.createdAt).toLocaleString()}</span>
          <span className="flex items-center gap-1">
            <div className="w-2 h-2 bg-success-500 rounded-full animate-pulse"></div>
            Active
          </span>
        </div>
      )}
    </div>
  );
};

export default RecommendationCard;
