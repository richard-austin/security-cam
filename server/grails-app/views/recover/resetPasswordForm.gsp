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
    <div class="col-sm-9 col-md-7 col-lg-5 mx-auto">
        <div class="card card-signin my-5">
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
                        <input type="password" onkeyup="passwordKeyup()" class="form-control" name="${passwordParameter ?: 'newPassword'}" id="password" autocapitalize="none"/>
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm New Password</label>
                        <input type="password" onkeyup="confirmPasswordKeyUp()" class="form-control" name="${confirmPasswordParameter ?: 'confirmNewPassword'}" id="confirmPassword"/>
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
    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/;
    let password, confirmPassword, submit;

    // Initial setup function
    document.addEventListener("DOMContentLoaded", function(ignore) {
        document.forms['updatePasswordForm'].elements['password'].focus();
        password = document.getElementById('password');
        confirmPassword = document.getElementById('confirmPassword');
        submit = document.getElementById('submit');
        passwordKeyup();
        confirmPasswordKeyUp();
    });

    // Check password is valid, set password field background red and disable submit button if not
    function passwordKeyup() {
        if(!passwordOk()) {
            password.style= 'background-color: #faa';
            submit.disabled = true;
        }
        else {
            password.style = 'background-color: #fff';
            if(confirmPasswordOk())
                submit.disabled = false;
        }
        confirmPasswordKeyUp();
    }

    // Check confirmPassword matches password, set confirmPassword field background red and disable submit button if not
    // Also confirm password is invalid if password is invalid.
    function confirmPasswordKeyUp() {
        if(!confirmPasswordOk()) {
            confirmPassword.style =  'background-color: #faa';
            submit.disabled = true;
        }
        else {
            confirmPassword.style = 'background-color: #fff';
            submit.disabled = false;
        }
    }

    // Return true if password is OK
    function passwordOk() {
        let password = document.getElementById('password')
        return passwordRegex.test(password.value);
    }

    // Return tru if confirmPassword matches password.
    function confirmPasswordOk() {
        return password.value === confirmPassword.value && passwordOk();
    }
</script>
</body>
</html>
