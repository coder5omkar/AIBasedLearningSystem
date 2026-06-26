import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import './SubjectSelectionPage.css';

import API_BASE from '../config';

const SubjectSelectionPage = ({ onSubjectSelect }) => {
  const { api } = useAuth();
  const [subjects, setSubjects] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSubjects();
  }, []);

  const loadSubjects = async () => {
    try {
      const res = await api.get(`${API_BASE}/api/subjects`);
      setSubjects(res.data || []);
    } catch {
      setSubjects([]);
    } finally {
      setLoading(false);
    }
  };

  const categories = ['All', ...new Set(subjects.map(s => s.category))];
  const filtered = selectedCategory === 'All' ? subjects : subjects.filter(s => s.category === selectedCategory);

  return (
    <div className="subjects-page">
      <div className="subjects-hero">
        <div className="hero-icon">🎓</div>
        <h1>What would you like to learn today?</h1>
        <p>Choose a subject and start your AI-powered learning journey</p>
        {!loading && subjects.length > 0 && (
          <div className="hero-stat"><strong>{subjects.length}</strong> subjects available</div>
        )}
      </div>
      <div className="subjects-categories">
        {categories.map(cat => (
          <button key={cat}
            className={`cat-pill ${selectedCategory === cat ? 'active' : ''}`}
            onClick={() => setSelectedCategory(cat)}>
            {cat === 'All' ? '🌟 All' : cat === 'Academic' ? '📚 Academic' : '💻 Technology'}
          </button>
        ))}
      </div>
      {loading ? (
        <div className="subjects-loading"><div className="spinner" /><p>Loading subjects...</p></div>
      ) : (
        <div className="subjects-grid">
          {filtered.map(subject => (
            <div key={subject.id} className="subject-card" onClick={() => onSubjectSelect(subject)}>
              <div className="subject-card-icon-wrap">
                <span className="subject-card-icon">{subject.icon}</span>
              </div>
              <h3>{subject.name}</h3>
              <p>{subject.description}</p>
              <span className="subject-card-category">{subject.category}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SubjectSelectionPage;
