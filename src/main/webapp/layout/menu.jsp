<%-- 
    Document   : menu
    Created on : Jul 12, 2009, 9:28:58 PM
    Author     : tim
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/tlds/js" prefix="js" %>
<%@ taglib uri="http://frcdb.net/taglibs/utils" prefix="utils" %>

<ul>
	<li>
		<h2>Navigation</h2>
		<ul>
			<li>
				<a href="/">
					Home
				</a>
			</li>
			<li>
				<a href="/search">
					Search
				</a>
			</li>
			<li>
				<a href="/about">
					About
				</a>
			</li>
			<li>
				<a href="http://code.google.com/p/frcdb/">
					Dev Site
				</a>
			</li>
		</ul>
	</li>
	<li>
		<c:choose>
			<c:when test="${empty user}">
				<h2>Login</h2>
				Login with a <utils:login text="Google Account"/>.
			</c:when>
			<c:otherwise>
				<h2>User</h2>
				Welcome, ${user.nickname}
				<utils:logout text="Logout?"/>
				<c:if test="${admin}">
					<br>You are an admin!
					<br>View the <a href="/admin">admin panel</a>.
				</c:if>
			</c:otherwise>
		</c:choose>
	</li>
</ul>
