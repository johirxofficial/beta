'use strict';

/* X live — Windows desktop client (Electron renderer). */

const DEFAULT_API = 'http://10.31.253.82/live/api/data.json';
const PUBLIC_API = 'https://raw.githubusercontent.com/johirxofficial/x/main/docs/api/categories.json';
const PROJECT_URL = 'https://github.com/johirxofficial/x';

const GRADIENTS = [
  ['#0E7490', '#0B3C47'],
  ['#1D4ED8', '#0B2C4A'],
  ['#7C3AED', '#2A1152'],
  ['#BE123C', '#3B0A18'],
  ['#047857', '#052E24'],
  ['#B45309', '#3A2205'],
  ['#0F766E', '#062E2B'],
  ['#4338CA', '#141046'],
  ['#0369A1', '#082F49'],
  ['#9D174D', '#3B0720'],
];

const ASPECTS = ['contain', 'cover', 'fill'];
const ASPECT_LABELS = ['FIT', 'ZOOM', 'STRETCH'];

const state = {
  apiUrl: localStorage.getItem('x.apiUrl') || DEFAULT_API,
  categories: [],
  notices: [],
  update: null,
  favorites: [],
  category: null,
  channels: [],
  groups: [],
  activeGroup: null,
  query: '',
  current: null,
  aspect: 0,
  hls: null,
  mpegts: null,
  appVersion: '4.0.0',
};

/* ---------------------------------------------------------------- helpers */

const $ = (id) => document.getElementById(id);

function escapeHtml(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function stripBom(text) {
  return String(text || '').replace(/^\uFEFF/, '');
}

function isStreamUrl(url) {
  return typeof url === 'string' && /^https?:\/\//i.test(url.trim());
}

function hostOf(url) {
  try {
    return new URL(url).host;
  } catch (error) {
    return '';
  }
}

function prettyName(url) {
  try {
    const parsed = new URL(url);
    const parts = parsed.pathname.split('/').filter(Boolean);
    const last = parts.length ? parts[parts.length - 1] : parsed.hostname;
    return decodeURIComponent(last).replace(/\.(m3u8|m3u|ts|mp4|flv)$/i, '').replace(/[_-]+/g, ' ') || parsed.hostname;
  } catch (error) {
    return 'Channel';
  }
}

function letter(name) {
  const trimmed = String(name || '').trim();
  return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
}

function qualityOf(name) {
  const match = /\((\d{3,4}p)\)/i.exec(String(name || ''));
  return match ? match[1] : null;
}

function formatCount(value) {
  const number = Number(value) || 0;
  if (number >= 10000) return Math.round(number / 1000) + 'k';
  if (number >= 1000) return (number / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
  return String(number);
}

function slugify(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function firstString(object, keys) {
  for (const key of keys) {
    if (object && object[key] != null) {
      const value = object[key];
      if (typeof value === 'string' && value.trim()) return value.trim();
      if (typeof value === 'number') return String(value);
    }
  }
  return null;
}

function messageFor(error) {
  const text = String((error && error.message) || error || 'Unknown error');
  if (/invalid url/i.test(text)) return 'The API URL is not valid.';
  if (/timeout/i.test(text)) return 'The server took too long to respond.';
  const http = /HTTP\s+(\d+)/i.exec(text);
  if (http) return 'The server answered with HTTP ' + http[1] + '.';
  if (/ENOTFOUND|ECONNREFUSED|EAI_AGAIN|ETIMEDOUT|network|fetch failed/i.test(text)) {
    return 'The server could not be reached. Check your internet connection or API URL.';
  }
  return text;
}

async function fetchText(url) {
  if (window.xapi && typeof window.xapi.fetchText === 'function') {
    return window.xapi.fetchText(url);
  }
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) throw new Error('HTTP ' + response.status);
  return response.text();
}

function openExternal(url) {
  if (window.xapi && typeof window.xapi.openExternal === 'function') {
    return window.xapi.openExternal(url);
  }
  window.open(url, '_blank', 'noopener');
  return Promise.resolve(true);
}

function normalizeVersion(value) {
  return String(value || '').trim().replace(/^v/i, '');
}

/* --------------------------------------------------------------- parsing */

function collectNotices(data) {
  const raw = data.notices || data.notice || data.announcements || data.messages || data.alerts || data.news;
  if (!Array.isArray(raw)) return [];
  return raw
    .map((item) => {
      if (typeof item === 'string') return item.trim();
      if (item && typeof item === 'object') {
        return firstString(item, ['text', 'message', 'notice', 'title', 'body']) || '';
      }
      return '';
    })
    .filter((item) => item.length > 0);
}

function normalizeChannel(raw) {
  if (typeof raw === 'string') {
    if (!isStreamUrl(raw)) return null;
    const name = prettyName(raw);
    return { name, url: raw.trim(), logo: null, group: null, headers: {}, quality: qualityOf(name) };
  }
  if (!raw || typeof raw !== 'object') return null;
  const url = firstString(raw, ['url', 'stream', 'stream_url', 'streamUrl', 'link', 'file', 'src']);
  if (!isStreamUrl(url)) return null;
  const name = firstString(raw, ['name', 'title', 'channel', 'channel_name', 'tvg_name', 'tvg-name']) || prettyName(url);
  return {
    name,
    url: url.trim(),
    logo: firstString(raw, ['logo', 'tvg_logo', 'tvg-logo', 'icon', 'image']),
    group: firstString(raw, ['group', 'group_title', 'group-title', 'category', 'genre']),
    headers: buildHeaders(raw),
    quality: qualityOf(name),
  };
}

function buildHeaders(raw) {
  const headers = {};
  const referer = firstString(raw, ['referer', 'referrer', 'http_referer', 'http-referrer']);
  const userAgent = firstString(raw, ['user_agent', 'user-agent', 'http_user_agent']);
  const cookie = firstString(raw, ['cookie', 'http_cookie']);
  const origin = firstString(raw, ['origin']);
  if (referer) headers.Referer = referer;
  if (userAgent) headers['User-Agent'] = userAgent;
  if (cookie) headers.Cookie = cookie;
  if (origin) headers.Origin = origin;
  return headers;
}

function normalizeCategory(raw, index) {
  if (!raw || typeof raw !== 'object') return null;
  const name = firstString(raw, ['name', 'title', 'category', 'category_name']) || 'Category ' + (index + 1);
  const playlist = firstString(raw, ['playlist_url', 'playlist', 'm3u', 'm3u_url', 'm3u8', 'source', 'url', 'link', 'file']);
  const inlineList = raw.channels || raw.items;
  const channels = Array.isArray(inlineList)
    ? inlineList.map((item) => normalizeChannel(item)).filter(Boolean)
    : [];
  const hint = Number(raw.channels_count || raw.channel_count || raw.count) || 0;
  return {
    id: firstString(raw, ['id', 'slug', 'key']) || slugify(name) + '_' + index,
    name,
    logo: firstString(raw, ['logo', 'icon', 'image', 'flag', 'thumbnail']),
    playlistUrl: isStreamUrl(playlist) ? playlist.trim() : null,
    channels,
    count: hint > 0 ? hint : channels.length || null,
    type: firstString(raw, ['type', 'kind', 'genre']),
  };
}

function parseCatalog(text) {
  const data = JSON.parse(stripBom(text));
  if (Array.isArray(data)) {
    return {
      categories: data.map((item, index) => normalizeCategory(item, index)).filter(Boolean),
      notices: [],
      update: null,
    };
  }
  const list = Array.isArray(data.categories) ? data.categories : [];
  return {
    categories: list.map((item, index) => normalizeCategory(item, index)).filter(Boolean),
    notices: collectNotices(data),
    update: data.update || data.app_update || data.appUpdate || null,
  };
}

function looksLikeM3u(text) {
  return /^\s*#EXTM3U/im.test(text) || /^\s*#EXTINF/im.test(text);
}

function parseM3u(text) {
  const lines = stripBom(text).split(/\r?\n/);
  const seen = new Set();
  const channels = [];
  let pending = null;
  let headers = {};

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) continue;

    if (/^#EXTINF/i.test(line)) {
      const logo = /tvg-logo="([^"]*)"/i.exec(line);
      const group = /group-title="([^"]*)"/i.exec(line);
      const name = line.substring(line.lastIndexOf(',') + 1).trim();
      pending = { name: name || null, logo: logo ? logo[1] : null, group: group ? group[1] : null };
      headers = {};
      continue;
    }

    if (/^#EXTVLCOPT:/i.test(line)) {
      const option = line.substring('#EXTVLCOPT:'.length);
      const eq = option.indexOf('=');
      if (eq > 0) {
        const key = option.substring(0, eq).trim().toLowerCase();
        const value = option.substring(eq + 1).trim();
        if (key === 'http-referrer' || key === 'http-referer') headers.Referer = value;
        if (key === 'http-user-agent') headers['User-Agent'] = value;
        if (key === 'http-cookie') headers.Cookie = value;
      }
      continue;
    }

    if (line.charAt(0) === '#') continue;
    if (!isStreamUrl(line)) continue;

    const url = line.split('#')[0].trim();
    if (seen.has(url)) {
      pending = null;
      continue;
    }
    seen.add(url);
    const name = (pending && pending.name) || prettyName(url);
    channels.push({
      name,
      url,
      logo: pending && pending.logo ? pending.logo : null,
      group: pending && pending.group ? pending.group : null,
      headers,
      quality: qualityOf(name),
    });
    pending = null;
    headers = {};
  }

  return channels;
}

function parseChannelJson(text) {
  const data = JSON.parse(stripBom(text));
  const list = Array.isArray(data)
    ? data
    : Array.isArray(data.channels)
      ? data.channels
      : Array.isArray(data.items)
        ? data.items
        : [];
  return list.map((item) => normalizeChannel(item)).filter(Boolean);
}

/* ---------------------------------------------------------------- rendering */

function renderTicker() {
  const ticker = $('ticker');
  const track = $('ticker-track');
  if (!state.notices.length) {
    ticker.classList.add('hidden');
    track.innerHTML = '';
    return;
  }
  ticker.classList.remove('hidden');
  const text = state.notices.join('        \u2022        ');
  const span = '<span>' + escapeHtml(text) + '</span>';
  track.innerHTML = span + span;
  track.style.animationDuration = Math.max(24, Math.round(text.length * 0.36)) + 's';
}

function renderHero() {
  let total = 0;
  for (const category of state.categories) {
    total += Number(category.count) || category.channels.length || 0;
  }
  const stats = state.categories.length + ' categories';
  $('hero-stats').textContent = total > 0 ? stats + ' \u00b7 ' + formatCount(total) + ' channels' : stats;
  const host = hostOf(state.apiUrl);
  $('hero-host').textContent = host ? 'Fresh \u00b7 ' + host + ' \u00b7 no cache' : 'Fresh from API \u00b7 no cache';
  $('foot-version').textContent = 'X desktop v' + state.appVersion;
  $('foot-host').textContent = host;
}

function renderCategories() {
  const grid = $('category-grid');
  grid.innerHTML = state.categories
    .map((category, index) => {
      const gradient = GRADIENTS[index % GRADIENTS.length];
      const logo = category.logo
        ? '<img class="flag" src="' + escapeHtml(category.logo) + '" alt="" loading="lazy" onerror="this.remove()">'
        : '<span class="watermark">' + escapeHtml(letter(category.name)) + '</span>';
      const meta = [];
      if (category.count) meta.push(formatCount(category.count) + ' ch');
      if (category.type) meta.push(category.type);
      const metaHtml = meta.map((item) => '<span>' + escapeHtml(item) + '</span>').join('');
      return (
        '<button class="card" data-index="' + index + '" style="background:linear-gradient(135deg,' +
        gradient[0] + ',' + gradient[1] + ')">' +
        logo +
        '<h4>' + escapeHtml(category.name) + '</h4>' +
        '<div class="meta">' + metaHtml + '</div>' +
        '</button>'
      );
    })
    .join('');

  grid.querySelectorAll('.card').forEach((card) => {
    card.addEventListener('click', () => {
      const category = state.categories[Number(card.dataset.index)];
      if (category) openCategory(category);
    });
  });

  $('category-count').textContent = state.categories.length + ' total';
}

function renderFavorites() {
  const block = $('favorites-block');
  const row = $('favorites-row');
  if (!state.favorites.length) {
    block.classList.add('hidden');
    row.innerHTML = '';
    return;
  }
  block.classList.remove('hidden');
  $('favorites-count').textContent = state.favorites.length + ' saved';
  row.innerHTML = state.favorites
    .map((channel, index) => {
      return (
        '<button class="mini" data-fav-index="' + index + '">' +
        '<span class="letter">' + escapeHtml(letter(channel.name)) + '</span>' +
        '<span style="min-width:0">' +
        '<span class="name" style="display:block">' + escapeHtml(channel.name) + '</span>' +
        '<span class="sub" style="display:block">' + escapeHtml(channel.group || channel.quality || 'Live') + '</span>' +
        '</span></button>'
      );
    })
    .join('');

  row.querySelectorAll('.mini').forEach((button) => {
    button.addEventListener('click', () => {
      const channel = state.favorites[Number(button.dataset.favIndex)];
      if (channel) playChannel(channel);
    });
  });
}

function channelRow(channel) {
  const favorite = state.favorites.some((item) => item.url === channel.url);
  const logo = channel.logo
    ? '<img class="logo" src="' + escapeHtml(channel.logo) + '" alt="" loading="lazy" onerror="this.remove()">'
    : '<div class="letter">' + escapeHtml(letter(channel.name)) + '</div>';
  const sub = [channel.group, channel.quality].filter(Boolean).join(' \u00b7 ') || 'Live';
  return (
    '<button class="channel" data-url="' + escapeHtml(channel.url) + '">' +
    logo +
    '<span class="info">' +
    '<span class="title" style="display:block">' + escapeHtml(channel.name) + '</span>' +
    '<span class="sub" style="display:block">' + escapeHtml(sub) + '</span>' +
    '</span>' +
    '<span class="star' + (favorite ? ' on' : '') + '">\u2605</span>' +
    '</button>'
  );
}

function renderGroups() {
  const container = $('groups');
  const counts = new Map();
  for (const channel of state.channels) {
    const group = channel.group || 'Other';
    counts.set(group, (counts.get(group) || 0) + 1);
  }
  const groups = Array.from(counts.entries()).sort((a, b) => b[1] - a[1]);
  container.innerHTML =
    '<button class="chip' + (state.activeGroup ? '' : ' active') + '" data-group="">All (' + state.channels.length + ')</button>' +
    groups
      .map(([group, count]) => {
        return (
          '<button class="chip' + (state.activeGroup === group ? ' active' : '') + '" data-group="' +
          escapeHtml(group) + '">' + escapeHtml(group) + ' (' + count + ')</button>'
        );
      })
      .join('');

  container.querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      state.activeGroup = chip.dataset.group || null;
      container.querySelectorAll('.chip').forEach((item) => item.classList.remove('active'));
      chip.classList.add('active');
      renderChannelList();
    });
  });
}

function renderChannelList() {
  const container = $('channel-list');
  const query = state.query.trim().toLowerCase();
  const filtered = state.channels.filter((channel) => {
    if (state.activeGroup && (channel.group || 'Other') !== state.activeGroup) return false;
    if (!query) return true;
    return (channel.name + ' ' + (channel.group || '')).toLowerCase().indexOf(query) >= 0;
  });

  if (!filtered.length) {
    container.innerHTML = '<p class="muted" style="padding:20px 4px">No channel matches your search.</p>';
    return;
  }

  const groups = new Map();
  for (const channel of filtered) {
    const group = channel.group || 'Other';
    if (!groups.has(group)) groups.set(group, []);
    groups.get(group).push(channel);
  }

  let html = '';
  groups.forEach((items, group) => {
    html += '<div class="group-head"><span>' + escapeHtml(group) + '</span><span>' + items.length + '</span></div>';
    for (const channel of items) html += channelRow(channel);
  });
  container.innerHTML = html;

  container.querySelectorAll('.channel').forEach((row) => {
    row.addEventListener('click', (event) => {
      if (event.target.closest('.star')) return;
      const channel = state.channels.find((item) => item.url === row.dataset.url);
      if (channel) playChannel(channel);
    });
  });
  container.querySelectorAll('.star').forEach((star) => {
    star.addEventListener('click', (event) => {
      event.stopPropagation();
      const row = star.closest('.channel');
      const channel = row ? state.channels.find((item) => item.url === row.dataset.url) : null;
      if (channel) toggleFavorite(channel);
    });
  });
}

/* ---------------------------------------------------------------- actions */

async function loadCatalog() {
  hideConnectionDialog();
  $('hero-stats').textContent = 'Loading catalog\u2026';
  $('hero-host').textContent = '';
  $('category-grid').innerHTML = '';

  try {
    const text = await fetchText(state.apiUrl);
    const catalog = parseCatalog(text);
    if (!catalog.categories.length) throw new Error('The API returned no categories.');

    state.categories = catalog.categories;
    state.notices = catalog.notices;
    state.update = catalog.update;

    renderTicker();
    renderHero();
    renderCategories();
    renderFavorites();
    renderSettings();
  } catch (error) {
    const message = messageFor(error);
    $('hero-stats').textContent = 'Could not load the catalog';
    $('hero-host').textContent = message;
    showConnectionDialog(message);
  }
}

async function openCategory(category) {
  state.category = category;
  state.query = '';
  state.activeGroup = null;
  state.channels = [];
  $('search').value = '';
  $('channels-title').textContent = category.name;
  $('channels-meta').textContent = 'Loading channels\u2026';
  $('groups').innerHTML = '';
  $('channel-list').innerHTML = '<p class="muted" style="padding:20px 4px">Loading channels\u2026</p>';
  showView('channels');

  try {
    const channels = category.channels.length ? category.channels : await fetchCategoryChannels(category);
    state.channels = channels;
    const groups = new Set(channels.map((channel) => channel.group || 'Other'));
    $('channels-meta').textContent = channels.length + ' channels \u00b7 ' + groups.size + ' groups';
    renderGroups();
    renderChannelList();
  } catch (error) {
    $('channels-meta').textContent = 'Could not load channels';
    $('channel-list').innerHTML =
      '<div style="padding:22px 4px">' +
      '<p class="muted">' + escapeHtml(messageFor(error)) + '</p>' +
      '<button class="primary" id="btn-channels-retry">Retry</button>' +
      '</div>';
    const retry = $('btn-channels-retry');
    if (retry) retry.addEventListener('click', () => openCategory(category));
  }
}

async function fetchCategoryChannels(category) {
  if (!category.playlistUrl) throw new Error('This category has no playlist URL.');
  const text = await fetchText(category.playlistUrl);
  const channels = looksLikeM3u(text) ? parseM3u(text) : parseChannelJson(text);
  if (!channels.length) throw new Error('No playable channels found in this playlist.');
  return channels;
}

function toggleFavorite(channel) {
  const index = state.favorites.findIndex((item) => item.url === channel.url);
  if (index >= 0) {
    state.favorites.splice(index, 1);
  } else {
    state.favorites.unshift({
      name: channel.name,
      url: channel.url,
      logo: channel.logo || null,
      group: channel.group || null,
      quality: channel.quality || null,
    });
  }
  localStorage.setItem('x.favorites', JSON.stringify(state.favorites));
  renderFavorites();
  if (state.channels.length) renderChannelList();
  updatePlayerFavorite();
}

/* ---------------------------------------------------------------- playback */

function destroyPlayers() {
  if (state.hls) {
    try { state.hls.destroy(); } catch (error) { /* ignore */ }
    state.hls = null;
  }
  if (state.mpegts) {
    try { state.mpegts.destroy(); } catch (error) { /* ignore */ }
    state.mpegts = null;
  }
  const video = $('video');
  try { video.pause(); } catch (error) { /* ignore */ }
  video.removeAttribute('src');
  try { video.load(); } catch (error) { /* ignore */ }
}

function startPlayback(channel) {
  destroyPlayers();
  const video = $('video');
  const url = channel.url.split('#')[0];
  const lower = url.toLowerCase();
  const headers = channel.headers || {};

  $('player-error').classList.add('hidden');
  video.onerror = () => showPlayerError('This stream could not be played. Try another channel or server.');

  if (lower.indexOf('.m3u8') >= 0 && window.Hls && window.Hls.isSupported()) {
    const hls = new window.Hls({ enableWorker: true, lowLatencyMode: true, backBufferLength: 30 });
    state.hls = hls;
    hls.loadSource(url);
    hls.attachMedia(video);
    if (Object.keys(headers).length) {
      hls.config.xhrSetup = (xhr) => {
        Object.keys(headers).forEach((key) => {
          try { xhr.setRequestHeader(key, headers[key]); } catch (error) { /* ignore */ }
        });
      };
    }
    hls.on(window.Hls.Events.ERROR, (_event, data) => {
      if (!data || !data.fatal) return;
      showPlayerError(data.type === window.Hls.ErrorTypes.NETWORK_ERROR
        ? 'Network error while loading this HLS stream.'
        : 'This HLS stream failed to play.');
    });
    video.play().catch(() => { /* the user can press play */ });
    return;
  }

  if (
    (lower.indexOf('.ts') >= 0 || lower.indexOf('.flv') >= 0 || lower.indexOf('.mpegts') >= 0) &&
    window.mpegts && window.mpegts.isSupported()
  ) {
    const type = lower.indexOf('.flv') >= 0 ? 'flv' : 'mpegts';
    const player = window.mpegts.createPlayer(
      { type, isLive: true, url },
      { enableWorker: true, liveBufferLatencyChasing: true }
    );
    state.mpegts = player;
    player.attachMediaElement(video);
    player.load();
    player.on(window.mpegts.Events.ERROR, () => showPlayerError('MPEG-TS stream failed. Try again or pick another channel.'));
    player.play().catch(() => { /* the user can press play */ });
    return;
  }

  video.src = url;
  video.play().catch(() => showPlayerError('Autoplay is blocked. Press play in the player controls.'));
}

function showPlayerError(message) {
  $('player-error-text').textContent = message;
  $('player-error').classList.remove('hidden');
}

function playChannel(channel) {
  if (!channel) return;
  state.current = channel;
  state.aspect = 0;
  $('player-title').textContent = channel.name;
  $('player-sub').textContent = [channel.group, channel.quality].filter(Boolean).join(' \u00b7 ') || hostOf(channel.url);
  const badges = ['<span class="badge">' + (channel.quality || 'LIVE') + '</span>'];
  if (channel.group) badges.push('<span class="badge">' + escapeHtml(channel.group) + '</span>');
  $('player-badges').innerHTML = badges.join('');
  $('player-error').classList.add('hidden');
  applyAspect();
  updatePlayerFavorite();
  showView('player');
  startPlayback(channel);
}

function updatePlayerFavorite() {
  const button = $('btn-player-favorite');
  if (!button) return;
  const favorite = state.current && state.favorites.some((item) => item.url === state.current.url);
  button.style.color = favorite ? 'var(--accent)' : '';
}

function applyAspect() {
  $('video').style.objectFit = ASPECTS[state.aspect];
  $('btn-aspect').textContent = ASPECT_LABELS[state.aspect];
}

function closePlayer() {
  destroyPlayers();
  state.current = null;
  showView(state.category ? 'channels' : 'home');
}

/* ---------------------------------------------------------------- settings */

function renderSettings() {
  const update = state.update || {};
  const latest = normalizeVersion(update.latest_version || update.version_name || update.version);
  $('current-version').textContent = 'Current v' + state.appVersion;
  $('latest-version').textContent = latest ? 'Latest v' + latest : 'Latest unknown';
  $('update-note').textContent = update.message || update.notes || 'No update information in the API yet.';
  const available = latest && latest !== normalizeVersion(state.appVersion);
  $('btn-update').textContent = available ? 'Download update' : 'Open release page';
  $('update-status').textContent = available
    ? 'A newer version is available for Windows.'
    : 'You are on the latest version.';
  $('api-input').value = state.apiUrl;
  $('settings-error').textContent = '';
}

function openSettings() {
  $('settings-panel').classList.remove('hidden');
  renderSettings();
}

function closeSettings() {
  $('settings-panel').classList.add('hidden');
}

function saveApi() {
  const value = $('api-input').value.trim();
  if (!isStreamUrl(value)) {
    $('settings-error').textContent = 'Enter a valid http(s) API URL.';
    return;
  }
  state.apiUrl = value;
  localStorage.setItem('x.apiUrl', value);
  state.category = null;
  state.channels = [];
  state.activeGroup = null;
  closeSettings();
  showView('home');
  loadCatalog();
}

function openUpdatePage() {
  const update = state.update || {};
  openExternal(update.desktop_url || update.apk_url || PROJECT_URL);
}

/* ------------------------------------------------------------------- dialog */

function showConnectionDialog(message) {
  $('dialog-text').textContent =
    message + '\n\nThe main source did not respond. Reload, check for a new version, or close the app.';
  $('dialog').classList.remove('hidden');
}

function hideConnectionDialog() {
  $('dialog').classList.add('hidden');
}

/* --------------------------------------------------------------------- view */

function showView(name) {
  $('home-view').classList.toggle('hidden', name !== 'home');
  $('channels-view').classList.toggle('hidden', name !== 'channels');
  $('player-view').classList.toggle('hidden', name !== 'player');
  if (name !== 'home') closeSettings();
}

/* --------------------------------------------------------------------- init */

function loadFavorites() {
  try {
    const parsed = JSON.parse(localStorage.getItem('x.favorites') || '[]');
    return Array.isArray(parsed) ? parsed.filter((item) => item && item.url) : [];
  } catch (error) {
    return [];
  }
}

function init() {
  state.favorites = loadFavorites();

  $('btn-refresh').addEventListener('click', loadCatalog);
  $('btn-settings').addEventListener('click', openSettings);
  $('btn-settings-close').addEventListener('click', closeSettings);
  $('btn-update').addEventListener('click', openUpdatePage);
  $('btn-project').addEventListener('click', () => openExternal(PROJECT_URL));
  $('btn-save-api').addEventListener('click', saveApi);
  $('btn-my-server').addEventListener('click', () => { $('api-input').value = DEFAULT_API; });
  $('btn-public').addEventListener('click', () => { $('api-input').value = PUBLIC_API; });
  $('btn-reload-now').addEventListener('click', saveApi);

  $('btn-back').addEventListener('click', () => showView('home'));
  $('btn-channels-refresh').addEventListener('click', () => {
    if (state.category) openCategory(state.category);
  });
  $('search').addEventListener('input', (event) => {
    state.query = event.target.value || '';
    renderChannelList();
  });

  $('btn-player-back').addEventListener('click', closePlayer);
  $('btn-player-retry').addEventListener('click', () => {
    if (state.current) startPlayback(state.current);
  });
  $('btn-aspect').addEventListener('click', () => {
    state.aspect = (state.aspect + 1) % ASPECTS.length;
    applyAspect();
  });
  $('btn-player-favorite').addEventListener('click', () => {
    if (state.current) toggleFavorite(state.current);
  });

  $('btn-dialog-reload').addEventListener('click', () => {
    hideConnectionDialog();
    loadCatalog();
  });
  $('btn-dialog-update').addEventListener('click', () => {
    hideConnectionDialog();
    openSettings();
  });
  $('btn-dialog-close').addEventListener('click', () => window.close());

  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') return;
    if (!$('dialog').classList.contains('hidden')) { hideConnectionDialog(); return; }
    if (!$('player-view').classList.contains('hidden')) { closePlayer(); return; }
    if (!$('settings-panel').classList.contains('hidden')) { closeSettings(); return; }
    if (!$('channels-view').classList.contains('hidden')) { showView('home'); }
  });

  if (window.xapi && typeof window.xapi.version === 'function') {
    window.xapi.version()
      .then((version) => {
        if (version) state.appVersion = version;
        $('foot-version').textContent = 'X desktop v' + state.appVersion;
        renderSettings();
      })
      .catch(() => { /* keep the default version */ });
  }

  renderFavorites();
  loadCatalog();
}

document.addEventListener('DOMContentLoaded', init);
