import { TrendingUp, TrendingDown, Minus, AlertCircle, Calendar, Target } from 'lucide-react';

const RecommendationCard = ({ recommendation }) => {
  if (!recommendation) return null;

  const getActionIcon = () => {
    switch (recommendation.action) {
      case 'BUY':
        return <TrendingUp className="w-5 h-5 text-green-600" />;
      case 'SELL':
        return <TrendingDown className="w-5 h-5 text-red-600" />;
      case 'HOLD':
        return <Minus className="w-5 h-5 text-yellow-600" />;
      default:
        return null;
    }
  };

  const getActionColor = () => {
    switch (recommendation.action) {
      case 'BUY':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'SELL':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case 'HOLD':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200';
    }
  };

  const getRiskColor = () => {
    switch (recommendation.riskLevel) {
      case 'LOW':
        return 'text-green-600 dark:text-green-400';
      case 'MEDIUM':
        return 'text-yellow-600 dark:text-yellow-400';
      case 'HIGH':
        return 'text-red-600 dark:text-red-400';
      default:
        return 'text-gray-600 dark:text-gray-400';
    }
  };

  const confidence = (recommendation.confidence || 0) * 100;
  const targetPrice = recommendation.targetPrice ? parseFloat(recommendation.targetPrice) : null;

  return (
    <div className="card hover:shadow-lg transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-lg ${getActionColor()}`}>
            {getActionIcon()}
          </div>
          <div>
            <h3 className="text-xl font-bold text-gray-900 dark:text-white">{recommendation.symbol}</h3>
            <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getActionColor()}`}>
              {recommendation.action}
            </span>
          </div>
        </div>
        <div className="text-right">
          <div className="text-sm text-gray-500 dark:text-gray-400">Confidence</div>
          <div className="text-lg font-semibold text-gray-900 dark:text-white">{confidence.toFixed(0)}%</div>
        </div>
      </div>

      {recommendation.reasoning && (
        <div className="mb-4">
          <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
            {recommendation.reasoning}
          </p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 mb-4">
        {recommendation.riskLevel && (
          <div>
            <div className="text-sm text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1">
              <AlertCircle className="w-4 h-4" />
              Risk Level
            </div>
            <div className={`font-semibold ${getRiskColor()} dark:text-${getRiskColor().replace('text-', '')}`}>
              {recommendation.riskLevel}
            </div>
          </div>
        )}

        {recommendation.timeHorizon && (
          <div>
            <div className="text-sm text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1">
              <Calendar className="w-4 h-4" />
              Time Horizon
            </div>
            <div className="font-semibold text-gray-900 dark:text-white">{recommendation.timeHorizon}</div>
          </div>
        )}

        {targetPrice && (
          <div className="col-span-2">
            <div className="text-sm text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1">
              <Target className="w-4 h-4" />
              Target Price
            </div>
            <div className="font-semibold text-lg text-gray-900 dark:text-white">${targetPrice.toFixed(2)}</div>
          </div>
        )}
      </div>

      {(recommendation.marketAnalysis || recommendation.riskAssessment || recommendation.researchSummary) && (
        <div className="border-t border-gray-200 dark:border-gray-700 pt-4 mt-4 space-y-3">
          {recommendation.marketAnalysis && (
            <div>
              <div className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Market Analysis
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {recommendation.marketAnalysis}
              </p>
            </div>
          )}

          {recommendation.riskAssessment && (
            <div>
              <div className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Risk Assessment
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {recommendation.riskAssessment}
              </p>
            </div>
          )}

          {recommendation.researchSummary && (
            <div>
              <div className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Research Summary
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {recommendation.researchSummary}
              </p>
            </div>
          )}
        </div>
      )}

      {recommendation.createdAt && (
        <div className="text-xs text-gray-500 dark:text-gray-400 mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          Created: {new Date(recommendation.createdAt).toLocaleString()}
        </div>
      )}
    </div>
  );
};

export default RecommendationCard;

