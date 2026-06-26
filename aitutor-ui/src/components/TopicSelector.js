import React, { useState } from 'react';
import './TopicSelector.css';

const TopicSelector = ({ onTopicSelect, currentTopic }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [customTopic, setCustomTopic] = useState('');

  const topics = [
    'Mathematics',
    'Science',
    'History',
    'Language Arts',
    'Programming',
    'Art',
    'Music',
    'Geography',
    'Biology',
    'Physics',
    'Chemistry',
    'General Knowledge'
  ];

  const handleTopicSelect = (topic) => {
    onTopicSelect(topic);
    setIsOpen(false);
  };

  const handleCustomTopic = () => {
    if (customTopic.trim()) {
      onTopicSelect(customTopic.trim());
      setCustomTopic('');
      setIsOpen(false);
    }
  };

  return (
    <div className="topic-selector">
      <button 
        className="topic-selector-btn"
        onClick={() => setIsOpen(!isOpen)}
      >
        📚 {currentTopic || 'Select Topic'}
        <span className={`dropdown-arrow ${isOpen ? 'open' : ''}`}>▼</span>
      </button>

      {isOpen && (
        <div className="topic-dropdown">
          <div className="topic-search">
            <input
              type="text"
              placeholder="Search or enter custom topic..."
              value={customTopic}
              onChange={(e) => setCustomTopic(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleCustomTopic()}
            />
            <button onClick={handleCustomTopic}>Add</button>
          </div>
          <div className="topic-list">
            {topics.map((topic) => (
              <div
                key={topic}
                className={`topic-item ${topic === currentTopic ? 'active' : ''}`}
                onClick={() => handleTopicSelect(topic)}
              >
                {topic}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default TopicSelector;