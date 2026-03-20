import React, { useEffect, useState } from 'react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend,
    Filler
} from 'chart.js';
import { Chart } from 'react-chartjs-2';

// ChartJS 설정 등록
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend,
    Filler
);

const PredictionChart = () => {
    const [chartData, setChartData] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const performanceId = params.get('id');

        if (!performanceId) {
            setLoading(false);
            return;
        }

        fetch(`http://localhost:8080/api/prediction/forecast/${performanceId}`)
            .then(res => res.json())
            .then(data => {
                // 백엔드(Spring)에서 labels와 current_counts, predictions를 모두 주는지 확인
                if (data && data.labels && data.predictions) {
                    setChartData({
                        labels: data.labels,
                        datasets: [
                            {
                                type: 'bar', // 현재값은 막대로 보는 게 더 직관적입니다!
                                label: '현재 예매율 (%)',
                                data: data.current_counts.map(v => (v / 30 * 100).toFixed(1)),
                                backgroundColor: 'rgba(108, 92, 231, 0.5)',
                                borderColor: '#6c5ce7',
                                borderWidth: 1
                            },
                            {
                                type: 'line', // 예측값은 시계열 점선으로
                                label: 'AI 예상 최종 점유율 (%)',
                                data: data.predictions,
                                borderColor: '#ff6384',
                                borderDash: [5, 5],
                                pointBackgroundColor: '#ff6384',
                                fill: false,
                                tension: 0.4
                            }
                        ]
                    });
                } else {
                    console.error("데이터 구조가 올바르지 않습니다:", data);
                }
                setLoading(false);
            })
            .catch(err => {
                console.error("데이터 로드 실패:", err);
                setLoading(false);
            });
    }, []);

    // 2. 스타일 정의
    const containerStyle = {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: '#f1f2f6',
        margin: 0
    };

    const cardStyle = {
        width: '90%',
        maxWidth: '850px',
        backgroundColor: '#ffffff',
        padding: '40px',
        borderRadius: '20px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.05)',
        textAlign: 'center'
    };

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { position: 'top' },
            tooltip: { mode: 'index', intersect: false }
        },
        scales: {
            y: { min: 0, max: 100, ticks: { callback: v => v + "%" } }
        }
    };

    // 3. 렌더링 부분
    return (
        <div style={containerStyle}>
            <div style={cardStyle}>
                <h2 style={{ marginBottom: '10px', color: '#2d3436' }}>📊 실시간 예매 및 AI 수요 예측</h2>
                <p style={{ color: '#636e72', marginBottom: '30px' }}>공연 회차별 시계열 데이터 분석 결과입니다.</p>
                
                <div style={{ height: '400px', marginBottom: '30px', position: 'relative' }}>
                    {loading ? (
                        <div style={{ lineHeight: '400px', color: '#6c5ce7' }}>AI 분석 데이터를 불러오는 중...</div>
                    ) : chartData ? (
                        <Chart type="line" data={chartData} options={options} />
                    ) : (
                        <div style={{ lineHeight: '400px', color: '#b2bec3' }}>데이터가 없습니다.</div>
                    )}
                </div>

                <button 
                    onClick={() => window.location.href = "http://localhost:8080/admin/listPage.do"}
                    style={{ 
                        textDecoration: 'none', color: '#6c5ce7', fontWeight: 'bold', cursor: 'pointer',
                        border: '2px solid #6c5ce7', padding: '10px 20px', borderRadius: '10px', backgroundColor: '#fff'
                    }}>
                    공연 목록으로 돌아가기
                </button>
            </div>
        </div>
    );
};

export default PredictionChart;