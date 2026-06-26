import React, { useState, useCallback, useEffect } from 'react';
import './App.css';
import ModelSelector from './components/ModelSelector';
import SubjectSelectionPage from './components/SubjectSelectionPage';
import SyllabusPage from './components/SyllabusPage';
import LearningPage from './components/LearningPage';
import LoginPage from './components/LoginPage';
import LogPage from './components/LogPage';
import { useAuth } from './AuthContext';

import API_BASE from './config';
const STORAGE_KEY = 'aitutor_prefs';

function App() {
  const { isAuthenticated, username, logout: authLogout, api } = useAuth();

  const [provider, setProvider] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved).provider || 'ollama' : 'ollama';
  });
  const [model, setModel] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved).model || 'llama3.2:3b' : 'llama3.2:3b';
  });
  const [apiKey, setApiKey] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved).apiKey || '' : '';
  });

  const savePrefs = useCallback((p, m, k) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ provider: p, model: m, apiKey: k }));
  }, []);

  const handleProviderChange = (p) => { setProvider(p); savePrefs(p, model, apiKey); };
  const handleModelChange = (m) => { setModel(m); savePrefs(provider, m, apiKey); };
  const handleApiKeyChange = (k) => { setApiKey(k); savePrefs(provider, model, k); };

  const [page, setPage] = useState('subjects');
  const [selectedSubject, setSelectedSubject] = useState(null);
  const [syllabusId, setSyllabusId] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);

  useEffect(() => {
    if (isAuthenticated) {
      checkActiveSyllabus();
    } else {
      setInitialLoading(false);
    }
  }, [isAuthenticated]);

  const checkActiveSyllabus = async () => {
    try {
      const res = await api.get(`${API_BASE}/api/syllabus/active`);
      if (res.status === 200 && res.data && res.data.id) {
        const subRes = await api.get(`${API_BASE}/api/subjects/${res.data.subjectId}`);
        setSelectedSubject(subRes.data);
        setSyllabusId(res.data.id);
        setPage('learning');
      } else {
        setPage('subjects');
      }
    } catch {
      setPage('subjects');
    } finally {
      setInitialLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setProvider('ollama');
    setModel('llama3.2:3b');
    setApiKey('');
    setPage('subjects');
    setSelectedSubject(null);
    setSyllabusId(null);
    authLogout();
  };

  const handleBackToSubjects = () => {
    setPage('subjects');
    setSelectedSubject(null);
    setSyllabusId(null);
  };

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  if (initialLoading) {
    return (
      <div className="app"><div className="initial-loading"><div className="spinner" /><p>Loading your learning journey...</p></div></div>
    );
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <div className="header-left">
            <div className="logo">
              <span className="logo-icon">📚</span>
              <h1>AI Tutor</h1>
              {username && <span className="header-username">{username}</span>}
            </div>
            {page === 'learning' && selectedSubject && (
              <div className="header-subject-info">
                <span className="header-subj-icon">{selectedSubject.icon}</span>
                <span className="header-subj-name">{selectedSubject.name}</span>
              </div>
            )}
          </div>
          <div className="header-controls">
            <ModelSelector
              provider={provider}
              model={model}
              apiKey={apiKey}
              onProviderChange={handleProviderChange}
              onModelChange={handleModelChange}
              onApiKeyChange={handleApiKeyChange}
            />
            {page === 'learning' && (
              <button onClick={handleBackToSubjects} className="header-action-btn">Subjects</button>
            )}
            <button onClick={() => setPage('logs')} className="header-action-btn">Logs</button>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </div>
      </header>
      <div className="app-body">
        {page === 'subjects' && (
          <SubjectSelectionPage
            onSubjectSelect={(subject) => { setSelectedSubject(subject); setPage('syllabus'); }}
          />
        )}
        {page === 'syllabus' && (
          <SyllabusPage
            subject={selectedSubject}
            provider={provider}
            model={model}
            apiKey={apiKey}
            onSyllabusReady={(id) => { setSyllabusId(id); setPage('learning'); }}
            onBack={() => setPage('subjects')}
          />
        )}
        {page === 'learning' && (
          <LearningPage
            syllabusId={syllabusId}
            subject={selectedSubject}
            provider={provider}
            model={model}
            apiKey={apiKey}
          />
        )}
        {page === 'logs' && (
          <LogPage onBack={() => setPage('subjects')} />
        )}
      </div>
    </div>
  );
}

export default App;
