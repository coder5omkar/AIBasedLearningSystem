import React, { useState, useEffect } from 'react';
import './MCQGenerator.css';
import { useAuth } from '../AuthContext';

import API_BASE from '../config';

const MCQGenerator = ({ sessionId, provider, model, apiKey, messageCount }) => {
  const { api } = useAuth();
  const [mcqs, setMcqs] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showResults, setShowResults] = useState(false);
  const [explanations, setExplanations] = useState({});
  const [isLoadingExplanations, setIsLoadingExplanations] = useState(false);

  useEffect(() => {
    if (sessionId) {
      loadMCQs();
    }
  }, [sessionId]);

  const loadMCQs = async () => {
    try {
      const response = await api.get(`${API_BASE}/api/mcq/session/${sessionId}`);
      setMcqs(response.data);
      if (response.data.length > 0) {
        setCurrentQuestionIndex(0);
      }
    } catch (err) {
      console.error('Error loading MCQs:', err);
    }
  };

  const generateMCQs = async () => {
    setIsLoading(true);
    setError(null);
    setSelectedAnswers({});
    setShowResults(false);
    setExplanations({});

    try {
      const response = await api.post(
        `${API_BASE}/api/mcq/generate?sessionId=${sessionId}`,
        {
          provider: provider || 'ollama',
          model: model || 'llama3.2:3b',
          apiKey: apiKey || ''
        }
      );
      setMcqs(response.data);
      setCurrentQuestionIndex(0);
    } catch (err) {
      console.error('Error generating MCQs:', err);
      setError('Failed to generate MCQs. Please make sure you have a conversation history.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAnswerSelect = (questionIndex, answer) => {
    setSelectedAnswers(prev => ({ ...prev, [questionIndex]: answer }));
    if (currentQuestionIndex < mcqs.length - 1) {
      setTimeout(() => setCurrentQuestionIndex(currentQuestionIndex + 1), 300);
    }
  };

  const handleNext = () => {
    if (currentQuestionIndex < mcqs.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    }
  };

  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
    }
  };

  const handleSubmit = async () => {
    setShowResults(true);
    setIsLoadingExplanations(true);

    try {
      const response = await api.post(
        `${API_BASE}/api/mcq/explanations?sessionId=${sessionId}&provider=${provider || 'ollama'}&model=${model || 'llama3.2:3b'}&apiKey=${apiKey || ''}`,
        selectedAnswers
      );
      setExplanations(response.data);
    } catch (err) {
      console.error('Error fetching explanations:', err);
    } finally {
      setIsLoadingExplanations(false);
    }
  };

  const calculateScore = () => {
    let correct = 0;
    mcqs.forEach((mcq) => {
      if (selectedAnswers[mcq.questionNumber] === mcq.correctAnswer) {
        correct++;
      }
    });
    return correct;
  };

  if (!sessionId) {
    return (
      <div className="mcq-generator">
        <div className="mcq-empty">
          <p>Select a conversation to generate MCQs</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mcq-generator">
      <div className="mcq-header">
        <h3>MCQ Generator</h3>
        <div className="mcq-controls">
          <button
            onClick={generateMCQs}
            disabled={isLoading}
            className="generate-btn"
          >
            {isLoading ? 'Generating...' : 'Generate MCQs from Conversation'}
          </button>
        </div>
      </div>

      {error && (
        <div className="mcq-error">
          {error}
        </div>
      )}

      {isLoading && (
        <div className="mcq-loading">
          <div className="mcq-spinner"></div>
          <p>Generating questions from your conversation...</p>
        </div>
      )}

      {mcqs.length > 0 && !isLoading && (
        <div className="mcq-content">
          {!showResults ? (
            <>
              <div className="mcq-progress">
                Question {currentQuestionIndex + 1} of {mcqs.length}
                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{ width: `${((currentQuestionIndex + 1) / mcqs.length) * 100}%` }}
                  ></div>
                </div>
              </div>

              <div className="mcq-question">
                <h4>{mcqs[currentQuestionIndex].question}</h4>
                <div className="mcq-options">
                  {['A', 'B', 'C', 'D'].map((optionKey) => {
                    const optionText = mcqs[currentQuestionIndex][`option${optionKey}`];
                    const isSelected = selectedAnswers[mcqs[currentQuestionIndex].questionNumber] === optionKey;
                    return (
                      <div
                        key={optionKey}
                        className={`mcq-option ${isSelected ? 'selected' : ''}`}
                        onClick={() => handleAnswerSelect(mcqs[currentQuestionIndex].questionNumber, optionKey)}
                      >
                        <span className="option-label">{optionKey}.</span>
                        <span className="option-text">{optionText}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="mcq-navigation">
                <button
                  onClick={handlePrevious}
                  disabled={currentQuestionIndex === 0}
                  className="nav-btn"
                >
                  Previous
                </button>
                {currentQuestionIndex === mcqs.length - 1 ? (
                  <button
                    onClick={handleSubmit}
                    className="nav-btn submit-btn"
                    disabled={Object.keys(selectedAnswers).length < mcqs.length}
                  >
                    Submit Answers
                  </button>
                ) : (
                  <button
                    onClick={handleNext}
                    className="nav-btn"
                  >
                    Next
                  </button>
                )}
              </div>
            </>
          ) : (
            <div className="mcq-results">
              <div className="results-header">
                <h3>Results</h3>
                <div className="score-display">
                  <span className="score-number">{calculateScore()}</span>
                  <span className="score-total">/{mcqs.length}</span>
                </div>
              </div>

              {isLoadingExplanations ? (
                <div className="mcq-loading">
                  <div className="mcq-spinner"></div>
                  <p>Loading explanations...</p>
                </div>
              ) : (
                <div className="results-list">
                  {mcqs.map((mcq) => {
                    const qNum = mcq.questionNumber;
                    const isCorrect = selectedAnswers[qNum] === mcq.correctAnswer;
                    const explanation = explanations[qNum] || 'No explanation available.';
                    return (
                      <div key={qNum} className="result-item">
                        <div className="result-question">
                          <span className="result-number">Q{qNum}:</span>
                          <span>{mcq.question}</span>
                        </div>
                        <div className="result-answer">
                          Your answer: <strong>{selectedAnswers[qNum] || 'Not answered'}</strong>
                          {isCorrect ? (
                            <span className="result-correct"> Correct</span>
                          ) : (
                            <span className="result-wrong"> Wrong (Correct: {mcq.correctAnswer})</span>
                          )}
                        </div>
                        <div className="result-explanation">
                          <strong>Explanation:</strong>
                          <p>{explanation}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              <button
                onClick={() => setShowResults(false)}
                className="nav-btn review-btn"
              >
                Review Questions
              </button>
            </div>
          )}
        </div>
      )}

      {mcqs.length === 0 && !isLoading && !error && (
        <div className="mcq-empty">
          <div className="empty-icon"></div>
          <h4>No Questions Generated</h4>
          <p>Have a conversation with the AI tutor, then click "Generate MCQs from Conversation"</p>
        </div>
      )}
    </div>
  );
};

export default MCQGenerator;
