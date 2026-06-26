import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import API_BASE from '../config';

const ModelSelector = ({ provider, model, apiKey, onProviderChange, onModelChange, onApiKeyChange }) => {
  const { api } = useAuth();
  const [models, setModels] = useState([]);
  const [ollamaModels, setOllamaModels] = useState([]);
  const [openaiModels, setOpenaiModels] = useState([]);
  const [showApiInput, setShowApiInput] = useState(false);
  const [localApiKey, setLocalApiKey] = useState(apiKey || '');
  const [isLoadingModels, setIsLoadingModels] = useState(false);

  useEffect(() => {
    if (provider === 'openai' && !apiKey) {
      setShowApiInput(true);
    } else {
      setShowApiInput(false);
    }
  }, [provider, apiKey]);

  useEffect(() => {
    fetchOllamaModels();
  }, []);

  useEffect(() => {
    const allModels = [...ollamaModels, ...openaiModels];
    setModels(allModels);
    if (allModels.length > 0) {
      const currentModelExists = allModels.some(m => m.name === model && m.provider === provider);
      if (!currentModelExists) {
        onModelChange(allModels[0].name);
      }
    }
  }, [ollamaModels, openaiModels]);

  const fetchOllamaModels = async () => {
    try {
      const response = await api.get(`${API_BASE}/api/models`);
      const ollamaList = (response.data || []).filter(m => m.provider === 'ollama');
      setOllamaModels(ollamaList);
    } catch (err) {
      console.error('Error fetching Ollama models:', err);
      setOllamaModels([{ name: 'llama3.2:3b', provider: 'ollama' }]);
    }
  };

  const fetchOpenAiModels = async (key) => {
    setIsLoadingModels(true);
    try {
      const response = await api.get(`${API_BASE}/api/models?apiKey=${encodeURIComponent(key)}`);
      const openaiList = (response.data || []).filter(m => m.provider === 'openai');
      setOpenaiModels(openaiList);
      if (openaiList.length > 0) {
        onModelChange(openaiList[0].name);
      }
    } catch (err) {
      console.error('Error fetching OpenAI models:', err);
      setOpenaiModels([]);
    } finally {
      setIsLoadingModels(false);
    }
  };

  const handleProviderSwitch = (newProvider) => {
    onProviderChange(newProvider);
    if (newProvider === 'openai') {
      if (apiKey) {
        fetchOpenAiModels(apiKey);
      } else {
        setShowApiInput(true);
        setOpenaiModels([]);
      }
    } else {
      setShowApiInput(false);
      const firstOllama = ollamaModels.length > 0 ? ollamaModels[0].name : 'llama3.2:3b';
      onModelChange(firstOllama);
    }
  };

  const handleApiKeySubmit = () => {
    const key = localApiKey.trim();
    if (key) {
      onApiKeyChange(key);
      fetchOpenAiModels(key);
      setShowApiInput(false);
    }
  };

  const handleApiKeyClear = () => {
    onApiKeyChange('');
    setLocalApiKey('');
    setOpenaiModels([]);
    setShowApiInput(true);
    onProviderChange('ollama');
  };

  const filteredModels = models.filter(m => m.provider === provider);

  return (
    <div className="model-selector">
      <div className="model-selector-row">
        <div className="model-field">
          <label className="model-label">Provider</label>
          <div className="provider-toggle">
            <button
              className={`toggle-btn ${provider === 'ollama' ? 'active' : ''}`}
              onClick={() => handleProviderSwitch('ollama')}
            >Ollama</button>
            <button
              className={`toggle-btn ${provider === 'openai' ? 'active' : ''}`}
              onClick={() => handleProviderSwitch('openai')}
            >OpenAI</button>
          </div>
        </div>

        <div className="model-field">
          <label className="model-label">Model</label>
          <select
            className="model-select"
            value={model || ''}
            onChange={(e) => onModelChange(e.target.value)}
            disabled={provider === 'openai' && !apiKey}
          >
            {filteredModels.length === 0 && (
              <option value="">{provider === 'openai' ? 'Enter API key first' : 'No models'}</option>
            )}
            {filteredModels.map((m, i) => (
              <option key={i} value={m.name}>{m.name}</option>
            ))}
          </select>
        </div>

        {provider === 'openai' && (
          <div className="model-field">
            <label className="model-label">API Key</label>
            {showApiInput ? (
              <div className="api-key-input-group">
                <input
                  type="password"
                  className="api-key-input"
                  placeholder="sk-..."
                  value={localApiKey}
                  onChange={(e) => setLocalApiKey(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleApiKeySubmit()}
                />
                <button className="api-key-btn" onClick={handleApiKeySubmit} disabled={!localApiKey.trim()}>
                  {isLoadingModels ? '...' : 'Set'}
                </button>
              </div>
            ) : (
              <div className="api-key-set">
                <span className="api-key-dot">Key set</span>
                <button className="api-key-btn secondary" onClick={handleApiKeyClear}>Clear</button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ModelSelector;
