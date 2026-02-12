# Agentic Financial Advisor - Frontend

React + Vite frontend for the Agentic Financial Advisor application.

## Features

- 💬 **Chat Interface** - Interactive chat with AI financial advisor
- 📊 **Portfolio Management** - View and manage your stock portfolio with visualizations
- 📈 **Recommendations** - View AI-generated investment recommendations
- 👤 **User Profile** - Configure your risk tolerance, goals, and preferences
- 🔄 **Real-time Updates** - WebSocket integration for live agent thinking updates

## Tech Stack

- **React 18** - UI framework
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Styling
- **Recharts** - Data visualizations
- **SockJS + STOMP** - WebSocket communication
- **Axios** - HTTP client

## Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure environment variables** (optional):
   Create a `.env` file:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   VITE_WS_URL=http://localhost:8080/ws
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

   The frontend will be available at `http://localhost:5173`

## Build for Production

```bash
npm run build
```

The built files will be in the `dist` directory.

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── ChatComponent.jsx      # Main chat interface
│   │   ├── PortfolioView.jsx      # Portfolio management and charts
│   │   ├── RecommendationCard.jsx # Recommendation display
│   │   └── UserProfileForm.jsx    # User profile configuration
│   ├── services/
│   │   ├── api.js                 # REST API client
│   │   └── websocket.js           # WebSocket service
│   ├── App.jsx                    # Main app component
│   ├── main.jsx                   # Entry point
│   └── index.css                  # Global styles
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## API Integration

The frontend communicates with the backend through:

- **REST API** (`/api/*`) - For user profiles, portfolios, and recommendations
- **WebSocket** (`/ws`) - For real-time agent thinking updates

See the main [README.md](../README.md) for API documentation.

