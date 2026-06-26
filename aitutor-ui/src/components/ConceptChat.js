import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import './ConceptChat.css';

import API_BASE from '../config';

const ConceptChat = ({ conceptId, conceptTitle, provider, model, apiKey }) => {
  const { api } = useAuth();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef(null);

  useEffect(() => {
    setMessages([]);
  }, [conceptId]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || sending) return;
    const q = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: q }]);
    setSending(true);
    try {
      const res = await api.post(`${API_BASE}/api/chat/ask-doubt`, {
        conceptId,
        question: q,
        provider,
        model,
        apiKey
      });
      setMessages(prev => [...prev, { role: 'assistant', content: res.data.response }]);
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Sorry, I encountered an error. Please try again.' }]);
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const renderContent = (content) => {
    return (
      <ReactMarkdown rehypePlugins={[rehypeRaw]} remarkPlugins={[remarkGfm]}>
        {content}
      </ReactMarkdown>
    );
  };

  return (
    <div className="concept-chat">
      <div className="concept-chat-header">
        <span className="chat-header-label">💬 Ask a doubt about <strong>{conceptTitle || 'this concept'}</strong></span>
      </div>
      <div className="concept-chat-messages">
        {messages.length === 0 && (
          <div className="chat-empty">Ask a question about this concept to get help from the AI tutor.</div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`chat-bubble ${m.role}`}>
            <div className={`chat-bubble-content ${m.role}`}>{renderContent(m.content)}</div>
          </div>
        ))}
        {sending && <div className="chat-bubble assistant"><div className="chat-bubble-content assistant"><span className="typing-dots">Thinking</span></div></div>}
        <div ref={endRef} />
      </div>
      <div className="concept-chat-input">
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask a doubt about this concept..."
          rows={2}
          disabled={sending}
        />
        <button onClick={handleSend} disabled={!input.trim() || sending} className="chat-send-btn">Send</button>
      </div>
    </div>
  );
};

export default ConceptChat;
