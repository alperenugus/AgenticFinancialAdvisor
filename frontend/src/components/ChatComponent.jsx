import { useState, useEffect, useRef } from 'react';
import { Send, Bot, User, Loader2 } from 'lucide-react';
import { advisorAPI } from '../services/api';
import websocketService from '../services/websocket';

const ChatComponent = ({ userId }) => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId] = useState(() => `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
  const messagesEndRef = useRef(null);
  const [thinkingMessages, setThinkingMessages] = useState([]);

  useEffect(() => {
    // Connect WebSocket
    websocketService.connect(sessionId, {
      onThinking: (data) => {
        setThinkingMessages((prev) => [...prev, data.content]);
      },
      onResponse: (data) => {
        setThinkingMessages([]);
        addMessage('assistant', data.content);
        setIsLoading(false);
      },
      onError: (data) => {
        setThinkingMessages([]);
        addMessage('error', data.content);
        setIsLoading(false);
      },
    });

    // Initial greeting
    addMessage('assistant', "Hello! I'm your AI financial advisor. How can I help you today?");

    return () => {
      websocketService.disconnect();
    };
  }, [sessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, thinkingMessages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const addMessage = (role, content) => {
    setMessages((prev) => [...prev, { role, content, timestamp: new Date() }]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMessage = input.trim();
    setInput('');
    addMessage('user', userMessage);
    setIsLoading(true);
    setThinkingMessages([]);

    try {
      const response = await advisorAPI.analyze(userId, userMessage, sessionId);
      
      if (response.data.status === 'success') {
        // Response will come through WebSocket
        // If WebSocket fails, fallback to HTTP response
        if (!websocketService.isConnected()) {
          addMessage('assistant', response.data.response);
          setIsLoading(false);
        }
      } else {
        addMessage('error', response.data.message || 'An error occurred');
        setIsLoading(false);
      }
    } catch (error) {
      console.error('Error sending message:', error);
      addMessage('error', error.response?.data?.message || 'Failed to get response. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-full bg-gray-50 dark:bg-gray-900">
      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((msg, idx) => (
          <div
            key={idx}
            className={`flex items-start gap-3 ${
              msg.role === 'user' ? 'justify-end' : 'justify-start'
            }`}
          >
            {msg.role !== 'user' && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
            )}
            <div
              className={`max-w-[80%] rounded-lg px-4 py-2 ${
                msg.role === 'user'
                  ? 'bg-primary-600 text-white'
                  : msg.role === 'error'
                  ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                  : 'bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 shadow-sm'
              }`}
            >
              <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
            </div>
            {msg.role === 'user' && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-300 dark:bg-gray-700 flex items-center justify-center">
                <User className="w-5 h-5 text-gray-700 dark:text-gray-300" />
              </div>
            )}
          </div>
        ))}

        {/* Thinking messages */}
        {thinkingMessages.length > 0 && (
          <div className="flex items-start gap-3 justify-start">
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center">
              <Loader2 className="w-5 h-5 text-white animate-spin" />
            </div>
            <div className="max-w-[80%] rounded-lg px-4 py-2 bg-blue-50 dark:bg-blue-900 text-blue-800 dark:text-blue-200 shadow-sm">
              <p className="text-sm font-medium">Thinking...</p>
              <ul className="text-xs mt-1 space-y-1">
                {thinkingMessages.map((msg, idx) => (
                  <li key={idx}>• {msg}</li>
                ))}
              </ul>
            </div>
          </div>
        )}

        {/* Loading indicator */}
        {isLoading && thinkingMessages.length === 0 && (
          <div className="flex items-start gap-3 justify-start">
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center">
              <Loader2 className="w-5 h-5 text-white animate-spin" />
            </div>
            <div className="max-w-[80%] rounded-lg px-4 py-2 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 shadow-sm">
              <p className="text-sm">Processing your request...</p>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSubmit} className="border-t border-gray-200 dark:border-gray-700 p-4">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about stocks, portfolio, or investment advice..."
            className="flex-1 input-field"
            disabled={isLoading}
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            Send
          </button>
        </div>
      </form>
    </div>
  );
};

export default ChatComponent;

