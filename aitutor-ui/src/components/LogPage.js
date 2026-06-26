import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../AuthContext';

function LogPage({ onBack }) {
    const { api } = useAuth();
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filterUser, setFilterUser] = useState('');

    const fetchLogs = useCallback(async () => {
        setLoading(true);
        try {
            const params = `page=${page}&size=50${filterUser ? `&username=${filterUser}` : ''}`;
            const res = await api.get(`/api/logs?${params}`);
            setLogs(res.data.content);
            setTotalPages(res.data.totalPages);
        } catch (err) {
            console.error('Failed to fetch logs', err);
        } finally {
            setLoading(false);
        }
    }, [api, page, filterUser]);

    useEffect(() => {
        fetchLogs();
    }, [fetchLogs]);

    return (
        <div className="log-page">
            <div className="log-header">
                <button className="back-button" onClick={onBack}>← Back</button>
                <h2>Activity Logs</h2>
                <div className="log-controls">
                    <input
                        type="text"
                        placeholder="Filter by username"
                        value={filterUser}
                        onChange={e => { setFilterUser(e.target.value); setPage(0); }}
                        className="log-filter-input"
                    />
                    <button className="refresh-button" onClick={fetchLogs}>Refresh</button>
                </div>
            </div>

            {loading ? (
                <div className="loading">Loading logs...</div>
            ) : (
                <div className="log-table-container">
                    <table className="log-table">
                        <thead>
                            <tr>
                                <th>Time</th>
                                <th>Username</th>
                                <th>Action</th>
                                <th>Details</th>
                                <th>IP Address</th>
                            </tr>
                        </thead>
                        <tbody>
                            {logs.length === 0 ? (
                                <tr><td colSpan="5" className="no-logs">No logs found</td></tr>
                            ) : (
                                logs.map(log => (
                                    <tr key={log.id}>
                                        <td>{new Date(log.timestamp).toLocaleString()}</td>
                                        <td>{log.username}</td>
                                        <td><span className={`action-badge action-${log.action.toLowerCase()}`}>{log.action}</span></td>
                                        <td>{log.details}</td>
                                        <td>{log.ipAddress}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
                            <span>Page {page + 1} of {totalPages}</span>
                            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default LogPage;
