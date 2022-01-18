const PROXY_CONFIG = [
  {
    context: [
      "/application",
      "/assets",
      "/cam",
      "/CloudProxy",
      "/motion",
      "/onvif",
      "/user",
      "/utils",
      "/recording",
      "/live",
      "/login"
    ],
    ws: true,
    target: "http://localhost:8080",
    secure: false
  }
]

module.exports = PROXY_CONFIG;
