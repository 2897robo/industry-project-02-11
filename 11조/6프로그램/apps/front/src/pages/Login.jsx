import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import Button from "../components/Button";
import "./Login.css";
import axiosInstance from "../utils/axiosInstance";
import Header from "../components/Header";

export default function LoginPage() {
  const navigate = useNavigate();
  const [uid, setUid] = useState("");
  const [password, setPassword] = useState("");
  const [uidSave, setUidSave] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const nav = useNavigate();

  useEffect(() => {
    const savedUid = localStorage.getItem("uid");
    if (savedUid) {
      setUid(savedUid);
      setUidSave(true);
    }
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();
    if (uidSave) {
      localStorage.setItem("uid", uid);
    } else {
      localStorage.removeItem("uid");
    }
    console.log("로그인 시도:", { uid, password });

    try {
      const response = await axiosInstance.post(
        "/auth-service/auth/login",
        {
          uid,
          password,
        },
        {
          withCredentials: true,
        }
      );

      console.log(response);

      const { accessToken } = response.data || {};
      if (!accessToken) {
        throw new Error("토큰이 존재하지 않습니다.");
      }
      localStorage.setItem("token", accessToken);

      console.log("로그인 성공:", response.data);
      navigate("/");
    } catch (error) {
      console.error("로그인 실패:", error);
      alert("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    <>
      <Header
        title="로그인"
        leftChild={
          <Button
            onClick={() => {
              nav(-1);
            }}
            text={"< 뒤로가기"}
          />
        }
      />
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

          <Button type="LOGIN" text="로그인" />

          <div className="signup-row">
            <span>신규회원이신가요? </span>
            <Link to="/register" className="link-text bold">
              회원가입
            </Link>
          </div>
        </form>
      </div>
    </>
  );
}
