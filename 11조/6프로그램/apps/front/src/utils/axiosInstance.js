import axios from "axios";

const axiosInstance = axios.create({
  baseURL: `${import.meta.env.VITE_API}`,
  headers: {
    "Content-Type": "application/json",
  },
});

axiosInstance.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

axiosInstance.interceptors.response.use(
  (response) => {
    const authHeader = response.headers["Authorization"];

    if (authHeader && authHeader.startsWith("Bearer ")) {
      const newToken = authHeader.replace("Bearer ", "").trim();
      localStorage.setItem("token", newToken);
    }

    return response;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default axiosInstance;
