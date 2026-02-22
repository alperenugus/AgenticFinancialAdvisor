import { useState, useEffect, useRef } from 'react';
import {
  Brain,
  Wrench,
  CheckCircle,
  Sparkles,
  GitBranch,
  BarChart3,
  MessageCircle,
  FileSearch,
  ShieldAlert,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import websocketService from '../services/websocket';

const AGENT_ICONS = {
  planner: Brain,
  orchestrator: GitBranch,
  evaluator: FileSearch,
  agent_step: BarChart3,
  tool_call: Wrench,
  tool_result: CheckCircle,
  query_start: MessageCircle,
  security: ShieldAlert,
};

const AGENT_COLORS = {
  planner: 'from-violet-500 to-purple-600',
  orchestrator: 'from-blue-500 to-indigo-600',
  evaluator: 'from-amber-500 to-orange-600',
  agent_step: 'from-emerald-500 to-teal-600',
  tool_call: 'from-sky-500 to-cyan-600',
  tool_result: 'from-green-500 to-emerald-600',
  query_start: 'from-slate-500 to-gray-600',
  security: 'from-red-500 to-rose-600',
};

const AGENT_BG = {
  planner: 'bg-violet-50 border-violet-200',
  orchestrator: 'bg-blue-50 border-blue-200',
  evaluator: 'bg-amber-50 border-amber-200',
  agent_step: 'bg-emerald-50 border-emerald-200',
  tool_call: 'bg-sky-50 border-sky-200',
  tool_result: 'bg-green-50 border-green-200',
  query_start: 'bg-slate-50 border-slate-200',
  security: 'bg-red-50 border-red-200',
};

const AgentThinkingPanel = ({ sessionId, clearTrigger }) => {
  const [events, setEvents] = useState([]);
  const [expandedIds, setExpandedIds] = useState(new Set());
  const panelEndRef = useRef(null);

  // Clear events when user sends a new query
  useEffect(() => {
    if (clearTrigger) {
      setEvents([]);
      setExpandedIds(new Set());
    }
  }, [clearTrigger]);

  useEffect(() => {
    if (!sessionId) return;

    const checkConnection = setInterval(() => {
      if (websocketService.isConnected()) {
        clearInterval(checkConnection);
        setupSubscription();
      }
    }, 100);

    const setupSubscription = () => {
      const sub = websocketService.subscribe(`/topic/agent-activity/${sessionId}`, (message) => {
        try {
          const body = typeof message.body === 'string' ? message.body : JSON.stringify(message.body);
          const data = JSON.parse(body);
          const event = {
            id: `evt-${data.timestamp || Date.now()}-${Math.random().toString(36).slice(2)}`,
            type: data.type || 'unknown',
            content: data.content || '',
            timestamp: new Date(data.timestamp || Date.now()),
            ...data,
          };
          setEvents((prev) => [...prev, event]);
        } catch (e) {
          console.error('[AgentThinkingPanel] Parse error:', e, message);
        }
      });

      if (websocketService.isConnected() && !sub) {
        clearInterval(checkConnection);
      }
    };

    if (websocketService.isConnected()) {
      setupSubscription();
      clearInterval(checkConnection);
    }

    return () => {
      clearInterval(checkConnection);
      websocketService.unsubscribe(`/topic/agent-activity/${sessionId}`);
    };
  }, [sessionId]);

  useEffect(() => {
    panelEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  const toggleExpand = (id) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const formatToolName = (name) => {
    if (!name) return '';
    return name
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const formatParams = (params) => {
    if (!params || typeof params !== 'object') return null;
    const entries = Object.entries(params).filter(([k]) => k !== 'timestamp' && k !== 'type' && k !== 'content');
    if (entries.length === 0) return null;
    return entries
      .map(([k, v]) => {
        const val = typeof v === 'string' && v.length > 40 ? v.slice(0, 37) + '...' : String(v);
        return `${k}: ${val}`;
      })
      .join(', ');
  };

  const getAgentLabel = (evt) => {
    switch (evt.type) {
      case 'planner':
        return 'Planner';
      case 'orchestrator':
        return 'Orchestrator';
      case 'evaluator':
        return 'Evaluator';
      case 'agent_step':
        return evt.agent || 'Agent';
      case 'tool_call':
        return formatToolName(evt.toolName) || 'Tool';
      case 'tool_result':
        return formatToolName(evt.toolName) + ' ✓' || 'Tool';
      case 'query_start':
        return 'Query';
      case 'security':
        return 'Security';
      default:
        return evt.type;
    }
  };

  return (
    <div className="h-full flex flex-col bg-white border-l border-gray-200">
      <div className="px-4 py-3 border-b border-gray-200 bg-gradient-to-r from-slate-50 to-gray-50">
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-primary-100 rounded-lg">
            <Brain className="w-4 h-4 text-primary-600" />
          </div>
          <div>
            <h3 className="font-semibold text-sm text-gray-900">Agent Thinking</h3>
            <p className="text-xs text-gray-500">Planner → Orchestrator → Agents → Evaluator</p>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {events.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <Sparkles className="w-10 h-10 mx-auto mb-3 opacity-50" />
            <p className="text-sm font-medium">Agent activity will appear here</p>
            <p className="text-xs mt-1">Ask a question to see the planning and execution flow</p>
          </div>
        ) : (
          <div className="space-y-3">
            {events.map((evt) => {
              const Icon = AGENT_ICONS[evt.type] || Brain;
              const colorClass = AGENT_COLORS[evt.type] || 'from-gray-500 to-gray-600';
              const bgClass = AGENT_BG[evt.type] || 'bg-gray-50 border-gray-200';
              const isExpanded = expandedIds.has(evt.id);
              const hasDetails =
                evt.plan ||
                evt.result ||
                evt.response ||
                evt.feedback ||
                evt.parameters ||
                (evt.type === 'agent_step' && (evt.task || evt.result));

              return (
                <div
                  key={evt.id}
                  className={`rounded-xl border p-3 transition-all ${bgClass}`}
                >
                  <div
                    className={`flex items-start gap-3 ${hasDetails ? 'cursor-pointer' : ''}`}
                    onClick={() => hasDetails && toggleExpand(evt.id)}
                  >
                    <div
                      className={`p-1.5 rounded-lg bg-gradient-to-br ${colorClass} flex-shrink-0`}
                    >
                      <Icon className="w-4 h-4 text-white" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-gray-700 uppercase tracking-wide">
                          {getAgentLabel(evt)}
                        </span>
                        {evt.status && (
                          <span
                            className={`text-xs px-1.5 py-0.5 rounded ${
                              evt.status === 'completed'
                                ? 'bg-green-100 text-green-700'
                                : evt.status === 'started'
                                ? 'bg-blue-100 text-blue-700'
                                : evt.status === 'timeout' || evt.status === 'error'
                                ? 'bg-red-100 text-red-700'
                                : 'bg-gray-100 text-gray-600'
                            }`}
                          >
                            {evt.status}
                          </span>
                        )}
                        {hasDetails && (
                          <span className="text-gray-400">
                            {isExpanded ? (
                              <ChevronDown className="w-4 h-4" />
                            ) : (
                              <ChevronRight className="w-4 h-4" />
                            )}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-800 mt-1">{evt.content}</p>
                      {evt.duration != null && evt.type === 'tool_result' && (
                        <p className="text-xs text-gray-500 mt-0.5">{evt.duration}ms</p>
                      )}
                    </div>
                  </div>

                  {isExpanded && hasDetails && (
                    <div className="mt-3 pt-3 border-t border-gray-200/80 space-y-2">
                      {evt.plan && (
                        <div>
                          <p className="text-xs font-medium text-gray-600 mb-1">Plan</p>
                          <pre className="text-xs bg-white/80 rounded-lg p-2 overflow-x-auto max-h-32 overflow-y-auto border border-gray-100">
                            {evt.plan}
                          </pre>
                        </div>
                      )}
                      {evt.task && (
                        <p className="text-xs text-gray-600">
                          <span className="font-medium">Task:</span> {evt.task}
                        </p>
                      )}
                      {evt.parameters && Object.keys(evt.parameters).length > 0 && (
                        <p className="text-xs text-gray-600">
                          <span className="font-medium">Params:</span>{' '}
                          {formatParams(evt.parameters)}
                        </p>
                      )}
                      {evt.result && (
                        <div>
                          <p className="text-xs font-medium text-gray-600 mb-1">Result</p>
                          <p className="text-xs bg-white/80 rounded p-2 max-h-24 overflow-y-auto border border-gray-100">
                            {typeof evt.result === 'string' && evt.result.length > 300
                              ? evt.result.slice(0, 297) + '...'
                              : String(evt.result)}
                          </p>
                        </div>
                      )}
                      {evt.response && (
                        <p className="text-xs text-gray-600 line-clamp-3">{evt.response}</p>
                      )}
                      {evt.feedback && (
                        <p className="text-xs text-amber-700 bg-amber-50 rounded p-2">{evt.feedback}</p>
                      )}
                    </div>
                  )}
                  <p className="text-xs text-gray-400 mt-2">
                    {evt.timestamp.toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                    })}
                  </p>
                </div>
              );
            })}
            <div ref={panelEndRef} />
          </div>
        )}
      </div>
    </div>
  );
};

export default AgentThinkingPanel;
