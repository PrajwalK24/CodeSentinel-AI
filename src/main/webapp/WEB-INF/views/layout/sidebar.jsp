<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<aside class="sidebar">
    <a class="${activePage == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="${activePage == 'analyze' ? 'active' : ''}" href="${pageContext.request.contextPath}/analyze">Analyze Code</a>
    <a class="${activePage == 'history' ? 'active' : ''}" href="${pageContext.request.contextPath}/history">History</a>
    <a href="${pageContext.request.contextPath}/dashboard">Profile</a>
</aside>
