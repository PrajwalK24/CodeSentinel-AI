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
            document.querySelectorAll(".monitor-table tbody tr").forEach(row => {
                row.style.display = !riskFilter.value || row.dataset.risk === riskFilter.value ? "" : "none";
            });
        });
    }

    makeDoughnut("severityChart", ["#ff6b6b", "#f0a500", "#58a6ff", "#8b949e"]);
    makeBar("languageChart", "#58a6ff");
    makeLine("activityChart", "#00ff9d");
});

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
