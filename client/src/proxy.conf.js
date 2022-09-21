const PROXY_CONFIG = [
  {  context: [
      "/application",
      "/assets",
      "/cam",
      "/motion",
      "/onvif",
      "/user",
      "/utils",
      "/recording",
      "/wifiUtils",
      "/cloudProxy",
      "/dc"
    ],
    target: "http://localhost:8080/",
    changeOrigin: false,
    secure: false
  }
]

module.exports = PROXY_CONFIG;
