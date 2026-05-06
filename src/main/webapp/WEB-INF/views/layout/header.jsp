<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav class="topbar">
    <button class="icon-btn sidebar-toggle" type="button" aria-label="Toggle navigation">☰</button>
    <a class="brand" href="${pageContext.request.contextPath}/dashboard"><span>Code</span>Sentinel<span class="cursor"></span></a>
    <div class="topbar-actions">
        <span class="user-chip">${not empty user.fullName ? user.fullName : pageContext.request.userPrincipal.name}</span>
        <a class="btn btn-outline-light btn-sm" href="${pageContext.request.contextPath}/logout">Logout</a>
    </div>
</nav>
