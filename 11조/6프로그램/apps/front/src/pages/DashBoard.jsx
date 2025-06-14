import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Monitor,
  Server,
  Cpu,
  MemoryStick,
  HardDrive,
  Zap,
  List,
  DollarSign,
  Clock,
  CheckCircle,
  AlertCircle,
  XCircle,
  TrendingUp,
  Calendar,
  BarChart3,
} from "lucide-react";
import * as Chart from "chart.js";
import Header from "../components/Header";
import Button from "../components/Button";
import axiosInstance from "../utils/axiosInstance";

// Chart.js 컴포넌트 등록
Chart.Chart.register(
  Chart.CategoryScale,
  Chart.LinearScale,
  Chart.PointElement,
  Chart.LineElement,
  Chart.BarElement,
  Chart.LineController,
  Chart.Title,
  Chart.Tooltip,
  Chart.Legend,
  Chart.ArcElement
);

const Dashboard = () => {
  const { id } = useParams(); // URL에서 대시보드 id 추출
  const [recommendations, setRecommendations] = useState([]);
  const [resources, setResources] = useState([]);
  const [costData, setCostData] = useState(null);
  const [serviceCostData, setServiceCostData] = useState([]);
  const [monthlyTrendData, setMonthlyTrendData] = useState([]);
  const [currentMonthSummary, setCurrentMonthSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [resourcesLoading, setResourcesLoading] = useState(true);
  const [costLoading, setCostLoading] = useState(true);
  const [chartType, setChartType] = useState("daily"); // 'daily', 'service', 'monthly'

  const dailyChartRef = useRef(null);
  const serviceChartRef = useRef(null);
  const monthlyChartRef = useRef(null);
  const chartInstanceRef = useRef(null);

  const nav = useNavigate();

  // 추천 데이터를 백엔드에서 가져오는 함수
  const fetchRecommendations = async () => {
    try {
      setLoading(true);
      const response = await axiosInstance.get(
        `/resource-service/api/recommendations`
      );
      setRecommendations(response.data || []);
    } catch (error) {
      console.error("추천 데이터 로드 실패:", error);
      setRecommendations([]);
    } finally {
      setLoading(false);
    }
  };

  // 사용중인 리소스 데이터를 백엔드에서 가져오는 함수
  const fetchResources = async () => {
    try {
      setResourcesLoading(true);
      const response = await axiosInstance.get(
        `/resource-service/api/resources`
      );
      setResources(response.data || []);
    } catch (error) {
      console.error("리소스 데이터 로드 실패:", error);
      setResources([]);
    } finally {
      setResourcesLoading(false);
    }
  };

  // 비용 데이터 가져오기
  const fetchCostData = async () => {
    try {
      setCostLoading(true);
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 30); // 최근 30일

      // 일별 비용 추이
      const dailyResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/daily-trend?startDate=${
          startDate.toISOString().split("T")[0]
        }&endDate=${endDate.toISOString().split("T")[0]}`
      );
      setCostData(dailyResponse.data);

      // 서비스별 비용 요약
      const serviceResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/service-summary?startDate=${
          startDate.toISOString().split("T")[0]
        }&endDate=${endDate.toISOString().split("T")[0]}`
      );
      setServiceCostData(serviceResponse.data || []);

      // 월별 비용 추이 (최근 6개월)
      const monthlyResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/monthly-trend?months=6`
      );
      setMonthlyTrendData(monthlyResponse.data || []);

      // 현재 월 요약
      const currentMonthResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/current-month`
      );
      setCurrentMonthSummary(currentMonthResponse.data);
    } catch (error) {
      console.error("비용 데이터 로드 실패:", error);
    } finally {
      setCostLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchRecommendations();
      fetchResources();
      fetchCostData();
    }
  }, [id]);

  // 차트 생성 함수
  const createChart = () => {
    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
    }

    const canvas =
      chartType === "daily"
        ? dailyChartRef.current
        : chartType === "service"
        ? serviceChartRef.current
        : monthlyChartRef.current;

    if (!canvas) return;

    const ctx = canvas.getContext("2d");

    let chartConfig = {};

    if (chartType === "daily" && costData) {
      chartConfig = {
        type: "line",
        data: {
          labels: costData.dates || [],
          datasets: [
            {
              label: "일별 비용 ($)",
              data: costData.costs || [],
              borderColor: "#2563eb",
              backgroundColor: "rgba(37, 99, 235, 0.1)",
              borderWidth: 2,
              fill: true,
              tension: 0.4,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: "top",
            },
            title: {
              display: true,
              text: "일별 비용 추이 (최근 30일)",
            },
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: function (value) {
                  return "$" + value.toFixed(2);
                },
              },
            },
          },
        },
      };
    } else if (chartType === "service" && serviceCostData.length > 0) {
      const colors = [
        "#2563eb",
        "#dc2626",
        "#059669",
        "#d97706",
        "#7c3aed",
        "#db2777",
        "#0891b2",
        "#65a30d",
        "#dc2626",
        "#6366f1",
      ];

      chartConfig = {
        type: "doughnut",
        data: {
          labels: serviceCostData.map((item) => item.serviceName),
          datasets: [
            {
              data: serviceCostData.map((item) => item.totalCost),
              backgroundColor: colors.slice(0, serviceCostData.length),
              borderWidth: 2,
              borderColor: "#ffffff",
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: "right",
            },
            title: {
              display: true,
              text: "서비스별 비용 분포",
            },
            tooltip: {
              callbacks: {
                label: function (context) {
                  const total = serviceCostData.reduce(
                    (sum, item) => sum + item.totalCost,
                    0
                  );
                  const percentage = ((context.raw / total) * 100).toFixed(1);
                  return `${context.label}: $${context.raw.toFixed(
                    2
                  )} (${percentage}%)`;
                },
              },
            },
          },
        },
      };
    } else if (chartType === "monthly" && monthlyTrendData.length > 0) {
      chartConfig = {
        type: "bar",
        data: {
          labels: monthlyTrendData.map(
            (item) => `${item.year}-${String(item.month).padStart(2, "0")}`
          ),
          datasets: [
            {
              label: "월별 비용 ($)",
              data: monthlyTrendData.map((item) => item.totalCost),
              backgroundColor: "rgba(37, 99, 235, 0.8)",
              borderColor: "#2563eb",
              borderWidth: 1,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: "top",
            },
            title: {
              display: true,
              text: "월별 비용 추이 (최근 6개월)",
            },
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: function (value) {
                  return "$" + value.toFixed(2);
                },
              },
            },
          },
        },
      };
    }

    if (chartConfig.type) {
      chartInstanceRef.current = new Chart.Chart(ctx, chartConfig);
    }
  };

  useEffect(() => {
    if (
      !costLoading &&
      (costData || serviceCostData.length > 0 || monthlyTrendData.length > 0)
    ) {
      // 약간의 딜레이를 주어 DOM이 완전히 렌더링된 후 차트 생성
      setTimeout(() => {
        createChart();
      }, 100);
    }

    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy();
      }
    };
  }, [chartType, costData, serviceCostData, monthlyTrendData, costLoading]);

  // 상태에 따른 아이콘 반환
  const getStatusIcon = (status) => {
    switch (status?.toLowerCase()) {
      case "completed":
      case "적용됨":
        return <CheckCircle size={16} color="#059669" />;
      case "pending":
      case "대기중":
        return <Clock size={16} color="#d97706" />;
      case "rejected":
      case "거절됨":
        return <XCircle size={16} color="#dc2626" />;
      default:
        return <AlertCircle size={16} color="#6b7280" />;
    }
  };

  // 날짜 포맷팅
  const formatDate = (dateString) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return "날짜 오류";
    }
  };

  const styles = {
    container: {
      minHeight: "100vh",
      backgroundColor: "#f9fafb",
      padding: "24px",
    },
    maxWidth: {
      maxWidth: "1400px",
      margin: "0 auto",
    },
    gridContainer: {
      display: "grid",
      gridTemplateColumns: "50% 25% 25%",
      gap: "24px",
      height: "90vh",
      "@media (max-width: 1024px)": {
        gridTemplateColumns: "1fr",
        height: "auto",
      },
    },
    card: {
      backgroundColor: "white",
      borderRadius: "8px",
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
      padding: "24px",
      display: "flex",
      flexDirection: "column",
    },
    header: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: "24px",
    },
    headerLeft: {
      display: "flex",
      alignItems: "center",
    },
    headerTitle: {
      fontSize: "18px",
      fontWeight: "600",
      color: "#1f2937",
      marginLeft: "12px",
    },
    chartContainer: {
      flex: 1,
      position: "relative",
      minHeight: "400px",
    },
    chartCanvas: {
      width: "100%",
      height: "100%",
    },
    chartControls: {
      display: "flex",
      gap: "8px",
    },
    chartButton: {
      padding: "6px 12px",
      fontSize: "12px",
      borderRadius: "4px",
      border: "1px solid #e5e7eb",
      backgroundColor: "white",
      cursor: "pointer",
      transition: "all 0.2s",
    },
    chartButtonActive: {
      backgroundColor: "#2563eb",
      color: "white",
      borderColor: "#2563eb",
    },
    summaryCard: {
      backgroundColor: "#f8fafc",
      padding: "16px",
      borderRadius: "6px",
      marginBottom: "16px",
      border: "1px solid #e2e8f0",
    },
    summaryTitle: {
      fontSize: "14px",
      fontWeight: "600",
      color: "#475569",
      marginBottom: "8px",
    },
    summaryValue: {
      fontSize: "24px",
      fontWeight: "700",
      color: "#1e293b",
    },
    summaryChange: {
      fontSize: "12px",
      marginTop: "4px",
    },
    positive: {
      color: "#dc2626",
    },
    negative: {
      color: "#059669",
    },
    listContainer: {
      flex: 1,
      overflowY: "auto",
    },
    loadingContainer: {
      display: "flex",
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
      marginLeft: "12px",
      color: "#6b7280",
    },
    itemCard: {
      border: "1px solid #e5e7eb",
      borderRadius: "8px",
      padding: "16px",
      marginBottom: "12px",
      transition: "box-shadow 0.3s ease",
      cursor: "pointer",
    },
    itemCardHover: {
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
    },
    itemHeader: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: "12px",
    },
    itemTitle: {
      fontSize: "14px",
      fontWeight: "600",
      color: "#1f2937",
    },
    statusBadge: {
      display: "flex",
      alignItems: "center",
      gap: "4px",
      fontSize: "12px",
      fontWeight: "500",
    },
    detailItem: {
      display: "flex",
      alignItems: "center",
      marginBottom: "8px",
    },
    detailText: {
      fontSize: "13px",
      color: "#6b7280",
      marginLeft: "8px",
    },
    recommendationText: {
      fontSize: "13px",
      color: "#374151",
      backgroundColor: "#f9fafb",
      padding: "8px",
      borderRadius: "4px",
      marginTop: "8px",
    },
    savingText: {
      fontSize: "13px",
      color: "#059669",
      fontWeight: "600",
    },
    dateText: {
      fontSize: "12px",
      color: "#9ca3af",
    },
    emptyState: {
      textAlign: "center",
      padding: "40px 20px",
      color: "#6b7280",
      fontSize: "14px",
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
        height: auto !important;
      }
      .grid-container > div {
        height: 400px;
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
            {/* 왼쪽: 비용 차트 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                <div style={styles.headerLeft}>
                  <BarChart3 size={24} color="#2563eb" />
                  <h2 style={styles.headerTitle}>비용 분석</h2>
                </div>
                <div style={styles.chartControls}>
                  <button
                    style={{
                      ...styles.chartButton,
                      ...(chartType === "daily"
                        ? styles.chartButtonActive
                        : {}),
                    }}
                    onClick={() => setChartType("daily")}
                  >
                    일별
                  </button>
                  <button
                    style={{
                      ...styles.chartButton,
                      ...(chartType === "service"
                        ? styles.chartButtonActive
                        : {}),
                    }}
                    onClick={() => setChartType("service")}
                  >
                    서비스별
                  </button>
                  <button
                    style={{
                      ...styles.chartButton,
                      ...(chartType === "monthly"
                        ? styles.chartButtonActive
                        : {}),
                    }}
                    onClick={() => setChartType("monthly")}
                  >
                    월별
                  </button>
                </div>
              </div>

              {/* 현재 월 요약 정보 */}
              {currentMonthSummary && (
                <div style={styles.summaryCard}>
                  <div style={styles.summaryTitle}>이번 달 총 비용</div>
                  <div style={styles.summaryValue}>
                    ${currentMonthSummary.totalCost?.toFixed(2) || "0.00"}
                  </div>
                  {currentMonthSummary.changeFromLastMonth && (
                    <div
                      style={{
                        ...styles.summaryChange,
                        ...(currentMonthSummary.changeFromLastMonth > 0
                          ? styles.positive
                          : styles.negative),
                      }}
                    >
                      {currentMonthSummary.changeFromLastMonth > 0 ? "↑" : "↓"}
                      {Math.abs(
                        currentMonthSummary.changeFromLastMonth
                      ).toFixed(1)}
                      % 전월 대비
                    </div>
                  )}
                </div>
              )}

              {/* 차트 영역 */}
              <div style={styles.chartContainer}>
                {costLoading ? (
                  <div style={styles.loadingContainer}>
                    <div style={styles.spinner}></div>
                    <span style={styles.loadingText}>차트 로딩 중...</span>
                  </div>
                ) : (
                  <>
                    <canvas
                      ref={dailyChartRef}
                      style={{
                        ...styles.chartCanvas,
                        display: chartType === "daily" ? "block" : "none",
                      }}
                    />
                    <canvas
                      ref={serviceChartRef}
                      style={{
                        ...styles.chartCanvas,
                        display: chartType === "service" ? "block" : "none",
                      }}
                    />
                    <canvas
                      ref={monthlyChartRef}
                      style={{
                        ...styles.chartCanvas,
                        display: chartType === "monthly" ? "block" : "none",
                      }}
                    />
                  </>
                )}
              </div>
            </div>

            {/* 가운데: 추천 사항 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                <div style={styles.headerLeft}>
                  <Server size={24} color="#ea580c" />
                  <h2 style={styles.headerTitle}>리소스 추천</h2>
                </div>
              </div>

              <div style={styles.listContainer}>
                {loading ? (
                  <div style={styles.loadingContainer}>
                    <div style={styles.spinner}></div>
                    <span style={styles.loadingText}>로딩 중...</span>
                  </div>
                ) : recommendations.length === 0 ? (
                  <div style={styles.emptyState}>
                    <Server
                      size={32}
                      color="#d1d5db"
                      style={{ marginBottom: "12px" }}
                    />
                    <p>추천 사항이 없습니다</p>
                  </div>
                ) : (
                  <div>
                    {recommendations.map((recommendation) => (
                      <div
                        key={recommendation.id}
                        style={styles.itemCard}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.boxShadow =
                            styles.itemCardHover.boxShadow;
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.boxShadow = "none";
                        }}
                      >
                        <div style={styles.itemHeader}>
                          <span style={styles.itemTitle}>
                            추천 #{recommendation.id}
                          </span>
                          <div style={styles.statusBadge}>
                            {getStatusIcon(recommendation.status)}
                            <span>{recommendation.status || "대기중"}</span>
                          </div>
                        </div>

                        <div style={styles.detailItem}>
                          <List size={14} color="#6b7280" />
                          <span style={styles.detailText}>
                            리소스 ID: {recommendation.resourceId}
                          </span>
                        </div>

                        {recommendation.expectedSaving && (
                          <div style={styles.detailItem}>
                            <DollarSign size={14} color="#059669" />
                            <span style={styles.savingText}>
                              예상 절약: $
                              {recommendation.expectedSaving.toFixed(2)}
                            </span>
                          </div>
                        )}

                        <div style={styles.detailItem}>
                          <Clock size={14} color="#9ca3af" />
                          <span style={styles.dateText}>
                            {formatDate(recommendation.createdAt)}
                          </span>
                        </div>

                        {recommendation.recommendationText && (
                          <div style={styles.recommendationText}>
                            {recommendation.recommendationText}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* 오른쪽: 사용중인 리소스 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                <div style={styles.headerLeft}>
                  <HardDrive size={24} color="#10b981" />
                  <h2 style={styles.headerTitle}>사용중인 리소스</h2>
                </div>
              </div>

              <div style={styles.listContainer}>
                {resourcesLoading ? (
                  <div style={styles.loadingContainer}>
                    <div style={styles.spinner}></div>
                    <span style={styles.loadingText}>로딩 중...</span>
                  </div>
                ) : resources.length === 0 ? (
                  <div style={styles.emptyState}>
                    <HardDrive
                      size={32}
                      color="#d1d5db"
                      style={{ marginBottom: "12px" }}
                    />
                    <p>사용중인 리소스가 없습니다</p>
                  </div>
                ) : (
                  <div>
                    {resources.map((resource) => (
                      <div
                        key={resource.id}
                        style={styles.itemCard}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.boxShadow =
                            styles.itemCardHover.boxShadow;
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.boxShadow = "none";
                        }}
                      >
                        <div style={styles.itemHeader}>
                          <span style={styles.itemTitle}>
                            {resource.resourceType || "리소스"} #{resource.id}
                          </span>
                          <div style={styles.statusBadge}>
                            <CheckCircle size={14} color="#059669" />
                            <span>활성</span>
                          </div>
                        </div>

                        <div style={styles.detailItem}>
                          <Server size={14} color="#6b7280" />
                          <span style={styles.detailText}>
                            {resource.instanceType ||
                              resource.resourceName ||
                              "정보 없음"}
                          </span>
                        </div>

                        {resource.region && (
                          <div style={styles.detailItem}>
                            <Monitor size={14} color="#8b5cf6" />
                            <span style={styles.detailText}>
                              리전: {resource.region}
                            </span>
                          </div>
                        )}

                        {resource.cost && (
                          <div style={styles.detailItem}>
                            <DollarSign size={14} color="#f59e0b" />
                            <span style={styles.detailText}>
                              비용: ${resource.cost}/월
                            </span>
                          </div>
                        )}

                        <div style={styles.detailItem}>
                          <Clock size={14} color="#9ca3af" />
                          <span style={styles.dateText}>
                            {formatDate(
                              resource.createdAt || resource.launchTime
                            )}
                          </span>
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
