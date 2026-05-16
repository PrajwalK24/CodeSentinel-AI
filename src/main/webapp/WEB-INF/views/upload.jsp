<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Analyze Code | CodeSentinel</title><meta name="_csrf" content="${_csrf.token}"><meta name="_csrf_header" content="${_csrf.headerName}"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet"></head>
<body><jsp:include page="layout/header.jsp"/><div class="app-frame"><jsp:include page="layout/sidebar.jsp"/>
<main class="content fade-in">
    <div class="page-head"><div><h1>Analyze Code</h1><p>Upload a file or paste code for automated review.</p></div><span class="time-chip">Estimated: under 2 seconds</span></div>
    <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
    <ul class="nav nav-tabs sentinel-tabs" role="tablist"><li class="nav-item"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#fileTab" type="button">Upload File</button></li><li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#pasteTab" type="button">Paste Code</button></li></ul>
    <div class="tab-content panel tab-panel">
        <div id="fileTab" class="tab-pane fade show active">
            <form class="analysis-form" method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/analyze/file">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input class="form-control dark-input mb-3" name="title" placeholder="Analysis title">
                <select class="form-select dark-input mb-3" name="language"><option>Java</option><option>Python</option><option>JavaScript</option><option>C++</option><option>Text</option></select>
                <label class="drop-zone"><input type="file" name="file" accept=".java,.py,.js,.cpp,.txt" required><strong>Drop source file here</strong><span>.java .py .js .cpp .txt</span></label>
                <button class="btn btn-sentinel mt-3 run-btn" type="submit">Run Analysis</button><div class="loading-bar"></div>
            </form>
        </div>
        <div id="pasteTab" class="tab-pane fade">
            <form class="analysis-form" method="post" action="${pageContext.request.contextPath}/analyze/paste">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input class="form-control dark-input mb-3" name="title" placeholder="Analysis title" required>
                <select class="form-select dark-input mb-3" name="language"><option>Java</option><option>Python</option><option>JavaScript</option><option>C++</option></select>
                <textarea class="code-editor" id="liveCodeEditor" name="sourceCode" spellcheck="false" required placeholder="Paste source code here..."></textarea>
                <div class="live-analysis" id="liveAnalysis" data-endpoint="${pageContext.request.contextPath}/analyze/live">
                    <div class="live-summary">
                        <span class="status on">LIVE</span>
                        <span>Risk <strong id="liveRisk">LOW</strong></span>
                        <span>Complexity <strong id="liveComplexity">1</strong></span>
                        <span>Time <strong id="liveTime">O(1)</strong></span>
                        <span>Space <strong id="liveSpace">O(1)</strong></span>
                    </div>
                    <ul class="live-issues" id="liveIssues"><li>Start typing to see live bug, security, and optimization findings.</li></ul>
                </div>
                <button class="btn btn-sentinel mt-3 run-btn" type="submit">Run Analysis</button><div class="loading-bar"></div>
            </form>
        </div>
    </div>
</main></div><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script><script src="${pageContext.request.contextPath}/static/js/app.js"></script></body></html>
