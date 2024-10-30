package com.securitycam.commands

import com.securitycam.proxies.IGetAccessTokenCommand


class GetAccessTokenCommand implements IGetAccessTokenCommand{
    String host
    int port
}
