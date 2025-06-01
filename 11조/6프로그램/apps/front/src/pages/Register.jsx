import React, { useState } from "react";
import "./Register.css";
import Button from "../components/Button";
import Input from "../components/Input";

const Register = () => {
  const [uidCheck, setUidCheck] = useState(true);

  const handleUidCheck = () => {
    // const uid = watch("uid");
    // 가짜 중복 확인 로직
    // setUidCheck(uid !== "duplicate");
  };

  return (
    <div className="signup-form">
      <h1 className="signup-title">회원가입</h1>
      <div className="form-row">
        <Input
          type="text"
          name="uid"
          className="input-register"
          placeholder="아이디를 입력해주세요."
          // {...register("uid")}
        />
        <Button text="중복확인" type="CONFIRM" onClick={handleUidCheck} />
      </div>
      {!uidCheck && (
        <p className="error-text">
          중복되는 아이디입니다. 아이디를 확인해주세요.
        </p>
      )}
      <Input
        type="password"
        name="password"
        className="input"
        placeholder="비밀번호를 입력해주세요."
        // {...register("password")}
      />
      <Input
        type="password"
        name="passwordConfirm"
        className="input"
        placeholder="비밀번호를 다시 입력해주세요."
        // {...register("passwordConfirm")}
      />
      <Input
        type="text"
        name="username"
        className="input"
        placeholder="이름을 입력해주세요."
        // {...register("username")}
      />
      <Button text="가입하기" className="submit-btn" type="REGISTER" />
    </div>
  );
};

export default Register;
