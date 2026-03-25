import React, { useEffect, useState } from 'react';
// 1. auto를 쓰면 별도의 register(등록) 과정이 필요 없습니다.
import 'chart.js/auto'; 
import { Bar } from 'react-chartjs-2';

const PredictionChart = () => {
    const params = new URLSearchParams(window.location.search);
    const performanceId = params.get('id');
    
    const [chartData, setChartData] = useState(null);
    const [loading, setLoading] = useState(!!performanceId);

    // ★ 중요: 이전 코드에 있던 ChartJS.register(...) 블록이 있다면 반드시 삭제하세요!

    useEffect(() => {
        if (!performanceId) return;

        fetch(`https://majustory.store/api/prediction/forecast/${performanceId}`)
            .then(res => res.json())
            .then(data => {
                if (data && data.labels && data.predictions) {
                    setChartData({
                        labels: data.labels,
                        datasets: [
                            {
                                type: 'bar',
                                label: '현재 예매율 (%)',
                                data: data.current_counts.map(v => ((v / 30) * 100).toFixed(1)),
                                backgroundColor: 'rgba(108, 92, 231, 0.5)',
                                borderColor: '#6c5ce7',
                                borderWidth: 1,
                                order: 2
                            },
                            {
                                type: 'line',
                                label: 'AI 예상 최종 점유율 (%)',
                                data: data.predictions,
                                borderColor: '#ff6384',
                                borderDash: [5, 5],
                                pointBackgroundColor: '#ff6384',
                                fill: false,
                                tension: 0.4,
                                order: 1
                            }
                        ]
                    });
                }
                setLoading(false);
            })
            .catch(err => {
                console.error("데이터 로드 실패:", err);
                setLoading(false);
            });
    }, [performanceId]);

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { position: 'top' },
            tooltip: { mode: 'index', intersect: false }
        },
        scales: {
            y: { 
                min: 0, 
                max: 100, 
                ticks: { callback: v => v + "%" }
            }
        }
    };

    // UI 스타일 생략 (이전과 동일)
    const containerStyle = { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: '#f1f2f6', margin: 0 };
    const cardStyle = { width: '90%', maxWidth: '850px', backgroundColor: '#ffffff', padding: '40px', borderRadius: '20px', boxShadow: '0 10px 25px rgba(0,0,0,0.05)', textAlign: 'center' };

    return (
        <div style={containerStyle}>
            <div style={cardStyle}>
                <h2 style={{ marginBottom: '10px', color: '#2d3436' }}>📊 실시간 예매 및 AI 수요 예측</h2>
                <p style={{ color: '#636e72', marginBottom: '30px' }}>공연 회차별 시계열 데이터 분석 (좌석 30석 기준)</p>
                
                <div style={{ height: '400px', marginBottom: '30px', position: 'relative' }}>
                    {loading ? (
                        <div style={{ lineHeight: '400px', color: '#6c5ce7' }}>AI 분석 데이터를 불러오는 중...</div>
                    ) : chartData ? (
                        <Bar data={chartData} options={options} />
                    ) : (
                        <div style={{ lineHeight: '400px', color: '#b2bec3' }}>데이터가 없습니다.</div>
                    )}
                </div>

                <button 
                    onClick={() => window.location.href = "http://majustory.store/admin/listPage.do"}
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