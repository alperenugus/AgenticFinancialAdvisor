import { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, RefreshCw, Plus, Trash2 } from 'lucide-react';
import { portfolioAPI } from '../services/api';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

const COLORS = ['#0ea5e9', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4'];

const PortfolioView = ({ userId }) => {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    symbol: '',
    quantity: '',
    averagePrice: '',
  });

  useEffect(() => {
    loadPortfolio();
  }, [userId]);

  const loadPortfolio = async () => {
    try {
      setLoading(true);
      const response = await portfolioAPI.get(userId);
      setPortfolio(response.data);
    } catch (error) {
      console.error('Error loading portfolio:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await portfolioAPI.refresh(userId);
      await loadPortfolio();
    } catch (error) {
      console.error('Error refreshing portfolio:', error);
    } finally {
      setRefreshing(false);
    }
  };

  const handleAddHolding = async (e) => {
    e.preventDefault();
    try {
      await portfolioAPI.addHolding(userId, {
        symbol: formData.symbol.toUpperCase(),
        quantity: parseInt(formData.quantity),
        averagePrice: parseFloat(formData.averagePrice),
      });
      setFormData({ symbol: '', quantity: '', averagePrice: '' });
      setShowAddForm(false);
      await loadPortfolio();
    } catch (error) {
      console.error('Error adding holding:', error);
      alert(error.response?.data?.message || 'Failed to add holding');
    }
  };

  const handleRemoveHolding = async (holdingId) => {
    if (!window.confirm('Are you sure you want to remove this holding?')) return;
    
    try {
      await portfolioAPI.removeHolding(userId, holdingId);
      await loadPortfolio();
    } catch (error) {
      console.error('Error removing holding:', error);
      alert('Failed to remove holding');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!portfolio || !portfolio.holdings || portfolio.holdings.length === 0) {
    return (
      <div className="card">
        <div className="text-center py-12">
          <p className="text-gray-500 dark:text-gray-400 mb-4">No holdings in your portfolio</p>
          <button
            onClick={() => setShowAddForm(true)}
            className="btn-primary inline-flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Add Your First Holding
          </button>
        </div>

        {showAddForm && (
          <form onSubmit={handleAddHolding} className="mt-6 space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Stock Symbol</label>
              <input
                type="text"
                value={formData.symbol}
                onChange={(e) => setFormData({ ...formData, symbol: e.target.value })}
                className="input-field"
                placeholder="e.g., AAPL"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Quantity</label>
              <input
                type="number"
                value={formData.quantity}
                onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                className="input-field"
                placeholder="e.g., 10"
                required
                min="1"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Average Price</label>
              <input
                type="number"
                step="0.01"
                value={formData.averagePrice}
                onChange={(e) => setFormData({ ...formData, averagePrice: e.target.value })}
                className="input-field"
                placeholder="e.g., 150.00"
                required
                min="0"
              />
            </div>
            <div className="flex gap-2">
              <button type="submit" className="btn-primary flex-1">
                Add Holding
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>
    );
  }

  const pieData = portfolio.holdings.map((holding) => ({
    name: holding.symbol,
    value: parseFloat(holding.value || 0),
  }));

  const barData = portfolio.holdings.map((holding) => ({
    symbol: holding.symbol,
    gainLoss: parseFloat(holding.gainLoss || 0),
    gainLossPercent: parseFloat(holding.gainLossPercent || 0),
  }));

  const totalValue = parseFloat(portfolio.totalValue || 0);
  const totalGainLoss = parseFloat(portfolio.totalGainLoss || 0);
  const totalGainLossPercent = parseFloat(portfolio.totalGainLossPercent || 0);

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card">
          <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">Total Value</div>
          <div className="text-2xl font-bold text-gray-900 dark:text-white">${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">Total Gain/Loss</div>
          <div className={`text-2xl font-bold flex items-center gap-2 ${
            totalGainLoss >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
          }`}>
            {totalGainLoss >= 0 ? <TrendingUp className="w-6 h-6" /> : <TrendingDown className="w-6 h-6" />}
            ${Math.abs(totalGainLoss).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">Total Gain/Loss %</div>
          <div className={`text-2xl font-bold ${
            totalGainLossPercent >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
          }`}>
            {totalGainLossPercent >= 0 ? '+' : ''}{totalGainLossPercent.toFixed(2)}%
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Portfolio Allocation</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Gain/Loss by Stock</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="symbol" />
              <YAxis />
              <Tooltip formatter={(value) => `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`} />
              <Legend />
              <Bar dataKey="gainLoss" fill="#0ea5e9" name="Gain/Loss ($)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Holdings Table */}
      <div className="card">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Holdings</h3>
          <div className="flex gap-2">
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="btn-secondary flex items-center gap-2"
            >
              <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
              Refresh Prices
            </button>
            <button
              onClick={() => setShowAddForm(!showAddForm)}
              className="btn-primary flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Holding
            </button>
          </div>
        </div>

        {showAddForm && (
          <form onSubmit={handleAddHolding} className="mb-6 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Stock Symbol</label>
                <input
                  type="text"
                  value={formData.symbol}
                  onChange={(e) => setFormData({ ...formData, symbol: e.target.value })}
                  className="input-field"
                  placeholder="e.g., AAPL"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Quantity</label>
                <input
                  type="number"
                  value={formData.quantity}
                  onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                  className="input-field"
                  placeholder="e.g., 10"
                  required
                  min="1"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">Average Price</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.averagePrice}
                  onChange={(e) => setFormData({ ...formData, averagePrice: e.target.value })}
                  className="input-field"
                  placeholder="e.g., 150.00"
                  required
                  min="0"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <button type="submit" className="btn-primary">
                Add Holding
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        <div className="overflow-x-auto">
          <table className="w-full">
              <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Symbol</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Quantity</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Avg Price</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Current Price</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Value</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Gain/Loss</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Gain/Loss %</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700 dark:text-gray-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.holdings.map((holding) => {
                const gainLoss = parseFloat(holding.gainLoss || 0);
                const gainLossPercent = parseFloat(holding.gainLossPercent || 0);
                return (
                  <tr
                    key={holding.id}
                    className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    <td className="py-3 px-4 font-medium text-gray-900 dark:text-gray-100">{holding.symbol}</td>
                    <td className="py-3 px-4 text-right text-gray-900 dark:text-gray-100">{holding.quantity}</td>
                    <td className="py-3 px-4 text-right text-gray-900 dark:text-gray-100">
                      ${parseFloat(holding.averagePrice || 0).toFixed(2)}
                    </td>
                    <td className="py-3 px-4 text-right text-gray-900 dark:text-gray-100">
                      ${parseFloat(holding.currentPrice || 0).toFixed(2)}
                    </td>
                    <td className="py-3 px-4 text-right text-gray-900 dark:text-gray-100">
                      ${parseFloat(holding.value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                    <td className={`py-3 px-4 text-right font-medium ${
                      gainLoss >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
                    }`}>
                      {gainLoss >= 0 ? '+' : ''}${gainLoss.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                    <td className={`py-3 px-4 text-right font-medium ${
                      gainLossPercent >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
                    }`}>
                      {gainLossPercent >= 0 ? '+' : ''}{gainLossPercent.toFixed(2)}%
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => handleRemoveHolding(holding.id)}
                        className="text-red-600 hover:text-red-800 p-1"
                        title="Remove holding"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default PortfolioView;

