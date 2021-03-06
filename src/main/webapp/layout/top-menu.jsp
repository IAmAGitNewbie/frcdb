<%-- 
    Document   : top-menu
    Created on : Jul 25, 2010, 1:23:00 AM
    Author     : tim
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://frcdb.net/taglibs/utils" prefix="utils" %>

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
	<c:choose>
		<c:when test="${empty user}">
			<li>
				<utils:login/>
			</li>
		</c:when>
		<c:otherwise>
			<li>
				<utils:logout/>
			</li>
		</c:otherwise>
	</c:choose>
	<li>
		<a href="/about">
			About
		</a>
	</li>
	<li>
		<a href="http://code.google.com/p/frcdb/">
			Dev
		</a>
	</li>
</ul>

<div id="searchbox-wrapper">
	<div id="searchbox">
		<script type="text/javascript">
			(function() {
				var cx = '015151542149676618601:yp-wr_3klpm';
				var gcse = document.createElement('script'); gcse.type = 'text/javascript'; gcse.async = true;
				gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
					'//www.google.com/cse/cse.js?cx=' + cx;
				var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(gcse, s);
			})();
		</script>

		<div class="gcse-searchbox-only"></div>
	</div>
</div>