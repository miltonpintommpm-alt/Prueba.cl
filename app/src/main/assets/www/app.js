/* Mis Clientes: registro de clientes por zona, con WhatsApp e historial (texto y voz). */
(function () {
  'use strict';

  const $app = document.getElementById('app');

  // ------------------------------------------------------------------
  // Utilidades
  // ------------------------------------------------------------------

  const ICONS = {
    back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
    plus: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
    search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
    zones: 'M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z',
    settings: 'M19.14 12.94a7.07 7.07 0 0 0 0-1.88l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.03 7.03 0 0 0-1.62-.94l-.36-2.54A.48.48 0 0 0 13.92 2h-3.84a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.49.49 0 0 0-.59.22L2.72 8.47a.48.48 0 0 0 .12.61l2.03 1.58a7.07 7.07 0 0 0 0 1.88l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.48.48 0 0 0-.12-.61l-2.01-1.58zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z',
    edit: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
    del: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
    phone: 'M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1C10.61 21 3 13.39 3 4c0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z',
    map: 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z',
    person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
    cal: 'M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14v11z',
    mic: 'M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z',
    stop: 'M6 6h12v12H6z',
    send: 'M2.01 21L23 12 2.01 3 2 10l15 2-15 2z',
    close: 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
    id: 'M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zM8 7a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm4 10H4v-1c0-1.33 2.67-2 4-2s4 .67 4 2v1zm8-3h-6v-2h6v2zm0-4h-6V8h6v2z',
    wa: 'M17.47 14.38c-.3-.15-1.76-.87-2.03-.97-.27-.1-.47-.15-.67.15-.2.3-.77.97-.94 1.16-.17.2-.35.22-.64.07-.3-.15-1.26-.46-2.39-1.47-.88-.79-1.48-1.76-1.65-2.06-.17-.3-.02-.46.13-.6.13-.13.3-.35.45-.52.15-.17.2-.3.3-.5.1-.2.05-.37-.02-.52-.08-.15-.67-1.62-.92-2.22-.24-.58-.49-.5-.67-.51h-.57c-.2 0-.52.07-.79.37-.27.3-1.04 1.02-1.04 2.48 0 1.46 1.07 2.88 1.22 3.08.15.2 2.1 3.2 5.08 4.49.71.31 1.26.49 1.69.63.71.23 1.36.2 1.87.12.57-.09 1.76-.72 2.01-1.41.25-.7.25-1.29.17-1.41-.07-.13-.27-.2-.57-.35zM12.05 21.5h-.01a9.4 9.4 0 0 1-4.8-1.32l-.34-.2-3.57.94.95-3.48-.22-.36A9.43 9.43 0 0 1 2.6 12.04C2.6 6.83 6.84 2.6 12.06 2.6a9.4 9.4 0 0 1 6.68 2.77 9.38 9.38 0 0 1 2.76 6.68c0 5.21-4.24 9.45-9.45 9.45zM20.1 3.98A11.32 11.32 0 0 0 12.05.67C5.78.67.67 5.77.67 12.04c0 2 .52 3.96 1.52 5.68L.57 23.64l6.05-1.59a11.35 11.35 0 0 0 5.43 1.38h.01c6.27 0 11.37-5.1 11.38-11.37 0-3.04-1.18-5.9-3.34-8.08z',
  };
  const icon = (name, cls = 'i') =>
    `<svg class="${cls}" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="${ICONS[name]}"/></svg>`;

  const esc = (s) => String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

  const COLORS = ['#2e90fa', '#12b76a', '#f79009', '#ee46bc', '#7a5af8', '#f04438',
    '#06aed4', '#66c61c', '#875bf7', '#667085'];
  const DAYS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  const KINDS = { visita: 'Visita', pedido: 'Pedido', nota: 'Nota' };

  const settings = {
    get(key, def) {
      try { const v = localStorage.getItem('mc.' + key); return v == null ? def : v; } catch (e) { return def; }
    },
    set(key, v) {
      try { localStorage.setItem('mc.' + key, v); } catch (e) { /* sin almacenamiento */ }
    },
  };

  function toast(msg) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(toast._t);
    toast._t = setTimeout(() => t.classList.remove('show'), 2400);
  }

  function initials(name) {
    const parts = String(name || '?').trim().split(/\s+/).slice(0, 2);
    return parts.map((p) => p[0]).join('').toUpperCase() || '?';
  }

  const dfDay = new Intl.DateTimeFormat('es-CL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  const dfTime = new Intl.DateTimeFormat('es-CL', { hour: '2-digit', minute: '2-digit' });

  function ago(ts) {
    if (!ts) return 'Sin contacto registrado';
    const days = Math.floor((startOfDay(Date.now()) - startOfDay(ts)) / 86400000);
    if (days <= 0) return 'Contacto: hoy';
    if (days === 1) return 'Contacto: ayer';
    if (days < 30) return `Contacto: hace ${days} días`;
    const m = Math.floor(days / 30);
    return `Contacto: hace ${m} ${m === 1 ? 'mes' : 'meses'}`;
  }
  function startOfDay(ts) { const d = new Date(ts); d.setHours(0, 0, 0, 0); return d.getTime(); }

  function fmtDur(sec) {
    sec = Math.max(0, Math.round(sec || 0));
    return Math.floor(sec / 60) + ':' + String(sec % 60).padStart(2, '0');
  }

  /** Normaliza un teléfono al formato internacional que usa WhatsApp (solo dígitos). */
  function intlNumber(phone) {
    const raw = String(phone || '').trim();
    if (!raw) return '';
    let d = raw.replace(/\D/g, '');
    if (!d) return '';
    if (raw.startsWith('+')) return d;
    if (raw.startsWith('00')) return d.slice(2);
    d = d.replace(/^0+/, '');
    const cc = settings.get('cc', '56');
    if (d.startsWith(cc) && d.length >= cc.length + 8) return d;
    return cc + d;
  }

  function openExternal(url) {
    if (window.AndroidBridge && window.AndroidBridge.openExternal) {
      window.AndroidBridge.openExternal(url);
    } else {
      window.open(url, '_blank', 'noopener');
    }
  }
  const openWhatsApp = (phone) => {
    const n = intlNumber(phone);
    if (n) openExternal('https://wa.me/' + n);
  };

  /** Hoja inferior modal. Devuelve una promesa con el valor de cierre. */
  function sheet(html, onMount) {
    return new Promise((resolve) => {
      const ov = document.createElement('div');
      ov.className = 'overlay';
      ov.innerHTML = `<div class="sheet" role="dialog" aria-modal="true">${html}</div>`;
      const close = (v) => { ov.remove(); resolve(v); };
      ov.addEventListener('click', (e) => { if (e.target === ov) close(null); });
      document.body.appendChild(ov);
      if (onMount) onMount(ov.querySelector('.sheet'), close);
    });
  }

  function confirmDialog(title, text, okLabel = 'Eliminar') {
    return sheet(`<h3>${esc(title)}</h3><p class="hint">${esc(text)}</p>
      <div class="btns"><button class="btn ghost" data-v="0">Cancelar</button>
      <button class="btn" style="background:var(--danger)" data-v="1">${esc(okLabel)}</button></div>`,
    (el, close) => {
      el.querySelectorAll('[data-v]').forEach((b) => b.addEventListener('click', () => close(b.dataset.v === '1')));
    });
  }

  /** Diálogo para crear o editar una zona. */
  function zoneDialog(zone) {
    const z = zone || { name: '', color: COLORS[Math.floor(Math.random() * COLORS.length)] };
    let color = z.color;
    return sheet(`<h3>${zone ? 'Editar zona' : 'Nueva zona'}</h3>
      <label class="f"><span>Nombre de la zona</span>
        <input class="in" id="zn" maxlength="40" placeholder="Ej: Centro, Maipú, Zona Norte" value="${esc(z.name)}"></label>
      <div><span class="hint" style="font-size:13px">Color</span>
      <div class="colors" style="margin-top:6px">${COLORS.map((c) =>
        `<button class="swatch${c === color ? ' on' : ''}" data-c="${c}" style="background:${c}" aria-label="color"></button>`).join('')}</div></div>
      <div class="btns"><button class="btn ghost" data-x>Cancelar</button><button class="btn" data-ok>Guardar</button></div>`,
    (el, close) => {
      const input = el.querySelector('#zn');
      setTimeout(() => input.focus(), 50);
      el.querySelectorAll('.swatch').forEach((s) => s.addEventListener('click', () => {
        color = s.dataset.c;
        el.querySelectorAll('.swatch').forEach((x) => x.classList.toggle('on', x === s));
      }));
      el.querySelector('[data-x]').addEventListener('click', () => close(null));
      const ok = async () => {
        const name = input.value.trim();
        if (!name) { input.focus(); return; }
        const saved = Object.assign({}, z, { id: z.id || DB.uid(), name, color, createdAt: z.createdAt || Date.now() });
        await DB.put('zones', saved);
        close(saved);
      };
      el.querySelector('[data-ok]').addEventListener('click', ok);
      input.addEventListener('keydown', (e) => { if (e.key === 'Enter') ok(); });
    });
  }

  async function loadZones() {
    const zones = await DB.all('zones');
    zones.sort((a, b) => a.name.localeCompare(b.name, 'es'));
    return zones;
  }

  // Las URL de audio se liberan al cambiar de pantalla.
  let objectUrls = [];
  function audioUrl(blob) { const u = URL.createObjectURL(blob); objectUrls.push(u); return u; }

  let cleanup = null; // función para detener grabaciones al salir de una pantalla

  // ------------------------------------------------------------------
  // Pantalla: lista de clientes
  // ------------------------------------------------------------------

  async function viewHome() {
    const [zones, clients] = await Promise.all([loadZones(), DB.all('clients')]);
    const zoneById = Object.fromEntries(zones.map((z) => [z.id, z]));
    let filter = settings.get('zone', 'all');
    if (filter !== 'all' && filter !== 'none' && !zoneById[filter]) filter = 'all';
    let sort = settings.get('sort', 'name');
    let query = '';

    const counts = {};
    clients.forEach((c) => {
      const k = zoneById[c.zoneId] ? c.zoneId : 'none';
      counts[k] = (counts[k] || 0) + 1;
    });

    $app.innerHTML = `
      <header class="top">
        <h1>Mis Clientes</h1>
        <button class="icon-btn" data-go="#/zonas" aria-label="Zonas">${icon('zones')}</button>
        <button class="icon-btn" data-go="#/ajustes" aria-label="Ajustes y respaldo">${icon('settings')}</button>
      </header>
      <div class="toolbar">
        <div class="search">${icon('search')}<input id="q" type="search" placeholder="Buscar cliente, contacto, teléfono…" autocomplete="off"></div>
      </div>
      <nav class="chips" id="chips">
        <button class="chip" data-z="all">Todas <span class="n">${clients.length}</span></button>
        ${zones.map((z) => `<button class="chip" data-z="${z.id}"><span class="dot" style="background:${z.color}"></span>${esc(z.name)} <span class="n">${counts[z.id] || 0}</span></button>`).join('')}
        ${counts.none ? `<button class="chip" data-z="none"><span class="dot"></span>Sin zona <span class="n">${counts.none}</span></button>` : ''}
        <button class="chip" data-newzone>${icon('plus')} Zona</button>
      </nav>
      <div class="listhead"><span id="count"></span><button class="sort" id="sort"></button></div>
      <main class="list" id="list"></main>
      <button class="fab">${icon('plus')} Cliente</button>`;

    const $list = document.getElementById('list');

    function draw() {
      document.querySelectorAll('#chips [data-z]').forEach((b) => b.classList.toggle('on', b.dataset.z === filter));
      document.getElementById('sort').textContent = sort === 'name' ? 'Orden: A–Z' : 'Orden: sin contacto reciente';
      const q = query.toLowerCase();
      let rows = clients.filter((c) => {
        const zid = zoneById[c.zoneId] ? c.zoneId : 'none';
        if (filter !== 'all' && zid !== filter) return false;
        if (!q) return true;
        return [c.business, c.contact, c.phone, c.address].some((v) => String(v || '').toLowerCase().includes(q));
      });
      if (sort === 'name') rows.sort((a, b) => a.business.localeCompare(b.business, 'es'));
      else rows.sort((a, b) => (a.lastActivity || 0) - (b.lastActivity || 0));

      document.getElementById('count').textContent = `${rows.length} ${rows.length === 1 ? 'cliente' : 'clientes'}`;

      if (!clients.length) {
        $list.innerHTML = `<div class="empty"><b>Aún no tienes clientes</b>
          ${zones.length ? 'Toca <b style="display:inline">+ Cliente</b> para registrar el primero.' : 'Crea tus zonas y luego agrega clientes.'}</div>`;
        return;
      }
      if (!rows.length) {
        $list.innerHTML = '<div class="empty"><b>Sin resultados</b>Prueba con otra zona o búsqueda.</div>';
        return;
      }
      $list.innerHTML = rows.map((c) => {
        const z = zoneById[c.zoneId];
        const color = z ? z.color : '#98a2b3';
        const sub = [c.contact, z ? z.name : 'Sin zona', c.visitDay ? c.visitDay : ''].filter(Boolean).map(esc).join(' · ');
        return `<div class="row" data-open="${c.id}">
          <div class="avatar" style="background:${color}">${esc(initials(c.business))}</div>
          <div class="main"><div class="name">${esc(c.business)}</div>
            <div class="sub">${sub}</div>
            <div class="sub">${esc(ago(c.lastActivity))}</div></div>
          <button class="wa-btn" data-wa="${c.id}" ${intlNumber(c.phone) ? '' : 'disabled'} aria-label="WhatsApp">${icon('wa')}</button>
        </div>`;
      }).join('');
    }

    document.getElementById('q').addEventListener('input', (e) => { query = e.target.value.trim(); draw(); });
    document.getElementById('chips').addEventListener('click', async (e) => {
      if (e.target.closest('[data-newzone]')) {
        const z = await zoneDialog();
        if (z) { settings.set('zone', z.id); render(); }
        return;
      }
      const b = e.target.closest('[data-z]');
      if (!b) return;
      filter = b.dataset.z;
      settings.set('zone', filter);
      draw();
    });
    document.getElementById('sort').addEventListener('click', () => {
      sort = sort === 'name' ? 'contact' : 'name';
      settings.set('sort', sort);
      draw();
    });
    $list.addEventListener('click', (e) => {
      const wa = e.target.closest('[data-wa]');
      if (wa) {
        const c = clients.find((x) => x.id === wa.dataset.wa);
        if (c) openWhatsApp(c.phone);
        return;
      }
      const row = e.target.closest('[data-open]');
      if (row) location.hash = '#/c/' + row.dataset.open;
    });

    // Al crear un cliente nuevo se propone la zona que está filtrada.
    $app.querySelector('.fab').addEventListener('click', (e) => {
      e.stopPropagation();
      location.hash = (filter !== 'all' && filter !== 'none') ? '#/nuevo?zona=' + filter : '#/nuevo';
    });

    draw();
  }

  // ------------------------------------------------------------------
  // Pantalla: zonas
  // ------------------------------------------------------------------

  async function viewZones() {
    const [zones, clients] = await Promise.all([loadZones(), DB.all('clients')]);
    const counts = {};
    clients.forEach((c) => { counts[c.zoneId] = (counts[c.zoneId] || 0) + 1; });

    $app.innerHTML = `
      <header class="top">
        <button class="icon-btn" data-back aria-label="Volver">${icon('back')}</button>
        <h1>Zonas</h1>
      </header>
      <main class="page">
        <p class="hint">Organiza tus clientes por sectores de la ciudad o por ruta de visita.</p>
        <div class="card" id="zl">
          ${zones.length ? zones.map((z) => `<div class="zone-row">
            <span class="dot" style="background:${z.color};width:16px;height:16px"></span>
            <div class="main"><div style="font-weight:600">${esc(z.name)}</div>
              <div class="hint" style="font-size:13px">${counts[z.id] || 0} clientes</div></div>
            <button class="icon-btn" data-edit="${z.id}" aria-label="Editar">${icon('edit')}</button>
            <button class="icon-btn" data-del="${z.id}" aria-label="Eliminar">${icon('del')}</button>
          </div>`).join('') : '<div class="empty" style="padding:24px"><b>Sin zonas</b>Crea la primera zona.</div>'}
        </div>
      </main>
      <button class="fab" data-add>${icon('plus')} Zona</button>`;

    $app.querySelector('[data-add]').addEventListener('click', async () => { if (await zoneDialog()) render(); });
    document.getElementById('zl').addEventListener('click', async (e) => {
      const ed = e.target.closest('[data-edit]');
      if (ed) {
        if (await zoneDialog(zones.find((z) => z.id === ed.dataset.edit))) render();
        return;
      }
      const del = e.target.closest('[data-del]');
      if (del) {
        const z = zones.find((x) => x.id === del.dataset.del);
        const n = counts[z.id] || 0;
        const ok = await confirmDialog(`¿Eliminar la zona "${z.name}"?`,
          n ? `Sus ${n} clientes no se borran: quedarán como "Sin zona".` : 'La zona no tiene clientes.');
        if (ok) { await DB.removeZone(z.id); toast('Zona eliminada'); render(); }
      }
    });
  }

  // ------------------------------------------------------------------
  // Pantalla: formulario de cliente (nuevo / editar)
  // ------------------------------------------------------------------

  async function viewForm(id, params) {
    const zones = await loadZones();
    const c = id ? await DB.get('clients', id) : null;
    if (id && !c) { location.replace('#/'); return; }
    const data = c || { business: '', contact: '', phone: '', address: '', zoneId: params.get('zona') || '', visitDay: '', rut: '', notes: '' };

    const zoneOptions = (sel) => `<option value="">Sin zona</option>` +
      zones.map((z) => `<option value="${z.id}" ${z.id === sel ? 'selected' : ''}>${esc(z.name)}</option>`).join('') +
      `<option value="__new">+ Crear nueva zona…</option>`;

    $app.innerHTML = `
      <header class="top">
        <button class="icon-btn" data-back aria-label="Volver">${icon('close')}</button>
        <h1>${c ? 'Editar cliente' : 'Nuevo cliente'}</h1>
      </header>
      <form class="page" id="f" autocomplete="off">
        <div class="card">
          <h2>Datos del cliente</h2>
          <label class="f"><span>Nombre de la tienda / negocio *</span>
            <input class="in" name="business" required maxlength="80" value="${esc(data.business)}" placeholder="Ej: Repuestos El Rápido"></label>
          <label class="f"><span>Persona de contacto</span>
            <input class="in" name="contact" maxlength="60" value="${esc(data.contact)}" placeholder="Ej: Don Luis"></label>
          <label class="f"><span>Teléfono / WhatsApp</span>
            <input class="in" name="phone" type="tel" inputmode="tel" maxlength="20" value="${esc(data.phone)}" placeholder="Ej: 9 1234 5678"></label>
          <label class="f"><span>RUT (opcional)</span>
            <input class="in" name="rut" maxlength="15" value="${esc(data.rut)}" placeholder="Ej: 76.123.456-7"></label>
        </div>
        <div class="card">
          <h2>Ubicación y ruta</h2>
          <label class="f"><span>Zona</span>
            <select class="in" name="zoneId" id="zsel">${zoneOptions(data.zoneId)}</select></label>
          <label class="f"><span>Dirección</span>
            <input class="in" name="address" maxlength="120" value="${esc(data.address)}" placeholder="Calle, número, comuna"></label>
          <label class="f"><span>Día de visita</span>
            <select class="in" name="visitDay">${DAYS.map((d) => `<option value="${d}" ${d === (data.visitDay || '') ? 'selected' : ''}>${d || 'Sin día fijo'}</option>`).join('')}</select></label>
        </div>
        <div class="card">
          <h2>Notas generales</h2>
          <label class="f"><span>Marcas que trabaja, condiciones de pago, horario…</span>
            <textarea class="in" name="notes" rows="3" maxlength="1000">${esc(data.notes)}</textarea></label>
        </div>
        <button class="btn block" type="submit">Guardar cliente</button>
        ${c ? `<button class="btn danger block" type="button" id="delc">${icon('del')} Eliminar cliente</button>` : ''}
      </form>`;

    const zsel = document.getElementById('zsel');
    let prevZone = zsel.value;
    zsel.addEventListener('change', async () => {
      if (zsel.value !== '__new') { prevZone = zsel.value; return; }
      const z = await zoneDialog();
      if (z) {
        zones.push(z);
        zones.sort((a, b) => a.name.localeCompare(b.name, 'es'));
        zsel.innerHTML = zoneOptions(z.id);
        prevZone = z.id;
      } else {
        zsel.value = prevZone;
      }
    });

    document.getElementById('f').addEventListener('submit', async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const v = Object.fromEntries(fd.entries());
      Object.keys(v).forEach((k) => { v[k] = String(v[k]).trim(); });
      if (!v.business) return;
      if (v.zoneId === '__new') v.zoneId = '';
      const saved = Object.assign({}, c || { id: DB.uid(), createdAt: Date.now(), lastActivity: 0 }, v, { updatedAt: Date.now() });
      await DB.put('clients', saved);
      toast(c ? 'Cliente actualizado' : 'Cliente guardado');
      if (c) history.back();
      else location.replace('#/c/' + saved.id);
    });

    const delc = document.getElementById('delc');
    if (delc) delc.addEventListener('click', async () => {
      const ok = await confirmDialog(`¿Eliminar "${c.business}"?`, 'Se borrará el cliente y todo su historial, incluidas las notas de voz.');
      if (!ok) return;
      await DB.removeClient(c.id);
      toast('Cliente eliminado');
      location.replace('#/');
    });
  }

  // ------------------------------------------------------------------
  // Pantalla: ficha del cliente con historial
  // ------------------------------------------------------------------

  async function viewClient(id) {
    const c = await DB.get('clients', id);
    if (!c) { location.replace('#/'); return; }
    const zone = c.zoneId ? await DB.get('zones', c.zoneId) : null;
    const num = intlNumber(c.phone);

    $app.innerHTML = `
      <header class="top">
        <button class="icon-btn" data-back aria-label="Volver">${icon('back')}</button>
        <h1>${esc(c.business)}</h1>
        <button class="icon-btn" data-go="#/c/${c.id}/editar" aria-label="Editar">${icon('edit')}</button>
      </header>
      <section class="hero">
        <span class="zone"><span class="dot" style="background:${zone ? zone.color : '#98a2b3'}"></span>${esc(zone ? zone.name : 'Sin zona')}</span>
        <div class="meta">
          ${c.contact ? `<div>${icon('person')}<span>${esc(c.contact)}</span></div>` : ''}
          ${c.phone ? `<div>${icon('phone')}<span>${esc(c.phone)}</span></div>` : ''}
          ${c.address ? `<div>${icon('map')}<span>${esc(c.address)}</span></div>` : ''}
          ${c.visitDay ? `<div>${icon('cal')}<span>Visita: ${esc(c.visitDay)}</span></div>` : ''}
          ${c.rut ? `<div>${icon('id')}<span>RUT ${esc(c.rut)}</span></div>` : ''}
        </div>
        <div class="actions">
          <button class="act wa" data-a="wa" ${num ? '' : 'disabled'}>${icon('wa')}WhatsApp</button>
          <button class="act" data-a="call" ${num ? '' : 'disabled'}>${icon('phone')}Llamar</button>
          <button class="act" data-a="map" ${c.address ? '' : 'disabled'}>${icon('map')}Mapa</button>
        </div>
      </section>
      <main class="page" style="padding-bottom:200px">
        ${c.notes ? `<div class="card"><h2>Notas generales</h2><div class="notes-box">${esc(c.notes)}</div></div>` : ''}
        <div class="card" style="padding:12px 14px"><h2 style="margin:0">Historial</h2></div>
        <div class="timeline" id="tl"></div>
      </main>
      <div class="composer" id="composer"></div>`;

    $app.querySelector('.actions').addEventListener('click', (e) => {
      const b = e.target.closest('[data-a]');
      if (!b) return;
      if (b.dataset.a === 'wa') openWhatsApp(c.phone);
      if (b.dataset.a === 'call') openExternal('tel:+' + num);
      if (b.dataset.a === 'map') openExternal('https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(c.address));
    });

    // ----- Historial -----
    const $tl = document.getElementById('tl');
    async function drawTimeline() {
      const entries = await DB.byIndex('entries', 'clientId', c.id);
      entries.sort((a, b) => b.createdAt - a.createdAt);
      if (!entries.length) {
        $tl.innerHTML = '<div class="empty" style="padding:28px 16px"><b>Sin registros todavía</b>Anota visitas, pedidos o deja una nota de voz abajo.</div>';
        return;
      }
      let lastDay = '';
      $tl.innerHTML = entries.map((en) => {
        const day = dfDay.format(en.createdAt);
        const head = day !== lastDay ? `<div class="day">${esc(day)}</div>` : '';
        lastDay = day;
        return head + `<article class="entry">
          <div class="h"><span class="badge ${en.kind}">${KINDS[en.kind] || 'Nota'}</span>
            <span>${dfTime.format(en.createdAt)}</span>
            ${en.audio ? `<span>· ${icon('mic', 'i sm')} ${fmtDur(en.duration)}</span>` : ''}
            <button class="icon-btn del" data-del="${en.id}" aria-label="Eliminar">${icon('del')}</button></div>
          ${en.text ? `<div class="t">${esc(en.text)}</div>` : ''}
          ${en.audio ? `<audio controls preload="metadata" src="${audioUrl(en.audio)}"></audio>` : ''}
        </article>`;
      }).join('');
    }
    $tl.addEventListener('click', async (e) => {
      const d = e.target.closest('[data-del]');
      if (!d) return;
      if (await confirmDialog('¿Eliminar este registro?', 'No se puede deshacer.')) {
        await DB.remove('entries', d.dataset.del);
        await refreshLastActivity();
        drawTimeline();
      }
    });

    async function refreshLastActivity() {
      const entries = await DB.byIndex('entries', 'clientId', c.id);
      c.lastActivity = entries.reduce((m, x) => Math.max(m, x.createdAt), 0);
      await DB.put('clients', c);
    }

    // ----- Compositor (texto + voz) -----
    const $comp = document.getElementById('composer');
    let kind = 'visita';
    let text = '';
    let rec = null;       // { recorder, stream, chunks, start, timer, cancelled }
    let pending = null;   // { blob, duration, url }

    function drawComposer() {
      const canSend = !rec && (text.trim() || pending);
      $comp.innerHTML = `
        <div class="kinds">${Object.keys(KINDS).map((k) =>
          `<button class="chip ${k}${k === kind ? ' on' : ''}" data-k="${k}">${KINDS[k]}</button>`).join('')}</div>
        ${pending ? `<div class="compose-row" style="margin-bottom:8px"><div class="recbar">${icon('mic')}
            <audio controls src="${pending.url}"></audio></div>
            <button class="round" style="background:var(--line);color:var(--text)" data-c="discard" aria-label="Descartar audio">${icon('close')}</button></div>` : ''}
        <div class="compose-row">
          ${rec ? `<div class="recbar"><span class="live"></span><span id="rt">0:00</span>
              <span class="hint" style="flex:1">Grabando…</span>
              <button class="link-btn" data-c="cancel">Cancelar</button></div>
            <button class="round rec" data-c="stop" aria-label="Detener grabación">${icon('stop')}</button>`
          : `<textarea class="in" id="txt" rows="1" placeholder="Escribe una nota, pedido o visita…">${esc(text)}</textarea>
            ${pending ? '' : `<button class="round mic" data-c="rec" aria-label="Grabar nota de voz">${icon('mic')}</button>`}
            <button class="round" data-c="send" ${canSend ? '' : 'disabled style="opacity:.4"'} aria-label="Guardar">${icon('send')}</button>`}
        </div>`;
      const ta = document.getElementById('txt');
      if (ta) {
        autoGrow(ta);
        ta.addEventListener('input', () => {
          const had = !!text.trim();
          text = ta.value;
          autoGrow(ta);
          if (had !== !!text.trim()) {
            const s = $comp.querySelector('[data-c="send"]');
            const on = !!(text.trim() || pending);
            s.disabled = !on;
            s.style.opacity = on ? '' : '.4';
          }
        });
      }
    }
    function autoGrow(ta) { ta.style.height = 'auto'; ta.style.height = Math.min(ta.scrollHeight + 2, 140) + 'px'; }

    function pickMime() {
      if (!window.MediaRecorder || !MediaRecorder.isTypeSupported) return '';
      return ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg;codecs=opus']
        .find((t) => MediaRecorder.isTypeSupported(t)) || '';
    }

    async function startRecording() {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
        toast('Este equipo no permite grabar audio');
        return;
      }
      let stream;
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      } catch (err) {
        toast('Permiso de micrófono denegado');
        return;
      }
      const mime = pickMime();
      const recorder = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined);
      rec = { recorder, stream, chunks: [], start: Date.now(), cancelled: false };
      recorder.ondataavailable = (ev) => { if (ev.data && ev.data.size) rec.chunks.push(ev.data); };
      recorder.onstop = () => {
        const r = rec;
        rec = null;
        clearInterval(r.timer);
        r.stream.getTracks().forEach((t) => t.stop());
        if (!r.cancelled && r.chunks.length) {
          const blob = new Blob(r.chunks, { type: recorder.mimeType || mime || 'audio/webm' });
          pending = { blob, duration: (Date.now() - r.start) / 1000, url: audioUrl(blob) };
        }
        if ($comp.isConnected) drawComposer();
      };
      recorder.start(1000);
      drawComposer();
      rec.timer = setInterval(() => {
        const sec = (Date.now() - rec.start) / 1000;
        const el = document.getElementById('rt');
        if (el) el.textContent = fmtDur(sec);
        if (sec >= 600) stopRecording(false); // máximo 10 minutos
      }, 250);
    }

    function stopRecording(cancel) {
      if (!rec) return;
      rec.cancelled = cancel;
      if (rec.recorder.state !== 'inactive') rec.recorder.stop();
    }

    async function save() {
      if (!text.trim() && !pending) return;
      const entry = {
        id: DB.uid(), clientId: c.id, kind, text: text.trim(), createdAt: Date.now(),
        audio: pending ? pending.blob : null, audioType: pending ? pending.blob.type : '',
        duration: pending ? pending.duration : 0,
      };
      await DB.put('entries', entry);
      c.lastActivity = entry.createdAt;
      await DB.put('clients', c);
      text = '';
      pending = null;
      drawComposer();
      await drawTimeline();
      toast(`${KINDS[kind]} guardada`);
    }

    $comp.addEventListener('click', (e) => {
      const k = e.target.closest('[data-k]');
      if (k) {
        kind = k.dataset.k;
        $comp.querySelectorAll('[data-k]').forEach((b) => b.classList.toggle('on', b === k));
        return;
      }
      const b = e.target.closest('[data-c]');
      if (!b) return;
      const a = b.dataset.c;
      if (a === 'rec') startRecording();
      else if (a === 'stop') stopRecording(false);
      else if (a === 'cancel') stopRecording(true);
      else if (a === 'discard') { pending = null; drawComposer(); }
      else if (a === 'send') save();
    });

    cleanup = () => stopRecording(true);
    drawComposer();
    await drawTimeline();
  }

  // ------------------------------------------------------------------
  // Pantalla: ajustes y respaldo
  // ------------------------------------------------------------------

  function blobToDataUrl(blob) {
    return new Promise((resolve, reject) => {
      const r = new FileReader();
      r.onload = () => resolve(r.result);
      r.onerror = () => reject(r.error);
      r.readAsDataURL(blob);
    });
  }
  function dataUrlToBlob(url) {
    const m = /^data:([^,]*?)(;base64)?,([\s\S]*)$/.exec(url || '');
    if (!m) return null;
    const bin = m[2] ? atob(m[3]) : decodeURIComponent(m[3]);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return new Blob([bytes], { type: m[1] || 'application/octet-stream' });
  }

  async function exportBackup() {
    const [zones, clients, entries] = await Promise.all([DB.all('zones'), DB.all('clients'), DB.all('entries')]);
    const outEntries = [];
    for (const e of entries) {
      const copy = Object.assign({}, e);
      copy.audio = e.audio ? await blobToDataUrl(e.audio) : null;
      outEntries.push(copy);
    }
    const json = JSON.stringify({ app: 'mis-clientes', version: 1, exportedAt: new Date().toISOString(), zones, clients, entries: outEntries });
    const name = 'respaldo-clientes-' + new Date().toISOString().slice(0, 10) + '.json';
    if (window.AndroidBridge && window.AndroidBridge.saveTextFile) {
      window.AndroidBridge.saveTextFile(name, 'application/json', json);
    } else {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      toast('Respaldo descargado');
    }
  }

  async function importBackup(file) {
    let data;
    try {
      data = JSON.parse(await file.text());
    } catch (e) {
      toast('El archivo no es un respaldo válido');
      return;
    }
    if (!data || data.app !== 'mis-clientes') { toast('El archivo no es un respaldo de Mis Clientes'); return; }
    const entries = (data.entries || []).map((e) => Object.assign({}, e, { audio: e.audio ? dataUrlToBlob(e.audio) : null }));
    await DB.bulkPut({ zones: data.zones, clients: data.clients, entries });
    toast(`Importados ${(data.clients || []).length} clientes`);
    location.hash = '#/';
  }

  async function viewSettings() {
    const [zones, clients, entries] = await Promise.all([DB.all('zones'), DB.all('clients'), DB.all('entries')]);
    const voice = entries.filter((e) => e.audio).length;
    $app.innerHTML = `
      <header class="top">
        <button class="icon-btn" data-back aria-label="Volver">${icon('back')}</button>
        <h1>Ajustes y respaldo</h1>
      </header>
      <main class="page">
        <div class="card">
          <h2>WhatsApp</h2>
          <label class="f"><span>Código de país para números sin "+" (Chile = 56)</span>
            <input class="in" id="cc" inputmode="numeric" maxlength="4" value="${esc(settings.get('cc', '56'))}"></label>
          <p class="hint">Ej: "9 1234 5678" se abrirá como +56 9 1234 5678.</p>
        </div>
        <div class="card">
          <h2>Tus datos</h2>
          <p class="hint" style="margin-bottom:12px">${zones.length} zonas · ${clients.length} clientes · ${entries.length} registros (${voice} notas de voz).
            Todo se guarda solo en este teléfono. Haz un respaldo de vez en cuando
            y guárdalo en Drive o envíatelo por correo.</p>
          <button class="btn block" id="exp">Exportar respaldo</button>
          <div style="height:8px"></div>
          <button class="btn ghost block" id="imp">Importar respaldo</button>
          <input type="file" id="file" accept="application/json,.json" hidden>
        </div>
      </main>`;
    document.getElementById('cc').addEventListener('change', (e) => {
      const v = e.target.value.replace(/\D/g, '') || '56';
      e.target.value = v;
      settings.set('cc', v);
      toast('Código de país guardado');
    });
    document.getElementById('exp').addEventListener('click', exportBackup);
    const file = document.getElementById('file');
    document.getElementById('imp').addEventListener('click', () => file.click());
    file.addEventListener('change', () => { if (file.files[0]) importBackup(file.files[0]); });
  }

  // ------------------------------------------------------------------
  // Navegación
  // ------------------------------------------------------------------

  document.addEventListener('click', (e) => {
    const go = e.target.closest('[data-go]');
    if (go) { location.hash = go.dataset.go; return; }
    if (e.target.closest('[data-back]')) {
      if (history.length > 1) history.back(); else location.hash = '#/';
    }
  });

  async function render() {
    if (cleanup) { cleanup(); cleanup = null; }
    objectUrls.forEach((u) => URL.revokeObjectURL(u));
    objectUrls = [];
    document.querySelectorAll('.overlay').forEach((o) => o.remove());

    const [path, qs] = (location.hash.replace(/^#/, '') || '/').split('?');
    const params = new URLSearchParams(qs || '');
    const parts = path.split('/').filter(Boolean);
    try {
      if (parts[0] === 'zonas') await viewZones();
      else if (parts[0] === 'ajustes') await viewSettings();
      else if (parts[0] === 'nuevo') await viewForm(null, params);
      else if (parts[0] === 'c' && parts[2] === 'editar') await viewForm(parts[1], params);
      else if (parts[0] === 'c' && parts[1]) await viewClient(parts[1]);
      else await viewHome();
      window.scrollTo(0, 0);
    } catch (err) {
      console.error(err);
      $app.innerHTML = `<div class="empty"><b>Ocurrió un error</b>${esc(err && err.message)}</div>`;
    }
  }

  /** En el primer uso se crean zonas de ejemplo que se pueden renombrar o borrar. */
  async function firstRun() {
    if (settings.get('seeded')) return;
    const zones = await DB.all('zones');
    if (!zones.length) {
      const names = ['Centro', 'Norte', 'Sur', 'Oriente', 'Poniente'];
      await DB.bulkPut({ zones: names.map((name, i) => ({ id: DB.uid() + i, name, color: COLORS[i], createdAt: Date.now() })) });
    }
    settings.set('seeded', '1');
  }

  window.addEventListener('hashchange', render);
  firstRun().then(render, render);
})();
