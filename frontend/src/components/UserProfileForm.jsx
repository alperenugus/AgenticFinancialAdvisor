import { useState, useEffect } from 'react';
import { Save, User } from 'lucide-react';
import { userProfileAPI } from '../services/api';

const UserProfileForm = ({ userId, onSave }) => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    riskTolerance: 'MODERATE',
    horizon: 'MEDIUM',
    goals: [],
    budget: '',
    preferredSectors: [],
    excludedSectors: [],
    ethicalInvesting: false,
  });

  const availableGoals = ['RETIREMENT', 'GROWTH', 'INCOME', 'EDUCATION', 'HOUSE'];
  const availableSectors = [
    'Technology',
    'Healthcare',
    'Finance',
    'Energy',
    'Consumer',
    'Industrial',
    'Real Estate',
    'Utilities',
    'Materials',
    'Communication',
  ];

  useEffect(() => {
    loadProfile();
  }, [userId]);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const response = await userProfileAPI.get(userId);
      if (response.data) {
        const profile = response.data;
        setFormData({
          riskTolerance: profile.riskTolerance || 'MODERATE',
          horizon: profile.horizon || 'MEDIUM',
          goals: profile.goals || [],
          budget: profile.budget ? profile.budget.toString() : '',
          preferredSectors: profile.preferredSectors || [],
          excludedSectors: profile.excludedSectors || [],
          ethicalInvesting: profile.ethicalInvesting || false,
        });
      }
    } catch (error) {
      // Profile doesn't exist yet, that's okay
      console.log('No existing profile found');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      const payload = {
        userId,
        riskTolerance: formData.riskTolerance,
        horizon: formData.horizon,
        goals: formData.goals,
        budget: formData.budget ? parseFloat(formData.budget) : null,
        preferredSectors: formData.preferredSectors,
        excludedSectors: formData.excludedSectors,
        ethicalInvesting: formData.ethicalInvesting,
      };

      try {
        await userProfileAPI.update(userId, payload);
      } catch (updateError) {
        // If update fails, try creating
        await userProfileAPI.create(payload);
      }

      if (onSave) {
        onSave();
      }
      alert('Profile saved successfully!');
    } catch (error) {
      console.error('Error saving profile:', error);
      alert(error.response?.data?.message || 'Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  const toggleGoal = (goal) => {
    setFormData((prev) => ({
      ...prev,
      goals: prev.goals.includes(goal)
        ? prev.goals.filter((g) => g !== goal)
        : [...prev.goals, goal],
    }));
  };

  const toggleSector = (sector, type) => {
    setFormData((prev) => {
      const list = type === 'preferred' ? prev.preferredSectors : prev.excludedSectors;
      const otherList = type === 'preferred' ? prev.excludedSectors : prev.preferredSectors;
      
      // Remove from other list if present
      const newOtherList = otherList.filter((s) => s !== sector);
      
      // Toggle in current list
      const newList = list.includes(sector)
        ? list.filter((s) => s !== sector)
        : [...list, sector];
      
      return {
        ...prev,
        preferredSectors: type === 'preferred' ? newList : newOtherList,
        excludedSectors: type === 'preferred' ? newOtherList : newList,
      };
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="card space-y-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2 bg-primary-100 dark:bg-primary-900 rounded-lg">
          <User className="w-6 h-6 text-primary-600 dark:text-primary-400" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">User Profile</h2>
      </div>

      {/* Risk Tolerance */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Risk Tolerance</label>
        <div className="grid grid-cols-3 gap-3">
          {['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'].map((risk) => (
            <button
              key={risk}
              type="button"
              onClick={() => setFormData({ ...formData, riskTolerance: risk })}
              className={`py-2 px-4 rounded-lg border-2 transition-colors ${
                formData.riskTolerance === risk
                  ? 'border-primary-600 bg-primary-50 dark:bg-primary-900 text-primary-700 dark:text-primary-300'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400'
              }`}
            >
              {risk}
            </button>
          ))}
        </div>
      </div>

      {/* Investment Horizon */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Investment Horizon</label>
        <div className="grid grid-cols-3 gap-3">
          {['SHORT', 'MEDIUM', 'LONG'].map((horizon) => (
            <button
              key={horizon}
              type="button"
              onClick={() => setFormData({ ...formData, horizon })}
              className={`py-2 px-4 rounded-lg border-2 transition-colors ${
                formData.horizon === horizon
                  ? 'border-primary-600 bg-primary-50 dark:bg-primary-900 text-primary-700 dark:text-primary-300'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400'
              }`}
            >
              {horizon}
            </button>
          ))}
        </div>
      </div>

      {/* Investment Goals */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Investment Goals</label>
        <div className="flex flex-wrap gap-2">
          {availableGoals.map((goal) => (
            <button
              key={goal}
              type="button"
              onClick={() => toggleGoal(goal)}
              className={`py-2 px-4 rounded-lg border-2 transition-colors ${
                formData.goals.includes(goal)
                  ? 'border-primary-600 bg-primary-50 dark:bg-primary-900 text-primary-700 dark:text-primary-300'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400'
              }`}
            >
              {goal}
            </button>
          ))}
        </div>
      </div>

      {/* Budget */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Investment Budget ($)</label>
        <input
          type="number"
          step="0.01"
          min="0"
          value={formData.budget}
          onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
          className="input-field"
          placeholder="e.g., 10000"
        />
      </div>

      {/* Preferred Sectors */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Preferred Sectors</label>
        <div className="flex flex-wrap gap-2">
          {availableSectors.map((sector) => (
            <button
              key={sector}
              type="button"
              onClick={() => toggleSector(sector, 'preferred')}
              className={`py-2 px-4 rounded-lg border-2 transition-colors ${
                formData.preferredSectors.includes(sector)
                  ? 'border-green-600 bg-green-50 dark:bg-green-900 text-green-700 dark:text-green-300'
                  : 'border-gray-300 dark:border-gray-600 hover:border-green-400'
              }`}
            >
              {sector}
            </button>
          ))}
        </div>
      </div>

      {/* Excluded Sectors */}
      <div>
        <label className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Excluded Sectors</label>
        <div className="flex flex-wrap gap-2">
          {availableSectors.map((sector) => (
            <button
              key={sector}
              type="button"
              onClick={() => toggleSector(sector, 'excluded')}
              className={`py-2 px-4 rounded-lg border-2 transition-colors ${
                formData.excludedSectors.includes(sector)
                  ? 'border-red-600 bg-red-50 dark:bg-red-900 text-red-700 dark:text-red-300'
                  : 'border-gray-300 dark:border-gray-600 hover:border-red-400'
              }`}
            >
              {sector}
            </button>
          ))}
        </div>
      </div>

      {/* Ethical Investing */}
      <div className="flex items-center gap-3">
        <input
          type="checkbox"
          id="ethicalInvesting"
          checked={formData.ethicalInvesting}
          onChange={(e) => setFormData({ ...formData, ethicalInvesting: e.target.checked })}
          className="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
        />
        <label htmlFor="ethicalInvesting" className="text-sm font-medium text-gray-700 dark:text-gray-300">
          Ethical/ESG Investing Preferences
        </label>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={saving}
        className="btn-primary w-full flex items-center justify-center gap-2 disabled:opacity-50"
      >
        <Save className="w-4 h-4" />
        {saving ? 'Saving...' : 'Save Profile'}
      </button>
    </form>
  );
};

export default UserProfileForm;

