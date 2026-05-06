<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Register | CodeSentinel</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/style.css" rel="stylesheet">
</head>
<body class="auth-page">
<main class="auth-shell fade-in">
    <form class="auth-card wide" method="post" action="${pageContext.request.contextPath}/register">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <h1 class="logo-title">Join CodeSentinel<span class="cursor"></span></h1>
        <c:if test="${not empty error}"><div class="alert alert-danger py-2">${error}</div></c:if>
        <div class="row g-3">
            <div class="col-md-6"><label>Full Name</label><input class="form-control dark-input" name="fullName" required></div>
            <div class="col-md-6"><label>Username</label><input class="form-control dark-input" name="username" required></div>
            <div class="col-12"><label>Email</label><input class="form-control dark-input" type="email" name="email" required></div>
            <div class="col-md-6"><label>Password</label><input id="password" class="form-control dark-input" type="password" name="password" required minlength="8"></div>
            <div class="col-md-6"><label>Confirm Password</label><input class="form-control dark-input" type="password" name="confirmPassword" required></div>
        </div>
        <div class="strength mt-3"><span></span></div>
        <div class="role-pill mt-3">Role: Developer</div>
        <button class="btn btn-sentinel w-100 mt-3" type="submit">Create Account</button>
        <p class="auth-switch">Already registered? <a href="${pageContext.request.contextPath}/login">Sign in</a></p>
    </form>
</main>
<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>
