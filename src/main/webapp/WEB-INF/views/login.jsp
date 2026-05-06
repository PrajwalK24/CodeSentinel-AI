<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login | CodeSentinel</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
</head>
<body class="auth-page">
<main class="auth-shell fade-in">
    <form class="auth-card" method="post" action="${pageContext.request.contextPath}/login">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <h1 class="logo-title">CodeSentinel<span class="cursor"></span></h1>
        <p class="muted">AI-assisted code review with rule-based bug intelligence.</p>
        <c:if test="${param.error == 'true'}"><div class="alert alert-danger py-2">Invalid credentials or inactive account.</div></c:if>
        <c:if test="${param.registered == 'true'}"><div class="alert alert-success py-2">Registration complete. Sign in to continue.</div></c:if>
        <c:if test="${param.logout == 'true'}"><div class="alert alert-info py-2">You have been signed out.</div></c:if>
        <label>Email</label>
        <input class="form-control dark-input" type="email" name="email" required autofocus>
        <label>Password</label>
        <input class="form-control dark-input" type="password" name="password" required>
        <button class="btn btn-sentinel w-100 mt-3" type="submit">Sign In</button>
        <p class="auth-switch">New here? <a href="${pageContext.request.contextPath}/register">Create developer account</a></p>
    </form>
</main>
</body>
</html>
