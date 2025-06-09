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
  const [newName, setNewName] = useState("");

  const handleAddAccount = () => {
    if (!newName.trim() || accounts.length >= MAX_ACCOUNTS) return;
    const newAccount = {
      name: newName,
      image:
        "https://velog.velcdn.com/images/bbaekddo/post/e42ea147-4df5-4e9f-96e8-d5dfb261f4f6/image.png",
    };
    setAccounts([...accounts, newAccount]);
    setNewName("");
    setShowModal(false);
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
            <div key={index} className="account-card">
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
            <h3>새 계정 추가</h3>
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="계정 이름을 입력하세요"
            />
            <div className="modal-buttons">
              <button onClick={handleAddAccount}>추가</button>
              <button onClick={() => setShowModal(false)}>취소</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
