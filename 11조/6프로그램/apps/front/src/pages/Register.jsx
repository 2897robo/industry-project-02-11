import React, { useState } from "react";
import "./Register.css";
import Button from "../components/Button";
import Input from "../components/Input";
import axiosInstance from "../utils/axiosInstance";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";

const Register = () => {
  const [uid, setUid] = useState("");
  const [uidCheck, setUidCheck] = useState(false);
  const [checkMessage, setCheckMessage] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isCheckingUid, setIsCheckingUid] = useState(false);
  const nav = useNavigate();

  const handleUidChange = (e) => {
    setUid(e.target.value);
    setUidCheck(false);
    setCheckMessage("");
    setErrorMessage("");
  };

  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
    setErrorMessage("");
  };

  const handlePasswordConfirmChange = (e) => {
    setPasswordConfirm(e.target.value);
    setErrorMessage("");
  };

  const handleUsernameChange = (e) => {
    setUsername(e.target.value);
    setErrorMessage("");
  };

  const handleEmailChange = (e) => {
    setEmail(e.target.value);
    setErrorMessage("");
  };

  const validateUid = (uid) => /^[a-zA-Z0-9]{4,20}$/.test(uid);

  const validatePassword = (password) =>
    /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/.test(
      password
    );

  const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleUidCheck = async () => {
    if (!uid.trim()) {
      setCheckMessage("아이디를 입력해주세요.");
      setUidCheck(false);
      return;
    }

    if (!validateUid(uid)) {
      setCheckMessage("아이디는 영문, 숫자 조합으로 4-20자여야 합니다.");
      setUidCheck(false);
      return;
    }

    setIsCheckingUid(true);

    try {
      const response = await axiosInstance.get("/user-service/users/check", {
        params: { uid: uid.trim() },
      });

      const isAvailable = response.data === true;

      if (!isAvailable) {
        setUidCheck(true);
        setCheckMessage("사용 가능한 아이디입니다.");
      } else {
        setUidCheck(false);
        setCheckMessage("중복되는 아이디입니다. 다른 아이디를 입력해주세요.");
      }
    } catch (error) {
      console.error("중복 확인 에러", error);
      if (error.response?.status === 400) {
        setCheckMessage("잘못된 요청입니다. 아이디를 확인해주세요.");
      } else if (error.response?.status >= 500) {
        setCheckMessage("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
      } else {
        setCheckMessage("네트워크 오류가 발생했습니다.");
      }
      setUidCheck(false);
    } finally {
      setIsCheckingUid(false);
    }
  };

  const handleSubmit = async () => {
    setErrorMessage("");

    if (!uid.trim()) {
      setErrorMessage("아이디를 입력해주세요.");
      return;
    }

    if (!uidCheck) {
      setErrorMessage("아이디 중복 확인이 필요합니다.");
      return;
    }

    if (!email.trim()) {
      setErrorMessage("이메일을 입력해주세요.");
      return;
    }

    if (!validateEmail(email)) {
      setErrorMessage("유효한 이메일 주소를 입력해주세요.");
      return;
    }

    if (!password) {
      setErrorMessage("비밀번호를 입력해주세요.");
      return;
    }

    if (!validatePassword(password)) {
      setErrorMessage(
        "비밀번호는 영문, 숫자, 특수문자 조합으로 8-20자여야 합니다."
      );
      return;
    }

    if (!passwordConfirm) {
      setErrorMessage("비밀번호 확인을 입력해주세요.");
      return;
    }

    if (password !== passwordConfirm) {
      setErrorMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
      return;
    }

    if (!username.trim()) {
      setErrorMessage("이름을 입력해주세요.");
      return;
    }

    if (username.trim().length < 2 || username.trim().length > 10) {
      setErrorMessage("이름은 2-10자여야 합니다.");
      return;
    }

    setIsLoading(true);

    try {
      const response = await axiosInstance.post("/user-service/users", {
        uid: uid.trim(),
        password,
        name: username.trim(),
        email: email.trim(),
      });

      if (
        response.status === 200 ||
        response.status === 201 ||
        response.status === 204
      ) {
        alert("회원가입이 완료되었습니다!");
        nav("/login");
      } else {
        setErrorMessage("회원가입에 실패했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("회원가입 에러", error);
      if (error.response?.status === 400) {
        setErrorMessage("입력 정보를 확인해주세요.");
      } else if (error.response?.status === 409) {
        setErrorMessage("이미 존재하는 아이디입니다.");
        setUidCheck(false);
        setCheckMessage("중복 확인이 필요해요.");
      } else if (error.response?.status >= 500) {
        setErrorMessage("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
      } else {
        setErrorMessage("네트워크 오류가 발생했습니다.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="signup-form">
      <Header
        title={"회원가입"}
        leftChild={<Button onClick={() => nav(-1)} text={"< 뒤로가기"} />}
      />

      <div className="form-row">
        <Input
          type="text"
          name="uid"
          value={uid}
          onChange={handleUidChange}
          className="input-register"
          placeholder="아이디를 입력해주세요."
          disabled={isLoading}
        />
        <Button
          text={isCheckingUid ? "확인중..." : "중복확인"}
          type="CONFIRM"
          onClick={handleUidCheck}
          disabled={isCheckingUid || isLoading || !uid.trim()}
        />
      </div>

      {checkMessage && (
        <p className={`check-message ${uidCheck ? "success" : "error"}`}>
          {checkMessage}
        </p>
      )}

      {/* ✅ 이메일 입력 필드 */}
      <Input
        type="email"
        name="email"
        value={email}
        onChange={handleEmailChange}
        className="input"
        placeholder="이메일을 입력해주세요."
        disabled={isLoading}
      />

      <Input
        type="password"
        name="password"
        value={password}
        onChange={handlePasswordChange}
        className="input"
        placeholder="비밀번호를 입력해주세요."
        disabled={isLoading}
      />

      <Input
        type="password"
        name="passwordConfirm"
        value={passwordConfirm}
        onChange={handlePasswordConfirmChange}
        className="input"
        placeholder="비밀번호를 다시 입력해주세요."
        disabled={isLoading}
      />

      <Input
        type="text"
        name="username"
        value={username}
        onChange={handleUsernameChange}
        className="input"
        placeholder="이름을 입력해주세요."
        disabled={isLoading}
      />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <Button
        text={isLoading ? "가입중..." : "가입하기"}
        className="submit-btn"
        type="REGISTER"
        onClick={handleSubmit}
        disabled={isLoading}
      />
    </div>
  );
};

export default Register;
