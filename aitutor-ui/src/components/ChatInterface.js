import React, { useState, useEffect, useRef } from 'react';
import './ChatInterface.css';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import { useAuth } from '../AuthContext';

import API_BASE from '../config';

const ChatInterface = ({ sessionId, provider, model, apiKey, onMessageSent }) => {
  const { api } = useAuth();
  const [messages, setMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (sessionId) {
      loadHistory();
    }
  }, [sessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadHistory = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await api.get(`${API_BASE}/api/chat/history?sessionId=${sessionId}`);

      const historyMessages = response.data.map((msg, index) => ({
        id: index,
        text: msg.text || msg.content || msg.message || '',
        sender: msg.messageType === 'user' || msg.role === 'user' ? 'user' : 'bot',
        timestamp: new Date().toLocaleTimeString()
      }));

      setMessages(historyMessages);
    } catch (err) {
      console.error('Error loading history:', err);
      setError('Failed to load conversation history');
    } finally {
      setIsLoading(false);
    }
  };

  const sendMessage = async (text) => {
    if (!text.trim()) return;

    const userMessage = {
      id: Date.now(),
      text: text,
      sender: 'user',
      timestamp: new Date().toLocaleTimeString()
    };
    setMessages(prev => [...prev, userMessage]);

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.post(
        `${API_BASE}/api/chat?sessionId=${sessionId}`,
        {
          message: text,
          provider: provider || 'ollama',
          model: model || 'llama3.2:3b',
          apiKey: apiKey || ''
        }
      );

      const botMessage = {
        id: Date.now() + 1,
        text: response.data.response,
        sender: 'bot',
        timestamp: new Date().toLocaleTimeString()
      };
      setMessages(prev => [...prev, botMessage]);
      if (onMessageSent) onMessageSent();
    } catch (err) {
      console.error('Error sending message:', err);
      setError('Failed to get response. Please try again.');
      setMessages(prev => prev.filter(msg => msg.id !== userMessage.id));
    } finally {
      setIsLoading(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="chat-interface">
      {error && (
        <div className="error-banner">
          {error}
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}
      <MessageList messages={messages} isLoading={isLoading} />
      <div ref={messagesEndRef} />
      <MessageInput onSend={sendMessage} disabled={isLoading} />
    </div>
  );
};

export default ChatInterface;
