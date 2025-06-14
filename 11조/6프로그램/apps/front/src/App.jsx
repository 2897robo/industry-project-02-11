import "./App.css";
import { Routes, Route, Link } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/DashBoard";
import MyPage from "./pages/MyPage";
import Register from "./pages/Register";
import NotFound from "./pages/NotFound";
import PrivateRoute from "./routes/PrivateRoute";
import Resource from "./pages/Resource";

function App() {
  const token = localStorage.getItem("token");

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<PrivateRoute element={<Resource />} />} />
      <Route path="/mypage" element={<PrivateRoute element={<MyPage />} />} />
      <Route
        path="/resource"
        element={<PrivateRoute element={<Resource />} />}
      />
      <Route
        path="/dashboard/:id"
        element={<PrivateRoute element={<Dashboard />} />}
      />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default App;
