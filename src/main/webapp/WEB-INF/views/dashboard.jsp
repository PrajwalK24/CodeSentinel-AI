<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard | CodeSentinel</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
</head>
<body>
<jsp:include page="layout/header.jsp"/><div class="app-frame"><jsp:include page="layout/sidebar.jsp"/>
<main class="content fade-in">
    <div class="page-head"><div><h1>Developer Dashboard</h1><p>Recent code quality activity and risk signals.</p></div><a class="btn btn-sentinel" href="${pageContext.request.contextPath}/analyze">Start New Analysis</a></div>
    <section class="stat-grid">
        <div class="stat-card"><span>Total Analyses</span><strong>${totalAnalyses}</strong></div>
        <div class="stat-card"><span>Total Bugs Found</span><strong>${totalBugs}</strong></div>
        <div class="stat-card"><span>Critical Issues</span><strong class="text-danger">${criticalIssues}</strong></div>
        <div class="stat-card"><span>Avg Complexity</span><strong>${avgComplexity}</strong></div>
    </section>
    <section class="chart-grid">
        <div class="panel"><h2>Bug Severity</h2><canvas id="severityChart" data-labels="CRITICAL,MAJOR,MINOR,INFO" data-values="${severityData.CRITICAL},${severityData.MAJOR},${severityData.MINOR},${severityData.INFO}"></canvas></div>
        <div class="panel"><h2>Submissions by Language</h2><canvas id="languageChart" data-labels="${languageLabels}" data-values="${languageValues}"></canvas></div>
    </section>
    <section class="panel"><h2>Recent Analyses</h2>
        <div class="table-responsive"><table class="table sentinel-table"><thead><tr><th>Title</th><th>Language</th><th>Risk</th><th>Date</th><th>Action</th></tr></thead><tbody>
        <c:forEach items="${recentSubmissions}" var="s"><tr><td>${s.title}</td><td><span class="badge badge-soft">${s.language}</span></td><td><span class="risk ${s.report.riskLevel}">${s.report.riskLevel}</span></td><td>${s.submittedAt}</td><td><a class="link-info" href="${pageContext.request.contextPath}/report/${s.report.id}">View Report</a></td></tr></c:forEach>
        <c:if test="${empty recentSubmissions}"><tr><td colspan="5" class="text-center muted">No analyses yet.</td></tr></c:if>
        </tbody></table></div>
    </section>
</main></div>
<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body></html>
