const PROXY_CONFIG = [
  {  context: [
      "/user",
    ],
    target: "http://localhost:8080/",
    changeOrigin: false,
    secure: false
  }
]

module.exports = PROXY_CONFIG;
