const fs = require('fs')
const path = require('path')
const { marked } = require('marked')

const docsDirectory = __dirname
const outputDirectory = path.join(docsDirectory, 'public')
const markdown = fs.readFileSync(
  path.join(docsDirectory, 'ja/privacy_policy.md'),
  'utf-8',
)
const body = marked.parse(markdown)
const html = `<!DOCTYPE HTML>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>PlayInNotification プライバシーポリシー</title>
  <link href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css" rel="stylesheet" type="text/css" media="all"/>
  <style>
    html,
    body {
      height: 100%;
      width: 100%;
      margin: 0;
      padding: 0;
      left: 0;
      top: 0;
      font-size: 100%;
    }

    .main {
      padding: 32px;
    }
  </style>
</head>
<body>
<div class="container">
<div class="markdown-body main">
${body}</div>
</div>
</body>
</html>
`

fs.mkdirSync(outputDirectory, { recursive: true })
fs.writeFileSync(path.join(outputDirectory, 'privacy_policy.html'), html)
console.log('Generated privacy_policy.html from ja/privacy_policy.md')
