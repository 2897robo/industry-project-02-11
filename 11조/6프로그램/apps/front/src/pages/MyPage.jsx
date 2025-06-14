import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  User,
  Mail,
  Lock,
  Save,
  Eye,
  EyeOff,
  CheckCircle,
  AlertCircle,
  Edit3,
} from "lucide-react";
import Header from "../components/Header";
import Button from "../components/Button";
import axiosInstance from "../utils/axiosInstance";

const MyProfile = () => {
  const nav = useNavigate();

  // 사용자 정보 상태
  const [userInfo, setUserInfo] = useState({
    uid: "",
    name: "",
    email: "",
  });

  // 폼 상태
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  // UI 상태
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });
  const [editMode, setEditMode] = useState({
    name: false,
    email: false,
    password: false,
  });

  // 사용자 정보 가져오기
  const fetchUserInfo = async () => {
    try {
      setLoading(true);
      const response = await axiosInstance.get("/user-service/users");
      const userData = response.data;

      setUserInfo({
        uid: userData.uid || "",
        name: userData.name || "",
        email: userData.email || "",
      });

      setFormData((prev) => ({
        ...prev,
        name: userData.name || "",
        email: userData.email || "",
      }));
    } catch (error) {
      console.error("사용자 정보 로드 실패:", error);
      setMessage({
        type: "error",
        text: "사용자 정보를 불러올 수 없습니다.",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  // 입력값 변경 처리
  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    // 메시지 초기화
    if (message.text) {
      setMessage({ type: "", text: "" });
    }
  };

  // 편집 모드 토글
  const toggleEditMode = (field) => {
    setEditMode((prev) => ({
      ...prev,
      [field]: !prev[field],
    }));

    // 편집 취소 시 원래 값으로 복원
    if (editMode[field]) {
      if (field === "name") {
        setFormData((prev) => ({ ...prev, name: userInfo.name }));
      } else if (field === "email") {
        setFormData((prev) => ({ ...prev, email: userInfo.email }));
      } else if (field === "password") {
        setFormData((prev) => ({
          ...prev,
          currentPassword: "",
          newPassword: "",
          confirmPassword: "",
        }));
      }
    }
  };

  // 개별 필드 저장
  const saveField = async (field) => {
    try {
      setSaving(true);

      let requestData = {};
      let endpoint = "";

      if (field === "name") {
        if (!formData.name.trim()) {
          setMessage({ type: "error", text: "이름을 입력해주세요." });
          return;
        }
        requestData = { name: formData.name.trim() };
        endpoint = "/user-service/users";
      } else if (field === "email") {
        if (!formData.email.trim()) {
          setMessage({ type: "error", text: "이메일을 입력해주세요." });
          return;
        }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(formData.email)) {
          setMessage({
            type: "error",
            text: "올바른 이메일 형식을 입력해주세요.",
          });
          return;
        }
        requestData = { email: formData.email.trim() };
        endpoint = "/user-service/users";
      } else if (field === "password") {
        if (!formData.currentPassword) {
          setMessage({ type: "error", text: "현재 비밀번호를 입력해주세요." });
          return;
        }
        if (!formData.newPassword) {
          setMessage({ type: "error", text: "새 비밀번호를 입력해주세요." });
          return;
        }
        if (formData.newPassword.length < 6) {
          setMessage({
            type: "error",
            text: "새 비밀번호는 6자 이상이어야 합니다.",
          });
          return;
        }
        if (formData.newPassword !== formData.confirmPassword) {
          setMessage({
            type: "error",
            text: "새 비밀번호 확인이 일치하지 않습니다.",
          });
          return;
        }
        requestData = {
          currentPassword: formData.currentPassword,
          newPassword: formData.newPassword,
        };
        endpoint = "/user-service/users";
      }

      await axiosInstance.put(endpoint, requestData);

      // 성공 시 사용자 정보 업데이트
      if (field === "name") {
        setUserInfo((prev) => ({ ...prev, name: formData.name.trim() }));
      } else if (field === "email") {
        setUserInfo((prev) => ({ ...prev, email: formData.email.trim() }));
      } else if (field === "password") {
        setFormData((prev) => ({
          ...prev,
          currentPassword: "",
          newPassword: "",
          confirmPassword: "",
        }));
      }

      setEditMode((prev) => ({ ...prev, [field]: false }));
      setMessage({
        type: "success",
        text: `${
          field === "name" ? "이름" : field === "email" ? "이메일" : "비밀번호"
        }이 성공적으로 업데이트되었습니다.`,
      });
    } catch (error) {
      console.error(`${field} 업데이트 실패:`, error);
      const errorMessage =
        error.response?.data?.message || `${field} 업데이트에 실패했습니다.`;
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setSaving(false);
    }
  };

  const styles = {
    container: {
      minHeight: "100vh",
      backgroundColor: "#f9fafb",
      padding: "24px",
    },
    maxWidth: {
      maxWidth: "800px",
      margin: "0 auto",
    },
    card: {
      backgroundColor: "white",
      borderRadius: "12px",
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
      padding: "32px",
    },
    header: {
      textAlign: "center",
      marginBottom: "32px",
    },
    profileIcon: {
      width: "80px",
      height: "80px",
      backgroundColor: "#e5e7eb",
      borderRadius: "50%",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      margin: "0 auto 16px",
    },
    title: {
      fontSize: "24px",
      fontWeight: "700",
      color: "#1f2937",
      marginBottom: "8px",
    },
    subtitle: {
      fontSize: "16px",
      color: "#6b7280",
    },
    form: {
      display: "flex",
      flexDirection: "column",
      gap: "24px",
    },
    fieldGroup: {
      display: "flex",
      flexDirection: "column",
      gap: "8px",
    },
    label: {
      fontSize: "14px",
      fontWeight: "600",
      color: "#374151",
      display: "flex",
      alignItems: "center",
      gap: "8px",
    },
    inputContainer: {
      position: "relative",
    },
    input: {
      width: "90%",
      padding: "12px 16px",
      border: "1px solid #d1d5db",
      borderRadius: "8px",
      fontSize: "14px",
      transition: "border-color 0.2s, box-shadow 0.2s",
      backgroundColor: "#ffffff",
    },
    inputFocused: {
      borderColor: "#2563eb",
      boxShadow: "0 0 0 3px rgba(37, 99, 235, 0.1)",
      outline: "none",
    },
    inputDisabled: {
      backgroundColor: "#f9fafb",
      color: "#6b7280",
      cursor: "not-allowed",
    },
    inputGroup: {
      display: "flex",
      gap: "12px",
      alignItems: "flex-end",
    },
    inputWithButton: {
      flex: 1,
    },
    passwordInputContainer: {
      position: "relative",
    },
    passwordToggle: {
      position: "absolute",
      right: "12px",
      top: "50%",
      transform: "translateY(-50%)",
      background: "none",
      border: "none",
      cursor: "pointer",
      padding: "4px",
      borderRadius: "4px",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
    },
    button: {
      padding: "8px 16px",
      borderRadius: "6px",
      fontSize: "14px",
      fontWeight: "500",
      cursor: "pointer",
      transition: "all 0.2s",
      border: "1px solid",
      display: "flex",
      alignItems: "center",
      gap: "6px",
      minWidth: "80px",
      justifyContent: "center",
    },
    primaryButton: {
      backgroundColor: "#2563eb",
      color: "white",
      borderColor: "#2563eb",
    },
    primaryButtonHover: {
      backgroundColor: "#1d4ed8",
    },
    secondaryButton: {
      backgroundColor: "white",
      color: "#374151",
      borderColor: "#d1d5db",
    },
    secondaryButtonHover: {
      backgroundColor: "#f9fafb",
    },
    dangerButton: {
      backgroundColor: "#dc2626",
      color: "white",
      borderColor: "#dc2626",
    },
    editButton: {
      backgroundColor: "#f59e0b",
      color: "white",
      borderColor: "#f59e0b",
    },
    message: {
      padding: "12px 16px",
      borderRadius: "8px",
      fontSize: "14px",
      display: "flex",
      alignItems: "center",
      gap: "8px",
      marginBottom: "16px",
    },
    successMessage: {
      backgroundColor: "#d1fae5",
      color: "#065f46",
      border: "1px solid #a7f3d0",
    },
    errorMessage: {
      backgroundColor: "#fee2e2",
      color: "#991b1b",
      border: "1px solid #fca5a5",
    },
    loadingContainer: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      height: "200px",
    },
    spinner: {
      width: "32px",
      height: "32px",
      border: "2px solid #e5e7eb",
      borderTop: "2px solid #2563eb",
      borderRadius: "50%",
      animation: "spin 1s linear infinite",
    },
    loadingText: {
      marginTop: "12px",
      color: "#6b7280",
    },
  };

  const cssAnimation = `
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `;

  if (loading) {
    return (
      <>
        <Header
          title="마이페이지"
          leftChild={<Button onClick={() => nav(-1)} text="< 뒤로가기" />}
          rightChild={
            <Button
              onClick={() => nav("/")}
              text={"마이페이지"}
              type="default"
            />
          }
        />
        <div style={styles.container}>
          <style>{cssAnimation}</style>
          <div style={styles.maxWidth}>
            <div style={styles.card}>
              <div style={styles.loadingContainer}>
                <div style={styles.spinner}></div>
                <span style={styles.loadingText}>
                  사용자 정보를 불러오는 중...
                </span>
              </div>
            </div>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Header
        title="마이페이지"
        leftChild={<Button onClick={() => nav(-1)} text="< 뒤로가기" />}
      />
      <div style={styles.container}>
        <style>{cssAnimation}</style>
        <div style={styles.maxWidth}>
          <div style={styles.card}>
            <div style={styles.header}>
              <div style={styles.profileIcon}>
                <User size={40} color="#6b7280" />
              </div>
              <h1 style={styles.title}>내 프로필</h1>
              <p style={styles.subtitle}>계정 정보를 관리하세요</p>
            </div>

            {message.text && (
              <div
                style={{
                  ...styles.message,
                  ...(message.type === "success"
                    ? styles.successMessage
                    : styles.errorMessage),
                }}
              >
                {message.type === "success" ? (
                  <CheckCircle size={16} />
                ) : (
                  <AlertCircle size={16} />
                )}
                {message.text}
              </div>
            )}

            <div style={styles.form}>
              {/* UID 필드 (읽기 전용) */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>
                  <User size={16} />
                  사용자 ID
                </label>
                <input
                  type="text"
                  value={userInfo.uid}
                  disabled
                  style={{
                    ...styles.input,
                    ...styles.inputDisabled,
                  }}
                />
              </div>

              {/* 이름 필드 */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>
                  <User size={16} />
                  이름
                </label>
                <div style={styles.inputGroup}>
                  <div style={styles.inputWithButton}>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) =>
                        handleInputChange("name", e.target.value)
                      }
                      disabled={!editMode.name}
                      style={{
                        ...styles.input,
                        ...(!editMode.name ? styles.inputDisabled : {}),
                      }}
                      placeholder="이름을 입력하세요"
                    />
                  </div>
                  {!editMode.name ? (
                    <button
                      style={{
                        ...styles.button,
                        ...styles.editButton,
                      }}
                      onClick={() => toggleEditMode("name")}
                    >
                      <Edit3 size={14} />
                      편집
                    </button>
                  ) : (
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        style={{
                          ...styles.button,
                          ...styles.primaryButton,
                        }}
                        onClick={() => saveField("name")}
                        disabled={saving}
                      >
                        <Save size={14} />
                        저장
                      </button>
                      <button
                        style={{
                          ...styles.button,
                          ...styles.secondaryButton,
                        }}
                        onClick={() => toggleEditMode("name")}
                        disabled={saving}
                      >
                        취소
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* 이메일 필드 */}
              <div style={styles.fieldGroup}>
                <label style={styles.label}>
                  <Mail size={16} />
                  이메일
                </label>
                <div style={styles.inputGroup}>
                  <div style={styles.inputWithButton}>
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) =>
                        handleInputChange("email", e.target.value)
                      }
                      disabled={!editMode.email}
                      style={{
                        ...styles.input,
                        ...(!editMode.email ? styles.inputDisabled : {}),
                      }}
                      placeholder="이메일을 입력하세요"
                    />
                  </div>
                  {!editMode.email ? (
                    <button
                      style={{
                        ...styles.button,
                        ...styles.editButton,
                      }}
                      onClick={() => toggleEditMode("email")}
                    >
                      <Edit3 size={14} />
                      편집
                    </button>
                  ) : (
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        style={{
                          ...styles.button,
                          ...styles.primaryButton,
                        }}
                        onClick={() => saveField("email")}
                        disabled={saving}
                      >
                        <Save size={14} />
                        저장
                      </button>
                      <button
                        style={{
                          ...styles.button,
                          ...styles.secondaryButton,
                        }}
                        onClick={() => toggleEditMode("email")}
                        disabled={saving}
                      >
                        취소
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* 비밀번호 변경 섹션 */}
              <div
                style={{
                  marginTop: "24px",
                  paddingTop: "24px",
                  borderTop: "1px solid #e5e7eb",
                }}
              >
                <div style={styles.fieldGroup}>
                  <label style={styles.label}>
                    <Lock size={16} />
                    비밀번호 변경
                  </label>

                  {!editMode.password ? (
                    <button
                      style={{
                        ...styles.button,
                        ...styles.editButton,
                        alignSelf: "flex-start",
                      }}
                      onClick={() => toggleEditMode("password")}
                    >
                      <Edit3 size={14} />
                      비밀번호 변경
                    </button>
                  ) : (
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "16px",
                      }}
                    >
                      {/* 현재 비밀번호 */}
                      <div style={styles.passwordInputContainer}>
                        <input
                          type={showCurrentPassword ? "text" : "password"}
                          value={formData.currentPassword}
                          onChange={(e) =>
                            handleInputChange("currentPassword", e.target.value)
                          }
                          style={styles.input}
                          placeholder="현재 비밀번호를 입력하세요"
                        />
                        <button
                          type="button"
                          style={styles.passwordToggle}
                          onClick={() =>
                            setShowCurrentPassword(!showCurrentPassword)
                          }
                        >
                          {showCurrentPassword ? (
                            <EyeOff size={16} />
                          ) : (
                            <Eye size={16} />
                          )}
                        </button>
                      </div>

                      {/* 새 비밀번호 */}
                      <div style={styles.passwordInputContainer}>
                        <input
                          type={showNewPassword ? "text" : "password"}
                          value={formData.newPassword}
                          onChange={(e) =>
                            handleInputChange("newPassword", e.target.value)
                          }
                          style={styles.input}
                          placeholder="새 비밀번호를 입력하세요 (6자 이상)"
                        />
                        <button
                          type="button"
                          style={styles.passwordToggle}
                          onClick={() => setShowNewPassword(!showNewPassword)}
                        >
                          {showNewPassword ? (
                            <EyeOff size={16} />
                          ) : (
                            <Eye size={16} />
                          )}
                        </button>
                      </div>

                      {/* 새 비밀번호 확인 */}
                      <div style={styles.passwordInputContainer}>
                        <input
                          type={showConfirmPassword ? "text" : "password"}
                          value={formData.confirmPassword}
                          onChange={(e) =>
                            handleInputChange("confirmPassword", e.target.value)
                          }
                          style={styles.input}
                          placeholder="새 비밀번호를 다시 입력하세요"
                        />
                        <button
                          type="button"
                          style={styles.passwordToggle}
                          onClick={() =>
                            setShowConfirmPassword(!showConfirmPassword)
                          }
                        >
                          {showConfirmPassword ? (
                            <EyeOff size={16} />
                          ) : (
                            <Eye size={16} />
                          )}
                        </button>
                      </div>

                      <div style={{ display: "flex", gap: "8px" }}>
                        <button
                          style={{
                            ...styles.button,
                            ...styles.primaryButton,
                          }}
                          onClick={() => saveField("password")}
                          disabled={saving}
                        >
                          <Save size={14} />
                          비밀번호 변경
                        </button>
                        <button
                          style={{
                            ...styles.button,
                            ...styles.secondaryButton,
                          }}
                          onClick={() => toggleEditMode("password")}
                          disabled={saving}
                        >
                          취소
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default MyProfile;
