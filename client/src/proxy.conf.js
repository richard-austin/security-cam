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
      "/login"
    ],
    target: "http://localhost:8080",
    secure: false
  },
  {
    context: [
      "/live"
    ],
    ws: true,
    target: "http://localhost:8009",
    secure: false
  },
  {
    context: [
      "/recording"
    ],
    target: "http://localhost:8080/recording",
    secure: false
  }]

module.exports = PROXY_CONFIG;
