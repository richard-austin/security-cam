<%--
  Created by IntelliJ IDEA.
  User: richard
  Date: 24/04/2023
  Time: 14:30
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title><g:meta name="info.app.applicationName"/></title>
    <link rel="stylesheet" href="<g:resource dir='assets/stylesheets' file='bootstrap.min.css'/>" />
    <script src="<g:resource dir='assets/javascripts' file='bootstrap.js' />"></script>
</head>

<body>
<div class="row">
    <div class="mx-auto">
        <div class="card card-signin my-3">
            <div class="card-body">
                <h5 class="card-title text-center">Please Enter New Password</h5>
                <g:if test='${flash.message}'>
                    <div class="alert alert-success" role="alert">${flash.message}</div>
                </g:if>
                <g:if test='${flash.error}'>
                    <div class="alert alert-danger" role="alert">${flash.error}</div>
                </g:if>
                <form action="${postUrl ?: '/recover/resetPassword'}" method="POST" id="updatePasswordForm" autocomplete="off">
                    <div class="form-group">
                        <label for="password">New Password</label>
                        <input type="password" class="form-control" name="${passwordParameter ?: 'newPassword'}" id="password" autocapitalize="none"/>
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm New Password</label>
                        <input type="password" class="form-control" name="${confirmPasswordParameter ?: 'confirmNewPassword'}" id="confirmPassword"/>
                    </div>
                    <div style="visibility: hidden; position: absolute">
                        <label for="resetKey"></label>
                        <input style="visibility: hidden; position: absolute" name="${resetKeyParameter ?: 'resetKey'}" value="${params.key}" id="resetKey">
                    </div>
                    <button id="submit" class="btn btn-lg btn-primary btn-block" type="submit">Submit</button>
                </form>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    document.addEventListener("DOMContentLoaded", function(ignore) {
 //       document.forms['updatePasswordForm'].elements['password'].focus();
    });
</script>
</body>
</html>
