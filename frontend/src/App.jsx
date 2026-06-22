import React, { useState, useEffect, useRef } from 'react';

// Resolve backend endpoints dynamically. Supports localhost, custom Env vars, and automatic GitHub Codespaces port mapping.
const getBackendUrls = () => {
  const defaultUrls = {
    identity: import.meta.env.VITE_IDENTITY_SERVICE_URL || 'http://localhost:8081',
    election: import.meta.env.VITE_ELECTION_SERVICE_URL || 'http://localhost:8082',
    vote: import.meta.env.VITE_VOTE_SERVICE_URL || 'http://localhost:8083',
    audit: import.meta.env.VITE_AUDIT_SERVICE_URL || 'http://localhost:8084',
  };

  if (typeof window !== 'undefined') {
    const hostname = window.location.hostname;
    // Handle GitHub Codespaces URL mapping dynamically via Nginx reverse proxy
    if (hostname.includes('.github.dev') || hostname.includes('.app.github.dev')) {
      const origin = window.location.origin;
      return {
        identity: origin,
        election: origin,
        vote: origin,
        audit: origin,
      };
    }
  }
  return defaultUrls;
};

const API_URLS = getBackendUrls();

export default function App() {
  // Authentication & Session States
  const [isRegistering, setIsRegistering] = useState(false);
  const [user, setUser] = useState(null); // { token, username, nationalId, fullName, hasVoted }
  const [regForm, setRegForm] = useState({ username: '', password: '', email: '', nationalId: '', fullName: '', fingerprintHash: 'simulated_biometric_hash_1290374' });
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });

  // Core Election & Voting States
  const [elections, setElections] = useState([]);
  const [selectedElection, setSelectedElection] = useState(null);
  const [selectedCandidate, setSelectedCandidate] = useState(null);
  const [signatureFile, setSignatureFile] = useState(null);
  const [signatureText, setSignatureText] = useState('');
  
  // Verification Screen States
  const [isScanning, setIsScanning] = useState(false);
  const [scanStatus, setScanStatus] = useState('');
  const [scanSteps, setScanSteps] = useState({ mintel: false, arcotel: false, firmaEc: false });

  // Public Ledger & Audit States
  const [ledger, setLedger] = useState([]);
  const [results, setResults] = useState({}); // electionId -> [ { candidateId, voteCount } ]
  
  // Notification states
  const [toast, setToast] = useState(null);
  const pollIntervalRef = useRef(null);

  // Generate or retrieve idempotency key for this session's voting action
  const [idempotencyKey, setIdempotencyKey] = useState('');

  // Initial load and polling setup
  useEffect(() => {
    fetchElections();
    fetchLedger();
    // Set a new idempotency key
    regenerateIdempotencyKey();

    // Start background polling for the public audit ledger
    pollIntervalRef.current = setInterval(() => {
      fetchLedger();
    }, 4000);

    return () => {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    };
  }, []);

  // Fetch results when an election is selected
  useEffect(() => {
    if (selectedElection) {
      fetchResults(selectedElection.id);
    }
  }, [selectedElection]);

  const showToast = (message, isError = false) => {
    setToast({ message, isError });
    setTimeout(() => setToast(null), 5000);
  };

  const regenerateIdempotencyKey = () => {
    const key = 'vs-key-' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    setIdempotencyKey(key);
  };

  const fetchElections = async () => {
    try {
      const res = await fetch(`${API_URLS.election}/api/elections`);
      if (res.ok) {
        const data = await res.json();
        setElections(data);
        // Automatically select the first active election
        const active = data.find(e => e.status === 'ACTIVE');
        if (active) setSelectedElection(active);
      }
    } catch (err) {
      console.warn("Failed to fetch from election service. Using fallback demo elections.", err);
      // Fallback
      const fallback = [
        {
          id: 1,
          title: "Presidential Elections 2026",
          description: "National election to choose the next president of Ecuador.",
          status: "ACTIVE",
          candidates: [
            { id: 1, name: "Diana Salazar", party: "Movimiento Alianza Libertad", photoUrl: "https://api.dicebear.com/7.x/bottts/svg?seed=Diana" },
            { id: 2, name: "Christian Zurita", party: "Partido Renovación Democrática", photoUrl: "https://api.dicebear.com/7.x/bottts/svg?seed=Christian" },
            { id: 3, name: "Blank Vote", party: "N/A", photoUrl: "https://api.dicebear.com/7.x/bottts/svg?seed=Blank" }
          ]
        }
      ];
      setElections(fallback);
      setSelectedElection(fallback[0]);
    }
  };

  const fetchLedger = async () => {
    try {
      const res = await fetch(`${API_URLS.audit}/api/audit/ledger`);
      if (res.ok) {
        const data = await res.json();
        setLedger(data);
      }
    } catch (err) {
      console.log("Audit service offline, using mock ledger values");
    }
  };

  const fetchResults = async (electionId) => {
    try {
      const res = await fetch(`${API_URLS.audit}/api/audit/results/${electionId}`);
      if (res.ok) {
        const data = await res.json();
        setResults(prev => ({ ...prev, [electionId]: data }));
      }
    } catch (err) {
      console.log("Counting results offline, using empty totals");
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URLS.identity}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(regForm)
      });
      if (res.ok) {
        showToast("Registration successful! Please login.");
        setIsRegistering(false);
      } else {
        const txt = await res.text();
        showToast(txt || "Registration failed", true);
      }
    } catch (err) {
      showToast("Identity Service is unreachable", true);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URLS.identity}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginForm)
      });
      if (res.ok) {
        const data = await res.json();
        setUser(data);
        showToast(`Welcome back, ${data.fullName}!`);
      } else {
        const txt = await res.text();
        showToast(txt || "Login failed", true);
      }
    } catch (err) {
      showToast("Identity Service is unreachable", true);
    }
  };

  const handleLogout = () => {
    setUser(null);
    setSelectedCandidate(null);
    setSignatureFile(null);
    setSignatureText('');
    showToast("Logged out successfully");
  };

  // Simulate file reading for signature validation
  const handleSignatureUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSignatureFile(file);
      // Generate a mock base64/hash representing the signature content
      const reader = new FileReader();
      reader.onload = (event) => {
        const content = event.target.result;
        // Create simulated hash of digital certificate
        const mockSignatureHash = "signature_p12_hash_" + btoa(content).substring(10, 60);
        setSignatureText(mockSignatureHash);
      };
      reader.readAsText(file);
    }
  };

  const executeVotingFlow = async () => {
    if (!selectedCandidate) {
      showToast("Please choose a candidate", true);
      return;
    }
    if (!signatureText) {
      showToast("Please provide or upload a digital signature file (.p12)", true);
      return;
    }

    setIsScanning(true);
    setScanStatus("Contacting National Citizen Registry...");
    setScanSteps({ mintel: false, arcotel: false, firmaEc: false });

    // Step 1: Simulate MINTEL Registry Check (1s)
    await new Promise(r => setTimeout(r, 1000));
    setScanSteps(prev => ({ ...prev, mintel: true }));
    setScanStatus("ARCOTEL: Checking biometric association and device ownership...");

    // Step 2: Simulate ARCOTEL Check (1s)
    await new Promise(r => setTimeout(r, 1000));
    setScanSteps(prev => ({ ...prev, arcotel: true }));
    setScanStatus("FirmaEc: Validating Cryptographic Signature integrity...");

    // Step 3: Simulate FirmaEc Validation (1s)
    await new Promise(r => setTimeout(r, 1000));
    setScanSteps(prev => ({ ...prev, firmaEc: true }));
    setScanStatus("Verifying vote validation tokens...");

    // Check with backend identity service
    try {
      const valRes = await fetch(`${API_URLS.identity}/api/auth/validate-signature`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nationalId: user.nationalId,
          signature: signatureText
        })
      });
      const valData = await valRes.json();
      if (!valRes.ok || !valData.valid) {
        setIsScanning(false);
        showToast("Governmental Validation failed. signature is invalid.", true);
        return;
      }
    } catch (err) {
      console.warn("Identity Service validation offline. Simulating validation approval.");
    }

    setScanStatus("Queuing vote securely to ingestion buffer...");
    await new Promise(r => setTimeout(r, 800));

    // Submit transaction with Idempotency Key
    try {
      const votePayload = {
        electionId: selectedElection.id,
        candidateId: selectedCandidate.id,
        nationalId: user.nationalId,
        signature: signatureText,
        authToken: user.token,
        logicalTimestamp: Date.now()
      };

      const res = await fetch(`${API_URLS.vote}/api/votes`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify(votePayload)
      });

      const resData = await res.json();

      setIsScanning(false);

      if (res.ok) {
        showToast("Your vote was buffered successfully!");
        // Update user state locally
        setUser(prev => ({ ...prev, hasVoted: true }));
        fetchLedger();
        fetchResults(selectedElection.id);
      } else {
        showToast(resData.message || "Failed to register vote", true);
      }
    } catch (err) {
      setIsScanning(false);
      showToast("Vote Registration Service is unreachable. Vote discarded.", true);
    }
  };

  // Calculate vote percentage for SVG charts
  const getCandidatePercentage = (candidateId) => {
    const electionResults = results[selectedElection?.id] || [];
    const totalVotes = electionResults.reduce((acc, curr) => acc + curr.voteCount, 0);
    if (totalVotes === 0) return 0;
    const match = electionResults.find(r => r.candidateId === candidateId);
    return match ? Math.round((match.voteCount / totalVotes) * 100) : 0;
  };

  const getCandidateVotesCount = (candidateId) => {
    const electionResults = results[selectedElection?.id] || [];
    const match = electionResults.find(r => r.candidateId === candidateId);
    return match ? match.voteCount : 0;
  };

  return (
    <div className="app-container">
      {/* Toast Notification */}
      {toast && (
        <div className={`toast ${toast.isError ? 'error' : ''}`}>
          {toast.isError ? '⚠️ ' : '✅ '}
          {toast.message}
        </div>
      )}

      {/* Main Header */}
      <header>
        <div className="logo-section">
          <div className="logo-icon">🗳️</div>
          <div>
            <h1 className="logo-text">VotoSync</h1>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Distributed Consensus Voting Platform</p>
          </div>
        </div>

        {user ? (
          <div className="user-badge">
            <span className="status-dot"></span>
            <span style={{ fontWeight: 600 }}>{user.fullName}</span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>({user.nationalId})</span>
            <button className="btn btn-secondary" style={{ padding: '0.25rem 0.75rem', fontSize: '0.8rem' }} onClick={handleLogout}>Logout</button>
          </div>
        ) : (
          <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Secure Digital Democracy</span>
        )}
      </header>

      {/* Main Dashboard Content */}
      {!user ? (
        /* Authentication Screen */
        <div className="auth-wrapper">
          <div className="glass-card">
            <div className="auth-header">
              <h2>{isRegistering ? 'Create Citizen Profile' : 'Authenticate Citizen'}</h2>
              <p>{isRegistering ? 'Register your National Identity credentials' : 'Access the electronic ballot with your credentials'}</p>
            </div>

            <form onSubmit={isRegistering ? handleRegister : handleLogin}>
              {isRegistering ? (
                <>
                  <div className="input-group">
                    <label>Full Name</label>
                    <input 
                      type="text" 
                      required 
                      className="input-field" 
                      value={regForm.fullName}
                      onChange={e => setRegForm({...regForm, fullName: e.target.value})}
                    />
                  </div>
                  <div className="input-group">
                    <label>National ID (Cedula)</label>
                    <input 
                      type="text" 
                      required 
                      maxLength={10}
                      className="input-field" 
                      placeholder="e.g. 1723456789"
                      value={regForm.nationalId}
                      onChange={e => setRegForm({...regForm, nationalId: e.target.value})}
                    />
                  </div>
                  <div className="input-group">
                    <label>Email Address</label>
                    <input 
                      type="email" 
                      required 
                      className="input-field" 
                      value={regForm.email}
                      onChange={e => setRegForm({...regForm, email: e.target.value})}
                    />
                  </div>
                </>
              ) : null}

              <div className="input-group">
                <label>Username</label>
                <input 
                  type="text" 
                  required 
                  className="input-field" 
                  value={isRegistering ? regForm.username : loginForm.username}
                  onChange={e => isRegistering 
                    ? setRegForm({...regForm, username: e.target.value})
                    : setLoginForm({...loginForm, username: e.target.value})
                  }
                />
              </div>

              <div className="input-group">
                <label>Password</label>
                <input 
                  type="password" 
                  required 
                  className="input-field" 
                  value={isRegistering ? regForm.password : loginForm.password}
                  onChange={e => isRegistering 
                    ? setRegForm({...regForm, password: e.target.value})
                    : setLoginForm({...loginForm, password: e.target.value})
                  }
                />
              </div>

              <button type="submit" className="btn" style={{ width: '100%', marginTop: '1rem' }}>
                {isRegistering ? 'Register Citizen' : 'Authorize Identity'}
              </button>

              <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem' }}>
                <a 
                  href="#" 
                  style={{ color: 'var(--accent-cyan)', textDecoration: 'none' }}
                  onClick={(e) => { e.preventDefault(); setIsRegistering(!isRegistering); }}
                >
                  {isRegistering ? 'Already registered? Login here' : 'New voter? Register here'}
                </a>
              </div>
            </form>
          </div>
        </div>
      ) : (
        /* Dashboard for Authenticated Citizen */
        <div className="dashboard-grid">
          
          {/* Column 1: Voting Station */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            
            {/* Active ballot details */}
            <div className="glass-card" style={{ maxWidth: '100%' }}>
              <div className="section-title">
                <h2>Electronic Voting Ballot</h2>
              </div>
              
              <div className="input-group">
                <label>Select Active Election Process</label>
                <select 
                  className="input-field"
                  value={selectedElection?.id || ''}
                  onChange={e => {
                    const match = elections.find(el => el.id === parseInt(e.target.value));
                    setSelectedElection(match);
                    setSelectedCandidate(null);
                  }}
                >
                  {elections.map(el => (
                    <option key={el.id} value={el.id}>{el.title} ({el.status})</option>
                  ))}
                </select>
              </div>

              {selectedElection && (
                <div style={{ marginTop: '1rem' }}>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>{selectedElection.description}</p>
                  
                  {user.hasVoted ? (
                    <div style={{ 
                      marginTop: '2rem', 
                      background: 'rgba(0, 255, 102, 0.05)', 
                      border: '1px solid rgba(0, 255, 102, 0.2)', 
                      padding: '1.5rem', 
                      borderRadius: '8px', 
                      textAlign: 'center' 
                    }}>
                      <h3 style={{ color: '#00ff66', marginBottom: '0.5rem' }}>🔒 Vote Already Cast</h3>
                      <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                        Your identity has successfully registered a transaction on the ledger for this election. 
                        To guarantee system integrity, duplicate votes are blocked.
                      </p>
                    </div>
                  ) : (
                    <>
                      <h3 style={{ marginTop: '2rem', fontSize: '1.2rem' }}>Choose your Candidate:</h3>
                      <div className="candidates-grid">
                        {selectedElection.candidates?.map(c => (
                          <div 
                            key={c.id}
                            className={`candidate-card ${selectedCandidate?.id === c.id ? 'selected' : ''}`}
                            onClick={() => setSelectedCandidate(c)}
                          >
                            <div className="candidate-avatar">
                              <img src={c.photoUrl} alt={c.name} style={{ width: '100%', borderRadius: '50%' }} />
                            </div>
                            <h4 style={{ fontSize: '1.05rem' }}>{c.name}</h4>
                            <div className="party-badge">{c.party}</div>
                          </div>
                        ))}
                      </div>

                      {/* Electronic Signature & Token Check */}
                      <div style={{ marginTop: '2.5rem', borderTop: '1px solid var(--glass-border)', paddingTop: '2rem' }}>
                        <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>🔏 Cryptographic Signature Verification</h3>
                        <div className="input-group">
                          <label>Upload Electronic Signature Certificate (.p12 / XML)</label>
                          <input 
                            type="file" 
                            accept=".p12,.xml,.pem,.txt"
                            className="input-field" 
                            onChange={handleSignatureUpload}
                          />
                        </div>
                        {signatureText && (
                          <div style={{ background: '#090a0f', padding: '0.75rem', borderRadius: '6px', fontSize: '0.8rem', fontFamily: 'monospace', color: 'var(--text-secondary)', marginBottom: '1.5rem', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            <strong>Certificate Hash:</strong> {signatureText}
                          </div>
                        )}

                        {/* Idempotency key tracking info */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                            <strong>Session Idempotency Key:</strong> <code style={{ color: 'var(--accent-cyan)' }}>{idempotencyKey.substring(0, 15)}...</code>
                          </span>
                          <button className="btn btn-secondary" style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }} onClick={regenerateIdempotencyKey}>Reset Key</button>
                        </div>

                        {isScanning ? (
                          <div className="verification-overlay">
                            <div className="scanner-box">
                              <div className="signature-icon">🔏</div>
                            </div>
                            <h4 style={{ color: 'var(--accent-cyan)' }}>{scanStatus}</h4>
                            <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem', fontSize: '0.8rem' }}>
                              <span style={{ color: scanSteps.mintel ? '#00ff66' : '#555' }}>MINTEL ✔</span>
                              <span style={{ color: scanSteps.arcotel ? '#00ff66' : '#555' }}>ARCOTEL ✔</span>
                              <span style={{ color: scanSteps.firmaEc ? '#00ff66' : '#555' }}>FirmaEc ✔</span>
                            </div>
                          </div>
                        ) : (
                          <button 
                            className="btn" 
                            style={{ width: '100%', background: 'var(--gradient-primary)' }}
                            disabled={!selectedCandidate || !signatureText}
                            onClick={executeVotingFlow}
                          >
                            Cast Secure Vote
                          </button>
                        )}
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Results aggregation */}
            {selectedElection && (
              <div className="glass-card" style={{ maxWidth: '100%' }}>
                <div className="section-title">
                  <h2>Real-Time Election Standings</h2>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                  Decoupled audit figures read directly from replica database slave nodes to preserve writing speeds.
                </p>

                <div className="svg-chart-container">
                  {selectedElection.candidates?.map(c => {
                    const pct = getCandidatePercentage(c.id);
                    const count = getCandidateVotesCount(c.id);
                    return (
                      <div className="chart-bar-row" key={c.id}>
                        <div className="chart-bar-header">
                          <span>{c.name} ({c.party})</span>
                          <strong>{pct}% ({count} votes)</strong>
                        </div>
                        <div className="chart-bar-outer">
                          <div className="chart-bar-inner" style={{ width: `${pct}%` }}></div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Column 2: Public Ledger Ledger */}
          <div className="glass-card" style={{ maxWidth: '100%' }}>
            <div className="section-title">
              <h2>Public Ledger Audit Ledger</h2>
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Sequential logical timestamp logs exposing SHA-256 voting block hashes. Voter identity hashes are decoupled from public details.
            </p>

            <div className="ledger-log">
              {ledger.length === 0 ? (
                <div style={{ padding: '2rem', textAlign: 'center', color: '#555' }}>
                  No transaction blocks registered on ledger yet.
                </div>
              ) : (
                ledger.map((logItem) => (
                  <div className="ledger-item" key={logItem.id}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                      <span style={{ color: '#00ff66' }}>[BLOCK #{logItem.id}]</span>
                      <span style={{ color: '#aaa' }}>Logical Time: {logItem.logicalTimestamp}</span>
                    </div>
                    <div>
                      <strong>TxHash:</strong> <span className="hash-text">{logItem.transactionHash}</span>
                    </div>
                    <div>
                      <strong>Voter ID Hash:</strong> <span style={{ color: '#ffc107' }}>{logItem.citizenIdHash.substring(0, 16)}...</span>
                    </div>
                    <div className="sig-text">
                      <strong>Signature Certificate CA:</strong> {logItem.signature.substring(0, 30)}...
                    </div>
                  </div>
                ))
              )}
            </div>

            <div style={{ marginTop: '1.5rem', textAlign: 'center' }}>
              <button 
                className="btn btn-secondary" 
                style={{ width: '100%', fontSize: '0.9rem' }}
                onClick={fetchLedger}
              >
                🔄 Refresh Ledger Logs
              </button>
            </div>
          </div>

        </div>
      )}
    </div>
  );
}
