import axios from "axios";

const axiosInstance = axios.create({
  baseURL: `${import.meta.env.VITE_API}`,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

// 요청 인터셉터: 토큰 설정
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 토큰 갱신 또는 401 처리
axiosInstance.interceptors.response.use(
  (response) => {
    console.log("응답 헤더 전체:", response); // 디버깅용

    // 대소문자 구분 없이 Authorization 헤더 찾기
    const authHeader =
      response.headers["authorization"] || response.headers["Authorization"];

    console.log("찾은 Authorization 헤더:", authHeader); // 디버깅용

    if (authHeader && authHeader.startsWith("Bearer ")) {
      const newToken = authHeader.substring(7).trim(); // "Bearer " 제거 (7글자)
      console.log("추출된 토큰:", newToken); // 디버깅용
      console.log("기존 토큰:", localStorage.getItem("token")); // 디버깅용

      localStorage.setItem("token", newToken);
      console.log("토큰 저장 후:", localStorage.getItem("token")); // 디버깅용
    }
    return response;
  },
  (error) => {
    console.log("에러 발생:", error);
    console.log("에러 응답:", error.response);

    if (error.response) {
      console.log("응답 상태 코드:", error.response.status);

      if (error.response.status === 401) {
        console.log("401 에러 - 인증 실패");
        localStorage.removeItem("token");
        console.log("토큰 제거 완료");

        // 여러 방법으로 시도
        try {
          console.log("현재 경로:", window.location.pathname);

          if (window.location.pathname !== "/login") {
            console.log("로그인 페이지로 이동 시도");

            // 방법 1: replace 사용
            window.location.replace("/login");

            // 방법 2: href 사용 (백업)
            setTimeout(() => {
              if (window.location.pathname !== "/login") {
                window.location.href = "/login";
              }
            }, 100);
          }
        } catch (redirectError) {
          console.error("리다이렉트 에러:", redirectError);
        }
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
