import { useState, useEffect } from "react";
import {
  ArrowRight,
  ArrowLeft,
  Check,
  User,
  Briefcase,
  Sparkles,
  Shield,
  Target,
  DollarSign,
  Building2,
  X,
  Plus,
  TrendingUp,
} from "lucide-react";
import { userProfileAPI, portfolioAPI } from "../services/api";

const OnboardingWizard = ({ onComplete }) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [loading, setLoading] = useState(false);

  // Step 2: Profile data
  const [profileData, setProfileData] = useState({
    riskTolerance: "MODERATE",
    horizon: "MEDIUM",
    goals: [],
    budget: "",
    preferredSectors: [],
    excludedSectors: [],
    ethicalInvesting: false,
  });

  // Step 3: Portfolio holdings
  const [holdings, setHoldings] = useState([]);
  const [holdingForm, setHoldingForm] = useState({
    symbol: "",
    quantity: "",
    averagePrice: "",
  });

  const availableGoals = [
    "RETIREMENT",
    "GROWTH",
    "INCOME",
    "EDUCATION",
    "HOUSE",
  ];
  const availableSectors = [
    "Technology",
    "Healthcare",
    "Finance",
    "Energy",
    "Consumer",
    "Industrial",
    "Real Estate",
    "Utilities",
    "Materials",
    "Communication",
  ];

  const totalSteps = 4;

  const handleNext = async () => {
    if (currentStep === 2) {
      // Save profile before moving to next step
      try {
        setLoading(true);
        const payload = {
          riskTolerance: profileData.riskTolerance,
          horizon: profileData.horizon,
          goals: profileData.goals,
          budget: profileData.budget ? parseFloat(profileData.budget) : null,
          preferredSectors: profileData.preferredSectors,
          excludedSectors: profileData.excludedSectors,
          ethicalInvesting: profileData.ethicalInvesting,
        };

        try {
          await userProfileAPI.update(payload);
        } catch (updateError) {
          await userProfileAPI.create(payload);
        }
        setCurrentStep(3);
      } catch (error) {
        console.error("Error saving profile:", error);
        alert(
          error.response?.data?.message ||
            "Failed to save profile. Please try again.",
        );
      } finally {
        setLoading(false);
      }
    } else if (currentStep === 3) {
      // Save portfolio holdings before moving to next step
      try {
        setLoading(true);
        for (const holding of holdings) {
          await portfolioAPI.addHolding({
            symbol: holding.symbol.toUpperCase(),
            quantity: parseInt(holding.quantity),
            averagePrice: parseFloat(holding.averagePrice),
          });
        }
        setCurrentStep(4);
      } catch (error) {
        console.error("Error saving portfolio:", error);
        alert(
          error.response?.data?.message ||
            "Failed to save portfolio. Please try again.",
        );
      } finally {
        setLoading(false);
      }
    } else if (currentStep === 4) {
      // Complete onboarding
      if (onComplete) {
        onComplete();
      }
    } else {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const toggleGoal = (goal) => {
    setProfileData((prev) => ({
      ...prev,
      goals: prev.goals.includes(goal)
        ? prev.goals.filter((g) => g !== goal)
        : [...prev.goals, goal],
    }));
  };

  const toggleSector = (sector, type) => {
    setProfileData((prev) => {
      const list =
        type === "preferred" ? prev.preferredSectors : prev.excludedSectors;
      const otherList =
        type === "preferred" ? prev.excludedSectors : prev.preferredSectors;

      const newOtherList = otherList.filter((s) => s !== sector);
      const newList = list.includes(sector)
        ? list.filter((s) => s !== sector)
        : [...list, sector];

      return {
        ...prev,
        preferredSectors: type === "preferred" ? newList : newOtherList,
        excludedSectors: type === "preferred" ? newOtherList : newList,
      };
    });
  };

  const handleAddHolding = () => {
    if (
      !holdingForm.symbol ||
      !holdingForm.quantity ||
      !holdingForm.averagePrice
    ) {
      alert("Please fill in all fields");
      return;
    }

    setHoldings([...holdings, { ...holdingForm }]);
    setHoldingForm({ symbol: "", quantity: "", averagePrice: "" });
  };

  const handleRemoveHolding = (index) => {
    setHoldings(holdings.filter((_, i) => i !== index));
  };

  const canProceed = () => {
    if (currentStep === 2) {
      // At least one goal should be selected
      return profileData.goals.length > 0;
    }
    if (currentStep === 3) {
      // Holdings are optional, but if added, they should be valid
      return true;
    }
    return true;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-slate-50 dark:bg-black dark:bg-none flex items-center justify-center p-4">
      <div className="w-full max-w-4xl">
        {/* Progress Bar */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            {[...Array(totalSteps)].map((_, index) => {
              const step = index + 1;
              const isActive = step === currentStep;
              const isCompleted = step < currentStep;

              return (
                <div key={step} className="flex items-center flex-1">
                  <div className="flex flex-col items-center flex-1">
                    <div
                      className={`w-12 h-12 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-300 ${
                        isCompleted
                          ? "bg-success-600 text-white"
                          : isActive
                            ? "bg-primary-600 text-white scale-110"
                            : "bg-gray-300 dark:bg-gray-700 text-gray-600 dark:text-gray-400"
                      }`}
                    >
                      {isCompleted ? <Check className="w-6 h-6" /> : step}
                    </div>
                    <span
                      className={`text-xs mt-2 font-medium ${
                        isActive
                          ? "text-primary-600 dark:text-primary-400"
                          : "text-gray-500 dark:text-gray-400"
                      }`}
                    >
                      {step === 1 && "Welcome"}
                      {step === 2 && "Profile"}
                      {step === 3 && "Portfolio"}
                      {step === 4 && "Complete"}
                    </span>
                  </div>
                  {step < totalSteps && (
                    <div
                      className={`flex-1 h-1 mx-2 rounded ${
                        isCompleted
                          ? "bg-success-600"
                          : "bg-gray-300 dark:bg-gray-700"
                      }`}
                    />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Step Content */}
        <div className="card-elevated p-8 md:p-12">
          {/* Step 1: Welcome */}
          {currentStep === 1 && (
            <div className="text-center space-y-6">
              <div className="w-24 h-24 bg-gradient-to-br from-primary-100 to-primary-200 dark:bg-black dark:bg-none rounded-3xl flex items-center justify-center mx-auto mb-6">
                <Sparkles className="w-12 h-12 text-primary-600 dark:text-primary-400" />
              </div>
              <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
                Welcome to Financial Advisor AI
              </h2>
              <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto leading-relaxed">
                Let's set up your personalized investment profile and portfolio.
                This will only take a few minutes, and we'll use this
                information to provide you with tailored financial advice.
              </p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
                <div className="p-6 bg-gradient-to-br from-blue-50 to-blue-100 dark:bg-black dark:bg-none rounded-2xl">
                  <User className="w-8 h-8 text-blue-600 dark:text-blue-400 mx-auto mb-3" />
                  <h3 className="font-bold text-gray-900 dark:text-white mb-2">
                    Your Profile
                  </h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Tell us about your risk tolerance and investment goals
                  </p>
                </div>
                <div className="p-6 bg-gradient-to-br from-green-50 to-green-100 dark:bg-black dark:bg-none rounded-2xl">
                  <Briefcase className="w-8 h-8 text-green-600 dark:text-green-400 mx-auto mb-3" />
                  <h3 className="font-bold text-gray-900 dark:text-white mb-2">
                    Your Portfolio
                  </h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Add your current investments to track performance
                  </p>
                </div>
                <div className="p-6 bg-gradient-to-br from-purple-50 to-purple-100 dark:bg-black dark:bg-none rounded-2xl">
                  <TrendingUp className="w-8 h-8 text-purple-600 dark:text-purple-400 mx-auto mb-3" />
                  <h3 className="font-bold text-gray-900 dark:text-white mb-2">
                    Get Started
                  </h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Start receiving personalized AI-powered advice
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Step 2: Profile */}
          {currentStep === 2 && (
            <div className="space-y-8">
              <div className="text-center mb-8">
                <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                  Investment Profile
                </h2>
                <p className="text-gray-600 dark:text-gray-400">
                  Help us understand your investment preferences
                </p>
              </div>

              {/* Risk Tolerance */}
              <div>
                <label className="flex items-center gap-2 text-sm font-bold mb-4 text-gray-700 dark:text-gray-300">
                  <Shield className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                  Risk Tolerance
                </label>
                <div className="grid grid-cols-3 gap-4">
                  {["CONSERVATIVE", "MODERATE", "AGGRESSIVE"].map((risk) => (
                    <button
                      key={risk}
                      type="button"
                      onClick={() =>
                        setProfileData({ ...profileData, riskTolerance: risk })
                      }
                      className={`py-4 px-6 rounded-xl border-2 transition-all duration-200 font-semibold ${
                        profileData.riskTolerance === risk
                          ? "border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black dark:bg-none text-primary-700 dark:text-primary-300 shadow-soft scale-105"
                          : "border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
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
                  {["SHORT", "MEDIUM", "LONG"].map((horizon) => (
                    <button
                      key={horizon}
                      type="button"
                      onClick={() =>
                        setProfileData({ ...profileData, horizon })
                      }
                      className={`py-4 px-6 rounded-xl border-2 transition-all duration-200 font-semibold ${
                        profileData.horizon === horizon
                          ? "border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black dark:bg-none text-primary-700 dark:text-primary-300 shadow-soft scale-105"
                          : "border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
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
                  Investment Goals <span className="text-danger-600">*</span>
                </label>
                <div className="flex flex-wrap gap-3">
                  {availableGoals.map((goal) => (
                    <button
                      key={goal}
                      type="button"
                      onClick={() => toggleGoal(goal)}
                      className={`py-3 px-5 rounded-xl border-2 transition-all duration-200 font-semibold flex items-center gap-2 ${
                        profileData.goals.includes(goal)
                          ? "border-primary-600 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black dark:bg-none text-primary-700 dark:text-primary-300 shadow-soft"
                          : "border-gray-300 dark:border-gray-600 hover:border-primary-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
                      }`}
                    >
                      {profileData.goals.includes(goal) && (
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
                  value={profileData.budget}
                  onChange={(e) =>
                    setProfileData({ ...profileData, budget: e.target.value })
                  }
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
                      onClick={() => toggleSector(sector, "preferred")}
                      className={`py-2.5 px-4 rounded-xl border-2 transition-all duration-200 font-medium text-sm flex items-center gap-2 ${
                        profileData.preferredSectors.includes(sector)
                          ? "border-success-600 bg-gradient-to-br from-success-50 to-success-100 dark:bg-black dark:bg-none text-success-700 dark:text-success-300 shadow-soft"
                          : "border-gray-300 dark:border-gray-600 hover:border-success-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
                      }`}
                    >
                      {profileData.preferredSectors.includes(sector) && (
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
                      onClick={() => toggleSector(sector, "excluded")}
                      className={`py-2.5 px-4 rounded-xl border-2 transition-all duration-200 font-medium text-sm flex items-center gap-2 ${
                        profileData.excludedSectors.includes(sector)
                          ? "border-danger-600 bg-gradient-to-br from-danger-50 to-danger-100 dark:bg-black dark:bg-none text-danger-700 dark:text-danger-300 shadow-soft"
                          : "border-gray-300 dark:border-gray-600 hover:border-danger-400 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
                      }`}
                    >
                      {profileData.excludedSectors.includes(sector) && (
                        <X className="w-3.5 h-3.5" />
                      )}
                      {sector}
                    </button>
                  ))}
                </div>
              </div>

              {/* Ethical Investing */}
              <div className="flex items-center gap-4 p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:bg-black dark:bg-none rounded-xl border border-gray-200 dark:border-gray-600">
                <input
                  type="checkbox"
                  id="ethicalInvesting"
                  checked={profileData.ethicalInvesting}
                  onChange={(e) =>
                    setProfileData({
                      ...profileData,
                      ethicalInvesting: e.target.checked,
                    })
                  }
                  className="w-6 h-6 text-primary-600 rounded-lg focus:ring-primary-500 focus:ring-2 cursor-pointer"
                />
                <label
                  htmlFor="ethicalInvesting"
                  className="text-sm font-semibold text-gray-700 dark:text-gray-300 cursor-pointer flex-1"
                >
                  Ethical/ESG Investing Preferences
                </label>
              </div>
            </div>
          )}

          {/* Step 3: Portfolio */}
          {currentStep === 3 && (
            <div className="space-y-8">
              <div className="text-center mb-8">
                <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                  Your Portfolio
                </h2>
                <p className="text-gray-600 dark:text-gray-400">
                  Add your current stock holdings (optional - you can add more
                  later)
                </p>
              </div>

              {/* Add Holding Form */}
              <div className="p-6 bg-gradient-to-br from-gray-50 to-gray-100 dark:bg-black dark:bg-none rounded-2xl border border-gray-200 dark:border-gray-600">
                <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
                  Add Holding
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label className="block text-sm font-semibold mb-2 text-gray-700 dark:text-gray-300">
                      Stock Symbol
                    </label>
                    <input
                      type="text"
                      value={holdingForm.symbol}
                      onChange={(e) =>
                        setHoldingForm({
                          ...holdingForm,
                          symbol: e.target.value.toUpperCase(),
                        })
                      }
                      className="input-field"
                      placeholder="e.g., AAPL"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold mb-2 text-gray-700 dark:text-gray-300">
                      Quantity
                    </label>
                    <input
                      type="number"
                      value={holdingForm.quantity}
                      onChange={(e) =>
                        setHoldingForm({
                          ...holdingForm,
                          quantity: e.target.value,
                        })
                      }
                      className="input-field"
                      placeholder="e.g., 10"
                      min="1"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold mb-2 text-gray-700 dark:text-gray-300">
                      Average Price ($)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={holdingForm.averagePrice}
                      onChange={(e) =>
                        setHoldingForm({
                          ...holdingForm,
                          averagePrice: e.target.value,
                        })
                      }
                      className="input-field"
                      placeholder="e.g., 150.00"
                      min="0.01"
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={handleAddHolding}
                      className="btn-primary w-full flex items-center justify-center gap-2"
                    >
                      <Plus className="w-4 h-4" />
                      Add
                    </button>
                  </div>
                </div>
              </div>

              {/* Holdings List */}
              {holdings.length > 0 && (
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
                    Your Holdings
                  </h3>
                  <div className="space-y-3">
                    {holdings.map((holding, index) => (
                      <div
                        key={index}
                        className="p-4 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 flex items-center justify-between"
                      >
                        <div className="flex items-center gap-4">
                          <div className="w-12 h-12 bg-gradient-to-br from-primary-100 to-primary-200 dark:bg-black dark:bg-none rounded-xl flex items-center justify-center">
                            <TrendingUp className="w-6 h-6 text-primary-600 dark:text-primary-400" />
                          </div>
                          <div>
                            <p className="font-bold text-gray-900 dark:text-white">
                              {holding.symbol}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                              {holding.quantity} shares @ $
                              {parseFloat(holding.averagePrice).toFixed(2)}
                            </p>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleRemoveHolding(index)}
                          className="p-2 text-danger-600 hover:bg-danger-50 dark:hover:bg-danger-900/20 rounded-lg transition-colors"
                        >
                          <X className="w-5 h-5" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {holdings.length === 0 && (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                  <Briefcase className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>
                    No holdings added yet. You can add them later from the
                    Portfolio tab.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Step 4: Complete */}
          {currentStep === 4 && (
            <div className="text-center space-y-6">
              <div className="w-24 h-24 bg-gradient-to-br from-success-100 to-success-200 dark:bg-black dark:bg-none rounded-3xl flex items-center justify-center mx-auto mb-6">
                <Check className="w-12 h-12 text-success-600 dark:text-success-400" />
              </div>
              <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
                You're All Set!
              </h2>
              <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto leading-relaxed">
                Your profile and portfolio have been set up successfully. You
                can now start using the AI Financial Advisor to get personalized
                investment advice and recommendations.
              </p>
              <div className="mt-8 p-6 bg-gradient-to-br from-primary-50 to-primary-100 dark:bg-black dark:bg-none rounded-2xl">
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  💡 <strong>Tip:</strong> You can always update your profile
                  and portfolio from their respective tabs.
                </p>
              </div>
            </div>
          )}

          {/* Navigation Buttons */}
          <div className="flex items-center justify-between mt-8 pt-6 border-t border-gray-200 dark:border-gray-700">
            <button
              type="button"
              onClick={handleBack}
              disabled={currentStep === 1 || loading}
              className="btn-secondary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>
            <button
              type="button"
              onClick={handleNext}
              disabled={!canProceed() || loading}
              className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                "Saving..."
              ) : currentStep === 4 ? (
                <>
                  Get Started
                  <ArrowRight className="w-4 h-4" />
                </>
              ) : (
                <>
                  Next
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OnboardingWizard;
