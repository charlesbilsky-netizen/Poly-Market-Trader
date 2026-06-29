"""
Polymarket Momentum Trading Algorithm (v2.0)
Asynchronous, robust, and ready for Google AI Studio integration.

Combines:
- Rate of Change (ROC) + Moving Average Crossover (SMAC) for momentum
- Bollinger Bands, RSI, and volume spikes for news overreaction detection
- Conflict resolution and risk management

Author: Enhanced version based on community contributions
"""

import asyncio
import logging
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Dict, List, Optional, Tuple, Callable
import statistics

import aiohttp
from polymarket import AsyncPublicClient, AsyncStreamingClient
from polymarket.models import Market, MarketOutcome

# ---------- Configuration ----------
# All parameters are externalized for easy tuning
CONFIG = {
    # Momentum
    "ROC_PERIOD": 14,
    "SMAC_SHORT": 5,
    "SMAC_LONG": 10,
    "ROC_THRESHOLD": 2.0,          # percentage

    # Overreaction
    "BOLLINGER_PERIOD": 20,
    "BOLLINGER_STD": 2,
    "RSI_PERIOD": 14,
    "RSI_OVERBOUGHT": 70,
    "RSI_OVERSOLD": 30,
    "VOLUME_SPIKE_MULTIPLIER": 2.0,

    # Risk
    "POSITION_SIZE_PERCENT": 0.01,
    "STOP_LOSS_PERCENT": 0.01,
    "PROFIT_TARGET_PERCENT": 0.02,

    # Data
    "PRICE_HISTORY_DAYS": 60,
    "INTERVAL": "1h",              # 1h, 6h, 1d, etc.
    "MIN_DATA_POINTS": 30,

    # Override threshold – if overreaction score > this, it overrides momentum
    "OVERRIDE_THRESHOLD": 0.8,
}

# ---------- Logging ----------
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# ---------- Core Class ----------
class PolymarketMomentumTraderV2:
    """
    Asynchronous momentum + overreaction trader for Polymarket.
    """

    def __init__(self, config: Optional[Dict] = None):
        self.config = {**CONFIG, **(config or {})}
        self.client = None
        self.streaming_client = None
        self._price_cache = {}
        self._volume_cache = {}

    async def __aenter__(self):
        self.client = AsyncPublicClient()
        self.streaming_client = AsyncStreamingClient()
        await self.streaming_client.connect()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.client:
            await self.client.close()
        if self.streaming_client:
            await self.streaming_client.disconnect()

    # ---------- Data Fetching ----------
    async def get_market(self, slug: str) -> Optional[Market]:
        """Fetch market details by slug."""
        try:
            return await self.client.get_market(slug=slug)
        except Exception as e:
            logger.error(f"Failed to fetch market {slug}: {e}")
            return None
    
    # ---------- Streaming ----------
    async def subscribe_to_token(self, token_id: str, callback: Callable[[Dict], None]):
        """Subscribe to real-time updates for a token."""
        await self.streaming_client.subscribe(token_id=token_id, callback=callback)


    async def get_token_price_history(self, token_id: str) -> Optional[Tuple[List[Decimal], List[Decimal]]]:
        """
        Fetch historical prices and volumes for a token.
        Returns (prices, volumes) as lists of Decimals.
        """
        if token_id in self._price_cache:
            return self._price_cache[token_id]

        end_ts = int(datetime.now().timestamp())
        start_ts = int((datetime.now() - timedelta(days=self.config["PRICE_HISTORY_DAYS"])).timestamp())

        try:
            history = await self.client.get_prices_history(
                token_id=token_id,
                start_ts=start_ts,
                end_ts=end_ts,
                interval=self.config["INTERVAL"]
            )
            if not history or not history.items:
                logger.warning(f"No price history for token {token_id}")
                return None

            prices = [Decimal(str(item.price)) for item in history.items]
            volumes = [Decimal(str(item.volume)) for item in history.items]

            self._price_cache[token_id] = (prices, volumes)
            return prices, volumes
        except Exception as e:
            logger.error(f"Error fetching price history for {token_id}: {e}")
            return None

    # ---------- Technical Indicators ----------
    @staticmethod
    def roc(prices: List[Decimal], period: int) -> Optional[Decimal]:
        if len(prices) <= period:
            return None
        current = prices[-1]
        past = prices[-period - 1]
        if past == Decimal('0'):
            return None
        return ((current - past) / past) * Decimal('100')

    @staticmethod
    def sma(prices: List[Decimal], period: int) -> Optional[Decimal]:
        if len(prices) < period:
            return None
        return sum(prices[-period:]) / Decimal(period)

    @staticmethod
    def bollinger_bands(prices: List[Decimal], period: int, std_dev: int) -> Optional[Tuple[Decimal, Decimal, Decimal]]:
        if len(prices) < period:
            return None
        middle = PolymarketMomentumTraderV2.sma(prices, period)
        if middle is None:
            return None
        recent = [float(p) for p in prices[-period:]]
        std_val = statistics.stdev(recent) if len(recent) > 1 else 0.0
        std_dec = Decimal(str(std_val))
        upper = middle + std_dec * Decimal(std_dev)
        lower = middle - std_dec * Decimal(std_dev)
        return middle, upper, lower

    @staticmethod
    def rsi(prices: List[Decimal], period: int) -> Optional[Decimal]:
        if len(prices) < period + 1:
            return None
        deltas = [prices[i] - prices[i-1] for i in range(1, len(prices))]
        gains = [d if d > 0 else Decimal('0') for d in deltas[-period:]]
        losses = [-d if d < 0 else Decimal('0') for d in deltas[-period:]]
        avg_gain = sum(gains) / Decimal(period) if gains else Decimal('0')
        avg_loss = sum(losses) / Decimal(period) if losses else Decimal('0')
        if avg_loss == Decimal('0'):
            return Decimal('100') if avg_gain > 0 else Decimal('50')
        rs = avg_gain / avg_loss
        return Decimal('100') - (Decimal('100') / (Decimal('1') + rs))

    @staticmethod
    def volume_spike(volumes: List[Decimal], multiplier: float) -> bool:
        if len(volumes) < 2:
            return False
        avg_vol = sum(volumes[:-1]) / Decimal(len(volumes) - 1)
        if avg_vol == Decimal('0'):
            return False
        return volumes[-1] > avg_vol * Decimal(multiplier)

    # ---------- Signal Generation ----------
    async def analyze(self, market_slug: str) -> Dict:
        """
        Generate trading signal for a given market slug.
        Returns a dictionary with recommendation, confidence, and details.
        """
        # 1. Get market
        market = await self.get_market(market_slug)
        if not market:
            return {"error": "Market not found", "recommendation": "HOLD", "confidence": 0.0}

        # 2. Locate "Yes" outcome (simplified; can be generalised)
        yes_outcome = next((o for o in market.outcomes if o.label.lower() == "yes"), None)
        if not yes_outcome or not yes_outcome.token_id:
            return {"error": "No 'Yes' outcome token", "recommendation": "HOLD", "confidence": 0.0}

        token_id = yes_outcome.token_id
        hist = await self.get_token_price_history(token_id)
        if not hist:
            return {"error": "Insufficient price data", "recommendation": "HOLD", "confidence": 0.0}

        prices, volumes = hist
        if len(prices) < self.config["MIN_DATA_POINTS"]:
            return {"error": "Not enough data points", "recommendation": "HOLD", "confidence": 0.0}

        current_price = prices[-1]

        # 3. Momentum
        roc_val = self.roc(prices, self.config["ROC_PERIOD"])
        sma_short = self.sma(prices, self.config["SMAC_SHORT"])
        sma_long = self.sma(prices, self.config["SMAC_LONG"])

        momentum = "NEUTRAL"
        if (roc_val is not None and roc_val > Decimal('0') and
            sma_short is not None and sma_long is not None and
            sma_short > sma_long):
            momentum = "BUY"
        elif (roc_val is not None and roc_val < Decimal('0') and
              sma_short is not None and sma_long is not None and
              sma_short < sma_long):
            momentum = "SELL"

        # 4. Overreaction detection
        bb = self.bollinger_bands(prices, self.config["BOLLINGER_PERIOD"], self.config["BOLLINGER_STD"])
        rsi_val = self.rsi(prices, self.config["RSI_PERIOD"])
        volume_spike = self.volume_spike(volumes, self.config["VOLUME_SPIKE_MULTIPLIER"])

        overreaction = "NONE"
        overreaction_score = 0.0
        if bb is not None and rsi_val is not None:
            _, upper, lower = bb
            # Conditions for overreaction
            if current_price > upper and rsi_val > self.config["RSI_OVERBOUGHT"] and volume_spike:
                overreaction = "POTENTIAL_SELL_OVERREACTION"
                overreaction_score = 0.9
            elif current_price < lower and rsi_val < self.config["RSI_OVERSOLD"] and volume_spike:
                overreaction = "POTENTIAL_BUY_OVERREACTION"
                overreaction_score = 0.9

        # 5. Combined logic
        recommendation = "HOLD"
        reason = ""
        confidence = 0.0

        if momentum == "BUY" and overreaction == "NONE":
            recommendation = "BUY"
            reason = "Strong upward momentum"
            confidence = 0.7
        elif momentum == "SELL" and overreaction == "NONE":
            recommendation = "SELL"
            reason = "Strong downward momentum"
            confidence = 0.7
        elif overreaction == "POTENTIAL_BUY_OVERREACTION" and overreaction_score > self.config["OVERRIDE_THRESHOLD"]:
            recommendation = "BUY"
            reason = "Oversold with volume spike – mean reversion expected"
            confidence = 0.8
        elif overreaction == "POTENTIAL_SELL_OVERREACTION" and overreaction_score > self.config["OVERRIDE_THRESHOLD"]:
            recommendation = "SELL"
            reason = "Overbought with volume spike – mean reversion expected"
            confidence = 0.8
        elif momentum == "BUY" and overreaction == "POTENTIAL_SELL_OVERREACTION":
            recommendation = "HOLD"
            reason = "Conflict: upward momentum but overbought"
            confidence = 0.0
        elif momentum == "SELL" and overreaction == "POTENTIAL_BUY_OVERREACTION":
            recommendation = "HOLD"
            reason = "Conflict: downward momentum but oversold"
            confidence = 0.0
        else:
            reason = "No clear signal from indicators"

        # 6. Risk management calculations
        position_size = self.config["POSITION_SIZE_PERCENT"]
        stop_loss = current_price * (1 - self.config["STOP_LOSS_PERCENT"]) if recommendation == "BUY" else current_price * (1 + self.config["STOP_LOSS_PERCENT"])
        profit_target = current_price * (1 + self.config["PROFIT_TARGET_PERCENT"]) if recommendation == "BUY" else current_price * (1 - self.config["PROFIT_TARGET_PERCENT"])

        return {
            "market_slug": market_slug,
            "token_id": token_id,
            "recommendation": recommendation,
            "confidence": confidence,
            "reason": reason,
            "current_price": float(current_price),
            "momentum_signal": momentum,
            "overreaction_signal": overreaction,
            "overreaction_score": overreaction_score,
            "rsi": float(rsi_val) if rsi_val else None,
            "bollinger_upper": float(bb[1]) if bb else None,
            "bollinger_lower": float(bb[2]) if bb else None,
            "volume_spike": volume_spike,
            "position_size_pct": position_size,
            "stop_loss": float(stop_loss),
            "profit_target": float(profit_target),
            "timestamp": datetime.now().isoformat()
        }

    # ---------- Market Scanning ----------
    async def scan_active_markets(self, max_markets: int = 50) -> List[Dict]:
        """
        Scan all active, liquid markets and return signals for each.
        """
        signals = []
        try:
            # Discover markets using Gamma API (since SDK list_markets might be limited)
            async with aiohttp.ClientSession() as session:
                url = "https://gamma-api.polymarket.com/markets"
                params = {
                    "active": "true",
                    "limit": max_markets,
                    "order": "volume24h:desc"   # prioritize high liquidity
                }
                async with session.get(url, params=params) as resp:
                    if resp.status != 200:
                        logger.error(f"Gamma API error: {resp.status}")
                        return []
                    markets = await resp.json()
                    for m in markets:
                        slug = m.get("slug")
                        if slug:
                            signal = await self.analyze(slug)
                            if "error" not in signal:
                                signals.append(signal)
        except Exception as e:
            logger.exception("Error during market scanning")
        return signals

# ---------- Example Usage ----------
async def main():
    async with PolymarketMomentumTraderV2() as trader:
        # Single market analysis
        signal = await trader.analyze("eth-flipped-in-2026")
        print(f"Single market: {signal}")

        # Scan top markets
        signals = await trader.scan_active_markets(5)
        for s in signals:
            print(f"Market {s['market_slug']}: {s['recommendation']} (conf: {s['confidence']:.1%})")

if __name__ == "__main__":
    asyncio.run(main())
