import React, { useState } from 'react';
import './SessionList.css';
import DeleteConfirmationModal from './DeleteConfirmationModal';
import { useAuth } from '../AuthContext';

import API_BASE from '../config';

const SessionList = ({
  sessions = [],
  currentSession = null,
  onSessionSelect,
  onSessionDelete,
  isLoading = false,
  error = null,
  maxSessions = null
}) => {
  const { api } = useAuth();
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [sessionToDelete, setSessionToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const safeSessions = Array.isArray(sessions) ? sessions : [];
  const displayedSessions = maxSessions ? safeSessions.slice(0, maxSessions) : safeSessions;

  const getSessionDisplayName = (sessionId, index) => {
    if (!sessionId) return 'Unknown Session';
    const sessionNumber = index + 1;
    return `Session ${sessionNumber}`;
  };

  const getSessionPreview = (sessionId) => {
    if (!sessionId) return 'New conversation';
    const parts = sessionId.split('-');
    if (parts.length >= 2) {
      const timestamp = parseInt(parts[1]);
      if (!isNaN(timestamp) && timestamp > 1000000000000) {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
      }
    }
    return 'New conversation';
  };

  const handleDeleteClick = (sessionId, e) => {
    e.stopPropagation();
    setSessionToDelete(sessionId);
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    if (!sessionToDelete) return;
    setIsDeleting(true);
    try {
      await api.delete(`${API_BASE}/api/chat/session/${sessionToDelete}`);
      onSessionDelete(sessionToDelete);
      setShowDeleteModal(false);
      setSessionToDelete(null);
    } catch (err) {
      console.error('Error deleting session:', err);
      alert('Failed to delete session. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleCancelDelete = () => {
    setShowDeleteModal(false);
    setSessionToDelete(null);
  };

  if (isLoading) {
    return (
      <div className="session-list">
        <div className="session-list-header"><h3>Conversations</h3></div>
        <div className="loading-sessions">
          <div className="loading-spinner-small"></div>
          <p>Loading sessions...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="session-list">
        <div className="session-list-header"><h3>Conversations</h3></div>
        <div className="error-state"><p>{error}</p></div>
      </div>
    );
  }

  return (
    <>
      <div className="session-list">
        <div className="session-list-header">
          <h3>Conversations</h3>
          <span className="session-count">{safeSessions.length}</span>
        </div>

        {safeSessions.length === 0 ? (
          <div className="empty-sessions">
            <p>No conversations yet</p>
            <p className="empty-hint">Start a new conversation above</p>
          </div>
        ) : (
          <div className="session-items">
            {displayedSessions.map((sessionId, index) => (
              <div
                key={sessionId || `session-${index}`}
                className={`session-item ${sessionId === currentSession ? 'active' : ''}`}
                onClick={() => onSessionSelect && onSessionSelect(sessionId)}
              >
                <div className="session-info">
                  <span className="session-icon">💬</span>
                  <div className="session-details">
                    <span className="session-name">{getSessionDisplayName(sessionId, index)}</span>
                    <span className="session-preview">{getSessionPreview(sessionId)}</span>
                  </div>
                </div>
                <button
                  className="delete-session-btn"
                  onClick={(e) => handleDeleteClick(sessionId, e)}
                  title="Delete conversation"
                >×</button>
              </div>
            ))}
            {maxSessions && safeSessions.length > maxSessions && (
              <div className="session-more">+{safeSessions.length - maxSessions} more</div>
            )}
          </div>
        )}
      </div>

      <DeleteConfirmationModal
        isOpen={showDeleteModal}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
        isDeleting={isDeleting}
        sessionName={sessionToDelete ? getSessionDisplayName(sessionToDelete, 0) : ''}
      />
    </>
  );
};

export default SessionList;
