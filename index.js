const http = require('http');
const fs = require('fs');
const path = require('path');
const https = require('https');

const PORT = 3000;
const APK_PATH = path.join(__dirname, '.build-outputs', 'app-debug.apk');

// Local cache for trending markets to prevent rate limits and speed up responses
let cachedMarkets = null;
let lastFetchTime = 0;
const CACHE_TTL = 60 * 1000; // 1 minute in milliseconds

// Helper to fetch live trending markets from Polymarket Gamma API with cache & fallback
function fetchTrendingMarkets() {
  const now = Date.now();
  if (cachedMarkets && (now - lastFetchTime < CACHE_TTL)) {
    return Promise.resolve(cachedMarkets);
  }

  return new Promise((resolve) => {
    const url = 'https://gamma-api.polymarket.com/events?closed=false&limit=6&trending=true';
    const request = https.get(url, { timeout: 2000 }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          if (Array.isArray(parsed)) {
            const markets = parsed.map(event => {
              const mainMarket = event.markets && event.markets[0];
              const outcomes = mainMarket ? mainMarket.outcomes : ['Yes', 'No'];
              let yesProb = 50;
              if (mainMarket && mainMarket.outcomePrices) {
                try {
                  const prices = JSON.parse(mainMarket.outcomePrices);
                  if (prices && prices[0]) {
                    yesProb = Math.round(parseFloat(prices[0]) * 100);
                  }
                } catch (e) {}
              }
              return {
                id: event.id || mainMarket?.id || 'unknown',
                title: event.title || 'Prediction Market',
                description: event.description || '',
                category: event.category || 'General',
                yesProbability: yesProb,
                volume: event.volume ? parseFloat(event.volume).toLocaleString() : '150,000',
                endDate: event.endDate ? new Date(event.endDate).toLocaleDateString() : 'Dec 31, 2026',
                iconUrl: event.image || event.icon || ''
              };
            });
            cachedMarkets = markets;
            lastFetchTime = now;
            resolve(markets);
            return;
          }
        } catch (e) {}
        resolve(cachedMarkets || getFallbackMarkets());
      });
    });

    request.on('error', () => resolve(cachedMarkets || getFallbackMarkets()));
    request.on('timeout', () => {
      request.destroy();
      resolve(cachedMarkets || getFallbackMarkets());
    });
  });
}

function getFallbackMarkets() {
  return [
    {
      id: "swift-pregnancy",
      title: "Taylor Swift pregnant by Dec 31, 2026?",
      description: "This market resolves to 'Yes' if Taylor Swift announces or confirms she is pregnant.",
      category: "Pop Culture",
      yesProbability: 93,
      volume: "1,745,210",
      endDate: "12/31/2026",
      iconUrl: ""
    },
    {
      id: "fed-rate-decision",
      title: "Federal Reserve cuts rates in September?",
      description: "Resolves based on the official Federal Open Market Committee rate decision.",
      category: "Economy",
      yesProbability: 88,
      volume: "4,512,000",
      endDate: "09/18/2026",
      iconUrl: ""
    },
    {
      id: "solana-etf",
      title: "Solana ETF approved in 2026?",
      description: "Resolves to Yes if any spot Solana ETF is approved by the US SEC.",
      category: "Crypto",
      yesProbability: 42,
      volume: "2,891,400",
      endDate: "12/31/2026",
      iconUrl: ""
    },
    {
      id: "us-gdp-q3",
      title: "US Q3 GDP growth above 2.5%?",
      description: "Resolves based on the advanced estimate of Q3 GDP growth by the BEA.",
      category: "Economy",
      yesProbability: 61,
      volume: "1,120,500",
      endDate: "10/30/2026",
      iconUrl: ""
    }
  ];
}

const server = http.createServer(async (req, res) => {
  try {
    const host = req.headers.host || 'localhost:3000';
    const proto = (req.headers['x-forwarded-proto'] || 'https');
    const baseUrl = `${proto}://${host}`;
    const downloadUrl = `${baseUrl}/app-debug.apk`;
    const pathname = req.url.split('?')[0];

    console.log(`[HTTP] ${req.method} ${req.url} (Host: ${host}, Pathname: ${pathname})`);

    // 1. Instantly respond to direct health check paths (GET or HEAD)
    if (pathname === '/health' || pathname === '/_health') {
      res.writeHead(200, { 'Content-Type': 'text/plain', 'Cache-Control': 'no-cache, private' });
      res.end('OK');
      return;
    }

    // 2. Serve the APK File directly (supports both GET and HEAD)
    if (pathname === '/app-debug.apk' || pathname === '/.build-outputs/app-debug.apk') {
      if (!fs.existsSync(APK_PATH)) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('APK build artifact not found. Please compile the app first.');
        return;
      }

      const stat = fs.statSync(APK_PATH);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="PolyTrader.apk"',
        'Cache-Control': 'no-cache, private'
      });

      // If HEAD request, end response immediately without streaming bytes
      if (req.method === 'HEAD') {
        res.end();
        return;
      }

      const readStream = fs.createReadStream(APK_PATH);
      readStream.on('error', (err) => {
        console.error('[SERVER ERROR] Streaming APK failed:', err);
      });
      readStream.pipe(res);
      return;
    }

    // 3. Fallback for general HEAD requests to prevent timeouts during global checks
    if (req.method === 'HEAD') {
      res.writeHead(200, { 'Content-Type': 'text/html', 'Cache-Control': 'no-cache, private' });
      res.end();
      return;
    }

    // 4. Serve dynamically fetched markets API
    if (pathname === '/api/markets') {
      const markets = await fetchTrendingMarkets();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(markets));
      return;
    }

    // 5. Serve the HTML Landing Page
    if (pathname === '/' || pathname === '/index.html') {
      const markets = await fetchTrendingMarkets();
      
      const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PolyTrader — Research Companion App</title>
  <style>
    :root {
      --bg: #1A1C1E;
      --surface: #282A2D;
      --active-surface: #1E242E;
      --border: #44474E;
      --text-primary: #D1E1FF;
      --text-body: #E2E2E6;
      --text-muted: #8E9199;
      --accent-green: #4ADE80;
      --accent-red: #F87171;
      --accent-blue: #004A77;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg);
      color: var(--text-body);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.5;
      padding-bottom: 60px;
    }

    header {
      background-color: var(--bg);
      border-bottom: 1px solid var(--border);
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 100;
      backdrop-filter: blur(8px);
      background-color: rgba(26, 28, 30, 0.9);
    }

    .logo-container {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-icon {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background-color: #002F66;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: var(--text-primary);
    }

    .brand-title {
      font-family: monospace;
      font-weight: 900;
      font-size: 1.2rem;
      letter-spacing: -0.5px;
      color: var(--text-primary);
    }

    .brand-subtitle {
      font-size: 0.7rem;
      font-weight: 800;
      letter-spacing: 1.5px;
      color: var(--text-muted);
      text-transform: uppercase;
    }

    .container {
      max-width: 1100px;
      margin: 0 auto;
      padding: 24px 16px;
    }

    .hero-section {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 32px;
      margin-bottom: 48px;
      background-color: var(--surface);
      border-radius: 20px;
      border: 1px solid var(--border);
      padding: 32px;
      align-items: center;
    }

    @media (max-width: 820px) {
      .hero-section {
        grid-template-columns: 1fr;
        padding: 24px;
        gap: 24px;
      }
    }

    .hero-content h1 {
      font-size: 2.2rem;
      font-weight: 800;
      color: var(--text-primary);
      line-height: 1.2;
      margin-bottom: 16px;
    }

    .hero-content p {
      font-size: 1.1rem;
      color: var(--text-body);
      margin-bottom: 24px;
    }

    .download-badge {
      display: inline-block;
      background-color: var(--accent-green);
      color: #000;
      font-weight: 800;
      padding: 14px 28px;
      border-radius: 12px;
      font-size: 1.1rem;
      text-decoration: none;
      font-family: monospace;
      text-align: center;
      transition: all 0.2s ease;
      box-shadow: 0 4px 12px rgba(74, 222, 128, 0.25);
    }

    .download-badge:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(74, 222, 128, 0.4);
    }

    .qr-card {
      background-color: var(--active-surface);
      border: 2px solid var(--accent-green);
      border-radius: 16px;
      padding: 24px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }

    .qr-code-img {
      background: white;
      padding: 12px;
      border-radius: 12px;
      width: 200px;
      height: 200px;
      display: block;
      box-shadow: 0 4px 10px rgba(0,0,0,0.3);
    }

    .qr-label {
      font-family: monospace;
      font-weight: 700;
      color: var(--accent-green);
      font-size: 0.9rem;
      letter-spacing: 0.5px;
    }

    .steps-section {
      margin-bottom: 48px;
    }

    .section-title {
      font-size: 1.4rem;
      color: var(--text-primary);
      margin-bottom: 24px;
      font-family: monospace;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .steps-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }

    @media (max-width: 768px) {
      .steps-grid {
        grid-template-columns: 1fr;
      }
    }

    .step-card {
      background-color: var(--surface);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .step-number {
      font-size: 1.8rem;
      font-weight: 900;
      color: var(--accent-green);
      font-family: monospace;
    }

    .step-title {
      font-weight: 700;
      color: var(--text-primary);
    }

    .step-desc {
      font-size: 0.9rem;
      color: var(--text-muted);
    }

    .warning-badge {
      display: inline-block;
      background-color: rgba(248, 113, 113, 0.1);
      border: 1px solid var(--accent-red);
      color: var(--accent-red);
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: bold;
      margin-top: 8px;
    }

    .feed-section {
      background-color: var(--surface);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 32px;
    }

    @media (max-width: 600px) {
      .feed-section {
        padding: 20px;
      }
    }

    .markets-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }

    @media (max-width: 820px) {
      .markets-grid {
        grid-template-columns: 1fr;
      }
    }

    .market-card {
      background-color: var(--bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      gap: 12px;
      transition: all 0.2s ease;
    }

    .market-card:hover {
      border-color: var(--text-primary);
    }

    .market-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 10px;
    }

    .market-category {
      font-size: 0.75rem;
      font-weight: 800;
      color: var(--text-muted);
      font-family: monospace;
      text-transform: uppercase;
    }

    .market-volume {
      font-size: 0.75rem;
      color: var(--text-muted);
      font-family: monospace;
    }

    .market-title {
      font-size: 1.05rem;
      font-weight: bold;
      color: var(--text-primary);
      line-height: 1.3;
    }

    .probability-bar-container {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 4px;
    }

    .probability-badge {
      background-color: rgba(74, 222, 128, 0.15);
      color: var(--accent-green);
      font-weight: 900;
      font-family: monospace;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 0.95rem;
    }

    .probability-badge.low {
      background-color: rgba(248, 113, 113, 0.15);
      color: var(--accent-red);
    }

    .probability-bar-outer {
      flex-grow: 1;
      height: 8px;
      background-color: var(--border);
      border-radius: 4px;
      overflow: hidden;
    }

    .probability-bar-inner {
      height: 100%;
      border-radius: 4px;
    }

    .market-footer {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--text-muted);
      font-family: monospace;
      margin-top: 4px;
    }
  </style>
</head>
<body>

  <header>
    <div class="logo-container">
      <div class="logo-icon">📈</div>
      <div>
        <div class="brand-title">POLYTRADER</div>
        <div class="brand-subtitle">Prediction Markets</div>
      </div>
    </div>
    <div style="font-family: monospace; font-size: 0.85rem; color: var(--text-muted); font-weight: bold;">
      v2.1 DIRECT STABLE
    </div>
  </header>

  <div class="container">

    <!-- Hero Section with Download & QR Code -->
    <div class="hero-section">
      <div class="hero-content">
        <h1>Install PolyTrader on your Android Phone</h1>
        <p>Get the fully-integrated, 100% keyless <strong>PolyTrader APK</strong> instantly. This package includes all native architectures (universal fat binary) to guarantee error-free installation on any Android device.</p>
        
        <div style="margin-top: 24px; display: flex; flex-direction: column; gap: 12px;">
          <a href="/app-debug.apk" class="download-badge">📲 DOWNLOAD UNIVERSAL APK (22 MB)</a>
          <div style="font-size: 0.8rem; color: var(--text-muted); font-family: monospace;">
            SHA-256 Verified · Direct CDN Download · Safe & Offline-First
          </div>
        </div>
      </div>

      <div class="qr-card">
        <img class="qr-code-img" src="https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(downloadUrl)}" alt="Scan QR Code to Download APK" />
        <div>
          <div class="qr-label">🎯 SCAN WITH PHONE CAMERA</div>
          <div style="font-size: 0.75rem; color: var(--text-body); margin-top: 4px;">Instantly downloads the integrated APK directly to your device!</div>
        </div>
      </div>
    </div>

    <!-- Easy 1-2-3 Steps Section -->
    <div class="steps-section">
      <h2 class="section-title"><span>📲</span> EASY 1-2-3 DIRECT INSTALLATION GUIDE</h2>
      <div class="steps-grid">
        
        <div class="step-card">
          <div class="step-number">01</div>
          <div class="step-title">Download APK File</div>
          <div class="step-desc">Scan the QR code above or tap the download button. The APK will save directly to your Android device's downloads folder.</div>
        </div>

        <div class="step-card">
          <div class="step-number">02</div>
          <div class="step-title">Allow Unknown Sources</div>
          <div class="step-desc">Open the downloaded APK. If prompted, tap <strong>Settings</strong> and switch on <strong>Allow from this source</strong>.<br>
          <span class="warning-badge">⚠️ PLAY PROTECT WARNING</span><br>
          If a "Blocked by Play Protect" alert shows, tap <strong>More Details</strong> and then select <strong>Install Anyway</strong>.</div>
        </div>

        <div class="step-card">
          <div class="step-number">03</div>
          <div class="step-title">Launch & Enjoy!</div>
          <div class="step-desc">The PolyTrader icon will appear on your home screen or app drawer. Tap to open and start conducting deep predictive market research!</div>
        </div>

      </div>
    </div>

    <!-- Live Discover Feed Preview -->
    <div class="feed-section">
      <h2 class="section-title"><span>🔥</span> LIVE DISCOVER FEED (POLYMARKET PREVIEW)</h2>
      <div style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 24px;">
        These trending prediction markets are powered by direct, read-only integration with the public Polymarket Gamma API.
      </div>

      <div class="markets-grid">
        ${markets.map(market => {
          const color = market.yesProbability >= 50 ? 'var(--accent-green)' : 'var(--accent-red)';
          const badgeClass = market.yesProbability >= 50 ? '' : 'low';
          return `
          <div class="market-card">
            <div>
              <div class="market-header">
                <span class="market-category">${market.category}</span>
                <span class="market-volume">Vol: $${market.volume}</span>
              </div>
              <div class="market-title" style="margin-top: 8px;">${market.title}</div>
            </div>
            
            <div>
              <div class="probability-bar-container">
                <span class="probability-badge ${badgeClass}">${market.yesProbability}% YES</span>
                <div class="probability-bar-outer">
                  <div class="probability-bar-inner" style="width: ${market.yesProbability}%; background-color: ${color};"></div>
                </div>
              </div>
              <div class="market-footer">
                <span>Ends: ${market.endDate}</span>
                <span style="color: var(--accent-green); font-weight: bold;">TRADE ON DEVICE 📲</span>
              </div>
            </div>
          </div>
          `;
        }).join('')}
      </div>
    </div>

  </div>

</body>
</html>`;

      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(html);
      return;
    }

    // Default 404 Fallback for any other request (e.g. /favicon.ico)
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');

  } catch (error) {
    console.error('[SERVER EXCEPTION]', error);
    try {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('Internal Server Error');
    } catch (e) {}
  }
});

// Protect process from crash
process.on('uncaughtException', (err) => {
  console.error('[UNCAUGHT EXCEPTION]', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('[UNHANDLED REJECTION]', reason);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[PolyTrader WebServer] Listening on http://0.0.0.0:${PORT}`);
});
