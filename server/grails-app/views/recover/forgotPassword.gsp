<%--
  Created by IntelliJ IDEA.
  User: richard
  Date: 25/04/2023
  Time: 17:38
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title><g:meta name="Request Reset Password Link"/></title>
    <link rel="stylesheet" href="<g:resource dir='assets/stylesheets' file='bootstrap.min.css'/>"/>
    <script src="<g:resource dir='assets/javascripts' file='bootstrap.js'/>"></script>
    <style>
        button.continue-btn {
            margin-left: auto;
            margin-right: 16px;
        }
        button.back-to-login-btn {
            margin-left: 16px;
        }
    </style>
</head>

<body>

<div class="row">
    <div class="col-sm-9 col-md-7 col-lg-5 mx-auto">
        <div class="card my-5">
            <div class="card-body">
                <h5 class="card-title text-center">Request Reset Password Link</h5>
                <g:if test='${flash.message}'>
                    <div class="alert alert-success" role="alert">${flash.message}</div>
                </g:if>
                <g:if test='${flash.error}'>
                    <div class="alert alert-danger" role="alert">${flash.error}</div>
                </g:if>
                <form class="form-signin" action="${postUrl ?: '/recover/sendResetPasswordLink'}" method="POST"
                      id="requestResetLink"
                      autocomplete="off">
                    <div class="form-group">
                        <label for="email">Your Email Address</label>
                        <input type="text" class="form-control" name="${emailParameter ?: 'email'}" id="email"
                               onkeyup="keyup(this)"
                               autocapitalize="none"/>
                    </div>
                    <input type="text" style="visibility: hidden" name="${clientUriParameter ?: 'clientUri'}" id="clientUri">
                    <div class="row flex-sm-nowrap flex-md-nowrap flex-lg-nowrap">
                        <button id="go-back" onclick="backToLogin()" class="back-to-login-btn btn btn-lg btn-primary btn-outline-info"
                                type="button">Back to Log in</button>
                        <button disabled id="submit" class="continue-btn btn btn-lg btn-primary btn-dark"
                                type="submit">Continue</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<script>
    let continueEnabled = false;
    const emailRegex = /^([a-zA-Z0-9_\-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/;
    (function setUpClientUri() {
        let clientUriInput = document.getElementById('clientUri');
        clientUriInput.value = window.location.protocol+'//'+window.location.hostname+':'+window.location.port;
        let email = document.getElementById('email');
        email.addEventListener('paste', () => {
            let button = document.getElementById('submit')
            setTimeout(() => {
                continueEnabled = emailRegex.test(email.value);
                button.disabled = !continueEnabled;
            }, 2);
        })
      })();

    function keyup(input) {
        let button = document.getElementById('submit')
        continueEnabled = emailRegex.test(input.value);
        button.disabled = !continueEnabled;
    }

    function backToLogin() {
        window.location.href = '/login/auth'
    }
</script>
</body>
</html>
