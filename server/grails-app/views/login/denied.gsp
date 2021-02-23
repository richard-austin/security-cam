<%--
  Created by IntelliJ IDEA.
  User: austinr
  Date: 21/02/2021
  Time: 19:07
--%>
<head>
    <title><g:message code="springSecurity.denied.title" /></title>
</head>

<body>
<div class='body'>
    <div class='errors'><g:message code="springSecurity.denied.message" /></div>
    <div class='errors'>
        Your user credentials have been authenticated (that is, you are currently 'logged-in'), but either:-
        <ul>
            <li>you do not have the authority required to access the page requested, or</li>
            <li>
                the requested page does not exist.
            </li>
        </ul>
        <i>(Please check what you have entered into the browser's Location field.)</i>
    </div>
    <div class='errors'>
        Click this link to go to the Security Cam Home-page
        <g:link resource = "index.html">Security Cam Home Page</g:link>
    </div>
</div>
</body>