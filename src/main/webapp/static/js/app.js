document.addEventListener("DOMContentLoaded", () => {
    const toggle = document.querySelector(".sidebar-toggle");
    const sidebar = document.querySelector(".sidebar");
    if (toggle && sidebar) {
        toggle.addEventListener("click", () => sidebar.classList.toggle("open"));
    }

    const password = document.getElementById("password");
    const strength = document.querySelector(".strength span");
    if (password && strength) {
        password.addEventListener("input", () => {
            const v = password.value;
            let score = 0;
            if (v.length >= 8) score++;
            if (/[A-Z]/.test(v)) score++;
            if (/[0-9]/.test(v)) score++;
            if (/[^A-Za-z0-9]/.test(v)) score++;
            strength.style.width = (score * 25) + "%";
            strength.style.background = score < 2 ? "#ff6b6b" : score < 4 ? "#f0a500" : "#00ff9d";
        });
    }

    document.querySelectorAll(".analysis-form").forEach(form => {
        form.addEventListener("submit", () => {
            form.classList.add("running");
            const button = form.querySelector(".run-btn");
            if (button) {
                button.disabled = true;
                button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Analyzing';
            }
        });
    });

    const fileInput = document.querySelector(".drop-zone input");
    const dropZone = document.querySelector(".drop-zone");
    if (fileInput && dropZone) {
        fileInput.addEventListener("change", () => {
            const name = fileInput.files[0] ? fileInput.files[0].name : "Drop source file here";
            dropZone.querySelector("strong").textContent = name;
        });
    }

    const tableSearch = document.getElementById("tableSearch");
    if (tableSearch) {
        tableSearch.addEventListener("input", () => {
            const q = tableSearch.value.toLowerCase();
            document.querySelectorAll(".searchable tbody tr").forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? "" : "none";
            });
        });
    }

    const historySearch = document.getElementById("historySearch");
    if (historySearch) {
        historySearch.addEventListener("input", () => {
            const q = historySearch.value.toLowerCase();
            document.querySelectorAll(".history-table tbody tr").forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? "" : "none";
            });
        });
    }

    const historyRiskFilter = document.getElementById("historyRiskFilter");
    if (historyRiskFilter) {
        historyRiskFilter.addEventListener("change", () => {
            document.querySelectorAll(".history-table tbody tr").forEach(row => {
                row.style.display = !historyRiskFilter.value || row.dataset.risk === historyRiskFilter.value ? "" : "none";
            });
        });
    }

    const riskFilter = document.getElementById("riskFilter");
    if (riskFilter) {
        riskFilter.addEventListener("change", () => {
            filterMonitorRows();
        });
    }

    const monitorSearch = document.getElementById("monitorSearch");
    if (monitorSearch) {
        monitorSearch.addEventListener("input", filterMonitorRows);
    }

    makeDoughnut("severityChart", ["#ff6b6b", "#f0a500", "#58a6ff", "#8b949e"]);
    makeBar("languageChart", "#58a6ff");
    makeLine("activityChart", "#00ff9d");
    initLiveAnalysis();
});

function initLiveAnalysis() {
    const editor = document.getElementById("liveCodeEditor");
    const panel = document.getElementById("liveAnalysis");
    if (!editor || !panel) return;

    const endpoint = panel.dataset.endpoint;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    let timer;
    let controller;

    editor.addEventListener("input", () => {
        window.clearTimeout(timer);
        timer = window.setTimeout(() => runLiveAnalysis(editor.value, endpoint, csrfToken, csrfHeader, controllerRef => {
            controller = controllerRef;
        }, controller), 450);
    });
}

function runLiveAnalysis(sourceCode, endpoint, csrfToken, csrfHeader, setController, previousController) {
    if (!sourceCode.trim()) {
        renderLiveAnalysis({ complexityScore: 1, timeComplexity: "O(1)", spaceComplexity: "O(1)", riskLevel: "LOW", issues: [] });
        return;
    }
    if (previousController) previousController.abort();
    const controller = new AbortController();
    setController(controller);
    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    fetch(endpoint, {
        method: "POST",
        headers,
        body: JSON.stringify({ sourceCode }),
        signal: controller.signal
    })
        .then(response => response.ok ? response.json() : Promise.reject(new Error("Live analysis failed")))
        .then(renderLiveAnalysis)
        .catch(error => {
            if (error.name !== "AbortError") {
                renderLiveError();
            }
        });
}

function renderLiveAnalysis(result) {
    const risk = document.getElementById("liveRisk");
    const complexity = document.getElementById("liveComplexity");
    const time = document.getElementById("liveTime");
    const space = document.getElementById("liveSpace");
    const list = document.getElementById("liveIssues");
    if (!risk || !complexity || !time || !space || !list) return;

    risk.textContent = result.riskLevel || "LOW";
    complexity.textContent = result.complexityScore ?? 1;
    time.textContent = result.timeComplexity || "O(1)";
    space.textContent = result.spaceComplexity || "O(1)";
    list.innerHTML = "";

    const issues = result.issues || [];
    if (!issues.length) {
        const item = document.createElement("li");
        item.textContent = "No live issues detected.";
        list.appendChild(item);
        return;
    }

    issues.slice(0, 6).forEach(issue => {
        const item = document.createElement("li");
        const title = document.createElement("strong");
        const detail = document.createElement("span");
        title.textContent = `L${issue.lineNumber} ${issue.issueType}`;
        title.className = issue.severity || "MINOR";
        detail.textContent = issue.suggestion || issue.description || "";
        item.append(title, detail);
        list.appendChild(item);
    });
}

function renderLiveError() {
    const list = document.getElementById("liveIssues");
    if (!list) return;
    list.innerHTML = "<li>Live analysis is temporarily unavailable.</li>";
}

function filterMonitorRows() {
    const risk = document.getElementById("riskFilter")?.value || "";
    const query = (document.getElementById("monitorSearch")?.value || "").toLowerCase();
    document.querySelectorAll(".monitor-table tbody tr").forEach(row => {
        const riskOk = !risk || row.dataset.risk === risk;
        const textOk = !query || row.textContent.toLowerCase().includes(query);
        row.style.display = riskOk && textOk ? "" : "none";
    });
}

function labels(canvas) {
    return (canvas.dataset.labels || "").split(",").filter(Boolean);
}

function values(canvas) {
    return (canvas.dataset.values || "").split(",").filter(Boolean).map(v => Number(v));
}

function chartOptions() {
    return {
        responsive: true,
        plugins: { legend: { labels: { color: "#e6edf3" } } },
        scales: {
            x: { ticks: { color: "#8b949e" }, grid: { color: "#30363d" } },
            y: { ticks: { color: "#8b949e" }, grid: { color: "#30363d" }, beginAtZero: true }
        }
    };
}

function makeDoughnut(id, colors) {
    const canvas = document.getElementById(id);
    if (!canvas || !window.Chart) return;
    new Chart(canvas, { type: "doughnut", data: { labels: labels(canvas), datasets: [{ data: values(canvas), backgroundColor: colors }] }, options: { plugins: { legend: { labels: { color: "#e6edf3" } } } } });
}

function makeBar(id, color) {
    const canvas = document.getElementById(id);
    if (!canvas || !window.Chart) return;
    new Chart(canvas, { type: "bar", data: { labels: labels(canvas), datasets: [{ label: "Submissions", data: values(canvas), backgroundColor: color }] }, options: chartOptions() });
}

function makeLine(id, color) {
    const canvas = document.getElementById(id);
    if (!canvas || !window.Chart) return;
    new Chart(canvas, { type: "line", data: { labels: labels(canvas), datasets: [{ label: "Reports", data: values(canvas), borderColor: color, backgroundColor: "rgba(0,255,157,.12)", tension: .35, fill: true }] }, options: chartOptions() });
}
