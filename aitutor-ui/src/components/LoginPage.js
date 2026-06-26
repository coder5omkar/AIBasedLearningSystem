import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../AuthContext';

import API_BASE from '../config';

const BrandPanel = () => (
  <aside className="login-brand-panel">
    <div className="login-brand-logo">
      <span className="logo-mark">📚</span>
      <span className="name">AI Tutor</span>
    </div>
    <h2 className="login-brand-headline">Learn anything, guided by AI.</h2>
    <p className="login-brand-sub">
      A personalized learning platform that builds a structured journey for any
      subject, teaches you one concept at a time, and adapts to your pace.
    </p>
    <div className="login-brand-features">
      <div className="login-brand-feature">
        <span className="feat-icon">🧭</span>
        <div>
          <div className="feat-title">AI-generated syllabus</div>
          <div className="feat-desc">Structured chapters, sections, and concepts tailored to your subject.</div>
        </div>
      </div>
      <div className="login-brand-feature">
        <span className="feat-icon">💡</span>
        <div>
          <div className="feat-title">Concept-by-concept learning</div>
          <div className="feat-desc">Study one idea at a time with clear explanations and a doubt chat.</div>
        </div>
      </div>
      <div className="login-brand-feature">
        <span className="feat-icon">🎯</span>
        <div>
          <div className="feat-title">Master before you advance</div>
          <div className="feat-desc">Pass quick assessments to unlock the next concept and track progress.</div>
        </div>
      </div>
    </div>
  </aside>
);

const LoginPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [showForgot, setShowForgot] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      if (showForgot) {
        if (password !== confirmPassword) {
          setError('Passwords do not match');
          setLoading(false);
          return;
        }
        await axios.post(`${API_BASE}/api/auth/forgot-password`, { username, password, confirmPassword });
        setSuccess('Password updated successfully. Please sign in.');
        setShowForgot(false);
        setPassword('');
        setConfirmPassword('');
      } else {
        const endpoint = isLogin ? 'login' : 'register';
        const response = await axios.post(`${API_BASE}/api/auth/${endpoint}`, { username, password });
        login(response.data.token, response.data.username);
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  if (showForgot) {
    return (
      <div className="login-page">
        <BrandPanel />
        <div className="login-form-panel">
          <div className="login-card">
            <div className="login-header">
              <span className="login-icon">🔑</span>
              <h1>Reset Password</h1>
            </div>
            <form onSubmit={handleSubmit} className="login-form">
              <div className="login-field">
                <label>Username</label>
                <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter username" required />
              </div>
              <div className="login-field">
                <label>New Password</label>
                <div className="password-wrapper">
                  <input type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter new password" required />
                  <button type="button" className="toggle-password" onClick={() => setShowPassword(!showPassword)} tabIndex={-1}>
                    {showPassword ? '🙈' : '👁️'}
                  </button>
                </div>
              </div>
              <div className="login-field">
                <label>Confirm Password</label>
                <div className="password-wrapper">
                  <input type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Confirm new password" required />
                </div>
              </div>
              {error && <div className="login-error">{error}</div>}
              {success && <div className="login-success">{success}</div>}
              <button type="submit" className="login-btn" disabled={loading}>
                {loading ? 'Please wait...' : 'Reset Password'}
              </button>
              <button type="button" className="login-link-btn" onClick={() => { setShowForgot(false); setError(''); setSuccess(''); setPassword(''); setConfirmPassword(''); }}>
                Back to Sign In
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <BrandPanel />
      <div className="login-form-panel">
        <div className="login-card">
          <div className="login-header">
            <span className="login-icon">📚</span>
            <h1>AI Tutor</h1>
            <p>{isLogin ? 'Sign in to your account' : 'Create a new account'}</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            <div className="login-field">
              <label>Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                required
              />
            </div>

            <div className="login-field">
              <label>Password</label>
              <div className="password-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  required
                />
                <button type="button" className="toggle-password" onClick={() => setShowPassword(!showPassword)} tabIndex={-1}>
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
              {isLogin && <button type="button" className="forgot-link" onClick={() => { setShowForgot(true); setError(''); }}>Forgot password?</button>}
            </div>

            {error && <div className="login-error">{error}</div>}

            <button type="submit" className="login-btn" disabled={loading}>
              {loading ? 'Please wait...' : (isLogin ? 'Sign In' : 'Register')}
            </button>
          </form>

          <div className="login-toggle">
            {isLogin ? (
              <p>Don't have an account? <button onClick={() => { setIsLogin(false); setError(''); }}>Register</button></p>
            ) : (
              <p>Already have an account? <button onClick={() => { setIsLogin(true); setError(''); }}>Sign In</button></p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
