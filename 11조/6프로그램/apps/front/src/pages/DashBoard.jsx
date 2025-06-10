import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Monitor,
  Server,
  Cpu,
  MemoryStick,
  HardDrive,
  Zap,
} from "lucide-react";
import Header from "../components/Header";
import Button from "../components/Button";

const Dashboard = () => {
  const { id } = useParams(); // URL에서 대시보드 id 추출
  const [ec2Recommendations, setEc2Recommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  // EC2 추천 사이즈 데이터를 백엔드에서 가져오는 함수
  const fetchEc2Recommendations = async () => {
    try {
      setLoading(true);
      // 실제 백엔드 API 호출 - 대시보드 id를 파라미터로 사용
      // const response = await fetch(`/api/ec2-recommendations/${id}`);
      // const data = await response.json();

      // 현재는 더미 데이터로 시뮬레이션 (실제 구현시 id를 사용)
      console.log("대시보드 ID:", id);
      setTimeout(() => {
        const dummyData = [
          {
            id: 1,
            instanceType: "t3.medium",
            vCPU: 2,
            memory: "4 GiB",
            storage: "EBS-Only",
            price: "$0.0416/hour",
            recommendation: "일반적인 웹 애플리케이션",
            score: 85,
          },
          {
            id: 2,
            instanceType: "m5.large",
            vCPU: 2,
            memory: "8 GiB",
            storage: "EBS-Only",
            price: "$0.096/hour",
            recommendation: "중간 규모 데이터베이스",
            score: 92,
          },
          {
            id: 3,
            instanceType: "c5.xlarge",
            vCPU: 4,
            memory: "8 GiB",
            storage: "EBS-Only",
            price: "$0.17/hour",
            recommendation: "CPU 집약적 작업",
            score: 78,
          },
        ];
        setEc2Recommendations(dummyData);
        setLoading(false);
      }, 1000);
    } catch (error) {
      console.error("EC2 추천 데이터 로드 실패:", error);
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchEc2Recommendations();
    }
  }, [id]);

  const getScoreColor = (score) => {
    if (score >= 90) return { color: "#059669", backgroundColor: "#dcfce7" };
    if (score >= 75) return { color: "#d97706", backgroundColor: "#fef3c7" };
    return { color: "#dc2626", backgroundColor: "#fee2e2" };
  };

  const styles = {
    container: {
      minHeight: "100vh",
      backgroundColor: "#f9fafb",
      padding: "24px",
    },
    maxWidth: {
      maxWidth: "1280px",
      margin: "0 auto",
    },
    gridContainer: {
      display: "grid",
      gridTemplateColumns: "65% 35%",
      gap: "32px",
      height: "90vh",
      "@media (max-width: 1024px)": {
        gridTemplateColumns: "65% 35%",
      },
    },
    card: {
      backgroundColor: "white",
      borderRadius: "8px",
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
      padding: "24px",
    },
    header: {
      display: "flex",
      alignItems: "center",
      marginBottom: "24px",
    },
    headerTitle: {
      fontSize: "20px",
      fontWeight: "600",
      color: "#1f2937",
      marginLeft: "12px",
    },
    grafanaPlaceholder: {
      height: "100%",
      backgroundColor: "#f3f4f6",
      borderRadius: "8px",
      border: "2px dashed #d1d5db",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
    },
    placeholderContent: {
      textAlign: "center",
    },
    placeholderTitle: {
      fontSize: "18px",
      fontWeight: "500",
      color: "#6b7280",
      marginBottom: "8px",
    },
    placeholderText: {
      color: "#9ca3af",
      fontSize: "14px",
      marginBottom: "16px",
    },
    placeholderLabel: {
      fontSize: "12px",
      color: "#9ca3af",
      backgroundColor: "#e5e7eb",
      padding: "4px 12px",
      borderRadius: "4px",
    },
    listContainer: {
      height: "100%",
      overflowY: "auto",
    },
    loadingContainer: {
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      height: "256px",
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
      marginLeft: "12px",
      color: "#6b7280",
    },
    instanceCard: {
      border: "1px solid #e5e7eb",
      borderRadius: "8px",
      padding: "16px",
      marginBottom: "16px",
      transition: "box-shadow 0.3s ease",
      cursor: "pointer",
    },
    instanceCardHover: {
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
    },
    instanceHeader: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: "12px",
    },
    instanceType: {
      fontSize: "18px",
      fontWeight: "600",
      color: "#1f2937",
    },
    scoreTag: {
      padding: "4px 12px",
      borderRadius: "9999px",
      fontSize: "14px",
      fontWeight: "500",
    },
    detailsGrid: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: "16px",
      marginBottom: "12px",
    },
    detailItem: {
      display: "flex",
      alignItems: "center",
    },
    detailText: {
      fontSize: "14px",
      color: "#6b7280",
      marginLeft: "8px",
    },
    recommendationBox: {
      backgroundColor: "#f9fafb",
      borderRadius: "4px",
      padding: "12px",
    },
    recommendationText: {
      fontSize: "14px",
      color: "#374151",
    },
    recommendationLabel: {
      fontWeight: "500",
    },
  };

  // CSS 애니메이션을 위한 스타일 태그
  const cssAnimation = `
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    @media (max-width: 1024px) {
      .grid-container {
        grid-template-columns: 1fr !important;
      }
    }
  `;

  return (
    <>
      <Header
        title={"대시보드"}
        leftChild={
          <Button
            onClick={() => {
              nav(-1);
            }}
            text={"< 뒤로가기"}
          />
        }
      />
      <div style={styles.container}>
        <style>{cssAnimation}</style>
        <div style={styles.maxWidth}>
          <div style={styles.gridContainer} className="grid-container">
            {/* 왼쪽: Grafana 대시보드 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                {/* <Monitor size={24} color="#2563eb" /> */}
                <h2 style={styles.headerTitle}>시스템 모니터링</h2>
              </div>

              {/* Grafana 임베드 영역 */}
              <div style={styles.grafanaPlaceholder}>
                <div style={styles.placeholderContent}>
                  {/* <Monitor
                  size={64}
                  color="#9ca3af"
                  style={{ margin: "0 auto 16px" }}
                /> */}
                  <h3 style={styles.placeholderTitle}>Grafana 대시보드</h3>
                  <p style={styles.placeholderText}>
                    여기에 Grafana 대시보드가 임베드됩니다
                  </p>
                  <div style={styles.placeholderLabel}>
                    iframe 또는 Grafana API 연동 예정
                  </div>
                </div>
              </div>
            </div>

            {/* 오른쪽: EC2 추천 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                <Server size={24} color="#ea580c" />
                <h2 style={styles.headerTitle}>EC2 인스턴스 추천</h2>
              </div>

              {/* EC2 추천 리스트 */}
              <div style={styles.listContainer}>
                {loading ? (
                  <div style={styles.loadingContainer}>
                    <div style={styles.spinner}></div>
                    <span style={styles.loadingText}>로딩 중...</span>
                  </div>
                ) : (
                  <div>
                    {ec2Recommendations.map((instance) => (
                      <div
                        key={instance.id}
                        style={styles.instanceCard}
                        onMouseEnter={(e) => {
                          e.target.style.boxShadow =
                            styles.instanceCardHover.boxShadow;
                        }}
                        onMouseLeave={(e) => {
                          e.target.style.boxShadow = "none";
                        }}
                      >
                        <div style={styles.instanceHeader}>
                          <h3 style={styles.instanceType}>
                            {instance.instanceType}
                          </h3>
                          <span
                            style={{
                              ...styles.scoreTag,
                              ...getScoreColor(instance.score),
                            }}
                          >
                            점수: {instance.score}
                          </span>
                        </div>

                        <div style={styles.detailsGrid}>
                          <div style={styles.detailItem}>
                            <Cpu size={16} color="#3b82f6" />
                            <span style={styles.detailText}>
                              vCPU: {instance.vCPU}
                            </span>
                          </div>
                          <div style={styles.detailItem}>
                            <MemoryStick size={16} color="#10b981" />
                            <span style={styles.detailText}>
                              메모리: {instance.memory}
                            </span>
                          </div>
                          <div style={styles.detailItem}>
                            <HardDrive size={16} color="#8b5cf6" />
                            <span style={styles.detailText}>
                              스토리지: {instance.storage}
                            </span>
                          </div>
                          <div style={styles.detailItem}>
                            <Zap size={16} color="#f59e0b" />
                            <span style={styles.detailText}>
                              {instance.price}
                            </span>
                          </div>
                        </div>

                        <div style={styles.recommendationBox}>
                          <p style={styles.recommendationText}>
                            <span style={styles.recommendationLabel}>
                              추천 사유:
                            </span>{" "}
                            {instance.recommendation}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard;
