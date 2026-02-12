import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
  }

  connect(sessionId, callbacks = {}) {
    const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
    
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.connected = true;
        console.log('WebSocket connected');
        
        if (callbacks.onConnect) {
          callbacks.onConnect();
        }

        // Subscribe to thinking updates
        if (sessionId) {
          this.subscribe(`/topic/thinking/${sessionId}`, (message) => {
            const data = JSON.parse(message.body);
            if (callbacks.onThinking) {
              callbacks.onThinking(data);
            }
          });

          // Subscribe to responses
          this.subscribe(`/topic/response/${sessionId}`, (message) => {
            const data = JSON.parse(message.body);
            if (callbacks.onResponse) {
              callbacks.onResponse(data);
            }
          });

          // Subscribe to errors
          this.subscribe(`/topic/error/${sessionId}`, (message) => {
            const data = JSON.parse(message.body);
            if (callbacks.onError) {
              callbacks.onError(data);
            }
          });
        }
      },
      onDisconnect: () => {
        this.connected = false;
        console.log('WebSocket disconnected');
        if (callbacks.onDisconnect) {
          callbacks.onDisconnect();
        }
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        if (callbacks.onError) {
          callbacks.onError({ type: 'error', content: frame.headers['message'] || 'WebSocket error' });
        }
      },
    });

    this.client.activate();
  }

  subscribe(topic, callback) {
    if (!this.client || !this.connected) {
      console.warn('Cannot subscribe: WebSocket not connected');
      return;
    }

    const subscription = this.client.subscribe(topic, callback);
    this.subscriptions.set(topic, subscription);
    return subscription;
  }

  unsubscribe(topic) {
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  isConnected() {
    return this.connected;
  }
}

export default new WebSocketService();

