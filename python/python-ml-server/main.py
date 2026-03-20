import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

app = FastAPI()

class SalesRequest(BaseModel):
    sales: List[int]

@app.post("/forecast")
def predict(data: SalesRequest):
    total_seats = 30
    actual_sales = data.sales
    n = len(actual_sales)
    
    # 1. 예약이 있는 데이터의 인덱스와 값 추출
    past_indices = [i for i, s in enumerate(actual_sales) if s > 0]
    past_values = [s for s in actual_sales if s > 0]

    if not past_values:
        # 데이터가 아예 없으면 보수적으로 10%부터 시작
        return {"predictions": [round(3/30*100, 1)] * n}

    # 2. 기울기(Slope) 계산
    if len(past_values) > 1:
        z = np.polyfit(past_indices, past_values, 1)
        slope = z[0]
        intercept = z[1]
    else:
        # 데이터가 하나뿐이면 아주 천천히 증가한다고 가정 (기울기 0.2)
        slope = 0.2
        intercept = past_values[0]

    predictions = []

    # 3. 💡 현실적인 시계열 예측 (가중치 대폭 하향)
    for i in range(n):
        if actual_sales[i] > 0:
            # 현재 예약된 날은 "현재 값 + 남은 기간 동안의 완만한 증가"
            # 무조건 1.2배 하는 게 아니라, 조금만 더 팔릴 것으로 예상
            pred = actual_sales[i] + (slope * 0.5) + 1. 
        else:
            # 미래 날짜 (0인 날)
            # 추세를 따라가되, 뒤로 갈수록 증가폭을 줄임 (감쇠 효과)
            # 30석 기준이므로 상수를 더하는 걸 조심해야 함 (+2 정도로 수정)
            pred = (slope * i) + intercept + 2 
            
        # 4. 💡 30석을 초과하지 않도록 철저히 계산
        # 예측값이 현재 최대 판매량보다 너무 높지 않게 제어 (안정성)
        max_limit = max(past_values) * 1.5 if past_values else 30
        final_val = min(total_seats, pred, max_limit)
        
        # 0보다 작아지지 않게 방어
        final_val = max(0, final_val)
        
        predictions.append(round((final_val / total_seats) * 100, 1))

    return {
        "status": "success",
        "predictions": predictions,
        "slope": float(slope)
    }