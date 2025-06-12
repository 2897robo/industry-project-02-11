import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Resource.css";
import Header from "../components/Header";
import Button from "../components/Button";

const MAX_ACCOUNTS = 6;

export default function Resource() {
  const nav = useNavigate();
  const [accounts, setAccounts] = useState([
    {
      name: "aws계정",
      image:
        "https://velog.velcdn.com/images/bbaekddo/post/e42ea147-4df5-4e9f-96e8-d5dfb261f4f6/image.png",
    },
  ]);

  const [showModal, setShowModal] = useState(false);
  const [accessKey, setAccessKey] = useState("");
  const [secretKey, setSecretKey] = useState("");

  const handleAddAccount = () => {
    if (
      !accessKey.trim() ||
      !secretKey.trim() ||
      accounts.length >= MAX_ACCOUNTS
    )
      return;

    const newAccount = {
      name: accessKey, // 임시로 accessKey를 ID로 활용 (별도 ID 사용 시 조정)
      image:
        "https://velog.velcdn.com/images/bbaekddo/post/e42ea147-4df5-4e9f-96e8-d5dfb261f4f6/image.png",
      accessKey,
      secretKey,
    };

    setAccounts([...accounts, newAccount]);
    setAccessKey("");
    setSecretKey("");
    setShowModal(false);
  };

  const handleCardClick = (resourceId) => {
    nav(`/dashboard/${resourceId}`);
  };

  return (
    <>
      <Header
        title={"리소스 계정"}
        leftChild={<Button onClick={() => nav(-1)} text={"< 뒤로가기"} />}
      />

      <div className="account-container">
        <div className="account-grid">
          {accounts.map((account, index) => (
            <div
              key={index}
              className="account-card"
              onClick={() => handleCardClick(account.name)}
            >
              <img
                src={account.image}
                alt={account.name}
                className="account-image"
              />
              <p>{account.name}</p>
            </div>
          ))}
          {accounts.length < MAX_ACCOUNTS && (
            <div
              className="account-card add-card"
              onClick={() => setShowModal(true)}
            >
              <span className="plus-sign">+</span>
            </div>
          )}
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>AWS IAM 계정 등록</h3>
            <input
              type="text"
              value={accessKey}
              onChange={(e) => setAccessKey(e.target.value)}
              placeholder="Access Key ID"
            />
            <input
              type="password"
              value={secretKey}
              onChange={(e) => setSecretKey(e.target.value)}
              placeholder="Secret Access Key"
            />
            <div className="modal-buttons">
              <button onClick={handleAddAccount}>등록</button>
              <button onClick={() => setShowModal(false)}>취소</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
