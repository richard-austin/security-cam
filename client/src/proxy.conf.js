const PROXY_CONFIG = [
  {  context: [
      "/application/",
      "/assets/",
      "/cam/",
      "/motion/",
      "/onvif/",
      "/user/",
      "/utils/",
      "/recording/",
      "/wifiUtils/",
      "/cloudProxy/",
      "/ptz/",
      "/stomp/",
      "/audio/",
      "/login/",
      "/dc/",
      "/cua/",
      "/stomp",
      "/error"
    ],
    target: "http://localhost:8080/",
    ws: true,
    changeOrigin: false,
    secure: false
  }
]

module.exports = PROXY_CONFIG;
