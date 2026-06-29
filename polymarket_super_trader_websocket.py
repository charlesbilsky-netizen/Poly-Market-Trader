"""
Polymarket Super Momentum Trader + FastAPI Server with WebSocket Streaming
====================================================================
A production-ready, unified trading signal engine and FastAPI server with 
asynchronous WebSocket support for real-time signal streaming and subscriptions.

Features:
- WebSocket connection manager with token-specific subscription routing.
- Real-time periodic signal broadcats for active subscriptions.
- Technical analysis engine (ROC, SMAC, Bollinger, RSI, Orderbook Imbalance).
- News Overreaction Detection and Combined Signal conflict resolution.
- Asynchronous polling of Polymarket CLOB base endpoints.
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, List, Set, Optional, Tuple
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ========================= CONFIG =========================
class Config:
    CLOB_BASE = "https://clob.polymarket.com"
    GAMMA_BASE = "https://gamma-api.polymarket.com"
    
    # Technical Parameters
    ROC_PERIOD = 14
    SMAC_SHORT = 5
    SMAC_LONG = 10
    BOLLINGER_PERIOD = 20
    BOLLINGER_STD = 2.0
    RSI_PERIOD = 14
    RSI_OVERBOUGHT = 70
    RSI_OVERSOLD = 30
    VOLUME_SPIKE_THRESHOLD = 2.5
    PRICE_SPIKE_THRESHOLD = 5.0  # %
    
    # Risk
    POSITION_SIZE_PCT = 0.01
    STOP_LOSS_PCT = 0.015
    PROFIT_TARGET_PCT = 0.04
    
    # Performance & Stream Config
    CACHE_TTL_SECONDS = 30
    REQUEST_TIMEOUT = 8
    STREAM_INTERVAL_SECONDS = 5.0  # Time between streaming updates

# ========================= LOGGING =========================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s | %(levelname)s | %(message)s'
)
logger = logging.getLogger(__name__)

# ========================= SESSION =========================
def create_session() -> requests.Session:
    session = requests.Session()
    retry = Retry(total=3, backoff_factor=0.5, status_forcelist=[429, 500, 502, 503, 504])
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session

session = create_session()

# ========================= TRADING LOGIC =========================
class PolymarketSuperTrader:
    def __init__(self):
        self.price_cache: Dict[str, List[Dict]] = {}
        self.orderbook_cache: Dict[str, Dict] = {}
        self.last_fetch: Dict[str, datetime] = {}

    def _get_cached(self, key: str) -> Optional[List[Dict]]:
        if key in self.price_cache and (datetime.now() - self.last_fetch.get(key, datetime.min)).total_seconds() < Config.CACHE_TTL_SECONDS:
            return self.price_cache[key]
        return None

    def fetch_price_history(self, token_id: str, interval: str = "1h", limit: int = 100) -> List[Dict]:
        cache_key = f"price_{token_id}_{interval}"
        cached = self._get_cached(cache_key)
        if cached:
            return cached

        try:
            params = {"market": token_id, "interval": interval, "fidelity": 1, "limit": limit}
            resp = session.get(f"{Config.CLOB_BASE}/prices-history", params=params, timeout=Config.REQUEST_TIMEOUT)
            resp.raise_for_status()
            data = resp.json().get("history", [])
            
            self.price_cache[cache_key] = data
            self.last_fetch[cache_key] = datetime.now()
            return data
        except Exception as e:
            logger.error(f"Price history failed for {token_id}: {e}")
            return []

    def fetch_orderbook(self, token_id: str) -> Optional[Dict]:
        cache_key = f"book_{token_id}"
        if cache_key in self.orderbook_cache and (datetime.now() - self.last_fetch.get(cache_key, datetime.min)).total_seconds() < 15:
            return self.orderbook_cache[cache_key]

        try:
            resp = session.get(f"{Config.CLOB_BASE}/book", params={"token_id": token_id}, timeout=Config.REQUEST_TIMEOUT)
            resp.raise_for_status()
            data = resp.json()
            self.orderbook_cache[cache_key] = data
            self.last_fetch[cache_key] = datetime.now()
            return data
        except Exception as e:
            logger.warning(f"Orderbook fetch failed: {e}")
            return None

    @staticmethod
    def calculate_roc(prices: List[float], period: int = 14) -> float:
        if len(prices) < period + 1:
            return 0.0
        return ((prices[-1] - prices[-period-1]) / prices[-period-1]) * 100

    @staticmethod
    def calculate_sma(prices: List[float], period: int) -> float:
        if len(prices) < period:
            return 0.0
        return sum(prices[-period:]) / period

    @staticmethod
    def calculate_bollinger(prices: List[float], period: int = 20, std_mult: float = 2.0) -> Tuple[float, float, float]:
        if len(prices) < period:
            return 0.0, 0.0, 0.0
        middle = PolymarketSuperTrader.calculate_sma(prices, period)
        std = (sum((x - middle) ** 2 for x in prices[-period:]) / period) ** 0.5
        return middle + std * std_mult, middle, middle - std * std_mult

    @staticmethod
    def calculate_rsi(prices: List[float], period: int = 14) -> float:
        if len(prices) < period + 1:
            return 50.0
        deltas = [prices[i] - prices[i-1] for i in range(1, len(prices))]
        gains = [d if d > 0 else 0 for d in deltas[-period:]]
        losses = [-d if d < 0 else 0 for d in deltas[-period:]]
        
        avg_gain = sum(gains) / period
        avg_loss = sum(losses) / period if sum(losses) > 0 else 0.0001
        rs = avg_gain / avg_loss
        return 100 - (100 / (1 + rs))

    def detect_momentum(self, token_id: str) -> Dict:
        history = self.fetch_price_history(token_id)
        if len(history) < Config.SMAC_LONG + 5:
            return {"signal": "HOLD", "confidence": 0.0, "reason": "Insufficient data"}

        prices = [float(p["p"]) for p in history]
        roc = self.calculate_roc(prices, Config.ROC_PERIOD)
        sma_short = self.calculate_sma(prices, Config.SMAC_SHORT)
        sma_long = self.calculate_sma(prices, Config.SMAC_LONG)

        if roc > 1.5 and sma_short > sma_long:
            return {"signal": "BUY", "confidence": min(roc / 15, 1.0), "roc": roc}
        elif roc < -1.5 and sma_short < sma_long:
            return {"signal": "SELL", "confidence": min(abs(roc) / 15, 1.0), "roc": roc}
        return {"signal": "HOLD", "confidence": 0.0, "roc": roc}

    def detect_overreaction(self, token_id: str) -> Dict:
        history = self.fetch_price_history(token_id, interval="15m")
        orderbook = self.fetch_orderbook(token_id)
        
        if len(history) < 10:
            return {"detected": False, "score": 0.0, "reason": "Low data"}

        prices = [float(p["p"]) for p in history]
        current = prices[-1]
        prev = prices[-2] if len(prices) > 1 else current
        price_change_pct = ((current - prev) / prev * 100) if prev != 0 else 0

        upper, _, lower = self.calculate_bollinger(prices, Config.BOLLINGER_PERIOD, Config.BOLLINGER_STD)
        rsi = self.calculate_rsi(prices, Config.RSI_PERIOD)

        score = 0.0
        signals = []

        if abs(price_change_pct) > Config.PRICE_SPIKE_THRESHOLD:
            score += 0.4
            signals.append(f"Price spike: {price_change_pct:.1f}%")

        if current > upper or current < lower:
            score += 0.3
            signals.append("Outside Bollinger Bands")

        if rsi > Config.RSI_OVERBOUGHT or rsi < Config.RSI_OVERSOLD:
            score += 0.2
            signals.append(f"RSI extreme: {rsi:.1f}")

        if orderbook:
            bids_vol = sum(float(b.get("size", 0)) for b in orderbook.get("bids", [])[:8])
            asks_vol = sum(float(a.get("size", 0)) for a in orderbook.get("asks", [])[:8])
            if bids_vol + asks_vol > 0:
                imbalance = abs(bids_vol - asks_vol) / (bids_vol + asks_vol)
                if imbalance > 0.65:
                    score += 0.2
                    signals.append(f"Orderbook imbalance: {imbalance:.2f}")

        return {
            "detected": score > 0.6,
            "score": score,
            "price_change_pct": price_change_pct,
            "rsi": rsi,
            "signals": signals
        }

    def generate_signal(self, token_id: str) -> Dict:
        """Main combined signal generator"""
        try:
            momentum = self.detect_momentum(token_id)
            overreaction = self.detect_overreaction(token_id)
        except Exception as e:
            logger.error(f"Signal generation processing error for {token_id}: {e}")
            return {
                "token_id": token_id,
                "recommendation": "HOLD",
                "confidence": 0.0,
                "current_price": 0.5,
                "rationale": [f"Error calculating parameters: {str(e)}"],
                "timestamp": datetime.now().isoformat()
            }

        recommendation = "HOLD"
        confidence = 0.0
        rationale = []

        # Pure momentum
        if momentum["signal"] != "HOLD" and not overreaction["detected"]:
            recommendation = momentum["signal"]
            confidence = momentum.get("confidence", 0.0) * 0.75
            rationale.append(f"Strong {momentum['signal']} momentum detected.")

        # Clear overreaction (mean reversion)
        elif overreaction["detected"] and overreaction["score"] > 0.75:
            if overreaction["price_change_pct"] > 4:
                recommendation = "SELL"
                rationale.append("Extreme overbought conditions - likely news overreaction.")
            else:
                recommendation = "BUY"
                rationale.append("Extreme oversold conditions - likely news overreaction.")
            confidence = min(overreaction["score"] * 0.85, 1.0)

        # Conflict resolution
        elif momentum["signal"] == "BUY" and overreaction["detected"]:
            recommendation = "HOLD"
            rationale.append("Momentum signals BUY but News Overreaction detects bubble. Conflict on standby.")

        try:
            hist = self.fetch_price_history(token_id)
            curr_price = float(hist[-1]["p"]) if hist else 0.5
        except Exception:
            curr_price = 0.5

        return {
            "token_id": token_id,
            "recommendation": recommendation,
            "confidence": round(confidence, 3),
            "current_price": curr_price,
            "momentum": momentum,
            "overreaction": overreaction,
            "rationale": rationale,
            "timestamp": datetime.now().isoformat(),
            "risk": {
                "position_size_pct": Config.POSITION_SIZE_PCT,
                "stop_loss_pct": Config.STOP_LOSS_PCT,
                "profit_target_pct": Config.PROFIT_TARGET_PCT
            }
        }

    def scan_markets(self, token_ids: List[str]) -> List[Dict]:
        results = []
        for tid in token_ids:
            try:
                signal = self.generate_signal(tid)
                results.append(signal)
            except Exception as e:
                logger.error(f"Error scanning {tid}: {e}")
        return results


# ========================= FASTAPI APPLICATION =========================
app = FastAPI(
    title="Polymarket Super Trader WebSockets",
    description="Momentum + News Overreaction Detector API with Real-time WebSocket Streaming",
    version="2.1.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

trader = PolymarketSuperTrader()

# ========================= WEBSOCKET CONNECTION MANAGER =========================
class ConnectionManager:
    def __init__(self):
        # Maps an active websocket to a set of token ID subscriptions
        self.active_connections: Dict[WebSocket, Set[str]] = {}
        self.lock = asyncio.Lock()

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections[websocket] = set()
        logger.info(f"WebSocket client connected from {websocket.client}")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            del self.active_connections[websocket]
            logger.info("WebSocket client disconnected.")

    def subscribe(self, websocket: WebSocket, token_ids: List[str]):
        if websocket in self.active_connections:
            for tid in token_ids:
                self.active_connections[websocket].add(tid)
            logger.info(f"Client subscribed to tokens: {token_ids}")

    def unsubscribe(self, websocket: WebSocket, token_ids: List[str]):
        if websocket in self.active_connections:
            for tid in token_ids:
                self.active_connections[websocket].discard(tid)
            logger.info(f"Client unsubscribed from tokens: {token_ids}")

    async def send_personal_message(self, message: dict, websocket: WebSocket):
        await websocket.send_json(message)

    async def broadcast_updates(self):
        """Asynchronously processes and broadcasts updates for all subscribed tokens."""
        if not self.active_connections:
            return

        # Gather unique token subscriptions across all active clients to optimize APIs
        all_tokens = set()
        for tokens in self.active_connections.values():
            all_tokens.update(tokens)

        if not all_tokens:
            return

        # Compile signals for all unique active tokens
        signals_by_token = {}
        for token_id in all_tokens:
            try:
                # Compile signal asynchronously
                signals_by_token[token_id] = trader.generate_signal(token_id)
            except Exception as e:
                logger.error(f"Error compiling stream signal for {token_id}: {e}")

        # Distribute updates to each socket based on their explicit subscriptions
        for websocket, subscriptions in list(self.active_connections.items()):
            to_send = []
            for sub_token in subscriptions:
                if sub_token in signals_by_token:
                    to_send.append(signals_by_token[sub_token])

            if to_send:
                try:
                    await websocket.send_json({
                        "type": "signals_update",
                        "timestamp": datetime.now().isoformat(),
                        "data": to_send
                    })
                except Exception:
                    # Connection closed or dropped
                    self.disconnect(websocket)

manager = ConnectionManager()

# ========================= FASTAPI ENDPOINTS =========================

class SignalRequest(BaseModel):
    token_id: str

class ScanRequest(BaseModel):
    token_ids: List[str]

@app.get("/")
async def health_check():
    return {
        "status": "healthy",
        "service": "Polymarket Super Trader Live Websocket Streamer",
        "time": datetime.now().isoformat(),
        "active_connections": len(manager.active_connections)
    }

@app.post("/signal")
async def get_signal(req: SignalRequest):
    try:
        return trader.generate_signal(req.token_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/scan")
async def scan_markets(req: ScanRequest):
    try:
        return trader.scan_markets(req.token_ids)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ========================= WEBSOCKET ENDPOINT =========================
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        # Send initial connection acknowledgment
        await manager.send_personal_message({
            "type": "connection_ack",
            "message": "Connected to Polymarket Real-time Quant Stream. Send 'subscribe' or 'unsubscribe' commands.",
            "timestamp": datetime.now().isoformat()
        }, websocket)

        while True:
            # Handle incoming client frame messages (e.g. subscribing or unsubscribing)
            data_str = await websocket.receive_text()
            try:
                payload = json.loads(data_str)
                action = payload.get("action")
                token_ids = payload.get("token_ids", [])

                if not isinstance(token_ids, list):
                    token_ids = [token_ids]

                if action == "subscribe":
                    manager.subscribe(websocket, token_ids)
                    await manager.send_personal_message({
                        "type": "subscription_success",
                        "subscribed_to": list(manager.active_connections[websocket]),
                        "timestamp": datetime.now().isoformat()
                    }, websocket)

                elif action == "unsubscribe":
                    manager.unsubscribe(websocket, token_ids)
                    await manager.send_personal_message({
                        "type": "unsubscription_success",
                        "subscribed_to": list(manager.active_connections[websocket]),
                        "timestamp": datetime.now().isoformat()
                    }, websocket)

                else:
                    await manager.send_personal_message({
                        "type": "error",
                        "message": "Invalid action. Supported: 'subscribe', 'unsubscribe'.",
                        "timestamp": datetime.now().isoformat()
                    }, websocket)

            except json.JSONDecodeError:
                await manager.send_personal_message({
                    "type": "error",
                    "message": "Malformed JSON payload.",
                    "timestamp": datetime.now().isoformat()
                }, websocket)

    except WebSocketDisconnect:
        manager.disconnect(websocket)
    except Exception as e:
        logger.error(f"WebSocket routing runtime error: {e}")
        manager.disconnect(websocket)

# ========================= BACKGROUND RECURRING BROADCAST TASK =========================
async def start_broadcaster_loop():
    logger.info("Initializing Polymarket continuous background broadcaster feed...")
    while True:
        try:
            await manager.broadcast_updates()
        except Exception as e:
            logger.error(f"Broadcaster loop failed: {e}")
        await asyncio.sleep(Config.STREAM_INTERVAL_SECONDS)

@app.on_event("startup")
async def startup_event():
    # Run the continuous streaming updates loop in the background
    asyncio.create_task(start_broadcaster_loop())

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("polymarket_super_trader_websocket:app", host="0.0.0.0", port=8080, reload=False)
