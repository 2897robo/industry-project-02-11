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
  Chart.BarController,
  Chart.DoughnutController,
  Chart.Title,
  Chart.Tooltip,
  Chart.Legend,
  Chart.ArcElement
);

const Dashboard = () => {
  const { id } = useParams();
  const [recommendations, setRecommendations] = useState([]);
  const [resources, setResources] = useState([]);
  const [dailyCostTrend, setDailyCostTrend] = useState(null);
  const [serviceCostData, setServiceCostData] = useState([]);
  const [monthlyTrendData, setMonthlyTrendData] = useState([]);
  const [currentMonthSummary, setCurrentMonthSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [resourcesLoading, setResourcesLoading] = useState(true);
  const [costLoading, setCostLoading] = useState(true);
  const [chartType, setChartType] = useState("daily");
  const [error, setError] = useState(null);
  const [idleResources, setIdleResources] = useState([]);
  const [idleResourcesLoading, setIdleResourcesLoading] = useState(true);

  // 단일 차트 ref와 인스턴스만 사용
  const chartRef = useRef(null);
  const chartInstanceRef = useRef(null);

  const nav = useNavigate();

  // 추천 데이터를 백엔드에서 가져오는 함수
  const fetchRecommendations = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axiosInstance.get(
        `/resource-service/api/recommendations`
      );
      setRecommendations(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error("추천 데이터 로드 실패:", error);
      setError("추천 데이터를 불러올 수 없습니다.");
      setRecommendations([]);
    } finally {
      setLoading(false);
    }
  };

  // 사용중인 리소스 데이터를 백엔드에서 가져오는 함수
  const fetchResources = async () => {
    try {
      setResourcesLoading(true);
      setError(null);
      const response = await axiosInstance.get(
        `/resource-service/api/resources`
      );
      setResources(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error("리소스 데이터 로드 실패:", error);
      setError("리소스 데이터를 불러올 수 없습니다.");
      setResources([]);
    } finally {
      setResourcesLoading(false);
    }
  };

  // 유휴 리소스 데이터를 백엔드에서 가져오는 함수
  const fetchIdleResources = async () => {
    try {
      setIdleResourcesLoading(true);
      setError(null);
      const response = await axiosInstance.get(
        `/resource-service/api/resources/idle`
      );
      setIdleResources(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error("유휴 리소스 데이터 로드 실패:", error);
      setError("유휴 리소스 데이터를 불러올 수 없습니다.");
      setIdleResources([]);
    } finally {
      setIdleResourcesLoading(false);
    }
  };

  // 비용 데이터 가져오기
  const fetchCostData = async () => {
    try {
      setCostLoading(true);
      setError(null);
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 30);

      // 일별 비용 추이
      const dailyResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/daily-trend?startDate=${
          startDate.toISOString().split("T")[0]
        }&endDate=${endDate.toISOString().split("T")[0]}`
      );
      setDailyCostTrend(dailyResponse.data);

      // 서비스별 비용 요약
      const serviceResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/service-summary?startDate=${
          startDate.toISOString().split("T")[0]
        }&endDate=${endDate.toISOString().split("T")[0]}`
      );
      setServiceCostData(
        Array.isArray(serviceResponse.data) ? serviceResponse.data : []
      );

      // 월별 비용 추이
      const monthlyResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/monthly-trend?months=6`
      );
      setMonthlyTrendData(
        Array.isArray(monthlyResponse.data) ? monthlyResponse.data : []
      );

      // 현재 월 요약
      const currentMonthResponse = await axiosInstance.get(
        `/resource-service/api/cost-history/current-month`
      );
      setCurrentMonthSummary(currentMonthResponse.data);
    } catch (error) {
      console.error("비용 데이터 로드 실패:", error);
      setError("비용 데이터를 불러올 수 없습니다.");
    } finally {
      setCostLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchRecommendations();
      fetchResources();
      fetchCostData();
      fetchIdleResources();
    }
  }, [id]);

  // 차트 인스턴스 정리 함수
  const destroyChart = () => {
    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
      chartInstanceRef.current = null;
    }
  };

  // 차트 생성 함수
  const createChart = () => {
    // 이전 차트 인스턴스 정리
    destroyChart();

    const canvas = chartRef.current;
    if (!canvas) {
      console.log("Canvas not found");
      return;
    }

    const ctx = canvas.getContext("2d");
    if (!ctx) {
      console.log("Context not found");
      return;
    }

    let chartConfig = null;

    // 차트 타입별 설정
    if (chartType === "daily") {
      if (
        !dailyCostTrend ||
        !Array.isArray(dailyCostTrend.dailyCosts) ||
        dailyCostTrend.dailyCosts.length === 0
      ) {
        console.log("Daily cost data not available");
        return;
      }

      const dates = dailyCostTrend.dailyCosts.map((item) => {
        try {
          return new Date(item.date).toLocaleDateString("ko-KR", {
            month: "short",
            day: "numeric",
          });
        } catch {
          return item.date;
        }
      });
      const costs = dailyCostTrend.dailyCosts.map((item) =>
        parseFloat(item.totalCost || 0)
      );

      chartConfig = {
        type: "line",
        data: {
          labels: dates,
          datasets: [
            {
              label: "일별 비용 ($)",
              data: costs,
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
    } else if (chartType === "service") {
      if (!Array.isArray(serviceCostData) || serviceCostData.length === 0) {
        console.log("Service cost data not available");
        return;
      }

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
          labels: serviceCostData.map((item) => item.serviceName || "Unknown"),
          datasets: [
            {
              data: serviceCostData.map((item) =>
                parseFloat(item.totalCost || 0)
              ),
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
                    (sum, item) => sum + parseFloat(item.totalCost || 0),
                    0
                  );
                  const percentage =
                    total > 0 ? ((context.raw / total) * 100).toFixed(1) : 0;
                  return `${context.label}: $${context.raw.toFixed(
                    2
                  )} (${percentage}%)`;
                },
              },
            },
          },
        },
      };
    } else if (chartType === "monthly") {
      if (!Array.isArray(monthlyTrendData) || monthlyTrendData.length === 0) {
        console.log("Monthly trend data not available");
        return;
      }

      chartConfig = {
        type: "bar",
        data: {
          labels: monthlyTrendData.map((item) => {
            try {
              const [year, month] = item.month.split("-");
              return `${month}월`;
            } catch {
              return item.month;
            }
          }),
          datasets: [
            {
              label: "월별 비용 ($)",
              data: monthlyTrendData.map((item) =>
                parseFloat(item.totalCost || 0)
              ),
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

    // 차트 생성
    if (chartConfig) {
      try {
        chartInstanceRef.current = new Chart.Chart(ctx, chartConfig);
        console.log(`Chart created successfully: ${chartType}`);
      } catch (error) {
        console.error("차트 생성 실패:", error);
      }
    }
  };

  // 차트 타입이 변경되거나 데이터가 로드되면 차트를 다시 생성
  useEffect(() => {
    if (!costLoading) {
      const timer = setTimeout(() => {
        createChart();
      }, 100);

      return () => clearTimeout(timer);
    }
  }, [
    chartType,
    dailyCostTrend,
    serviceCostData,
    monthlyTrendData,
    costLoading,
  ]);

  // 컴포넌트 언마운트 시 차트 정리
  useEffect(() => {
    return () => {
      destroyChart();
    };
  }, []);

  // 상태에 따른 아이콘 반환
  const getStatusIcon = (status) => {
    const statusLower = (status || "").toLowerCase();
    switch (statusLower) {
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
    if (!dateString) return "날짜 없음";
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return "날짜 오류";
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

  // 전월 대비 변화율 계산
  const calculateMonthlyChange = () => {
    if (
      !currentMonthSummary ||
      !monthlyTrendData ||
      monthlyTrendData.length < 2
    ) {
      return null;
    }

    const currentCost = parseFloat(currentMonthSummary.totalCost || 0);
    const previousMonthData = monthlyTrendData[monthlyTrendData.length - 2];
    const previousCost = parseFloat(previousMonthData?.totalCost || 0);

    if (previousCost === 0) return null;

    return ((currentCost - previousCost) / previousCost) * 100;
  };

  // 차트 데이터 가용성 확인
  const isChartDataAvailable = () => {
    switch (chartType) {
      case "daily":
        return (
          dailyCostTrend &&
          Array.isArray(dailyCostTrend.dailyCosts) &&
          dailyCostTrend.dailyCosts.length > 0
        );
      case "service":
        return Array.isArray(serviceCostData) && serviceCostData.length > 0;
      case "monthly":
        return Array.isArray(monthlyTrendData) && monthlyTrendData.length > 0;
      default:
        return false;
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
      gridTemplateColumns: "40% 20% 20% 20%",
      gap: "24px",
      height: "90vh",
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
    summaryDetails: {
      fontSize: "12px",
      color: "#64748b",
      marginTop: "8px",
    },
    listContainer: {
      flex: 1,
      overflowY: "auto",
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
    errorText: {
      color: "#dc2626",
      fontSize: "14px",
      textAlign: "center",
      padding: "20px",
    },
    noDataContainer: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      height: "300px",
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
        rightChild={
          <Button
            onClick={() => nav("/mypage")}
            text={"마이페이지"}
            type="default"
          />
        }
      />
      <div style={styles.container}>
        <style>{cssAnimation}</style>
        <div style={styles.maxWidth}>
          {error && <div style={styles.errorText}>{error}</div>}
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
                    ${parseFloat(currentMonthSummary.totalCost || 0).toFixed(2)}{" "}
                    {currentMonthSummary.currency || "USD"}
                  </div>
                  <div style={styles.summaryDetails}>
                    일평균: $
                    {parseFloat(currentMonthSummary.dailyAverage || 0).toFixed(
                      2
                    )}
                  </div>
                  {(() => {
                    const change = calculateMonthlyChange();
                    return change !== null ? (
                      <div
                        style={{
                          ...styles.summaryChange,
                          ...(change > 0 ? styles.positive : styles.negative),
                        }}
                      >
                        {change > 0 ? "↑" : "↓"}
                        {Math.abs(change).toFixed(1)}% 전월 대비
                      </div>
                    ) : null;
                  })()}
                </div>
              )}

              {/* 일별 차트 표시 시 추가 정보 */}
              {chartType === "daily" && dailyCostTrend && (
                <div style={styles.summaryCard}>
                  <div style={styles.summaryTitle}>최근 30일 요약</div>
                  <div style={styles.summaryDetails}>
                    총 비용: $
                    {parseFloat(dailyCostTrend.totalCost || 0).toFixed(2)}{" "}
                    {dailyCostTrend.currency || "USD"}
                  </div>
                  <div style={styles.summaryDetails}>
                    일평균: $
                    {parseFloat(dailyCostTrend.averageDailyCost || 0).toFixed(
                      2
                    )}
                  </div>
                  {dailyCostTrend.projectedMonthlyCost && (
                    <div style={styles.summaryDetails}>
                      예상 월 비용: $
                      {parseFloat(dailyCostTrend.projectedMonthlyCost).toFixed(
                        2
                      )}
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
                ) : isChartDataAvailable() ? (
                  <canvas ref={chartRef} style={styles.chartCanvas} />
                ) : (
                  <div style={styles.noDataContainer}>
                    <BarChart3 size={48} color="#d1d5db" />
                    <p style={{ marginTop: "16px", fontSize: "16px" }}>
                      {chartType === "daily" && "일별 비용 데이터가 없습니다"}
                      {chartType === "service" &&
                        "서비스별 비용 데이터가 없습니다"}
                      {chartType === "monthly" && "월별 비용 데이터가 없습니다"}
                    </p>
                  </div>
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
                            리소스 ID: {recommendation.resourceId || "N/A"}
                          </span>
                        </div>

                        {recommendation.expectedSaving && (
                          <div style={styles.detailItem}>
                            <DollarSign size={14} color="#059669" />
                            <span style={styles.savingText}>
                              예상 절약: $
                              {parseFloat(
                                recommendation.expectedSaving
                              ).toFixed(2)}
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

            {/* 네 번째: 유휴 리소스 섹션 */}
            <div style={styles.card}>
              <div style={styles.header}>
                <div style={styles.headerLeft}>
                  <AlertCircle size={24} color="#f59e0b" />
                  <h2 style={styles.headerTitle}>유휴 리소스</h2>
                </div>
              </div>

              <div style={styles.listContainer}>
                {idleResourcesLoading ? (
                  <div style={styles.loadingContainer}>
                    <div style={styles.spinner}></div>
                    <span style={styles.loadingText}>로딩 중...</span>
                  </div>
                ) : idleResources.length === 0 ? (
                  <div style={styles.emptyState}>
                    <AlertCircle
                      size={32}
                      color="#d1d5db"
                      style={{ marginBottom: "12px" }}
                    />
                    <p>유휴 리소스가 없습니다</p>
                  </div>
                ) : (
                  <div>
                    {idleResources.map((resource) => (
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
                            <AlertCircle size={14} color="#f59e0b" />
                            <span>유휴</span>
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

                        {resource.idleDuration && (
                          <div style={styles.detailItem}>
                            <Clock size={14} color="#f59e0b" />
                            <span style={styles.detailText}>
                              유휴 기간: {resource.idleDuration}
                            </span>
                          </div>
                        )}

                        {resource.wasteCost && (
                          <div style={styles.detailItem}>
                            <DollarSign size={14} color="#dc2626" />
                            <span
                              style={{ ...styles.detailText, color: "#dc2626" }}
                            >
                              낭비 비용: ${resource.wasteCost}/월
                            </span>
                          </div>
                        )}

                        <div style={styles.detailItem}>
                          <Clock size={14} color="#9ca3af" />
                          <span style={styles.dateText}>
                            {formatDate(
                              resource.lastUsed || resource.createdAt
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
