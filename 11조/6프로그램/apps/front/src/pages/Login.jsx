import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import "./Login.css";

export default function LoginPage() {
  const navigate = useNavigate();
  const [uid, setUid] = useState("");
  const [password, setPassword] = useState("");
  const [uidSave, setUidSave] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    const savedUid = localStorage.getItem("uid");
    if (savedUid) {
      setUid(savedUid);
      setUidSave(true);
    }
  }, []);

  const handleLogin = (e) => {
    e.preventDefault();
    if (uidSave) {
      localStorage.setItem("uid", uid);
    } else {
      localStorage.removeItem("uid");
    }
    console.log("로그인 시도:", { uid, password });
  };

  return (
    <div className="login-container">
      <form onSubmit={handleLogin} className="login-form">
        <input
          type="text"
          placeholder="아이디"
          value={uid}
          onChange={(e) => setUid(e.target.value)}
          className="input-field"
        />

        <div className="input-wrapper">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input-field"
          />
          <button
            type="button"
            className="show-password"
            onClick={() => setShowPassword(!showPassword)}
          >
            👁
          </button>
        </div>

        <div className="options-row">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={uidSave}
              onChange={() => setUidSave(!uidSave)}
            />
            <span>아이디 저장</span>
          </label>
        </div>

        <button type="submit" className="login-button">
          로그인
        </button>

        <div className="signup-row">
          <span>신규회원이신가요? </span>
          <Link to="/auth/signup" className="link-text bold">
            회원가입
          </Link>
        </div>
      </form>
    </div>
  );
}
