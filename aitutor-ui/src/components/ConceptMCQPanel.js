import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import './ConceptMCQPanel.css';

import API_BASE from '../config';

const ConceptMCQPanel = ({ conceptId, provider, model, apiKey, onPass, getConceptStatus }) => {
  const { api } = useAuth();
  const [mcqs, setMcqs] = useState([]);
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!conceptId) return;
    setMcqs([]);
    setAnswers({});
    setSubmitted(false);
    setResult(null);
    setError('');
    loadMCQs();
  }, [conceptId]);

  const loadMCQs = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`${API_BASE}/api/learning/mcq/${conceptId}?provider=${provider}&model=${model}&apiKey=${apiKey}`);
      setMcqs(res.data || []);
    } catch {
      setError('Failed to load MCQs');
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (qNum, answer) => {
    if (submitted) return;
    setAnswers(prev => ({ ...prev, [qNum]: answer }));
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const res = await api.post(`${API_BASE}/api/learning/mcq/${conceptId}/submit`, answers);
      setResult(res.data);
      setSubmitted(true);
      if (res.data.passed) {
        setTimeout(() => onPass && onPass(), 2000);
      }
    } catch {
      setError('Failed to submit answers');
    } finally {
      setSubmitting(false);
    }
  };

  if (!conceptId) {
    return (
      <div className="mcq-panel-empty">
        <div className="mcq-panel-header"><h3>📝 MCQ Test</h3></div>
        <div className="mcq-panel-body"><p className="mcq-placeholder">Select a concept to start the MCQ test.</p></div>
      </div>
    );
  }

  const status = getConceptStatus ? getConceptStatus(conceptId) : null;

  if (status === 'completed') {
    return (
      <div className="mcq-panel-completed">
        <div className="mcq-panel-header"><h3>📝 MCQ Test</h3></div>
        <div className="mcq-panel-body">
          <div className="mcq-completed-msg">✅ You passed this concept!</div>
        </div>
      </div>
    );
  }

  return (
    <div className="mcq-panel">
      <div className="mcq-panel-header">
        <h3>📝 MCQ Test</h3>
        {mcqs.length > 0 && !submitted && (
          <span className="mcq-progress">{Object.keys(answers).length}/{mcqs.length}</span>
        )}
      </div>
      <div className="mcq-panel-body">
        {loading ? (
          <div className="mcq-loading"><div className="spinner" /><p>Generating questions...</p></div>
        ) : error ? (
          <div className="mcq-error">
            <p>{error}</p>
            <button className="retry-btn" onClick={loadMCQs}>Retry</button>
          </div>
        ) : mcqs.length === 0 ? (
          <p className="mcq-placeholder">No questions available.</p>
        ) : submitted && result ? (
          <div className="mcq-result">
            <div className={`mcq-result-banner ${result.passed ? 'passed' : 'failed'}`}>
              {result.passed ? '✅ Passed!' : '❌ Failed'}
            </div>
            <p className="mcq-score">{result.correct}/{result.total} correct</p>
            {!result.passed && <p className="mcq-attempts">Attempts: {result.attempts}</p>}
            <div className="mcq-result-details">
              {mcqs.map((mcq, i) => {
                const r = result.results?.[i];
                return (
                  <div key={mcq.id || i} className={`mcq-result-item ${r?.isCorrect ? 'correct' : 'wrong'}`}>
                    <p className="mcq-result-q">Q{mcq.questionNumber}. {mcq.question}</p>
                    <p className="mcq-result-answer">Your answer: {answers[mcq.questionNumber] || '-'} {r && (r.isCorrect ? '✅' : `❌ (Correct: ${r.correctAnswer})`)}</p>
                  </div>
                );
              })}
            </div>
            {!result.passed && (
              <button className="retry-btn full" onClick={() => { setSubmitted(false); setAnswers({}); setResult(null); setMcqs([]); loadMCQs(); }}>Retry Test</button>
            )}
            {result.passed && <p className="mcq-next-hint">🎉 Moving to next concept...</p>}
          </div>
        ) : (
          <>
            <p className="mcq-desc">⚠️ Answer all 5 questions correctly to pass.</p>
            {mcqs.map((mcq, i) => (
              <div key={mcq.id || i} className="mcq-item">
                <p className="mcq-question-text">Q{mcq.questionNumber}. {mcq.question}</p>
                <div className="mcq-options">
                  {['A', 'B', 'C', 'D'].map(opt => (
                    <div key={opt}
                      className={`mcq-option ${answers[mcq.questionNumber] === opt ? 'selected' : ''}`}
                      onClick={() => handleSelect(mcq.questionNumber, opt)}>
                      <span className="mcq-opt-letter">{opt}</span>
                      <span>{mcq[`option${opt}`]}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
            <button className="submit-btn"
              onClick={handleSubmit}
              disabled={mcqs.some(m => !answers[m.questionNumber]) || submitting}>
              {submitting ? 'Submitting...' : `Submit (${Object.keys(answers).length}/${mcqs.length})`}
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default ConceptMCQPanel;
