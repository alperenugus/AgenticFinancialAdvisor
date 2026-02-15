import { useState, useEffect, useRef } from "react";
import { Send, Bot, User, Loader2, Sparkles, AlertCircle } from "lucide-react";
import { advisorAPI } from "../services/api";
import websocketService from "../services/websocket";

const ChatComponent = () => {
  // Get or create sessionId from localStorage
  const getOrCreateSessionId = () => {
    const stored = localStorage.getItem("advisor_sessionId");
    if (stored) {
      return stored;
    }
    const newSessionId = `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem("advisor_sessionId", newSessionId);
    return newSessionId;
  };

  // Load messages from localStorage
  const loadMessages = (sessionId) => {
    try {
      const stored = localStorage.getItem(`advisor_messages_${sessionId}`);
      if (stored) {
        const parsed = JSON.parse(stored);
        // Convert timestamp strings back to Date objects
        return parsed.map((msg) => ({
          ...msg,
          timestamp: new Date(msg.timestamp),
        }));
      }
    } catch (error) {
      console.error("Error loading messages from localStorage:", error);
    }
    return [];
  };

  // Save messages to localStorage
  const saveMessages = (sessionId, messages) => {
    try {
      localStorage.setItem(
        `advisor_messages_${sessionId}`,
        JSON.stringify(messages),
      );
    } catch (error) {
      console.error("Error saving messages to localStorage:", error);
    }
  };

  const [sessionId] = useState(() => getOrCreateSessionId());
  const [messages, setMessages] = useState(() => {
    const loaded = loadMessages(sessionId);
    // Only add initial greeting if no messages exist
    if (loaded.length === 0) {
      return [
        {
          role: "assistant",
          content:
            "Hello! I'm your AI financial advisor, powered by advanced language models. I can help you with:\n\n• Stock analysis and recommendations\n• Portfolio management advice\n• Risk assessment\n• Investment strategy planning\n• Market insights\n\nHow can I assist you with your financial goals today?",
          timestamp: new Date(),
        },
      ];
    }
    return loaded;
  });
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const isInitialMount = useRef(true);
  const previousMessageCount = useRef(0);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const addMessage = (role, content) => {
    setMessages((prev) => [...prev, { role, content, timestamp: new Date() }]);
  };

  // Save messages to localStorage whenever they change
  useEffect(() => {
    saveMessages(sessionId, messages);
  }, [sessionId, messages]);

  useEffect(() => {
    // Connect WebSocket
    websocketService.connect(sessionId, {
      onResponse: (data) => {
        addMessage("assistant", data.content);
        setIsLoading(false);
      },
      onError: (data) => {
        addMessage("error", data.content);
        setIsLoading(false);
      },
    });

    return () => {
      websocketService.disconnect();
    };
  }, [sessionId]);

  useEffect(() => {
    // Skip scrolling on initial mount
    if (isInitialMount.current) {
      isInitialMount.current = false;
      previousMessageCount.current = messages.length;
      return;
    }

    // Only scroll if new messages were added (count increased)
    if (messages.length > previousMessageCount.current) {
      setTimeout(() => {
        scrollToBottom();
      }, 100);
    }

    previousMessageCount.current = messages.length;
  }, [messages.length]);

  const handleSubmit = async (e) => {
    if (e && e.preventDefault) {
      e.preventDefault();
    }
    const messageToSend = input.trim();
    if (!messageToSend || isLoading) return;

    const userMessage = messageToSend.trim();
    setInput("");
    addMessage("user", userMessage);
    setIsLoading(true);

    try {
      const response = await advisorAPI.analyze(userMessage, sessionId);

      if (response.data.status === "success") {
        // Response will come through WebSocket
        // If WebSocket fails, fallback to HTTP response
        if (!websocketService.isConnected()) {
          addMessage("assistant", response.data.response);
          setIsLoading(false);
        }
      } else {
        addMessage("error", response.data.message || "An error occurred");
        setIsLoading(false);
      }
    } catch (error) {
      console.error("Error sending message:", error);
      addMessage(
        "error",
        error.response?.data?.message ||
          "Failed to get response. Please try again.",
      );
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-full bg-gradient-to-b from-white to-gray-50/50 dark:bg-black dark:bg-none">
      {/* Chat Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Chat Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-800 bg-white/80 dark:bg-black/80 backdrop-blur-sm">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl blur opacity-50"></div>
              <div className="relative p-2 bg-gradient-to-br from-primary-600 to-primary-700 rounded-xl">
                <Bot className="w-5 h-5 text-white" />
              </div>
            </div>
            <div>
              <h3 className="font-bold text-gray-900 dark:text-white">
                AI Financial Advisor
              </h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Always here to help
              </p>
            </div>
          </div>
        </div>

        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-gradient-to-b from-transparent to-gray-50/30 dark:bg-black dark:bg-none">
          {messages.map((msg, idx) => (
            <div
              key={idx}
              className={`flex items-start gap-4 ${
                msg.role === "user" ? "justify-end" : "justify-start"
              }`}
            >
              {msg.role !== "user" && (
                <div className="flex-shrink-0 relative">
                  <div className="absolute inset-0 bg-gradient-to-br from-primary-400 to-primary-600 rounded-2xl blur opacity-30"></div>
                  <div className="relative w-10 h-10 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg">
                    {msg.role === "error" ? (
                      <AlertCircle className="w-5 h-5 text-white" />
                    ) : (
                      <Bot className="w-5 h-5 text-white" />
                    )}
                  </div>
                </div>
              )}
              <div
                className={`max-w-[75%] rounded-2xl px-5 py-4 shadow-soft ${
                  msg.role === "user"
                    ? "bg-gradient-to-br from-primary-600 to-primary-700 text-white"
                    : msg.role === "error"
                      ? "bg-gradient-to-br from-danger-50 to-danger-100 dark:bg-black dark:bg-none text-danger-800 dark:text-danger-200 border border-danger-200 dark:border-danger-800"
                      : "bg-white dark:bg-black dark:bg-none text-gray-900 dark:text-gray-100 border border-gray-200 dark:border-gray-800"
                }`}
              >
                <p className="text-sm leading-relaxed whitespace-pre-wrap font-medium">
                  {msg.content}
                </p>
                <p
                  className={`text-xs mt-2 ${
                    msg.role === "user"
                      ? "text-primary-100"
                      : "text-gray-400 dark:text-gray-500"
                  }`}
                >
                  {msg.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </p>
              </div>
              {msg.role === "user" && (
                <div className="flex-shrink-0 w-10 h-10 rounded-2xl bg-gradient-to-br from-gray-400 to-gray-500 flex items-center justify-center shadow-lg">
                  <User className="w-5 h-5 text-white" />
                </div>
              )}
            </div>
          ))}

          {/* Loading indicator */}
          {isLoading && (
            <div className="flex items-start gap-4 justify-start">
              <div className="flex-shrink-0 relative">
                <div className="absolute inset-0 bg-gradient-to-br from-primary-400 to-primary-600 rounded-2xl blur opacity-30 animate-pulse"></div>
                <div className="relative w-10 h-10 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg">
                  <Loader2 className="w-5 h-5 text-white animate-spin" />
                </div>
              </div>
              <div className="max-w-[75%] rounded-2xl px-5 py-4 bg-white dark:bg-black text-gray-900 dark:text-gray-100 border border-gray-200 dark:border-gray-800 shadow-soft">
                <p className="text-sm font-medium">
                  Processing your request...
                </p>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Professional Input Area */}
        <form
          onSubmit={handleSubmit}
          className="border-t border-gray-200 dark:border-gray-800 p-5 bg-white/80 dark:bg-black/80 backdrop-blur-sm"
        >
          <div className="flex gap-3">
            <div className="flex-1 relative">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about stocks, portfolio analysis, investment strategies..."
                className="input-field pr-12"
                disabled={isLoading}
              />
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <Sparkles className="w-5 h-5 text-gray-400" />
              </div>
            </div>
            <button
              type="submit"
              disabled={isLoading || !input.trim()}
              className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 min-w-[100px] justify-center"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Sending</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>Send</span>
                </>
              )}
            </button>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-2 ml-1">
            Press Enter to send • AI responses may take a few moments
          </p>
        </form>
      </div>
    </div>
  );
};

export default ChatComponent;
