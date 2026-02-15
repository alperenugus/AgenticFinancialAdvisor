import { useState, useEffect } from 'react';
import { Save, User, Shield, Target, DollarSign, Building2, X, Check } from 'lucide-react';
import { userProfileAPI } from '../services/api';

const UserProfileForm = ({ onSave }) => {
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
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const response = await userProfileAPI.get();
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
        riskTolerance: formData.riskTolerance,
        horizon: formData.horizon,
        goals: formData.goals,
        budget: formData.budget ? parseFloat(formData.budget) : null,
        preferredSectors: formData.preferredSectors,
        excludedSectors: formData.excludedSectors,
        ethicalInvesting: formData.ethicalInvesting,
      };

      try {
        await userProfileAPI.update(payload);
      } catch (updateError) {
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
      
      const newOtherList = otherList.filter((s) => s !== sector);
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
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400 font-medium">Loading profile...</p>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="card-elevated space-y-8">
      <div className="flex items-center gap-4 mb-2">
        <div className="p-3 bg-gradient-to-br from-primary-100 to-primary-200 dark:bg-black rounded-2xl">
          <User className="w-7 h-7 text-primary-600 dark:text-primary-400" />
        </div>
        <div>
          <h2 className="text-3xl font-bold text-gray-900 dark:text-white">Investment Profile</h2>
          <p className="text-gray-600 dark:text-gray-400 mt-1">Customize your preferences for personalized recommendations</p>
        </div>
      </div>

      {/* Risk Tolerance */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <Shield className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          Risk Tolerance
        </label>
        <div className="grid grid-cols-3 gap-4">
          {['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'].map((risk) => (
            <button
              key={risk}
              type="button"
              onClick={() => setFormData({ ...formData, riskTolerance: risk })}
              className={`py-4 px-6 rounded-xl border-2 transition-all duration-200 font-semibold ${
                formData.riskTolerance === risk
                  ? 'border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black text-primary-700 dark:text-primary-300 shadow-soft scale-105'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
              }`}
            >
              {risk}
            </button>
          ))}
        </div>
      </div>

      {/* Investment Horizon */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <Target className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          Investment Horizon
        </label>
        <div className="grid grid-cols-3 gap-4">
          {['SHORT', 'MEDIUM', 'LONG'].map((horizon) => (
            <button
              key={horizon}
              type="button"
              onClick={() => setFormData({ ...formData, horizon })}
              className={`py-4 px-6 rounded-xl border-2 transition-all duration-200 font-semibold ${
                formData.horizon === horizon
                  ? 'border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black text-primary-700 dark:text-primary-300 shadow-soft scale-105'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
              }`}
            >
              {horizon}
            </button>
          ))}
        </div>
      </div>

      {/* Investment Goals */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <Target className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          Investment Goals
        </label>
        <div className="flex flex-wrap gap-3">
          {availableGoals.map((goal) => (
            <button
              key={goal}
              type="button"
              onClick={() => toggleGoal(goal)}
              className={`py-3 px-5 rounded-xl border-2 transition-all duration-200 font-semibold flex items-center gap-2 ${
                formData.goals.includes(goal)
                  ? 'border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black text-primary-700 dark:text-primary-300 shadow-soft'
                  : 'border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
              }`}
            >
              {formData.goals.includes(goal) && (
                <Check className="w-4 h-4" />
              )}
              {goal}
            </button>
          ))}
        </div>
      </div>

      {/* Budget */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <DollarSign className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          Investment Budget ($)
        </label>
        <input
          type="number"
          step="0.01"
          min="0"
          value={formData.budget}
          onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
          className="input-field max-w-md"
          placeholder="e.g., 10000"
        />
      </div>

      {/* Preferred Sectors */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <Building2 className="w-5 h-5 text-success-600 dark:text-success-400" />
          Preferred Sectors
        </label>
        <div className="flex flex-wrap gap-3">
          {availableSectors.map((sector) => (
            <button
              key={sector}
              type="button"
              onClick={() => toggleSector(sector, 'preferred')}
              className={`py-2.5 px-4 rounded-xl border-2 transition-all duration-200 font-medium text-sm flex items-center gap-2 ${
                formData.preferredSectors.includes(sector)
                  ? 'border-success-600 bg-gradient-to-br from-success-50 to-success-100 dark:bg-black text-success-700 dark:text-success-300 shadow-soft'
                  : 'border-gray-300 dark:border-gray-600 hover:border-success-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
              }`}
            >
              {formData.preferredSectors.includes(sector) && (
                <Check className="w-3.5 h-3.5" />
              )}
              {sector}
            </button>
          ))}
        </div>
      </div>

      {/* Excluded Sectors */}
      <div>
        <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
          <X className="w-5 h-5 text-danger-600 dark:text-danger-400" />
          Excluded Sectors
        </label>
        <div className="flex flex-wrap gap-3">
          {availableSectors.map((sector) => (
            <button
              key={sector}
              type="button"
              onClick={() => toggleSector(sector, 'excluded')}
              className={`py-2.5 px-4 rounded-xl border-2 transition-all duration-200 font-medium text-sm flex items-center gap-2 ${
                formData.excludedSectors.includes(sector)
                  ? 'border-danger-600 bg-gradient-to-br from-danger-50 to-danger-100 dark:bg-black text-danger-700 dark:text-danger-300 shadow-soft'
                  : 'border-gray-300 dark:border-gray-600 hover:border-danger-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
              }`}
            >
              {formData.excludedSectors.includes(sector) && (
                <X className="w-3.5 h-3.5" />
              )}
              {sector}
            </button>
          ))}
        </div>
      </div>

      {/* Ethical Investing */}
      <div className="flex items-center gap-4 p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:bg-black rounded-xl border border-gray-200 dark:border-gray-600">
        <input
          type="checkbox"
          id="ethicalInvesting"
          checked={formData.ethicalInvesting}
          onChange={(e) => setFormData({ ...formData, ethicalInvesting: e.target.checked })}
          className="w-6 h-6 text-primary-600 rounded-lg focus:ring-primary-500 focus:ring-2 cursor-pointer"
        />
        <label htmlFor="ethicalInvesting" className="text-sm font-semibold text-gray-700 dark:text-gray-300 cursor-pointer flex-1">
          Ethical/ESG Investing Preferences
        </label>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={saving}
        className="btn-primary w-full flex items-center justify-center gap-3 disabled:opacity-50 text-lg py-4"
      >
        <Save className="w-5 h-5" />
        {saving ? 'Saving Profile...' : 'Save Profile'}
      </button>
    </form>
  );
};

export default UserProfileForm;
