# How to Create a User Profile

## Quick Guide

1. **Open the Frontend Application**
   - Go to your deployed frontend URL (e.g., `https://your-frontend.railway.app`)

2. **Navigate to Profile Tab**
   - Click on the **"Profile"** tab in the navigation bar (user icon)

3. **Fill Out the Form**
   - **Risk Tolerance**: Select CONSERVATIVE, MODERATE, or AGGRESSIVE
   - **Investment Horizon**: Select SHORT, MEDIUM, or LONG
   - **Investment Goals**: Click to select one or more goals (RETIREMENT, GROWTH, INCOME, etc.)
   - **Budget**: Enter your investment budget (e.g., 10000)
   - **Preferred Sectors**: Click sectors you want to invest in (green buttons)
   - **Excluded Sectors**: Click sectors you want to avoid (red buttons)
   - **Ethical Investing**: Check if you prefer ESG investments

4. **Save Your Profile**
   - Click the **"Save Profile"** button at the bottom
   - You'll see a success message when saved

## What Happens

- The form automatically tries to **update** your existing profile first
- If no profile exists, it **creates** a new one
- Your profile is saved to the database and used by the AI agents for personalized recommendations

## API Endpoints

- **GET** `/api/profile/{userId}` - Get your profile
- **POST** `/api/profile` - Create a new profile
- **PUT** `/api/profile/{userId}` - Update existing profile

## Example Request

```json
{
  "userId": "user-123",
  "riskTolerance": "MODERATE",
  "horizon": "MEDIUM",
  "goals": ["RETIREMENT", "GROWTH"],
  "budget": 10000,
  "preferredSectors": ["Technology", "Healthcare"],
  "excludedSectors": ["Energy"],
  "ethicalInvesting": true
}
```

## Notes

- Your `userId` is automatically generated and stored in browser localStorage
- You can update your profile anytime by going back to the Profile tab
- The AI agents use your profile to provide personalized investment advice

