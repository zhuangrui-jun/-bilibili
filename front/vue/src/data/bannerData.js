// Banner 背景数据
// 使用本地资源路径
// 方案1: 如果图片在 public/assets/2023-10-01/ 目录下，使用此路径
// 方案2: 如果使用 Vite 配置了静态资源服务，可以使用相对路径
const getAssetPath = (filename) => {
  // 使用 public 目录下的资源（推荐）
  // 需要将图片复制到 front/vue/public/assets/2023-10-01/ 目录
  return `/assets/2023-10-01/${filename}`
}

export default [
  {
    name: "海上明月 - 兔子",
    data: [
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 0, 0],
        width: 2100,
        height: 197,
        src: getAssetPath("39f5846ccf601e178afe37eef2e9759d38078d56.png@1c.webp"),
        a: 0,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 78, 6],
        width: 201,
        height: 107,
        src: getAssetPath("0a3bf7a4c7c3b8b5b63aae6adf57c5fabeeb0cff.png@1c.webp"),
        a: 0,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["0.7", "0.7"],
        transform: [1, 0, 0, 1, -15, 32.5],
        width: 62,
        height: 23,
        src: getAssetPath("e737e375a9bf412f802feaec18ee52ad517b4db9.png@1c.webp"),
        a: 0.007,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 800, -32],
        width: 755,
        height: 159,
        src: getAssetPath("9539f58accbf88eb810a45ff2cbefbf1c29840c3.png@1c.webp"),
        a: 0.02,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 850, -10],
        width: 1141,
        height: 170,
        src: getAssetPath("b5514fc043485ce31d0c91a398aeb288261256dc.png@1c.webp"),
        a: 0.03,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 225, 30],
        width: 125,
        height: 46,
        src: getAssetPath("9cc0c1c3b87ca0ea32b80c3b08611e794108348b.png@1c.webp"),
        a: 0.034,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, -560, 0],
        width: 660,
        height: 162,
        src: getAssetPath("8512395c322494ef32b0fb40821e8e66fb291b53.png@1c.webp"),
        a: 0.04,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, -660, 24],
        width: 149,
        height: 55,
        src: getAssetPath("e737e375a9bf412f802feaec18ee52ad517b4db9.png@1c.webp"),
        a: 0.05,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 0, 0],
        width: 1465,
        height: 156,
        src: getAssetPath("6e9dd4d7f26b117773661aeed3a357641d519f2a.png@1c.webp"),
        a: 0.018,
        deg: -0.000025,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, -110, 27.5],
        width: 226,
        height: 87,
        src: getAssetPath("f48b2d7535cae1ed97c7bb3c69d29e574cbe30c6.png@1c.webp"),
        a: 0.04,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, -385, 22],
        width: 326,
        height: 109,
        src: getAssetPath("f3a02af183b808d236d0cc7997cbeb8958ef9bb0.png@1c.webp"),
        a: 0.12,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 800, 0],
        width: 582,
        height: 225,
        src: getAssetPath("cd873292b7b3ae5603b546e1ecdf05fecaa955f3.png@1c.webp"),
        a: 0.2,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, -140, 0],
        width: 446,
        height: 141,
        src: getAssetPath("3483ce9cad9fc3dd80f34cb164bcd5eeb1606332.png@1c.webp"),
        a: 0.15,
        blur: 0
      },
      {
        tagName: "img",
        opacity: ["1", "1"],
        transform: [1, 0, 0, 1, 560, 0],
        width: 446,
        height: 141,
        src: getAssetPath("988a546df1cdf5eeefa0a5c319bc6b4bfca7d42d.png@1c.webp"),
        a: 0.24,
        blur: 0
      }
    ]
  }
]
