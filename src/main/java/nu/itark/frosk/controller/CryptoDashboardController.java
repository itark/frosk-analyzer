package nu.itark.frosk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CryptoPaperAccountDTO;
import nu.itark.frosk.analysis.CryptoPortfolioItemDTO;
import nu.itark.frosk.service.CryptoPaperTradingService;
import nu.itark.frosk.service.CryptoPortfolioService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class CryptoDashboardController {

    private final CryptoPortfolioService cryptoPortfolioService;
    private final CryptoPaperTradingService cryptoPaperTradingService;

    /**
     * @Example GET http://localhost:8081/crypto/portfolio
     */
    @GetMapping(value = "/crypto/portfolio")
    public List<CryptoPortfolioItemDTO> getCryptoPortfolio() {
        log.info("GET /crypto/portfolio");
        return cryptoPortfolioService.getOpenPositions();
    }

    /**
     * @Example GET http://localhost:8081/crypto/paper-account
     */
    @GetMapping(value = "/crypto/paper-account")
    public CryptoPaperAccountDTO getPaperAccount() {
        log.info("GET /crypto/paper-account");
        return cryptoPaperTradingService.getAccountSummary();
    }

    @GetMapping(value = "/crypto", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dashboard() {
        String html = """
<!DOCTYPE html>
<html lang="sv">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
  <title>Crypto PnL</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: #0d0d0d;
      color: #e0e0e0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      min-height: 100vh;
      padding: 16px;
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }
    h1 { font-size: 1.2rem; color: #9e9e9e; font-weight: 500; }
    #timestamp { font-size: 0.75rem; color: #555; }
    #refresh-btn {
      background: #1e1e1e;
      border: 1px solid #333;
      border-radius: 8px;
      color: #9e9e9e;
      padding: 6px 12px;
      font-size: 0.8rem;
      cursor: pointer;
    }
    #refresh-btn:active { background: #2a2a2a; }
    #total-card {
      background: #1a1a1a;
      border: 1px solid #2a2a2a;
      border-radius: 16px;
      padding: 24px 20px;
      text-align: center;
      margin-bottom: 20px;
    }
    #total-label { font-size: 0.9rem; color: #777; margin-bottom: 6px; }
    #total-pnl {
      font-size: 3.5rem;
      font-weight: 700;
      letter-spacing: -1px;
      transition: color 0.3s;
    }
    #total-pnl.positive { color: #4caf50; }
    #total-pnl.negative { color: #f44336; }
    #total-pnl.neutral  { color: #9e9e9e; }
    #total-trades { font-size: 0.85rem; color: #555; margin-top: 6px; }
    #cards { display: flex; flex-direction: column; gap: 12px; }
    .card {
      background: #1a1a1a;
      border: 1px solid #2a2a2a;
      border-radius: 12px;
      padding: 16px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .card-left {}
    .card-ticker { font-size: 1.05rem; font-weight: 600; color: #e0e0e0; }
    .card-meta { font-size: 0.78rem; color: #555; margin-top: 3px; }
    .card-pnl { font-size: 1.6rem; font-weight: 700; }
    .card-pnl.positive { color: #4caf50; }
    .card-pnl.negative { color: #f44336; }
    .card-pnl.neutral  { color: #9e9e9e; }
    #status {
      text-align: center;
      color: #555;
      font-size: 0.8rem;
      margin-top: 24px;
    }
    #spinner {
      display: none;
      width: 28px; height: 28px;
      border: 3px solid #2a2a2a;
      border-top-color: #4caf50;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 40px auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    #error-msg {
      display: none;
      background: #1f0a0a;
      border: 1px solid #f44336;
      border-radius: 12px;
      padding: 14px;
      color: #f44336;
      font-size: 0.85rem;
      text-align: center;
      margin-top: 12px;
    }
  </style>
</head>
<body>
  <header>
    <h1>Crypto PnL</h1>
    <button id="refresh-btn" onclick="load()">↻ Refresh</button>
  </header>

  <div id="total-card">
    <div id="total-label">Totalt PnL</div>
    <div id="total-pnl" class="neutral">—</div>
    <div id="total-trades"></div>
  </div>

  <div id="spinner"></div>
  <div id="cards"></div>
  <div id="error-msg"></div>
  <div id="status"></div>

  <script>
    let refreshTimer;

    function fmt(val) {
      if (val == null) return '—';
      const n = parseFloat(val);
      return (n >= 0 ? '+' : '') + n.toFixed(2) + '%';
    }

    function colorClass(val) {
      if (val == null) return 'neutral';
      const n = parseFloat(val);
      return n > 0 ? 'positive' : n < 0 ? 'negative' : 'neutral';
    }

    async function load() {
      const spinner  = document.getElementById('spinner');
      const cards    = document.getElementById('cards');
      const errMsg   = document.getElementById('error-msg');
      const status   = document.getElementById('status');
      const totalPnl = document.getElementById('total-pnl');
      const totalTr  = document.getElementById('total-trades');

      spinner.style.display = 'block';
      cards.innerHTML = '';
      errMsg.style.display = 'none';

      try {
        const res  = await fetch('/intraday/pnl');
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();

        spinner.style.display = 'none';

        if (!Array.isArray(data) || data.length === 0) {
          cards.innerHTML = '<p style="text-align:center;color:#555;margin-top:20px">Inga trades idag</p>';
          totalPnl.textContent = '—';
          totalPnl.className   = 'neutral';
          totalTr.textContent  = '';
          scheduleRefresh();
          return;
        }

        // Aggregate total PnL (weighted avg via sum / count of non-null rows)
        let sumPnl   = 0, countPnl = 0, totalTrades = 0;
        data.forEach(d => {
          if (d.totalPnlPercent != null) {
            sumPnl  += parseFloat(d.totalPnlPercent);
            countPnl++;
          }
          totalTrades += (d.totalTrades || 0);
        });
        const aggPnl = countPnl > 0 ? sumPnl / countPnl : null;

        totalPnl.textContent = fmt(aggPnl);
        totalPnl.className   = colorClass(aggPnl);
        totalTr.textContent  = totalTrades + ' trade' + (totalTrades !== 1 ? 's' : '');

        // Per-ticker cards
        data.forEach(d => {
          const card = document.createElement('div');
          card.className = 'card';
          const wins = d.winningTrades || 0;
          const loss = d.losingTrades  || 0;
          card.innerHTML = `
            <div class="card-left">
              <div class="card-ticker">${d.ticker || d.strategyName || '?'}</div>
              <div class="card-meta">${d.totalTrades || 0} trades · ${wins}W / ${loss}L</div>
            </div>
            <div class="card-pnl ${colorClass(d.totalPnlPercent)}">${fmt(d.totalPnlPercent)}</div>
          `;
          cards.appendChild(card);
        });

        const now = new Date().toLocaleTimeString('sv-SE', { hour: '2-digit', minute: '2-digit' });
        status.textContent = 'Uppdaterad ' + now + ' · auto-refresh var 5 min';
        document.getElementById('timestamp').textContent = now;

      } catch (e) {
        spinner.style.display = 'none';
        errMsg.style.display  = 'block';
        errMsg.textContent    = 'Kunde inte hämta data: ' + e.message;
        status.textContent    = 'Försöker igen om 5 min…';
      }

      scheduleRefresh();
    }

    function scheduleRefresh() {
      clearTimeout(refreshTimer);
      refreshTimer = setTimeout(load, 5 * 60 * 1000);
    }

    // Bootstrap
    document.getElementById('timestamp').textContent = '–';
    load();
  </script>

  <div style="text-align:center;margin-top:4px">
    <span id="timestamp" style="font-size:0.7rem;color:#333"></span>
  </div>
</body>
</html>
""";
        return ResponseEntity.ok(html);
    }
}
