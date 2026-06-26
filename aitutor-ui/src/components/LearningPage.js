import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import ConceptChat from './ConceptChat';
import ConceptMCQPanel from './ConceptMCQPanel';
import './LearningPage.css';

import API_BASE from '../config';
const TREE_WIDTH_KEY = 'aitutor_tree_width';
const MCQ_PANEL_WIDTH_KEY = 'aitutor_mcq_panel_width';
const CHAT_HEIGHT_KEY = 'aitutor_chat_height';

const LearningPage = ({ syllabusId, subject, provider, model, apiKey }) => {
  const { api } = useAuth();
  const [progress, setProgress] = useState([]);
  const [structure, setStructure] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedConceptId, setSelectedConceptId] = useState(null);
  const [conceptContent, setConceptContent] = useState(null);
  const [contentLoading, setContentLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);

  const [treeWidth, setTreeWidth] = useState(() => {
    const saved = localStorage.getItem(TREE_WIDTH_KEY);
    return saved ? parseInt(saved, 10) : 280;
  });
  const [mcqPanelWidth, setMcqPanelWidth] = useState(() => {
    const saved = localStorage.getItem(MCQ_PANEL_WIDTH_KEY);
    return saved ? parseInt(saved, 10) : 400;
  });
  const [chatHeight, setChatHeight] = useState(() => {
    const saved = localStorage.getItem(CHAT_HEIGHT_KEY);
    return saved ? parseInt(saved, 10) : 220;
  });
  const [dragTarget, setDragTarget] = useState(null);

  useEffect(() => {
    loadData();
  }, [syllabusId]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [strRes, progRes] = await Promise.all([
        api.get(`${API_BASE}/api/syllabus/${syllabusId}/structure`),
        api.get(`${API_BASE}/api/learning/progress/${syllabusId}`)
      ]);
      setStructure(strRes.data);
      setProgress(progRes.data || []);
      const firstAvail = findFirstAvailable(progRes.data || []);
      if (firstAvail) setSelectedConceptId(firstAvail);
    } catch (e) {
      console.error('Load failed:', e);
    } finally {
      setLoading(false);
    }
  };

  const findFirstAvailable = (pd) => {
    for (const ch of pd) {
      for (const sec of ch.sections || []) {
        for (const c of sec.concepts || []) {
          if (c.status === 'available') return c.id;
        }
      }
    }
    for (const ch of pd) {
      for (const sec of ch.sections || []) {
        for (const c of sec.concepts || []) {
          if (c.status === 'in_progress') return c.id;
        }
      }
    }
    return null;
  };

  const getConceptStatus = (conceptId) => {
    for (const ch of progress) {
      for (const sec of ch.sections || []) {
        for (const c of sec.concepts || []) {
          if (c.id === conceptId) return c.status;
        }
      }
    }
    return 'locked';
  };

  const loadContent = async (conceptId) => {
    setConceptContent(null);
    setContentLoading(true);
    try {
      const res = await api.get(`${API_BASE}/api/syllabus/concept/${conceptId}/content?provider=${provider}&model=${model}&apiKey=${apiKey}`);
      setConceptContent(res.data);
    } catch {
      setConceptContent(null);
    } finally {
      setContentLoading(false);
    }
  };

  useEffect(() => {
    if (!selectedConceptId) return;
    loadContent(selectedConceptId);
  }, [selectedConceptId]);

  const handleConceptSelect = (id, status) => {
    if (status !== 'locked') setSelectedConceptId(id);
  };

  const handleProgressUpdate = () => {
    api.get(`${API_BASE}/api/learning/progress/${syllabusId}`).then(res => {
      setProgress(res.data || []);
    }).catch(() => {});
  };

  const handleRegenerate = async (simplified) => {
    if (!selectedConceptId || regenerating) return;
    setRegenerating(true);
    try {
      await api.post(`${API_BASE}/api/syllabus/concept/${selectedConceptId}/regenerate`, {
        simplified,
        provider,
        model,
        apiKey
      });
      await loadContent(selectedConceptId);
    } catch {
      // handle error
    } finally {
      setRegenerating(false);
    }
  };

  const handleTreeResizeDown = (e) => { e.preventDefault(); setDragTarget('tree'); };
  const handleMcqResizeDown = (e) => { e.preventDefault(); setDragTarget('mcq'); };
  const handleChatResizeDown = (e) => { e.preventDefault(); setDragTarget('chat'); };

  useEffect(() => {
    if (!dragTarget) return;
    const onMove = (e) => {
      if (dragTarget === 'tree') {
        setTreeWidth(Math.max(200, Math.min(400, e.clientX)));
      } else if (dragTarget === 'mcq') {
        setMcqPanelWidth(Math.max(320, Math.min(600, window.innerWidth - e.clientX)));
      } else if (dragTarget === 'chat') {
        const centerRect = document.querySelector('.learning-center')?.getBoundingClientRect();
        if (centerRect) {
          const maxH = centerRect.height - 100;
          const newH = centerRect.bottom - e.clientY;
          setChatHeight(Math.max(120, Math.min(maxH, newH)));
        }
      }
    };
    const onUp = () => {
      if (dragTarget === 'tree') localStorage.setItem(TREE_WIDTH_KEY, treeWidth);
      if (dragTarget === 'mcq') localStorage.setItem(MCQ_PANEL_WIDTH_KEY, mcqPanelWidth);
      if (dragTarget === 'chat') localStorage.setItem(CHAT_HEIGHT_KEY, chatHeight);
      setDragTarget(null);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
    return () => { document.removeEventListener('mousemove', onMove); document.removeEventListener('mouseup', onUp); };
  }, [dragTarget, treeWidth, mcqPanelWidth]);

  if (loading) {
    return <div className="learning-loading"><div className="spinner" /><p>Loading your learning journey...</p></div>;
  }

  const totalConcepts = progress.reduce((s, ch) => s + (ch.totalConcepts || 0), 0);
  const completedConcepts = progress.reduce((s, ch) => s + (ch.completedConcepts || 0), 0);
  const overallPct = totalConcepts > 0 ? Math.round((completedConcepts / totalConcepts) * 100) : 0;

  return (
    <div className="learning-page">
      <aside className="syllabus-tree-panel" style={{ width: treeWidth, minWidth: 200, maxWidth: 400 }}>
        <div className="tree-panel-header">
          <h3><span className="header-subj-icon">{subject?.icon}</span> Syllabus</h3>
          {totalConcepts > 0 && (
            <div className="tree-progress">
              <div className="tree-progress-bar"><div className="tree-progress-fill" style={{ width: `${overallPct}%` }} /></div>
              <span className="tree-progress-text">{completedConcepts}/{totalConcepts} · {overallPct}%</span>
            </div>
          )}
        </div>
        <div className="tree-panel-content">
          {progress.length === 0 ? (
            <p className="no-data">No chapters loaded.</p>
          ) : (
            <div className="syllabus-tree">
              {progress.map((chapter, ci) => (
                <div key={chapter.id || ci} className="tree-chapter">
                  <div className="tree-chapter-title">
                    <span className="chapter-badge">{ci + 1}</span>
                    <span>{chapter.title}</span>
                    <span className="chapter-progress">{chapter.completedConcepts}/{chapter.totalConcepts}</span>
                  </div>
                  {chapter.sections?.map((section, si) => (
                    <div key={section.id || si} className="tree-section">
                      <div className="tree-section-title">{section.title}</div>
                      {section.concepts?.map((concept, coi) => (
                        <div key={concept.id || coi}
                          className={`tree-concept ${concept.status} ${selectedConceptId === concept.id ? 'selected' : ''}`}
                          onClick={() => handleConceptSelect(concept.id, concept.status)}>
                          <span className="concept-status-icon">
                            {concept.status === 'completed' ? '✅' : concept.status === 'in_progress' ? '🔄' : concept.status === 'available' ? '📖' : '🔒'}
                          </span>
                          <span className="concept-title">{concept.title}</span>
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </div>
      </aside>

      <div className="resize-handle-tree" onMouseDown={handleTreeResizeDown} />

      <main className="learning-center">
        {selectedConceptId && conceptContent ? (
          <>
            <div className="concept-content-area">
              <div className="concept-content-header">
                <h2 className="concept-title-heading">{conceptContent.title}</h2>
                <div className="concept-regenerate-actions">
                  <button className="regenerate-btn" onClick={() => handleRegenerate(false)} disabled={regenerating}>
                    {regenerating ? '...' : '🔄 Regenerate'}
                  </button>
                  <button className="regenerate-btn simplified" onClick={() => handleRegenerate(true)} disabled={regenerating}>
                    {regenerating ? '...' : '🔅 Simplified'}
                  </button>
                </div>
              </div>
              <div className="concept-content-body">
                <ReactMarkdown rehypePlugins={[rehypeRaw]} remarkPlugins={[remarkGfm]}>
                  {conceptContent.content || ''}
                </ReactMarkdown>
              </div>
            </div>
            <div className="chat-resize-handle" onMouseDown={handleChatResizeDown} />
            <div className="concept-chat-area" style={{ height: chatHeight, minHeight: 120 }}>
              <ConceptChat conceptId={selectedConceptId} conceptTitle={conceptContent?.title} provider={provider} model={model} apiKey={apiKey} />
            </div>
          </>
        ) : contentLoading ? (
          <div className="center-loading"><div className="spinner" /><p>Loading concept...</p></div>
        ) : (
          <div className="center-placeholder">
            <span className="placeholder-icon">🚀</span>
            <h2>Welcome to {structure?.title || 'Learning'}</h2>
            <p>Select a concept from the syllabus to start learning.</p>
          </div>
        )}
      </main>

      <div className="resize-handle-mcq" onMouseDown={handleMcqResizeDown} />

      <aside className="mcq-right-panel" style={{ width: mcqPanelWidth, minWidth: 320, maxWidth: 600 }}>
        <ConceptMCQPanel
          conceptId={selectedConceptId}
          provider={provider}
          model={model}
          apiKey={apiKey}
          onPass={handleProgressUpdate}
          getConceptStatus={getConceptStatus}
        />
      </aside>
    </div>
  );
};

export default LearningPage;
