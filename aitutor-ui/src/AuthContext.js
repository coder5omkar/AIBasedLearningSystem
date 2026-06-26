import React, { createContext, useState, useContext, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [username, setUsername] = useState(localStorage.getItem('username') || null);

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token);
      localStorage.setItem('username', username);
    } else {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
    }
  }, [token, username]);

  const login = (newToken, newUsername) => {
    setToken(newToken);
    setUsername(newUsername);
  };

  const logout = () => {
    setToken(null);
    setUsername(null);
  };

  const api = {
    get: (url, config) => axios.get(url, { ...config, headers: { ...config?.headers, 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } }),
    post: (url, data, config) => axios.post(url, data, { ...config, headers: { ...config?.headers, 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } }),
    delete: (url, config) => axios.delete(url, { ...config, headers: { ...config?.headers, 'Authorization': `Bearer ${token}` } }),
  };

  return (
    <AuthContext.Provider value={{ token, username, isAuthenticated: !!token, login, logout, api }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
