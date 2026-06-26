import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import './MessageList.css';

const MessageList = ({ messages, isLoading }) => {
  if (!messages || messages.length === 0) {
    return (
      <div className="message-list empty">
        <div className="empty-state">
          <div className="empty-icon">💬</div>
          <h3>Start a Conversation</h3>
          <p>Say hello to your AI tutor!</p>
        </div>
      </div>
    );
  }

  return (
    <div className="message-list">
      {messages.map((message) => (
        <div
          key={message.id}
          className={`message ${message.sender === 'user' ? 'user' : 'bot'}`}
        >
          <div className="message-avatar">
            {message.sender === 'user' ? '👤' : '🤖'}
          </div>
          <div className="message-content">
            <div className="message-text">
              {message.sender === 'bot' ? (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  rehypePlugins={[rehypeRaw]}
                  components={{
                    // Custom styling for markdown elements
                    p: ({ children }) => <p className="markdown-p">{children}</p>,
                    strong: ({ children }) => <strong className="markdown-strong">{children}</strong>,
                    b: ({ children }) => <strong className="markdown-strong">{children}</strong>,
                    em: ({ children }) => <em className="markdown-em">{children}</em>,
                    i: ({ children }) => <em className="markdown-em">{children}</em>,
                    h1: ({ children }) => <h1 className="markdown-h1">{children}</h1>,
                    h2: ({ children }) => <h2 className="markdown-h2">{children}</h2>,
                    h3: ({ children }) => <h3 className="markdown-h3">{children}</h3>,
                    h4: ({ children }) => <h4 className="markdown-h4">{children}</h4>,
                    ul: ({ children }) => <ul className="markdown-ul">{children}</ul>,
                    ol: ({ children }) => <ol className="markdown-ol">{children}</ol>,
                    li: ({ children }) => <li className="markdown-li">{children}</li>,
                    code: ({ children, inline }) => 
                      inline ? 
                        <code className="markdown-code-inline">{children}</code> :
                        <code className="markdown-code-block">{children}</code>,
                    pre: ({ children }) => <pre className="markdown-pre">{children}</pre>,
                    blockquote: ({ children }) => <blockquote className="markdown-blockquote">{children}</blockquote>,
                    a: ({ href, children }) => <a href={href} className="markdown-link" target="_blank" rel="noopener noreferrer">{children}</a>,
                    img: ({ src, alt }) => <img src={src} alt={alt} className="markdown-image" />,
                    hr: () => <hr className="markdown-hr" />,
                    table: ({ children }) => <table className="markdown-table">{children}</table>,
                    thead: ({ children }) => <thead className="markdown-thead">{children}</thead>,
                    tbody: ({ children }) => <tbody className="markdown-tbody">{children}</tbody>,
                    tr: ({ children }) => <tr className="markdown-tr">{children}</tr>,
                    th: ({ children }) => <th className="markdown-th">{children}</th>,
                    td: ({ children }) => <td className="markdown-td">{children}</td>,
                  }}
                >
                  {message.text}
                </ReactMarkdown>
              ) : (
                <span className="user-message-text">{message.text}</span>
              )}
            </div>
            <div className="message-time">{message.timestamp}</div>
          </div>
        </div>
      ))}
      {isLoading && (
        <div className="message bot">
          <div className="message-avatar">🤖</div>
          <div className="message-content">
            <div className="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MessageList;