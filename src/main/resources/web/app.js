const form = document.getElementById("book");
const reportEl = document.getElementById("report");
const errorEl = document.getElementById("error");

const num = (n, dp = 2) =>
  n.toLocaleString(undefined, {
    minimumFractionDigits: dp,
    maximumFractionDigits: dp,
  });

const signed = (n) => `<span class="${n < 0 ? "neg" : "pos"}">${num(n)}</span>`;

function card(title, body) {
  return `<div class="card"><h2>${title}</h2>${body}</div>`;
}

function rows(pairs) {
  const body = pairs
    .map(([k, v]) => `<tr><td>${k}</td><td>${v}</td></tr>`)
    .join("");
  return `<table><tbody>${body}</tbody></table>`;
}

function render(r) {
  const g = r.greeks;
  const valuation = card(
    "Valuation",
    `<div class="valuation">${num(r.valuation)}</div>`,
  );

  const greeks = card(
    "Greeks",
    rows([
      ["delta", num(g.delta)],
      ["gamma", num(g.gamma)],
      ["vega", num(g.vega)],
      ["theta", num(g.theta)],
      ["rho", num(g.rho)],
    ]),
  );

  const pct = Math.round(r.confidence * 100);
  const v = r.var;
  const varCard = card(
    `Value at Risk / ES · ${pct}%`,
    `<table>
       <thead><tr><th>method</th><th>VaR</th><th>ES</th></tr></thead>
       <tbody>
         <tr><td>parametric</td><td>${num(v.parametric.valueAtRisk)}</td><td>${num(v.parametric.expectedShortfall)}</td></tr>
         <tr><td>historical</td><td>${num(v.historical.valueAtRisk)}</td><td>${num(v.historical.expectedShortfall)}</td></tr>
       </tbody>
     </table>`,
  );

  const cards = [valuation, greeks, varCard];
  if (r.pnl) {
    const p = r.pnl;
    cards.push(
      card(
        "PnL explain",
        rows([
          ["actual", signed(p.actual)],
          ["delta", signed(p.delta)],
          ["gamma", signed(p.gamma)],
          ["vega", signed(p.vega)],
          ["theta", signed(p.theta)],
          ["rho", signed(p.rho)],
          ["residual", signed(p.residual)],
        ]),
      ),
    );
  }
  reportEl.innerHTML = cards.join("");
}

async function load(method, body) {
  errorEl.hidden = true;
  try {
    const res = await fetch(
      "/api/report",
      method === "POST" ? { method, body } : {},
    );
    const json = await res.json();
    if (!res.ok) throw new Error(json.error || `HTTP ${res.status}`);
    render(json);
  } catch (e) {
    errorEl.textContent = e.message;
    errorEl.hidden = false;
  }
}

form.addEventListener("submit", (e) => {
  e.preventDefault();
  load("POST", new URLSearchParams(new FormData(form)).toString());
});

load("GET");
