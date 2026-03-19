import PredictionChart from './PredictionChart'; // 같은 폴더니까 경로가 './파일이름'

function App() {
  return (
    <div className="App">
      <h1 style={{ textAlign: 'center' }}>공연 티켓 판매 예측 시스템</h1>
      <PredictionChart />
    </div>
  );
}

export default App;