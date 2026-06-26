import React, { useState } from 'react';
import { useAuth } from '../AuthContext';
import './SyllabusPage.css';

import API_BASE from '../config';

const SyllabusPage = ({ subject, onSyllabusReady, onBack, provider, model, apiKey }) => {
  const { api } = useAuth();
  const [mode, setMode] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadContent, setUploadContent] = useState('');

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.post(`${API_BASE}/api/syllabus/generate`, {
        subjectId: subject.id,
        provider: provider || 'ollama',
        model: model || 'llama3.2:3b',
        apiKey: apiKey || ''
      });
      onSyllabusReady(res.data.id);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to generate syllabus');
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async () => {
    if (!uploadTitle.trim() || !uploadContent.trim()) {
      setError('Please provide both title and content');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await api.post(`${API_BASE}/api/syllabus/upload`, {
        subjectId: subject.id,
        title: uploadTitle,
        content: uploadContent
      });
      onSyllabusReady(res.data.id);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to upload syllabus');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="syllabus-page">
      <button className="back-btn" onClick={onBack}>← Back to subjects</button>
      <div className="syllabus-header">
        <span className="subject-icon-large">{subject.icon}</span>
        <h1>{subject.name}</h1>
        <p>Set up your learning syllabus</p>
      </div>
      {!mode ? (
        <div className="syllabus-options">
          <div className="syllabus-option" onClick={() => setMode('generate')}>
            <span className="option-icon">🤖</span>
            <h3>Generate with AI</h3>
            <p>Let AI create a structured syllabus for {subject.name} automatically</p>
          </div>
          <div className="syllabus-option" onClick={() => setMode('upload')}>
            <span className="option-icon">📄</span>
            <h3>Upload Syllabus</h3>
            <p>Paste your own syllabus content in structured format</p>
          </div>
        </div>
      ) : mode === 'generate' ? (
        <div className="syllabus-action">
          <p className="action-info">The AI will create 3-5 chapters with sections and concepts for {subject.name}.</p>
          <button className="generate-btn" onClick={handleGenerate} disabled={loading}>
            {loading ? <><span className="btn-spinner" /> Generating...</> : 'Generate Syllabus'}
          </button>
          {error && <div className="error-msg">{error}</div>}
          <button className="link-btn" onClick={() => setMode(null)}>Choose different option</button>
        </div>
      ) : (
        <div className="syllabus-action">
          <div className="upload-field">
            <label>Syllabus Title</label>
            <input type="text" value={uploadTitle} onChange={e => setUploadTitle(e.target.value)}
              placeholder="e.g., Introduction to Machine Learning" />
          </div>
          <div className="upload-field">
            <label>Syllabus Content</label>
            <textarea value={uploadContent} onChange={e => setUploadContent(e.target.value)}
              placeholder={`Paste your syllabus here. Use format like:\nChapter 1: Title\n  Section 1.1: Title\n    Concept: Title`} rows={12} />
          </div>
          <button className="generate-btn" onClick={handleUpload} disabled={loading}>
            {loading ? 'Uploading...' : 'Upload Syllabus'}
          </button>
          {error && <div className="error-msg">{error}</div>}
          <button className="link-btn" onClick={() => setMode(null)}>Choose different option</button>
        </div>
      )}
    </div>
  );
};

export default SyllabusPage;
