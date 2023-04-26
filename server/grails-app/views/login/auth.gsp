<html>
<head>
    %{--    <meta name="layout" content="${gspLayout ?: 'main'}"/>--}%
    <title><g:meta name="info.app.applicationName"/></title>
    <link rel="stylesheet" href="<g:resource dir='assets/stylesheets' file='bootstrap.min.css'/>"/>
    <script src="<g:resource dir='assets/javascripts' file='bootstrap.js'/>"></script>
    <style>
    .set-cursor:hover {
        cursor: default;
    }

    div.forgot-password-link {
        padding-bottom: 1rem;
    }
    div.forgot-password-link > a {
        color: darkred;
    }

   .sign-in-button {
        margin-left: auto;
        margin-right: auto;
        /*width: 70%;*/
    }
    </style>
</head>

<body>

<div class="row">
    <div class="col-sm-9 col-md-7 col-lg-5 mx-auto">
        <div class="card card-signin my-5">
            <div class="card-body">
                <h5 class="card-title text-center">Please Login to Security Cam</h5>
                <g:if test='${flash.message}'>
                    <div class="alert alert-danger" role="alert">${flash.message}</div>
                </g:if>
                <form action="${postUrl ?: '/login/authenticate'}" method="POST" id="loginForm"
                      autocomplete="off">
                    <div class="form-group">
                        <label for="username">Username</label>
                        <input type="text" class="form-control" name="${usernameParameter ?: 'username'}" id="username"
                               autocapitalize="none"/>
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" class="form-control" name="${passwordParameter ?: 'password'}"
                               id="password"/>
                        <i class="set-cursor" id="passwordToggler" title="toggle password display"
                           onclick="passwordDisplayToggle()">&#128065;</i>
                    </div>

                    <div class="form-group form-check">
                        <label class="form-check-label">
                            <input type="checkbox" class="form-check-input"
                                   name="${rememberMeParameter ?: 'remember-me'}" id="remember_me"
                                   <g:if test='${hasCookie}'>checked="checked"</g:if>/> Remember me
                        </label>
                    </div>
                    <div class="forgot-password-link">
                        <a href="/recover/forgotPassword">Forgot your password?</a>
                    </div>
                    <button id="submit" class="sign-in-button btn btn-lg btn-primary btn-block text-uppercase"
                            type="submit">Sign in</button>
                </form>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    document.addEventListener("DOMContentLoaded", function () {
        document.forms['loginForm'].elements['username'].focus();
    });

    function passwordDisplayToggle() {
        let toggleEl = document.getElementById("passwordToggler");
        let eyeIcon = '\u{1F441}';
        let xIcon = '\u{2715}';
        let passEl = document.getElementById("password");
        if (passEl.type === "password") {
            toggleEl.innerHTML = xIcon;
            passEl.type = "text";
        } else {
            toggleEl.innerHTML = eyeIcon;
            passEl.type = "password";
        }
    }
</script>
</body>
</html>
