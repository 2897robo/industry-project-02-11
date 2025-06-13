import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './MyPage.css';

function MyPage() {
  const [user, setUser] = useState(null);
  const [awsAccounts, setAwsAccounts] = useState([]);
  const [config, setConfig] = useState(null);
  const [showAddAccount, setShowAddAccount] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchUserInfo();
    fetchAwsAccounts();
    fetchConfig();
  }, []);

  const fetchUserInfo = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get('/user-service/users', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setUser(response.data);
    } catch (error) {
      console.error('Failed to fetch user info:', error);
      navigate('/login');
    }
  };

  const fetchAwsAccounts = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get('/resource-service/api/aws-accounts', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAwsAccounts(response.data);
    } catch (error) {
      console.error('Failed to fetch AWS accounts:', error);
    }
  };

  const fetchConfig = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get('/resource-service/api/configs', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setConfig(response.data);
    } catch (error) {
      console.error('Failed to fetch config:', error);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const handleAddAccount = async (accountData) => {
    try {
      const token = localStorage.getItem('token');
      await axios.post('/resource-service/api/aws-accounts', accountData, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setShowAddAccount(false);
      fetchAwsAccounts();
    } catch (error) {
      console.error('Failed to add AWS account:', error);
      alert('AWS 계정 추가 실패');
    }
  };

  return (
    <div className="mypage-container">
      <h1>마이페이지</h1>
      
      <div className="user-info-section">
        <h2>사용자 정보</h2>
        {user && (
          <div className="user-info">
            <p><strong>이름:</strong> {user.name}</p>
            <p><strong>이메일:</strong> {user.email}</p>
            <p><strong>ID:</strong> {user.uid}</p>
          </div>
        )}
        <button onClick={handleLogout} className="logout-btn">로그아웃</button>
      </div>

      <div className="aws-accounts-section">
        <h2>AWS 계정 관리</h2>
        <button onClick={() => setShowAddAccount(true)} className="add-account-btn">
          AWS 계정 추가
        </button>
        
        <div className="accounts-list">
          {awsAccounts.map(account => (
            <div key={account.id} className="account-card">
              <h3>{account.accountName}</h3>
              <p>Region: {account.region}</p>
              <p>Status: {account.isActive ? '활성' : '비활성'}</p>
            </div>
          ))}
        </div>
      </div>

      <div className="config-section">
        <h2>설정</h2>
        {config && (
          <div className="config-info">
            <p><strong>유휴 임계값:</strong> {config.idleThreshold}%</p>
            <p><strong>알림 활성화:</strong> {config.enableAlert ? '예' : '아니오'}</p>
          </div>
        )}
      </div>

      {showAddAccount && (
        <div className="modal">
          <div className="modal-content">
            <h3>AWS 계정 추가</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              const formData = new FormData(e.target);
              handleAddAccount({
                accountName: formData.get('accountName'),
                accessKey: formData.get('accessKey'),
                secretKey: formData.get('secretKey'),
                region: formData.get('region') || 'ap-northeast-2'
              });
            }}>
              <input name="accountName" placeholder="계정 이름" required />
              <input name="accessKey" placeholder="Access Key" required />
              <input name="secretKey" type="password" placeholder="Secret Key" required />
              <select name="region">
                <option value="ap-northeast-2">Asia Pacific (Seoul)</option>
                <option value="us-east-1">US East (N. Virginia)</option>
                <option value="us-west-2">US West (Oregon)</option>
                <option value="eu-west-1">EU (Ireland)</option>
              </select>
              <button type="submit">추가</button>
              <button type="button" onClick={() => setShowAddAccount(false)}>취소</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default MyPage;
