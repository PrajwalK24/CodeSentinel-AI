<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>History | CodeSentinel</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet"></head>
<body><jsp:include page="layout/header.jsp"/><div class="app-frame"><jsp:include page="layout/sidebar.jsp"/>
<main class="content fade-in"><div class="page-head"><div><h1>Analysis History</h1><p>Your complete review timeline.</p></div></div>
<section class="panel"><div class="table-responsive"><table class="table sentinel-table"><thead><tr><th>Title</th><th>Language</th><th>Bugs</th><th>Risk</th><th>Date</th><th>Action</th></tr></thead><tbody>
<c:forEach items="${reports}" var="r"><tr><td>${r.submission.title}</td><td>${r.submission.language}</td><td>${r.totalBugs}</td><td><span class="risk ${r.riskLevel}">${r.riskLevel}</span></td><td>${r.createdAt}</td><td><a class="link-info" href="${pageContext.request.contextPath}/report/${r.id}">View Report</a></td></tr></c:forEach>
<c:if test="${empty reports}"><tr><td colspan="6" class="text-center muted">No reports yet.</td></tr></c:if>
</tbody></table></div></section></main></div><script src="${pageContext.request.contextPath}/static/js/app.js"></script></body></html>
