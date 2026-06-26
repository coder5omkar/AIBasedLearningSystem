import React from 'react';
import './DeleteConfirmationModal.css';

const DeleteConfirmationModal = ({ 
  isOpen, 
  onConfirm, 
  onCancel, 
  isDeleting,
  sessionName 
}) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-icon">🗑️</div>
          <h2>Delete Conversation</h2>
        </div>
        
        <div className="modal-body">
          <p>
            Are you sure you want to delete this conversation?
          </p>
          {sessionName && (
            <p className="session-name-highlight">
              "{sessionName}"
            </p>
          )}
          <p className="modal-warning">
            This action cannot be undone. All messages in this conversation will be permanently deleted.
          </p>
        </div>
        
        <div className="modal-footer">
          <button 
            className="modal-btn modal-btn-cancel" 
            onClick={onCancel}
            disabled={isDeleting}
          >
            Cancel
          </button>
          <button 
            className="modal-btn modal-btn-delete" 
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? (
              <>
                <span className="spinner-small"></span>
                Deleting...
              </>
            ) : (
              'Delete'
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeleteConfirmationModal;