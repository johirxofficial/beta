const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const http = require('http');
const https = require('https');

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
  '(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

function fetchText(url, redirects) {
  const depth = redirects || 0;
  return new Promise((resolve, reject) => {
    if (depth > 6) {
      reject(new Error('Too many redirects'));
      return;
    }
    const lib = url.toLowerCase().startsWith('https') ? https : http;
    const request = lib.get(
      url,
      {
        headers: {
          'User-Agent': USER_AGENT,
          'Accept': '*/*',
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache'
        }
      },
      (response) => {
        const status = response.statusCode || 0;
        if (status >= 300 && status < 400 && response.headers.location) {
          const next = new URL(response.headers.location, url).toString();
          response.resume();
          fetchText(next, depth + 1).then(resolve, reject);
          return;
        }
        if (status !== 200) {
          response.resume();
          reject(new Error('HTTP ' + status));
          return;
        }
        const chunks = [];
        response.on('data', (chunk) => chunks.push(chunk));
        response.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
        response.on('error', reject);
      }
    );

    request.setTimeout(25000, () => request.destroy(new Error('Timeout')));
    request.on('error', reject);
  });
}

ipcMain.handle('fetch-text', async (_event, url) => {
  if (typeof url !== 'string' || !/^https?:\/\//i.test(url)) {
    throw new Error('Invalid URL');
  }
  return fetchText(url);
});

ipcMain.handle('open-external', async (_event, url) => {
  if (typeof url !== 'string' || !/^https?:\/\//i.test(url)) {
    return false;
  }
  await shell.openExternal(url);
  return true;
});

ipcMain.handle('app-version', () => app.getVersion());

function createWindow() {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 900,
    minHeight: 560,
    backgroundColor: '#070B10',
    title: 'X live',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      // IPTV streams rarely send CORS headers, so the renderer fetches them directly.
      webSecurity: false,
      allowRunningInsecureContent: true
    }
  });

  window.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  window.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
