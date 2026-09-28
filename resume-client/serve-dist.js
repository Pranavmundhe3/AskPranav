// Minimal static server for the built client (dist/resume-client) with single-page-app fallback.
// Exists because Angular CLI 10's `ng serve` cannot run on Node 20+ (it needs a Node internal that was
// removed), while `ng build` still works. Use with `npm run build:watch` in another terminal.
// No dependencies; not meant for production hosting.
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, 'dist', 'resume-client');
const port = process.env.PORT || 4200;
const types = {
  '.html': 'text/html; charset=utf-8', '.js': 'application/javascript', '.css': 'text/css',
  '.json': 'application/json', '.map': 'application/json', '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png', '.svg': 'image/svg+xml',
  '.woff': 'font/woff', '.woff2': 'font/woff2'
};

http.createServer((req, res) => {
  let urlPath;
  try {
    urlPath = decodeURIComponent(req.url.split('?')[0]);
  } catch (e) {
    res.writeHead(400);
    return res.end();
  }
  let file = path.normalize(path.join(root, urlPath));
  if (!file.startsWith(root)) {
    res.writeHead(403);
    return res.end();
  }
  fs.stat(file, (statErr, stats) => {
    if (statErr || stats.isDirectory()) {
      file = path.join(root, 'index.html'); // unknown path -> let the Angular router handle it
    }
    fs.readFile(file, (readErr, data) => {
      if (readErr) {
        res.writeHead(503, { 'Content-Type': 'text/plain' });
        return res.end('Nothing built yet. Run: npm run build:watch');
      }
      res.writeHead(200, {
        'Content-Type': types[path.extname(file)] || 'application/octet-stream',
        'Cache-Control': 'no-store'
      });
      res.end(data);
    });
  });
}).listen(port, () => console.log('Serving ' + root + ' on http://localhost:' + port));
