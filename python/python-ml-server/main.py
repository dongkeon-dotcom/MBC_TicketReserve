from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
import pandas as pd
from datetime import datetime, timedelta

app = FastAPI()

# CORS 설정 (React/Spring Boot와 통신 허용)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# Spring Boot에서 보낼 요청 규격
class SalesRequest(BaseModel):
    sales: List[int]  # 각 회차별 현재 예약된 좌석 수 (예: [5, 12, 8...])
    days: int         # 예측할 회차 개수 (보통 sales 리스트의 길이와 같음)

@app.post("/forecast")
def predict(data: SalesRequest):
    total_seats = 30
    predictions = []
    
    # 데이터가 아예 없을 때만 최소 기준(5장) 적용
    max_reserved = max(data.sales) if data.sales and max(data.sales) > 0 else 5

    for current_count in data.sales:
        # 1. 💡 바닥값(Base) 하향 조정 (현실화)
        # 기존 max_reserved * 0.6 -> 0.3으로 낮춤 (낙관 지수 감소)
        # 현재 0장이면, 다른 날 잘 팔렸어도 "예측치도 낮게" 시작합니다.
        base_line = max(current_count, max_reserved * 0.3)
        
        # 2. 💡 성장 가중치 축소
        # 기존 1.4배 -> 1.15배로 축소 (폭발적 성장 대신 완만한 증가)
        if current_count == 0:
            # 아예 0장인 날은 더 냉정하게 계산
            estimated_final = base_line * 1.1 
        else:
            # 조금이라도 팔린 날은 탄력을 받음
            estimated_final = current_count * 1.25 + 1

        # 3. 최대 좌석수 제한 및 반올림
        estimated_final = min(total_seats, estimated_final)
        final_rate = round((estimated_final / total_seats) * 100, 1)
        predictions.append(final_rate)

    return {"status": "success", "predictions": predictions}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)