import { useState, useEffect } from "react";
import {
  TrendingUp,
  TrendingDown,
  RefreshCw,
  Plus,
  Trash2,
  Briefcase,
  DollarSign,
  Percent,
  ArrowUpRight,
  ArrowDownRight,
} from "lucide-react";
import { portfolioAPI } from "../services/api";
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
} from "recharts";

const COLORS = [
  "#6366f1",
  "#8b5cf6",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#06b6d4",
  "#ec4899",
  "#14b8a6",
];

const PortfolioView = () => {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    symbol: "",
    quantity: "",
    averagePrice: "",
  });

  useEffect(() => {
    loadPortfolio();
  }, []);

  const loadPortfolio = async () => {
    try {
      setLoading(true);
      const response = await portfolioAPI.get();
      setPortfolio(response.data);
    } catch (error) {
      console.error("Error loading portfolio:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await portfolioAPI.refresh();
      await loadPortfolio();
    } catch (error) {
      console.error("Error refreshing portfolio:", error);
    } finally {
      setRefreshing(false);
    }
  };

  const handleAddHolding = async (e) => {
    e.preventDefault();
    try {
      await portfolioAPI.addHolding({
        symbol: formData.symbol.toUpperCase(),
        quantity: parseInt(formData.quantity),
        averagePrice: parseFloat(formData.averagePrice),
      });
      setFormData({ symbol: "", quantity: "", averagePrice: "" });
      setShowAddForm(false);
      await loadPortfolio();
    } catch (error) {
      console.error("Error adding holding:", error);
      alert(error.response?.data?.message || "Failed to add holding");
    }
  };

  const handleRemoveHolding = async (holdingId) => {
    if (!window.confirm("Are you sure you want to remove this holding?"))
      return;

    try {
      await portfolioAPI.removeHolding(holdingId);
      await loadPortfolio();
    } catch (error) {
      console.error("Error removing holding:", error);
      alert("Failed to remove holding");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600text-gray-400 font-medium">
            Loading portfolio...
          </p>
        </div>
      </div>
    );
  }

  if (!portfolio || !portfolio.holdings || portfolio.holdings.length === 0) {
    return (
      <div className="card-elevated">
        <div className="text-center py-16">
          <div className="w-20 h-20 bg-gradient-to-br from-primary-100 to-primary-200bg-blackbg-none rounded-2xl flex items-center justify-center mx-auto mb-6">
            <Briefcase className="w-10 h-10 text-primary-600text-primary-400" />
          </div>
          <h3 className="text-2xl font-bold text-gray-900text-white mb-2">
            Your Portfolio is Empty
          </h3>
          <p className="text-gray-600text-gray-400 mb-8 max-w-md mx-auto">
            Start building your investment portfolio by adding your first
            holding.
          </p>
          <button
            onClick={() => setShowAddForm(true)}
            className="btn-primary inline-flex items-center gap-2"
          >
            <Plus className="w-5 h-5" />
            Add Your First Holding
          </button>
        </div>

        {showAddForm && (
          <div className="mt-8 p-6 bg-gradient-to-br from-gray-50 to-gray-100bg-blackbg-none rounded-2xl border border-gray-200border-gray-600">
            <h4 className="text-lg font-bold text-gray-900text-white mb-4">
              Add New Holding
            </h4>
            <form onSubmit={handleAddHolding} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Stock Symbol
                  </label>
                  <input
                    type="text"
                    value={formData.symbol}
                    onChange={(e) =>
                      setFormData({ ...formData, symbol: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., AAPL"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Quantity
                  </label>
                  <input
                    type="number"
                    value={formData.quantity}
                    onChange={(e) =>
                      setFormData({ ...formData, quantity: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., 10"
                    required
                    min="1"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Average Price ($)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.averagePrice}
                    onChange={(e) =>
                      setFormData({ ...formData, averagePrice: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., 150.00"
                    required
                    min="0"
                  />
                </div>
              </div>
              <div className="flex gap-3">
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
          </div>
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
      {/* Professional Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="stat-card">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 bg-gradient-to-br from-primary-100 to-primary-200bg-blackbg-none rounded-xl">
              <DollarSign className="w-6 h-6 text-primary-600text-primary-400" />
            </div>
            <span className="text-xs font-semibold text-gray-500text-gray-400 uppercase tracking-wide">
              Total Value
            </span>
          </div>
          <div className="text-3xl font-bold text-gray-900text-white mb-1">
            $
            {totalValue.toLocaleString("en-US", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between mb-4">
            <div
              className={`p-3 rounded-xl ${
                totalGainLoss >= 0
                  ? "bg-gradient-to-br from-success-100 to-success-200bg-blackbg-none"
                  : "bg-gradient-to-br from-danger-100 to-danger-200bg-blackbg-none"
              }`}
            >
              {totalGainLoss >= 0 ? (
                <ArrowUpRight className="w-6 h-6 text-success-600text-success-400" />
              ) : (
                <ArrowDownRight className="w-6 h-6 text-danger-600text-danger-400" />
              )}
            </div>
            <span className="text-xs font-semibold text-gray-500text-gray-400 uppercase tracking-wide">
              Total Gain/Loss
            </span>
          </div>
          <div
            className={`text-3xl font-bold flex items-center gap-2 ${
              totalGainLoss >= 0
                ? "text-success-600text-success-400"
                : "text-danger-600text-danger-400"
            }`}
          >
            {totalGainLoss >= 0 ? "+" : ""}$
            {Math.abs(totalGainLoss).toLocaleString("en-US", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between mb-4">
            <div
              className={`p-3 rounded-xl ${
                totalGainLossPercent >= 0
                  ? "bg-gradient-to-br from-success-100 to-success-200bg-blackbg-none"
                  : "bg-gradient-to-br from-danger-100 to-danger-200bg-blackbg-none"
              }`}
            >
              <Percent
                className={`w-6 h-6 ${
                  totalGainLossPercent >= 0
                    ? "text-success-600text-success-400"
                    : "text-danger-600text-danger-400"
                }`}
              />
            </div>
            <span className="text-xs font-semibold text-gray-500text-gray-400 uppercase tracking-wide">
              Gain/Loss %
            </span>
          </div>
          <div
            className={`text-3xl font-bold ${
              totalGainLossPercent >= 0
                ? "text-success-600text-success-400"
                : "text-danger-600text-danger-400"
            }`}
          >
            {totalGainLossPercent >= 0 ? "+" : ""}
            {totalGainLossPercent.toFixed(2)}%
          </div>
        </div>
      </div>

      {/* Professional Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card-elevated">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-xl font-bold text-gray-900text-white">
              Portfolio Allocation
            </h3>
          </div>
          <ResponsiveContainer width="100%" height={320}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(0)}%`
                }
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                formatter={(value) =>
                  `$${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                }
                contentStyle={{
                  backgroundColor: "rgba(255, 255, 255, 0.95)",
                  border: "1px solid #e5e7eb",
                  borderRadius: "12px",
                  padding: "12px",
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card-elevated">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-xl font-bold text-gray-900text-white">
              Performance by Stock
            </h3>
          </div>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis
                dataKey="symbol"
                tick={{ fill: "#6b7280", fontSize: 12 }}
                stroke="#9ca3af"
              />
              <YAxis
                tick={{ fill: "#6b7280", fontSize: 12 }}
                stroke="#9ca3af"
              />
              <Tooltip
                formatter={(value) =>
                  `$${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                }
                contentStyle={{
                  backgroundColor: "rgba(255, 255, 255, 0.95)",
                  border: "1px solid #e5e7eb",
                  borderRadius: "12px",
                  padding: "12px",
                }}
              />
              <Bar
                dataKey="gainLoss"
                fill="#6366f1"
                radius={[8, 8, 0, 0]}
                name="Gain/Loss ($)"
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Professional Holdings Table */}
      <div className="card-elevated">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-xl font-bold text-gray-900text-white">
            Holdings
          </h3>
          <div className="flex gap-3">
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="btn-secondary flex items-center gap-2"
            >
              <RefreshCw
                className={`w-4 h-4 ${refreshing ? "animate-spin" : ""}`}
              />
              Refresh
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
          <div className="mb-6 p-6 bg-gradient-to-br from-gray-50 to-gray-100bg-blackbg-none rounded-2xl border border-gray-200border-gray-600">
            <h4 className="text-lg font-bold text-gray-900text-white mb-4">
              Add New Holding
            </h4>
            <form onSubmit={handleAddHolding} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Stock Symbol
                  </label>
                  <input
                    type="text"
                    value={formData.symbol}
                    onChange={(e) =>
                      setFormData({ ...formData, symbol: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., AAPL"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Quantity
                  </label>
                  <input
                    type="number"
                    value={formData.quantity}
                    onChange={(e) =>
                      setFormData({ ...formData, quantity: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., 10"
                    required
                    min="1"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold mb-2 text-gray-700text-gray-300">
                    Average Price ($)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.averagePrice}
                    onChange={(e) =>
                      setFormData({ ...formData, averagePrice: e.target.value })
                    }
                    className="input-field"
                    placeholder="e.g., 150.00"
                    required
                    min="0"
                  />
                </div>
              </div>
              <div className="flex gap-3">
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
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b-2 border-gray-200border-gray-700">
                <th className="text-left py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Symbol
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Avg Price
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Current
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Value
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Gain/Loss
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  %
                </th>
                <th className="text-right py-4 px-4 font-bold text-sm text-gray-700text-gray-300 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {portfolio.holdings.map((holding) => {
                const gainLoss = parseFloat(holding.gainLoss || 0);
                const gainLossPercent = parseFloat(
                  holding.gainLossPercent || 0,
                );
                return (
                  <tr
                    key={holding.id}
                    className="border-b border-gray-100border-gray-800 hover:bg-gray-50hover:bg-gray-700/50 transition-colors"
                  >
                    <td className="py-4 px-4">
                      <span className="font-bold text-lg text-gray-900text-white">
                        {holding.symbol}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-right font-semibold text-gray-900text-white">
                      {holding.quantity}
                    </td>
                    <td className="py-4 px-4 text-right font-medium text-gray-700text-gray-300">
                      ${parseFloat(holding.averagePrice || 0).toFixed(2)}
                    </td>
                    <td className="py-4 px-4 text-right font-medium text-gray-700text-gray-300">
                      ${parseFloat(holding.currentPrice || 0).toFixed(2)}
                    </td>
                    <td className="py-4 px-4 text-right font-semibold text-gray-900text-white">
                      $
                      {parseFloat(holding.value || 0).toLocaleString("en-US", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </td>
                    <td
                      className={`py-4 px-4 text-right font-bold ${
                        gainLoss >= 0
                          ? "text-success-600text-success-400"
                          : "text-danger-600text-danger-400"
                      }`}
                    >
                      <div className="flex items-center justify-end gap-1">
                        {gainLoss >= 0 ? (
                          <ArrowUpRight className="w-4 h-4" />
                        ) : (
                          <ArrowDownRight className="w-4 h-4" />
                        )}
                        <span>
                          {gainLoss >= 0 ? "+" : ""}$
                          {gainLoss.toLocaleString("en-US", {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </span>
                      </div>
                    </td>
                    <td
                      className={`py-4 px-4 text-right font-bold ${
                        gainLossPercent >= 0
                          ? "text-success-600text-success-400"
                          : "text-danger-600text-danger-400"
                      }`}
                    >
                      {gainLossPercent >= 0 ? "+" : ""}
                      {gainLossPercent.toFixed(2)}%
                    </td>
                    <td className="py-4 px-4 text-right">
                      <button
                        onClick={() => handleRemoveHolding(holding.id)}
                        className="text-danger-600 hover:text-danger-800text-danger-400hover:text-danger-300 p-2 hover:bg-danger-50hover:bg-danger-900/20 rounded-lg transition-colors"
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
