<%--
Custom page for all HTTP error codes.
--%>
<%@ page session="false" isErrorPage="true" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="exception" value="${requestScope['javax.servlet.error.exception']}"/>
<!DOCTYPE html>
<html lang="en">
<body>
<h2><%=response.getStatus() %></h2>
<br/>
<c:if test="${exception != null}">
Message: <c:out value="${exception.getMessage()}"/><br/>
</c:if>
</body>
</html>
