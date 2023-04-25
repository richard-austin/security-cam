<%--
  Created by IntelliJ IDEA.
  User: richard
  Date: 25/04/2023
  Time: 17:38
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title></title>
</head>

<body>
    <button onclick="sendResetPasswordLink('richard.david.austin@gmail.com', 'localhost:8080')">Click Me</button>
<script>
    sendResetPasswordLink = function (p1, p2) {
        console.log(p1,p2);
        window.location.href = "${createLink(action:'sendResetPasswordLink', controller:'recover')}"+"?email="+p1+"&clientUri="+p2;
    }
</script>
</body>
</html>
