const form = document.getElementById("book");
const reportEl = document.getElementById("report");
const errorEl = document.getElementById("error");
const ageEl = document.getElementById("age");
const reqsEl = document.getElementById("reqs");

// Fields the form displays as a percentage (e.g. "10" for 10%) but the API takes as a decimal
// fraction (0.10) -- see workspace-config/CLAUDE.md for why only these two, not every rate field.
const PERCENT_FIELDS = ["riskFreeRate", "dividendYield"];

let requestCount = 0;
let lastRecomputeAt = 0;

const num = (n, dp = 2) =>
  n.toLocaleString(undefined, {
    minimumFractionDigits: dp,
    maximumFractionDigits: dp,
  });

const signed = (n) => `<span class="${n < 0 ? "neg" : "pos"}">${num(n)}</span>`;

function block(title, sub, body) {
  const subHtml = sub ? `<span class="sub">${sub}</span>` : "";
  return `<div class="report-block"><h3>${title}${subHtml}</h3>${body}</div>`;
}

function rows(pairs) {
  const body = pairs
    .map(([k, v]) => `<tr><td>${k}</td><td>${v}</td></tr>`)
    .join("");
  return `<table class="risk"><tbody>${body}</tbody></table>`;
}

function render(r) {
  const g = r.greeks;
  const valuation = block(
    "Valuation",
    null,
    `<div class="valuation">${num(r.valuation)}</div>`,
  );

  const greeks = block(
    "Greeks",
    null,
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
  const varBlock = block(
    "Value at Risk / ES",
    `${pct}%`,
    `<table class="risk">
       <thead><tr><th>method</th><th>VaR</th><th>ES</th></tr></thead>
       <tbody>
         <tr><td>parametric</td><td>${num(v.parametric.valueAtRisk)}</td><td>${num(v.parametric.expectedShortfall)}</td></tr>
         <tr><td>historical</td><td>${num(v.historical.valueAtRisk)}</td><td>${num(v.historical.expectedShortfall)}</td></tr>
       </tbody>
     </table>`,
  );

  const blocks = [valuation, greeks, varBlock];
  if (r.pnl) {
    const p = r.pnl;
    blocks.push(
      block(
        "PnL Explain",
        null,
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
  reportEl.innerHTML = blocks.join("");
}

function touchStatus() {
  requestCount += 1;
  lastRecomputeAt = Date.now();
  reqsEl.textContent = requestCount;
  ageEl.textContent = "just now";
}

setInterval(() => {
  if (!lastRecomputeAt) return;
  const age = (Date.now() - lastRecomputeAt) / 1000;
  ageEl.textContent = age < 1.5 ? "just now" : `${Math.round(age)}s ago`;
}, 1000);

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
    touchStatus();
  } catch (e) {
    errorEl.textContent = e.message;
    errorEl.hidden = false;
  }
}

form.addEventListener("submit", (e) => {
  e.preventDefault();
  const data = new FormData(form);
  for (const field of PERCENT_FIELDS) {
    const raw = data.get(field);
    if (raw !== null && raw !== "") data.set(field, String(Number(raw) / 100));
  }
  load("POST", new URLSearchParams(data).toString());
});

load("GET");
