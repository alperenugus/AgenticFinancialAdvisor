import { useState, useEffect, useRef } from 'react';
import { Brain, Wrench, CheckCircle, XCircle, Clock, Sparkles } from 'lucide-react';
import websocketService from '../services/websocket';

const AgentThinkingPanel = ({ sessionId }) => {
  const [reasoningSteps, setReasoningSteps] = useState([]);
  const [toolCalls, setToolCalls] = useState([]);
  const panelEndRef = useRef(null);

  useEffect(() => {
    if (!sessionId) return;

    // Wait for WebSocket to be connected
    const checkConnection = setInterval(() => {
      if (websocketService.isConnected()) {
        clearInterval(checkConnection);
        setupSubscriptions();
      }
    }, 100);

    const setupSubscriptions = () => {
      console.log('🔌 [AgentThinkingPanel] Setting up WebSocket subscriptions for session:', sessionId);
      
      // Subscribe to reasoning updates
      const reasoningSub = websocketService.subscribe(`/topic/reasoning/${sessionId}`, (message) => {
        console.log('📨 [AgentThinkingPanel] Received reasoning message:', message.body);
        try {
          const data = JSON.parse(message.body);
          setReasoningSteps((prev) => [...prev, {
            type: 'reasoning',
            content: data.content,
            timestamp: new Date(data.timestamp || Date.now()),
          }]);
        } catch (e) {
          console.error('❌ [AgentThinkingPanel] Error parsing reasoning message:', e, message.body);
        }
      });
      console.log('✅ [AgentThinkingPanel] Subscribed to reasoning:', reasoningSub ? 'success' : 'failed');

      // Subscribe to tool calls
      const toolCallSub = websocketService.subscribe(`/topic/tool-call/${sessionId}`, (message) => {
        console.log('🔧 [AgentThinkingPanel] Received tool call message:', message.body);
        try {
          const data = JSON.parse(message.body);
          setToolCalls((prev) => [...prev, {
            id: `tool-${data.timestamp}-${Math.random()}`,
            toolName: data.toolName,
            parameters: data.parameters,
            status: 'calling',
            timestamp: new Date(data.timestamp),
          }]);
        } catch (e) {
          console.error('❌ [AgentThinkingPanel] Error parsing tool call message:', e, message.body);
        }
      });
      console.log('✅ [AgentThinkingPanel] Subscribed to tool-call:', toolCallSub ? 'success' : 'failed');

      // Subscribe to tool results
      const toolResultSub = websocketService.subscribe(`/topic/tool-result/${sessionId}`, (message) => {
        console.log('✅ [AgentThinkingPanel] Received tool result message:', message.body);
        try {
          const data = JSON.parse(message.body);
          setToolCalls((prev) => prev.map((call) => 
            call.toolName === data.toolName && call.status === 'calling'
              ? { ...call, status: 'completed', result: data.result, duration: data.duration }
              : call
          ));
        } catch (e) {
          console.error('❌ [AgentThinkingPanel] Error parsing tool result message:', e, message.body);
        }
      });
      console.log('✅ [AgentThinkingPanel] Subscribed to tool-result:', toolResultSub ? 'success' : 'failed');
    };

    // If already connected, set up subscriptions immediately
    if (websocketService.isConnected()) {
      setupSubscriptions();
      clearInterval(checkConnection);
    }

    return () => {
      clearInterval(checkConnection);
      websocketService.unsubscribe(`/topic/reasoning/${sessionId}`);
      websocketService.unsubscribe(`/topic/tool-call/${sessionId}`);
      websocketService.unsubscribe(`/topic/tool-result/${sessionId}`);
    };
  }, [sessionId]);

  useEffect(() => {
    panelEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [reasoningSteps, toolCalls]);

  const formatToolName = (toolName) => {
    return toolName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const formatParameters = (params) => {
    if (!params || Object.keys(params).length === 0) return '';
    return Object.entries(params)
      .map(([key, value]) => {
        const val = typeof value === 'string' && value.length > 30 
          ? value.substring(0, 27) + '...' 
          : String(value);
        return `${key}: ${val}`;
      })
      .join(', ');
  };

  const allSteps = [
    ...reasoningSteps.map((step, idx) => ({ ...step, id: `reasoning-${idx}`, order: step.timestamp.getTime() })),
    ...toolCalls.map((call) => ({ ...call, type: 'tool', order: call.timestamp.getTime() })),
  ].sort((a, b) => a.order - b.order);

  return (
    <div className="h-full flex flex-col bg-whitebg-gray-800 border-l border-gray-200border-gray-700">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200border-gray-700 bg-gradient-to-r from-blue-50 to-indigo-50bg-black">
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-primary-100bg-primary-900 rounded-lg">
            <Brain className="w-4 h-4 text-primary-600text-primary-400" />
          </div>
          <div>
            <h3 className="font-semibold text-sm text-gray-900text-white">Agent Thinking</h3>
            <p className="text-xs text-gray-500text-gray-400">Planning & tool usage</p>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {allSteps.length === 0 ? (
          <div className="text-center py-8 text-gray-400text-gray-500">
            <Sparkles className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">Agent thinking will appear here</p>
          </div>
        ) : (
          allSteps.map((step) => (
            <div
              key={step.id}
              className={`rounded-lg p-3 border transition-all ${
                step.type === 'reasoning'
                  ? 'bg-blue-50bg-blue-900/20 border-blue-200border-blue-800'
                  : step.status === 'calling'
                  ? 'bg-yellow-50bg-yellow-900/20 border-yellow-200border-yellow-800'
                  : step.status === 'completed'
                  ? 'bg-green-50bg-green-900/20 border-green-200border-green-800'
                  : 'bg-red-50bg-red-900/20 border-red-200border-red-800'
              }`}
            >
              {step.type === 'reasoning' ? (
                <div className="flex items-start gap-2">
                  <Brain className="w-4 h-4 text-blue-600text-blue-400 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <p className="text-xs font-medium text-blue-900text-blue-200 mb-1">Reasoning</p>
                    <p className="text-xs text-blue-700text-blue-300">{step.content}</p>
                  </div>
                </div>
              ) : (
                <div className="flex items-start gap-2">
                  {step.status === 'calling' ? (
                    <Clock className="w-4 h-4 text-yellow-600text-yellow-400 mt-0.5 flex-shrink-0 animate-spin" />
                  ) : step.status === 'completed' ? (
                    <CheckCircle className="w-4 h-4 text-green-600text-green-400 mt-0.5 flex-shrink-0" />
                  ) : (
                    <XCircle className="w-4 h-4 text-red-600text-red-400 mt-0.5 flex-shrink-0" />
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <Wrench className="w-3 h-3 text-gray-600text-gray-400" />
                      <p className="text-xs font-semibold text-gray-900text-white">
                        {formatToolName(step.toolName)}
                      </p>
                      {step.duration && (
                        <span className="text-xs text-gray-500text-gray-400">
                          ({step.duration}ms)
                        </span>
                      )}
                    </div>
                    {formatParameters(step.parameters) && (
                      <p className="text-xs text-gray-600text-gray-400 mb-1">
                        {formatParameters(step.parameters)}
                      </p>
                    )}
                    {step.status === 'completed' && step.result && (
                      <p className="text-xs text-gray-500text-gray-500 mt-1 italic">
                        Result: {typeof step.result === 'string' && step.result.length > 100
                          ? step.result.substring(0, 97) + '...'
                          : String(step.result)}
                      </p>
                    )}
                  </div>
                </div>
              )}
              <p className="text-xs text-gray-400text-gray-500 mt-1.5">
                {step.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
              </p>
            </div>
          ))
        )}
        <div ref={panelEndRef} />
      </div>
    </div>
  );
};

export default AgentThinkingPanel;

