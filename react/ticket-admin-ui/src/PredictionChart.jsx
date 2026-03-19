import React, { useEffect, useState } from 'react';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, Filler } from 'chart.js';
import { Chart } from 'react-chartjs-2'; // Line 대신 통합 Chart 컴포넌트 사용

// BarElement를 추가로 등록해야 막대그래프를 그릴 수 있습니다.
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, Filler);

const PredictionChart = () => {
    const [chartData, setChartData] = useState({ labels: [], datasets: [] });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const performanceId = params.get('id');

        if (!performanceId) {
            console.error("공연 ID가 없습니다.");
            setLoading(false);
            return;
        }

        // Spring Boot 서비스에서 설정한 API 호출
        fetch(`http://localhost:8080/api/prediction/forecast/${performanceId}`)
            .then(res => {
                if (!res.ok) throw new Error("데이터를 가져오지 못했습니다.");
                return res.json();
            })
            .then(data => {
                console.log("서버 응답 데이터:", data);

                if (data.status === "no_data" || !data.labels || data.labels.length === 0) {
                    setLoading(false);
                    return;
                }

                // 30석 기준 현재 점유율 (%) 계산
                const currentRates = data.current_counts.map(val => ((val / 30) * 100).toFixed(1));

                setChartData({
                    labels: data.labels, // X축: "03/18 19:30" 등 회차 시간
                    datasets: [
                        {
                            type: 'bar', // 📊 현재 점유율은 막대그래프로
                            label: '현재 예매율 (%)',
                            data: currentRates,
                            backgroundColor: 'rgba(108, 92, 231, 0.5)',
                            borderColor: 'rgb(108, 92, 231)',
                            borderWidth: 1,
                            order: 2 // 막대를 뒤로
                        },
                        {
                            type: 'line', // 📈 AI 예측치는 선그래프로
                            label: 'AI 예상 최종 점유율 (%)',
                            data: data.predictions, // Python에서 계산해준 % 값
                            borderColor: 'rgb(255, 99, 132)',
                            borderWidth: 3,
                            pointBackgroundColor: 'rgb(255, 99, 132)',
                            fill: false,
                            tension: 0.4,
                            borderDash: [5, 5], // 점선 효과로 '예측'임을 강조
                            order: 1 // 선을 앞으로
                        }
                    ]
                });
                setLoading(false);
            })
            .catch(err => {
                console.error("통신 에러:", err);
                setLoading(false);
            });
    }, []);

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                min: 0,
                max: 100, // 점유율이므로 100% 고정
                ticks: {
                    callback: (value) => value + "%"
                },
                title: {
                    display: true,
                    text: '점유율 (%)'
                }
            },
            x: {
                title: {
                    display: true,
                    text: '공연 회차 (날짜 시간)'
                }
            }
        },
        plugins: {
            tooltip: {
                callbacks: {
                    label: (context) => `${context.dataset.label}: ${context.raw}%`
                }
            },
            legend: {
                position: 'top'
            }
        }
    };

    return (
        <div style={{ width: '85%', margin: '0 auto', padding: '40px', textAlign: 'center' }}>
            <h2 style={{ marginBottom: '10px' }}>📊 회차별 예매 현황 및 AI 수요 예측</h2>
            <p style={{ color: '#666', marginBottom: '30px' }}>향후 7일간의 공연 스케줄별 실시간 점유율과 최종 도달 예상치입니다.</p>
            
            <div style={{ backgroundColor: '#fff', padding: '25px', borderRadius: '20px', boxShadow: '0 10px 30px rgba(0,0,0,0.1)', height: '450px' }}>
                {chartData.labels.length > 0 ? (
                    <Chart type='bar' data={chartData} options={options} />
                ) : (
                    <div style={{ paddingTop: '150px' }}>
                        {loading ? "🤖 AI가 회차별 데이터를 정밀 분석 중입니다..." : "조회된 공연 회차가 없습니다."}
                    </div>
                )}
            </div>
            
            <div style={{ marginTop: '30px' }}>
                <a href="http://localhost:8080/admin/listPage.do" 
                   style={{ textDecoration: 'none', color: '#6c5ce7', fontWeight: 'bold', border: '2px solid #6c5ce7', padding: '10px 20px', borderRadius: '10px' }}>
                   ← 공연 목록으로 돌아가기
                </a>
            </div>
        </div>
    );
};

export default PredictionChart;