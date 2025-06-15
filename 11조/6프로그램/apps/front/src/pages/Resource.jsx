import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Resource.css";
import Header from "../components/Header";
import Button from "../components/Button";
import axiosInstance from "../utils/axiosInstance";

export default function Resource() {
  const nav = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    accountAlias: "",
    awsAccountId: "",
    accessKeyId: "",
    secretAccessKey: "",
    region: "us-east-1",
  });

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axiosInstance.get(
        "/resource-service/api/aws-accounts"
      );
      setAccounts(response.data || []);
    } catch (err) {
      setError(
        err.response?.data?.message || "계정 목록을 불러오는데 실패했습니다."
      );
      console.error("Error fetching accounts:", err);
      setAccounts([]);
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트 마운트 시 계정 목록 조회
  useEffect(() => {
    fetchAccounts();
  }, []);

  // 폼 데이터 변경 핸들러
  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // 계정 추가
  const handleAddAccount = async () => {
    // 필수 필드 검증
    if (
      !formData.accountAlias.trim() ||
      !formData.awsAccountId.trim() ||
      !formData.accessKeyId.trim() ||
      !formData.secretAccessKey.trim() ||
      !formData.region.trim()
    ) {
      alert("모든 필드를 입력해주세요.");
      return;
    }

    try {
      const response = await axiosInstance.post(
        "/resource-service/api/aws-accounts",
        formData
      );

      if (response.status === 200 || response.status === 201) {
        // 성공적으로 등록되면 목록 새로고침
        await fetchAccounts();

        // 폼 초기화 및 모달 닫기
        setFormData({
          accountAlias: "",
          awsAccountId: "",
          accessKeyId: "",
          secretAccessKey: "",
          region: "us-east-1",
        });
        setShowModal(false);
      }
    } catch (err) {
      alert(err.response?.data?.message || "계정 등록에 실패했습니다.");
      console.error("Error adding account:", err);
    }
  };

  // 계정 카드 클릭 핸들러
  const handleCardClick = (accountId) => {
    nav(`/dashboard/${accountId}`);
  };

  // 모달 닫기
  const handleCloseModal = () => {
    setFormData({
      accountAlias: "",
      awsAccountId: "",
      accessKeyId: "",
      secretAccessKey: "",
      region: "us-east-1",
    });
    setShowModal(false);
  };

  if (loading) {
    return (
      <>
        <Header
          title={"리소스 계정"}
          leftChild={<Button onClick={() => nav(-1)} text={"< 뒤로가기"} />}
          rightChild={
            <Button
              onClick={() => nav("/mypage")}
              text={"마이페이지"}
              type="default"
            />
          }
        />
        <div className="account-container">
          <div className="loading">계정 목록을 불러오는 중...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Header
        title={"리소스 계정"}
        leftChild={<Button onClick={() => nav(-1)} text={"< 뒤로가기"} />}
        rightChild={
          <Button
            onClick={() => nav("/mypage")}
            text={"마이페이지"}
            type="default"
          />
        }
      />

      <div className="account-container">
        {error && (
          <div className="error-banner">
            <p>⚠️ {error}</p>
            <button onClick={fetchAccounts} className="retry-button">
              다시 시도
            </button>
          </div>
        )}

        <div className="account-grid">
          {accounts.map((account) => (
            <div
              key={account.id}
              className="account-card"
              onClick={() => handleCardClick(account.id)}
            >
              <img
                src="https://velog.velcdn.com/images/bbaekddo/post/e42ea147-4df5-4e9f-96e8-d5dfb261f4f6/image.png"
                alt={account.accountAlias}
                className="account-image"
              />
              <p>{account.accountAlias}</p>
            </div>
          ))}

          <div
            className="account-card add-card"
            onClick={() => setShowModal(true)}
          >
            <span className="plus-sign">+</span>
            <p>계정 추가</p>
          </div>
        </div>

        {accounts.length === 0 && !error && (
          <div className="empty-state">
            <p>등록된 AWS 계정이 없습니다.</p>
            <p>+ 버튼을 클릭하여 첫 계정을 추가해보세요.</p>
          </div>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>AWS IAM 계정 등록</h3>

            <input
              type="text"
              value={formData.accountAlias}
              onChange={(e) =>
                handleInputChange("accountAlias", e.target.value)
              }
              placeholder="계정 별칭 (Account Alias)"
            />

            <input
              type="text"
              value={formData.awsAccountId}
              onChange={(e) =>
                handleInputChange("awsAccountId", e.target.value)
              }
              placeholder="AWS 계정 ID"
            />

            <input
              type="text"
              value={formData.accessKeyId}
              onChange={(e) => handleInputChange("accessKeyId", e.target.value)}
              placeholder="Access Key ID"
            />

            <input
              type="password"
              value={formData.secretAccessKey}
              onChange={(e) =>
                handleInputChange("secretAccessKey", e.target.value)
              }
              placeholder="Secret Access Key"
            />

            <select
              value={formData.region}
              onChange={(e) => handleInputChange("region", e.target.value)}
            >
              <option value="us-east-1">US East (N. Virginia)</option>
              <option value="us-west-2">US West (Oregon)</option>
              <option value="ap-northeast-2">Asia Pacific (Seoul)</option>
              <option value="ap-northeast-1">Asia Pacific (Tokyo)</option>
              <option value="eu-west-1">Europe (Ireland)</option>
              <option value="eu-central-1">Europe (Frankfurt)</option>
            </select>

            <div className="modal-buttons">
              <button onClick={handleAddAccount}>등록</button>
              <button onClick={handleCloseModal}>취소</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
