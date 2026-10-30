const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('xapi', {
  fetchText: (url) => ipcRenderer.invoke('fetch-text', url),
  openExternal: (url) => ipcRenderer.invoke('open-external', url),
  version: () => ipcRenderer.invoke('app-version')
});
