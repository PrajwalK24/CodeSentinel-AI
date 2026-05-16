<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Report | CodeSentinel</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet"></head>
<body><jsp:include page="layout/header.jsp"/><div class="app-frame"><jsp:include page="layout/sidebar.jsp"/>
<main class="content fade-in">
    <div class="page-head"><div><h1>${report.submission.title}</h1><p><span class="badge badge-soft">${report.submission.language}</span> analyzed on ${report.createdAt}</p></div><span class="risk ${report.riskLevel}">${report.riskLevel}</span></div>
    <section class="stat-grid eight">
        <div class="stat-card"><span>Total Bugs</span><strong>${report.totalBugs}</strong></div><div class="stat-card"><span>Critical</span><strong class="text-danger">${report.criticalCount}</strong></div><div class="stat-card"><span>Major</span><strong class="text-warning">${report.majorCount}</strong></div><div class="stat-card"><span>Minor</span><strong>${report.minorCount}</strong></div><div class="stat-card"><span>Cyclomatic</span><strong>${report.complexityScore}</strong></div><div class="stat-card"><span>Time</span><strong>${timeComplexity}</strong></div><div class="stat-card"><span>Space</span><strong>${spaceComplexity}</strong></div><div class="stat-card"><span>Risk</span><strong>${report.riskLevel}</strong></div>
    </section>
    <section class="panel"><div class="panel-head"><h2>Line-by-Line Issues</h2><button class="btn btn-outline-light btn-sm" onclick="window.print()">Download Report</button></div>
        <div class="table-responsive"><table class="table sentinel-table"><thead><tr><th>Line</th><th>Code</th><th>Issue</th><th>Why it is real</th><th>Impact</th><th>Correct Fix</th><th>Severity</th><th>Confidence</th></tr></thead><tbody>
        <c:forEach items="${report.issues}" var="i"><tr class="issue-row ${i.severity}"><td>${i.lineNumber}</td><td><code>${i.codeSnippet}</code></td><td>${i.issueType}</td><td>${i.description}</td><td>${i.impact}</td><td>${i.suggestion}</td><td><span class="severity ${i.severity}">${i.displaySeverity}</span></td><td>${i.confidence}</td></tr></c:forEach>
        <c:if test="${empty report.issues}"><tr><td colspan="8" class="text-center muted">No real runtime, correctness, security, or complexity issues detected.</td></tr></c:if>
        </tbody></table></div>
    </section>
    <section class="panel"><h2>Code Viewer</h2><pre class="code-viewer"><c:forEach items="${codeLines}" var="line" varStatus="st"><code class="${issueLines.contains(st.count) ? 'flagged' : ''}"><span>${st.count}</span>${line}</code></c:forEach></pre></section>
    <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/dashboard">Back to Dashboard</a>
</main></div><script src="${pageContext.request.contextPath}/static/js/app.js"></script></body></html>
