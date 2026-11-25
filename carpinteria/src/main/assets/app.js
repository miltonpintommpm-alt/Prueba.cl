/* Maestro Carpintero — lógica de la app (sin dependencias, 100 % offline). */
(function () {
  'use strict';

  // ---------------------------------------------------------------- constantes
  const KEY = 'maestro-carpintero-v1';
  const MAX_MIN_DIA = 120;
  const INTERVALOS_REPASO = [2, 7, 21, 60]; // días (repetición espaciada)
  const NOTA_APROBACION_QUIZ = 0.7;
  const NOTA_APROBACION_EXAMEN = 0.8;

  // Horas de práctica autónoma exigidas por nivel antes del examen (además de las lecciones)
  const HORAS_PRACTICA = { basico: 15, intermedio: 40, avanzado: 80, profesional: 100 };

  const PRACTICA_LIBRE = {
    basico: [
      'Haz 10 cortes al través y 3 al hilo siguiendo línea. Mide tu error con la escuadra.',
      'Afila tu formón y la cuchilla del cepillo hasta que corten la testa del pino limpiamente.',
      'Cepilla el canto de un listón hasta que quede recto y a escuadra. Repite en otro.',
      'Fabrica un taco de lijar, un empujador o un gancho de banco con retazos.',
      'Construye una repisa simple con tornillos y cola, verificando escuadra.',
      'Haz un comedero para pájaros o una caja pequeña con tu propio diseño.',
    ],
    intermedio: [
      'Repite una unión a media madera y una de caja y espiga hasta que cierren sin luz.',
      'Escuadra 2 tablas en bruto completas (6 caras).',
      'Haz 5 cortes de tablero con la sierra circular y guía, con tolerancia ±0,5 mm.',
      'Construye un taburete o banquito con uniones de caja y espiga.',
      'Fabrica una plantilla para tarugos o un carro de corte para tu taller.',
      'Construye un estante con ranuras (dados) y trasera en rebaje.',
    ],
    avanzado: [
      'Haz una cola de milano pasante cada sesión, cronometrando tu tiempo.',
      'Fabrica una puerta pequeña de marco y panel.',
      'Construye un cajón con correderas e instálalo con luces parejas.',
      'Diseña y fabrica un mueble de melamina con tapacantos y minifix.',
      'Aplica un sistema de acabado completo (sellador + 2 manos) en un retazo grande.',
      'Calcula y dibuja una escalera para una altura real de tu entorno.',
    ],
    profesional: [
      'Cotiza un mueble real que te pidan (aunque no lo hagas) con tu planilla.',
      'Fabrica un mueble por encargo para un familiar o conocido y regístralo.',
      'Fabrica 10 piezas iguales con plantilla y mide la variación.',
      'Fotografía tus trabajos y actualiza tu portafolio y redes.',
      'Visita una barraca o mueblería y conversa con profesionales sobre materiales y precios.',
      'Ofrece un trabajo de reparación o instalación a un cliente real.',
    ],
  };

  // ---------------------------------------------------------------- utilidades
  const $ = (sel, el = document) => el.querySelector(sel);
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const pad = (n) => String(n).padStart(2, '0');
  const fecha = (d = new Date()) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const sumarDias = (str, n) => { const d = new Date(str + 'T12:00:00'); d.setDate(d.getDate() + n); return fecha(d); };
  const hoy = () => fecha();
  const fmtMin = (m) => { m = Math.round(m); if (m < 60) return `${m} min`; const h = Math.floor(m / 60), r = m % 60; return r ? `${h} h ${r} min` : `${h} h`; };
  const shuffle = (a) => { a = a.slice(); for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  const norm = (s) => String(s).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const hashStr = (s) => { let h = 0; for (const c of s) h = (h * 31 + c.charCodeAt(0)) | 0; return Math.abs(h); };

  // índices del plan de estudios
  const LECCIONES = [];
  const POR_ID = {};
  CURRICULUM.forEach((niv, ni) => niv.modulos.forEach((mod) => mod.lecciones.forEach((lec) => {
    const x = Object.assign({ nivel: niv.id, nivelIdx: ni, modulo: mod.titulo }, lec);
    LECCIONES.push(x); POR_ID[lec.id] = x;
  })));
  const NIVEL = Object.fromEntries(CURRICULUM.map((n) => [n.id, n]));
  const leccionesDe = (nivelId) => LECCIONES.filter((l) => l.nivel === nivelId);
  const TIPO = { teoria: '📖 Teoría', practica: '🔨 Práctica', proyecto: '🏗️ Proyecto' };

  // ---------------------------------------------------------------- estado
  let S = cargar();
  function nuevoEstado() {
    return { v: 1, perfil: null, lec: {}, repasos: {}, refuerzo: [], niveles: {}, practica: {}, plan: null, log: {}, crono: { inicio: null, acum: 0, fecha: null }, chat: [] };
  }
  function cargar() {
    try { const s = JSON.parse(localStorage.getItem(KEY)); if (s && s.v === 1) return Object.assign(nuevoEstado(), s); } catch (e) { /* almacenamiento no disponible */ }
    return nuevoEstado();
  }
  function guardar() { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) { /* ignorar */ } }
  const L = (id) => (S.lec[id] = S.lec[id] || { minHecho: 0, quizOk: false, quizBest: 0, check: [], rating: null, done: false });

  function registrarMinutos(min) { const d = hoy(); S.log[d] = (S.log[d] || 0) + min; }

  // ---------------------------------------------------------------- progreso
  function nivelActual() {
    for (const n of CURRICULUM) if (!(S.niveles[n.id] && S.niveles[n.id].aprobado)) return n;
    return null; // todos aprobados
  }
  function leccionesPendientes(nivelId) { return leccionesDe(nivelId).filter((l) => !L(l.id).done); }
  function minPractica(nivelId) { return S.practica[nivelId] || 0; }
  function practicaRequerida(nivelId) { return HORAS_PRACTICA[nivelId] * 60; }
  function practicaPendiente(nivelId) { return Math.max(0, practicaRequerida(nivelId) - minPractica(nivelId)); }
  function puedeRendirExamen(nivelId) { return leccionesPendientes(nivelId).length === 0 && practicaPendiente(nivelId) === 0; }

  function minutosRestantes(nivelId) {
    let m = 0;
    for (const l of leccionesDe(nivelId)) { const st = L(l.id); if (!st.done) m += Math.max(10, l.min - st.minHecho); }
    m += practicaPendiente(nivelId);
    if (!(S.niveles[nivelId] && S.niveles[nivelId].aprobado)) m += 20; // examen
    return m;
  }
  function porcentajeNivel(nivelId) {
    if (S.niveles[nivelId] && S.niveles[nivelId].aprobado) return 100;
    const total = leccionesDe(nivelId).reduce((a, l) => a + l.min, 0) + practicaRequerida(nivelId) + 20;
    return Math.min(99, Math.round(100 * (1 - minutosRestantes(nivelId) / total)));
  }
  function minutosSemana() { return S.perfil.minDia * S.perfil.diasSemana; }
  function semanasPara(nivelId) {
    let m = 0;
    for (const n of CURRICULUM) { m += minutosRestantes(n.id); if (n.id === nivelId) break; }
    return m / minutosSemana();
  }
  function fmtSemanas(s) {
    if (s <= 0.01) return '¡logrado!';
    if (s < 1) return 'menos de 1 semana';
    if (s < 9) return `≈ ${Math.ceil(s)} semanas`;
    return `≈ ${Math.round(s / 4.345)} meses`;
  }
  function diasSemanaActual() {
    const d = new Date(); const dow = (d.getDay() + 6) % 7; // lunes = 0
    let n = 0; for (let i = 0; i <= dow; i++) { const x = new Date(d); x.setDate(d.getDate() - i); if ((S.log[fecha(x)] || 0) >= 10) n++; }
    return n;
  }
  function racha() {
    let n = 0; const d = new Date();
    if (!(S.log[fecha(d)] >= 10)) d.setDate(d.getDate() - 1);
    while ((S.log[fecha(d)] || 0) >= 10) { n++; d.setDate(d.getDate() - 1); }
    return n;
  }
  const totalMin = () => Object.values(S.log).reduce((a, b) => a + b, 0);

  // ---------------------------------------------------------------- planificador
  /*
   * Arma la sesión del día sin pasar del tiempo disponible (máx. 120 min):
   *  1. Repaso espaciado de lecciones ya aprobadas (≤ 10 min)
   *  2. Refuerzo de prácticas que costaron (≤ 30 min)
   *  3. Lecciones nuevas en orden, divididas en partes si no caben
   *  4. Práctica autónoma (≈ 25 % del tiempo cuando el nivel ya tiene prácticas hechas,
   *     o todo el tiempo restante cuando las lecciones del nivel están terminadas)
   *  5. Examen de nivel cuando corresponde
   */
  function generarPlan() {
    const presupuesto = Math.min(MAX_MIN_DIA, S.perfil.minDia);
    let libre = presupuesto;
    const bloques = [];
    const niv = nivelActual();

    const vencidos = Object.keys(S.repasos).filter((id) => S.repasos[id].due <= hoy()).slice(0, 4);
    if (vencidos.length && libre >= 25) {
      const m = Math.min(10, 3 * vencidos.length + 2);
      bloques.push({ tipo: 'repaso', ids: vencidos, min: m }); libre -= m;
    }
    if (!niv) { S.plan = { fecha: hoy(), presupuesto, bloques }; guardar(); return; }

    if (S.refuerzo.length && libre >= 30) {
      const id = S.refuerzo[0];
      const m = Math.min(30, Math.max(20, Math.round(POR_ID[id].min / 2)));
      bloques.push({ tipo: 'refuerzo', id, min: m }); libre -= m;
    }

    const pend = leccionesPendientes(niv.id);
    const hayPracticaHecha = leccionesDe(niv.id).some((l) => l.tipo !== 'teoria' && L(l.id).done);
    let reservaPractica = 0;
    if (pend.length && hayPracticaHecha && practicaPendiente(niv.id) > 0 && presupuesto >= 60) {
      reservaPractica = Math.min(practicaPendiente(niv.id), Math.round(presupuesto * 0.25 / 5) * 5);
    }
    libre -= reservaPractica;

    for (const l of pend) {
      if (libre < 10) break;
      const st = L(l.id);
      const restante = Math.max(0, l.min - st.minHecho);
      if (restante === 0) { // solo falta la evaluación
        bloques.push({ tipo: 'leccion', id: l.id, min: 10, final: true, soloEval: true }); libre -= 10; continue;
      }
      if (restante > libre && libre < 20) break; // no empezar trozos demasiado cortos
      const m = Math.min(restante, libre);
      const final = m >= restante;
      const partes = Math.max(1, Math.ceil(l.min / presupuesto));
      const parte = Math.min(partes, Math.floor(st.minHecho / presupuesto) + 1);
      bloques.push({ tipo: 'leccion', id: l.id, min: m, final, parte, partes });
      libre -= m;
      if (!final) break; // la lección continúa mañana
    }

    libre += reservaPractica;
    if (pend.length === 0 && practicaPendiente(niv.id) > 0) reservaPractica = Math.min(libre, practicaPendiente(niv.id));
    else reservaPractica = Math.min(reservaPractica, libre);
    if (reservaPractica >= 15) {
      bloques.push({ tipo: 'practica', nivel: niv.id, min: reservaPractica }); libre -= reservaPractica;
    }

    if (puedeRendirExamen(niv.id) && libre >= 20) bloques.push({ tipo: 'examen', nivel: niv.id, min: 20 });

    S.plan = { fecha: hoy(), presupuesto, bloques };
    guardar();
  }
  function asegurarPlan() {
    if (!S.plan || S.plan.fecha !== hoy() || S.plan.presupuesto !== Math.min(MAX_MIN_DIA, S.perfil.minDia)) generarPlan();
  }
  function marcarBloque(idx) {
    const b = S.plan && S.plan.bloques[idx];
    if (b && !b.hecho) { b.hecho = true; registrarMinutos(b.min); }
  }

  // ---------------------------------------------------------------- el maestro
  function saludo() {
    const h = new Date().getHours();
    return h < 12 ? 'Buenos días' : h < 20 ? 'Buenas tardes' : 'Buenas noches';
  }
  function consejoDelDia() { return CONSEJOS[hashStr(hoy()) % CONSEJOS.length]; }

  function mensajeMaestro() {
    const p = S.perfil; const niv = nivelActual(); const nombre = esc(p.nombre);
    const r = racha(); const dias = diasSemanaActual();
    const msgs = [];
    if (!niv) return `${saludo()}, maestro ${nombre}. Completaste los cuatro niveles. Desde ahora tu escuela es cada encargo: sigue registrando trabajos, mide tus tiempos y enseña a otros.`;
    if (totalMin() === 0) msgs.push(`${saludo()}, ${nombre}. Soy tu maestro de taller. Iremos paso a paso, desde la seguridad hasta trabajar como profesional, respetando tus ${fmtMin(p.minDia)} diarios.`);
    else msgs.push(`${saludo()}, ${nombre}.`);
    if (r >= 3) msgs.push(`Llevas ${r} días seguidos: así se forma un oficio.`);
    if (S.plan && S.plan.bloques.every((b) => b.hecho) && S.plan.bloques.length) msgs.push('Terminaste el trabajo de hoy. Ordena el taller y descansa: mañana seguimos.');
    else if (dias >= p.diasSemana && !(S.log[hoy()] >= 10)) msgs.push(`Ya cumpliste tus ${p.diasSemana} días de esta semana. Hoy puedes descansar o adelantar si tienes ganas.`);
    const fallado = LECCIONES.find((l) => S.lec[l.id] && S.lec[l.id].quizBest > 0 && !S.lec[l.id].quizOk);
    if (fallado) msgs.push(`Tenemos pendiente reforzar «${esc(fallado.titulo)}». Relee el contenido con calma antes de volver a evaluarte.`);
    if (S.refuerzo.length) msgs.push('Incluí un bloque de refuerzo: repetir lo que costó es como se gana pulso.');
    const pl = S.plan && S.plan.bloques.find((b) => !b.hecho && b.tipo === 'leccion' && (POR_ID[b.id].c || []).some((x) => x.startsWith('! ')));
    if (pl) msgs.push('⚠️ Hoy hay trabajo con riesgo: lentes, protección auditiva y la pieza siempre prensada.');
    if (puedeRendirExamen(niv.id)) msgs.push(`Estás listo para el examen del nivel ${niv.nombre}. Respira y confía en lo que practicaste.`);
    else if (leccionesPendientes(niv.id).length === 0) msgs.push(`Terminaste las lecciones del nivel ${niv.nombre}. Ahora acumula práctica de banco: te faltan ${fmtMin(practicaPendiente(niv.id))}.`);
    return msgs.join(' ');
  }

  // ---------------------------------------------------------------- render helpers
  const app = () => $('#app');
  function setVista(html, titulo, conAtras) {
    $('#titulo').textContent = titulo || 'Maestro Carpintero';
    $('#atras').hidden = !conAtras;
    app().innerHTML = html;
    window.scrollTo(0, 0);
    const tab = location.hash.split('/')[0] || '#hoy';
    document.querySelectorAll('nav.tabs a').forEach((a) => a.classList.toggle('activo', a.getAttribute('href') === tab));
    $('nav.tabs').hidden = !S.perfil;
  }
  function barra(pct) { return `<div class="barra"><div style="width:${Math.max(0, Math.min(100, pct))}%"></div></div>`; }
  function contenidoHTML(lineas) {
    let html = ''; let enLista = false;
    for (const ln of lineas || []) {
      if (ln.startsWith('- ')) { if (!enLista) { html += '<ul>'; enLista = true; } html += `<li>${esc(ln.slice(2))}</li>`; continue; }
      if (enLista) { html += '</ul>'; enLista = false; }
      if (ln.startsWith('## ')) html += `<h3>${esc(ln.slice(3))}</h3>`;
      else if (ln.startsWith('! ')) html += `<div class="aviso">⚠️ ${esc(ln.slice(2))}</div>`;
      else html += `<p>${esc(ln)}</p>`;
    }
    if (enLista) html += '</ul>';
    return html;
  }
  function toast(msg) {
    const t = $('#toast'); t.textContent = msg; t.classList.add('ver');
    clearTimeout(toast._t); toast._t = setTimeout(() => t.classList.remove('ver'), 2600);
  }

  // ---------------------------------------------------------------- vistas
  function vistaBienvenida() {
    setVista(`
      <section class="card hero">
        <div class="avatar">👴🏽🪚</div>
        <h2>Tu maestro carpintero</h2>
        <p>Te guiaré desde cero hasta trabajar como carpintero en cuatro niveles: <b>básico, intermedio, avanzado y profesional</b>. Cada día armo tu sesión de teoría y práctica según el tiempo que tengas (máximo 2 horas).</p>
      </section>
      <form class="card" id="fPerfil">
        <label>¿Cómo te llamas?<input name="nombre" required maxlength="30" placeholder="Tu nombre"></label>
        <label>¿Cuánto tiempo puedes dedicar al día? <b id="vMin">60 min</b>
          <input type="range" name="minDia" min="30" max="120" step="15" value="60"></label>
        <label>¿Cuántos días a la semana?
          <select name="diasSemana">${[3, 4, 5, 6, 7].map((d) => `<option ${d === 5 ? 'selected' : ''}>${d}</option>`).join('')}</select></label>
        <label>¿Qué experiencia tienes?
          <select name="exp">
            <option value="0">Ninguna, parto de cero</option>
            <option value="1">He hecho arreglos en casa</option>
            <option value="2">He trabajado como ayudante</option>
            <option value="3">Ya trabajo en carpintería</option>
          </select></label>
        <p class="nota" id="nExp"></p>
        <button class="btn primario">Comenzar mi formación</button>
      </form>`, 'Bienvenida', false);
    const f = $('#fPerfil');
    const nota = () => {
      const e = +f.exp.value;
      $('#nExp').textContent = e === 0 ? 'Perfecto: empezaremos por la seguridad y la madera.'
        : 'Podrás rendir un examen de convalidación en la pestaña Ruta para saltar lo que ya dominas. Igual te recomiendo repasar la seguridad.';
    };
    f.minDia.oninput = () => { $('#vMin').textContent = fmtMin(+f.minDia.value); };
    f.exp.onchange = nota; nota();
    f.onsubmit = (e) => {
      e.preventDefault();
      S.perfil = { nombre: f.nombre.value.trim() || 'aprendiz', minDia: +f.minDia.value, diasSemana: +f.diasSemana.value, exp: +f.exp.value, creado: hoy() };
      guardar(); generarPlan(); location.hash = '#hoy'; render();
    };
  }

  function descripcionBloque(b) {
    switch (b.tipo) {
      case 'repaso': return { icono: '🔁', titulo: 'Repaso rápido', sub: `${b.ids.length} tema(s) para no olvidar` };
      case 'refuerzo': return { icono: '💪', titulo: `Refuerzo: ${POR_ID[b.id].titulo}`, sub: 'Repite la práctica que te costó' };
      case 'practica': return { icono: '🪚', titulo: 'Práctica de banco', sub: `Ejercicio autónomo · nivel ${NIVEL[b.nivel].nombre}` };
      case 'examen': return { icono: '🎓', titulo: `Examen nivel ${NIVEL[b.nivel].nombre}`, sub: 'Aprueba con 80 % para subir de nivel' };
      default: {
        const l = POR_ID[b.id];
        const parte = b.soloEval ? 'Evaluación pendiente' : b.partes > 1 ? `Parte ${b.parte} de ${b.partes}` : l.modulo;
        return { icono: TIPO[l.tipo].split(' ')[0], titulo: l.titulo, sub: `${TIPO[l.tipo].split(' ')[1]} · ${parte}` };
      }
    }
  }

  function vistaHoy() {
    asegurarPlan();
    const p = S.perfil; const niv = nivelActual();
    const hecho = S.plan.bloques.filter((b) => b.hecho).reduce((a, b) => a + b.min, 0);
    const total = S.plan.bloques.reduce((a, b) => a + b.min, 0);
    const dias = diasSemanaActual();
    const descanso = dias >= p.diasSemana && !(S.log[hoy()] >= 10);
    const bloques = S.plan.bloques.map((b, i) => {
      const d = descripcionBloque(b);
      return `<a class="bloque ${b.hecho ? 'hecho' : ''}" href="#bloque/${i}">
        <span class="ico">${b.hecho ? '✅' : d.icono}</span>
        <span class="txt"><b>${esc(d.titulo)}</b><small>${esc(d.sub)}</small></span>
        <span class="min">${b.min}′</span></a>`;
    }).join('');
    setVista(`
      <section class="card maestro">
        <div class="avatar sm">👴🏽</div>
        <p>${mensajeMaestro()}</p>
      </section>
      ${niv ? `
      <section class="card">
        <div class="fila"><h2>Sesión de hoy</h2><span class="chip">${fmtMin(total)} de ${fmtMin(p.minDia)}</span></div>
        ${descanso ? '<p class="nota">📅 Día de descanso según tu plan semanal. Si quieres, puedes adelantar igual.</p>' : ''}
        ${barra(total ? 100 * hecho / total : 0)}
        <div class="bloques">${bloques || '<p class="nota">No hay bloques para hoy.</p>'}</div>
        <div class="crono" id="crono"></div>
      </section>
      <section class="card">
        <div class="fila"><h3>Nivel ${esc(niv.nombre)} ${niv.icono}</h3><span class="chip">${porcentajeNivel(niv.id)} %</span></div>
        ${barra(porcentajeNivel(niv.id))}
        <p class="nota">Meta: ${esc(niv.meta)}</p>
        <p class="nota">Esta semana: <b>${dias} de ${p.diasSemana}</b> días · Racha: <b>${racha()}</b> 🔥 · Nivel terminado en <b>${fmtSemanas(semanasPara(niv.id))}</b></p>
      </section>` : `
      <section class="card"><h2>🏆 Formación completa</h2><p>Revisa tus certificados en <a href="#progreso">Progreso</a>.</p></section>`}
      <section class="card consejo"><b>💡 Consejo del maestro</b><p>${esc(consejoDelDia())}</p></section>
    `, 'Hoy', false);
    pintarCrono();
  }

  // Cronómetro de sesión (persistente)
  function segundosCrono() {
    if (S.crono.fecha !== hoy()) { S.crono = { inicio: null, acum: 0, fecha: hoy() }; guardar(); }
    return S.crono.acum + (S.crono.inicio ? (Date.now() - S.crono.inicio) / 1000 : 0);
  }
  function pintarCrono() {
    const el = $('#crono'); if (!el) return;
    const s = segundosCrono(); const lim = S.perfil.minDia * 60;
    const corriendo = !!S.crono.inicio;
    el.innerHTML = `<div class="fila"><span>⏱️ <b>${pad(Math.floor(s / 3600))}:${pad(Math.floor(s / 60) % 60)}:${pad(Math.floor(s) % 60)}</b> / ${fmtMin(S.perfil.minDia)}</span>
      <button class="btn chico" id="bCrono">${corriendo ? 'Pausar' : s ? 'Continuar' : 'Iniciar sesión'}</button></div>
      ${s >= lim ? '<p class="nota alerta">Cumpliste tu tiempo de hoy. Termina el bloque actual con calma, ordena y descansa.</p>' : ''}`;
    $('#bCrono').onclick = () => {
      segundosCrono();
      if (S.crono.inicio) { S.crono.acum += (Date.now() - S.crono.inicio) / 1000; S.crono.inicio = null; }
      else S.crono.inicio = Date.now();
      guardar(); pintarCrono();
    };
  }
  setInterval(() => { if (S.crono && S.crono.inicio) pintarCrono(); }, 1000);

  function vistaBloque(idx) {
    asegurarPlan();
    const b = S.plan.bloques[idx];
    if (!b) { location.hash = '#hoy'; return; }
    if (b.tipo === 'leccion' || b.tipo === 'refuerzo') return vistaLeccion(b.id, idx);
    if (b.tipo === 'repaso') return vistaRepaso(idx);
    if (b.tipo === 'practica') return vistaPracticaLibre(idx);
    if (b.tipo === 'examen') return vistaExamen(b.nivel, idx, false);
  }

  // ---------------------------------------------------------------- lección
  function quizHTML(preguntas, prefijo) {
    return preguntas.map((q, qi) => `
      <fieldset class="pregunta" data-q="${qi}">
        <legend>${qi + 1}. ${esc(q.q)}</legend>
        ${q.o.map((o, oi) => `<label class="opcion"><input type="radio" name="${prefijo}${qi}" value="${oi}"> ${esc(o)}</label>`).join('')}
        <div class="explica" hidden></div>
      </fieldset>`).join('');
  }
  function corregir(contenedor, preguntas, prefijo) {
    let ok = 0; let respondidas = 0;
    preguntas.forEach((q, qi) => {
      const sel = contenedor.querySelector(`input[name="${prefijo}${qi}"]:checked`);
      const fs = contenedor.querySelector(`[data-q="${qi}"]`);
      const ex = fs.querySelector('.explica');
      if (sel) respondidas++;
      const bien = sel && +sel.value === q.a;
      if (bien) ok++;
      fs.classList.toggle('bien', !!bien); fs.classList.toggle('mal', !bien);
      ex.hidden = false;
      ex.innerHTML = bien ? `✅ ${esc(q.e)}` : `❌ Respuesta correcta: <b>${esc(q.o[q.a])}</b>. ${esc(q.e)}`;
    });
    return { ok, total: preguntas.length, respondidas };
  }

  function vistaLeccion(id, bloqueIdx) {
    const l = POR_ID[id]; if (!l) { location.hash = '#ruta'; return; }
    const st = L(id);
    const b = bloqueIdx != null ? S.plan.bloques[bloqueIdx] : null;
    const esRefuerzo = b && b.tipo === 'refuerzo';
    const final = esRefuerzo || !b || b.final;
    const bloqueado = l.nivelIdx > CURRICULUM.indexOf(nivelActual() || CURRICULUM[CURRICULUM.length - 1]);
    const tieneEval = (l.quiz && l.quiz.length) || (l.check && l.check.length);
    const parte = b && !esRefuerzo && b.partes > 1 ? `<span class="chip">Parte ${b.parte} de ${b.partes}</span>` : '';
    const minutosBloque = b ? b.min : Math.max(0, l.min - st.minHecho);

    let pie = '';
    if (st.done && !esRefuerzo) {
      pie = `<div class="card ok">✅ Lección completada${st.convalidada ? ' (convalidada)' : ''}.</div>`;
    } else if (bloqueado) {
      pie = '<div class="card nota">🔒 Puedes leer esta lección, pero se activa al llegar a su nivel.</div>';
    } else if (esRefuerzo) {
      pie = `<section class="card"><h3>Refuerzo</h3><p>Repite los pasos de la práctica concentrándote en lo que te costó. ¿Cómo te fue esta vez?</p>
        <div class="rating">${ratingBtns()}</div></section>`;
    } else if (!final) {
      pie = `<section class="card"><p>Esta lección continúa en tu próxima sesión. Trabaja ${fmtMin(minutosBloque)} en ella y avanza en los pasos que alcances.</p>
        <button class="btn primario" id="bParte">Terminé esta parte (${minutosBloque} min)</button></section>`;
    } else {
      pie = `
        ${l.check && l.check.length ? `<section class="card"><h3>✔️ Autoevaluación</h3><p class="nota">Marca solo lo que realmente lograste. Ser honesto contigo es parte del oficio.</p>
          ${l.check.map((c, i) => `<label class="check"><input type="checkbox" data-c="${i}" ${st.check[i] ? 'checked' : ''}> ${esc(c)}</label>`).join('')}</section>` : ''}
        ${l.quiz && l.quiz.length ? `<section class="card" id="quiz"><h3>🧠 Comprueba lo aprendido</h3>${quizHTML(l.quiz, 'q')}</section>` : ''}
        ${l.tipo !== 'teoria' ? `<section class="card"><h3>¿Cómo te fue?</h3><div class="rating">${ratingBtns(st.rating)}</div></section>` : ''}
        <button class="btn primario" id="bCompletar">${tieneEval ? 'Evaluar y completar' : 'Completar lección'}</button>`;
    }

    setVista(`
      <section class="card">
        <div class="fila"><span class="chip">${TIPO[l.tipo]}</span>${parte}<span class="chip">${fmtMin(l.min)}</span></div>
        <h2>${esc(l.titulo)}</h2>
        <p class="nota">${esc(NIVEL[l.nivel].nombre)} · ${esc(l.modulo)}</p>
        <p class="resumen">${esc(l.resumen || '')}</p>
      </section>
      ${l.c ? `<section class="card contenido">${contenidoHTML(l.c)}</section>` : ''}
      ${l.materiales ? `<section class="card"><h3>🧰 Materiales y herramientas</h3><ul>${l.materiales.map((m) => `<li>${esc(m)}</li>`).join('')}</ul></section>` : ''}
      ${l.pasos ? `<section class="card"><h3>👣 Paso a paso</h3><ol class="pasos">${l.pasos.map((p) => `<li>${esc(p)}</li>`).join('')}</ol></section>` : ''}
      ${l.errores ? `<section class="card"><h3>🚫 Errores frecuentes</h3><ul>${l.errores.map((m) => `<li>${esc(m)}</li>`).join('')}</ul></section>` : ''}
      ${pie}
    `, l.titulo, true);

    const bParte = $('#bParte');
    if (bParte) bParte.onclick = () => {
      st.minHecho = Math.min(l.min, st.minHecho + b.min);
      marcarBloque(bloqueIdx); guardar(); toast('Parte registrada. ¡Buen trabajo!'); location.hash = '#hoy';
    };
    let rating = st.rating;
    app().querySelectorAll('.rating button').forEach((btn) => btn.onclick = () => {
      rating = btn.dataset.r;
      app().querySelectorAll('.rating button').forEach((x) => x.classList.toggle('sel', x === btn));
      if (esRefuerzo) {
        S.refuerzo.shift();
        if (rating === 'costo') S.refuerzo.push(id);
        marcarBloque(bloqueIdx); guardar();
        toast(rating === 'costo' ? 'Lo repetiremos otro día. La constancia gana.' : '¡Bien! Ese pulso ya es tuyo.');
        setTimeout(() => { location.hash = '#hoy'; }, 600);
      }
    });
    const bComp = $('#bCompletar');
    if (bComp) bComp.onclick = () => {
      const checks = [...app().querySelectorAll('[data-c]')].map((x) => x.checked);
      st.check = checks;
      let quizOk = true; let res = null;
      if (l.quiz && l.quiz.length) {
        res = corregir($('#quiz'), l.quiz, 'q');
        if (res.respondidas < res.total) { toast('Responde todas las preguntas.'); return; }
        const nota = res.ok / res.total;
        st.quizBest = Math.max(st.quizBest, nota);
        quizOk = nota >= NOTA_APROBACION_QUIZ;
        st.quizOk = st.quizOk || quizOk;
      }
      if (l.tipo !== 'teoria' && !rating) { toast('Indica cómo te fue en la práctica.'); return; }
      const checksOk = checks.every(Boolean);
      // el tiempo del bloque cuenta aunque falte aprobar
      if (b && !b.hecho) { st.minHecho = Math.min(l.min, st.minHecho + (b.soloEval ? 0 : b.min)); marcarBloque(bloqueIdx); }
      else if (!b) { const m = Math.max(0, l.min - st.minHecho); st.minHecho = l.min; registrarMinutos(m); }
      st.rating = rating;
      if (quizOk && checksOk) {
        st.done = true; st.doneAt = hoy();
        if (l.quiz && l.quiz.length) S.repasos[id] = { due: sumarDias(hoy(), INTERVALOS_REPASO[0]), paso: 0 };
        if (rating === 'costo' && !S.refuerzo.includes(id)) S.refuerzo.push(id);
        guardar();
        mostrarResultado(true, res, rating === 'costo'
          ? 'Lección aprobada. Como te costó, agregué un bloque de refuerzo en los próximos días.'
          : '¡Excelente! Lección completada. Programé repasos para que no se te olvide.');
      } else {
        guardar();
        const motivos = [];
        if (!quizOk) motivos.push(`obtuviste ${res.ok} de ${res.total} en la evaluación (necesitas 70 %). Revisa las explicaciones en rojo y relee el contenido`);
        if (!checksOk) motivos.push('aún no logras todos los criterios de práctica. Repite los pasos hasta lograrlos: no hay apuro, la calidad es lo primero');
        mostrarResultado(false, res, `Todavía no: ${motivos.join('; y ')}. Volverá a aparecer en tu plan.`);
      }
    };
  }
  function ratingBtns(sel) {
    return [['costo', '😓 Me costó'], ['bien', '🙂 Bien'], ['dominado', '😎 Lo domino']]
      .map(([k, t]) => `<button type="button" class="btn chico ${sel === k ? 'sel' : ''}" data-r="${k}">${t}</button>`).join('');
  }
  function mostrarResultado(ok, res, msg) {
    const div = document.createElement('section');
    div.className = 'card ' + (ok ? 'ok' : 'alerta');
    div.innerHTML = `<p>👴🏽 ${esc(msg)}</p><a class="btn primario" href="#hoy">Volver a hoy</a>`;
    const b = $('#bCompletar'); if (b) b.replaceWith(div);
    div.scrollIntoView({ behavior: 'smooth' });
  }

  // ---------------------------------------------------------------- repaso espaciado
  function vistaRepaso(idx) {
    const b = S.plan.bloques[idx];
    const preguntas = [];
    b.ids.forEach((id) => {
      const q = (POR_ID[id].quiz || []); if (!q.length) return;
      const pick = shuffle(q).slice(0, 2); pick.forEach((x) => preguntas.push(Object.assign({ lec: id }, x)));
    });
    setVista(`
      <section class="card maestro"><div class="avatar sm">👴🏽</div><p>Repasemos un poco. Recordar lo aprendido en días distintos es lo que lo fija para siempre.</p></section>
      <section class="card" id="rep">${b.hecho ? '<p>✅ Repaso de hoy completado.</p>' : quizHTML(preguntas, 'r')}</section>
      ${b.hecho ? '' : '<button class="btn primario" id="bRep">Revisar</button>'}`, 'Repaso', true);
    const bt = $('#bRep'); if (!bt) return;
    bt.onclick = () => {
      const res = corregir($('#rep'), preguntas, 'r');
      if (res.respondidas < res.total) { toast('Responde todas las preguntas.'); return; }
      const porLec = {};
      preguntas.forEach((q, qi) => {
        const sel = $(`#rep input[name="r${qi}"]:checked`);
        porLec[q.lec] = (porLec[q.lec] !== false) && sel && +sel.value === q.a;
      });
      b.ids.forEach((id) => {
        const r = S.repasos[id]; if (!r) return;
        if (porLec[id] === false) { r.paso = 0; r.due = sumarDias(hoy(), 1); }
        else { r.paso++; if (r.paso >= INTERVALOS_REPASO.length) delete S.repasos[id]; else r.due = sumarDias(hoy(), INTERVALOS_REPASO[r.paso]); }
      });
      marcarBloque(idx); guardar();
      bt.outerHTML = `<section class="card ${res.ok === res.total ? 'ok' : ''}"><p>👴🏽 ${res.ok} de ${res.total}. ${res.ok === res.total ? 'Memoria de carpintero.' : 'Lo que fallaste lo repasaremos mañana.'}</p><a class="btn primario" href="#hoy">Volver a hoy</a></section>`;
    };
  }

  // ---------------------------------------------------------------- práctica autónoma
  function vistaPracticaLibre(idx) {
    const b = S.plan.bloques[idx];
    const lista = PRACTICA_LIBRE[b.nivel];
    const sugerido = lista[hashStr(hoy() + b.nivel) % lista.length];
    const hecho = minPractica(b.nivel); const req = practicaRequerida(b.nivel);
    setVista(`
      <section class="card maestro"><div class="avatar sm">👴🏽</div>
        <p>El oficio se aprende con horas de banco. Hoy te propongo: <b>${esc(sugerido)}</b></p></section>
      <section class="card"><h3>Horas de práctica del nivel ${esc(NIVEL[b.nivel].nombre)}</h3>
        ${barra(100 * hecho / req)}<p class="nota">${fmtMin(hecho)} de ${fmtMin(req)}</p></section>
      <section class="card"><h3>Otras ideas</h3><ul>${lista.map((x) => `<li>${esc(x)}</li>`).join('')}</ul>
        <p class="nota">Puedes elegir cualquiera o trabajar en un proyecto propio de este nivel.</p></section>
      ${b.hecho ? '<div class="card ok">✅ Práctica registrada.</div>' : `
      <section class="card"><label>¿Cuántos minutos practicaste?
        <input type="number" id="mPrac" min="5" max="${b.min}" value="${b.min}"></label>
        <label>Nota de taller (opcional)<input id="nPrac" maxlength="140" placeholder="Qué hiciste, qué mejorar"></label>
        <button class="btn primario" id="bPrac">Registrar práctica</button></section>`}`, 'Práctica de banco', true);
    const bt = $('#bPrac'); if (!bt) return;
    bt.onclick = () => {
      const m = Math.max(5, Math.min(b.min, parseInt($('#mPrac').value, 10) || 0));
      S.practica[b.nivel] = (S.practica[b.nivel] || 0) + m;
      const nota = $('#nPrac').value.trim();
      if (nota) { S.notas = S.notas || []; S.notas.unshift({ f: hoy(), t: nota }); S.notas = S.notas.slice(0, 50); }
      b.min = m; marcarBloque(idx); guardar();
      toast('Práctica registrada. ¡Cada hora cuenta!'); location.hash = '#hoy';
    };
  }

  // ---------------------------------------------------------------- examen de nivel
  function preguntasExamen(nivelId) {
    const banco = NIVEL[nivelId].examen.slice();
    leccionesDe(nivelId).forEach((l) => (l.quiz || []).forEach((q) => banco.push(Object.assign({ lec: l.id }, q))));
    return shuffle(banco).slice(0, 12);
  }
  function vistaExamen(nivelId, bloqueIdx, convalidacion) {
    const niv = NIVEL[nivelId];
    const preguntas = preguntasExamen(nivelId);
    setVista(`
      <section class="card maestro"><div class="avatar sm">👴🏽</div>
        <p>${convalidacion
          ? `Examen de convalidación del nivel ${esc(niv.nombre)}. Si apruebas con 80 % o más, daré por aprobado el nivel. Úsalo solo si de verdad dominas la práctica de estos temas.`
          : `Examen del nivel ${esc(niv.nombre)}: ${preguntas.length} preguntas, necesitas 80 %. Tómate tu tiempo.`}</p></section>
      <section class="card" id="exa">${quizHTML(preguntas, 'e')}</section>
      <button class="btn primario" id="bExa">Entregar examen</button>`, `Examen ${niv.nombre}`, true);
    $('#bExa').onclick = () => {
      const res = corregir($('#exa'), preguntas, 'e');
      if (res.respondidas < res.total) { toast('Responde todas las preguntas.'); return; }
      const nota = res.ok / res.total;
      const aprobado = nota >= NOTA_APROBACION_EXAMEN;
      if (bloqueIdx != null) marcarBloque(bloqueIdx);
      let msg;
      if (aprobado) {
        S.niveles[nivelId] = { aprobado: true, fecha: hoy(), nota: Math.round(nota * 100), convalidado: convalidacion };
        if (convalidacion) leccionesDe(nivelId).forEach((l) => { const st = L(l.id); if (!st.done) Object.assign(st, { done: true, convalidada: true, doneAt: hoy(), minHecho: l.min, quizOk: true }); });
        S.plan = null;
        msg = `¡Felicitaciones! Aprobaste el nivel ${niv.nombre} con ${Math.round(nota * 100)} %. Tu certificado está en Progreso.`;
      } else {
        // los temas fallados vuelven a repaso mañana
        preguntas.forEach((q, qi) => {
          const sel = $(`#exa input[name="e${qi}"]:checked`);
          if (q.lec && !(sel && +sel.value === q.a) && L(q.lec).done) S.repasos[q.lec] = { due: sumarDias(hoy(), 1), paso: 0 };
        });
        msg = `Obtuviste ${Math.round(nota * 100)} %. Aún no alcanza el 80 %. ${convalidacion ? 'Te recomiendo seguir la ruta normal del nivel.' : 'Programé repasos de los temas fallados; podrás rendirlo de nuevo pronto.'}`;
      }
      guardar();
      $('#bExa').outerHTML = `<section class="card ${aprobado ? 'ok' : 'alerta'}"><p>👴🏽 ${esc(msg)}</p><a class="btn primario" href="${aprobado ? '#progreso' : '#hoy'}">Continuar</a></section>`;
    };
  }

  // ---------------------------------------------------------------- ruta
  function vistaRuta() {
    const actual = nivelActual();
    const html = CURRICULUM.map((niv, ni) => {
      const apr = S.niveles[niv.id] && S.niveles[niv.id].aprobado;
      const esActual = actual && actual.id === niv.id;
      const mods = niv.modulos.map((m) => `
        <h4>${esc(m.titulo)}</h4>
        ${m.lecciones.map((l) => { const st = L(l.id); return `<a class="lec ${st.done ? 'hecho' : ''}" href="#leccion/${l.id}">
          <span>${st.done ? '✅' : TIPO[l.tipo].split(' ')[0]}</span><span class="txt">${esc(l.titulo)}</span><small>${fmtMin(l.min)}</small></a>`; }).join('')}`).join('');
      return `<details class="card nivel" ${esActual ? 'open' : ''}>
        <summary><span class="ico">${niv.icono}</span><span class="txt"><b>${ni + 1}. ${esc(niv.nombre)}</b><small>${esc(niv.titulo)}</small></span>
          <span class="chip">${apr ? '🎓' : porcentajeNivel(niv.id) + ' %'}</span></summary>
        ${barra(porcentajeNivel(niv.id))}
        <p>${esc(niv.meta)}</p>
        <p class="nota">Competencias:</p><ul>${niv.competencias.map((c) => `<li>${esc(c)}</li>`).join('')}</ul>
        <p class="nota">Práctica de banco: ${fmtMin(minPractica(niv.id))} de ${fmtMin(practicaRequerida(niv.id))} · Termina ${fmtSemanas(semanasPara(niv.id))}</p>
        ${mods}
        ${esActual && !puedeRendirExamen(niv.id) ? `<a class="btn" href="#convalidar/${niv.id}">¿Ya dominas este nivel? Rendir examen de convalidación</a>` : ''}
        ${esActual && puedeRendirExamen(niv.id) ? `<a class="btn primario" href="#examen/${niv.id}">Rendir examen del nivel</a>` : ''}
      </details>`;
    }).join('');
    setVista(`<section class="card"><p>Tu ruta completa: <b>${LECCIONES.length} lecciones</b> en 4 niveles, más horas de práctica de banco. Puedes leer cualquier lección cuando quieras; tu plan diario te lleva en orden.</p></section>${html}`, 'Ruta de aprendizaje', false);
  }

  // ---------------------------------------------------------------- maestro: consulta y calculadoras
  const STOP = new Set('el la los las un una unos unas de del al a en y o que como cual cuales para por con sin es son se mi me tu te lo le les su sus mas muy cuanto cuantos cuanta debo puedo hacer hago usar uso sirve sobre entre este esta esto ese esa'.split(' '));
  const INDICE = (() => {
    const docs = [];
    GLOSARIO.forEach(([t, d]) => docs.push({ tipo: 'glosario', titulo: t, texto: `${t}: ${d}`, peso: 1.6 }));
    LECCIONES.forEach((l) => {
      const partes = [].concat(l.c || [], l.pasos || [], l.errores || []);
      partes.forEach((t) => docs.push({ tipo: 'leccion', id: l.id, titulo: l.titulo, texto: t.replace(/^(## |- |! )/, ''), peso: 1 }));
      docs.push({ tipo: 'leccion', id: l.id, titulo: l.titulo, texto: `${l.titulo}. ${l.resumen || ''}`, peso: 1.3 });
    });
    docs.forEach((d) => { d.tokens = new Set(norm(d.texto).split(/[^a-z0-9ñ]+/).filter((w) => w.length > 2 && !STOP.has(w))); });
    return docs;
  })();
  function buscar(pregunta) {
    const q = norm(pregunta).split(/[^a-z0-9ñ]+/).filter((w) => w.length > 2 && !STOP.has(w));
    if (!q.length) return [];
    const res = INDICE.map((d) => {
      let s = 0;
      for (const w of q) {
        if (d.tokens.has(w)) s += 2;
        else for (const t of d.tokens) { if (w.length >= 4 && (t.startsWith(w.slice(0, Math.max(4, w.length - 2))))) { s += 1; break; } }
      }
      return { d, s: s * d.peso };
    }).filter((x) => x.s >= 2).sort((a, b) => b.s - a.s);
    const vistos = new Set(); const out = [];
    for (const r of res) { const k = r.d.texto; if (vistos.has(k)) continue; vistos.add(k); out.push(r.d); if (out.length >= 3) break; }
    return out;
  }
  function responder(pregunta) {
    const n = norm(pregunta);
    const niv = nivelActual();
    if (/(que|qué) (hago|toca|estudio) hoy|plan de hoy/.test(n)) {
      asegurarPlan();
      return 'Hoy te toca: ' + S.plan.bloques.map((b) => `${descripcionBloque(b).titulo} (${b.min} min)`).join(', ') + '.';
    }
    if (/cuanto (me )?falta|cuando termin|progreso/.test(n)) {
      if (!niv) return 'Ya completaste los cuatro niveles. ¡Eres maestro!';
      return `Vas en el nivel ${niv.nombre} (${porcentajeNivel(niv.id)} %). A tu ritmo de ${fmtMin(S.perfil.minDia)} × ${S.perfil.diasSemana} días, lo terminas en ${fmtSemanas(semanasPara(niv.id))} y llegas a profesional en ${fmtSemanas(semanasPara('profesional'))}.`;
    }
    const r = buscar(pregunta);
    if (!r.length) return 'No encontré eso en mis apuntes. Prueba con otras palabras (por ejemplo: "afilar formón", "tarugos", "cuánto cobrar") o revisa el glosario y las calculadoras.';
    return r.map((d) => d.tipo === 'glosario'
      ? `📘 <b>${esc(d.titulo)}</b>: ${esc(d.texto.slice(d.titulo.length + 2))}`
      : `📖 ${esc(d.texto)} <a href="#leccion/${d.id}">→ ${esc(d.titulo)}</a>`).join('<br><br>');
  }

  function vistaMaestro() {
    const chat = (S.chat || []).slice(-12).map((m) => `<div class="msg ${m.yo ? 'yo' : ''}">${m.yo ? esc(m.t) : m.t}</div>`).join('');
    setVista(`
      <section class="card">
        <h2>👴🏽 Pregúntale al maestro</h2>
        <div class="chat" id="chat">${chat || '<div class="msg">Pregúntame sobre herramientas, uniones, maderas, acabados, seguridad o cómo cobrar. También: "¿qué hago hoy?" o "¿cuánto me falta?".</div>'}</div>
        <form id="fChat" class="fila"><input id="qChat" placeholder="Escribe tu pregunta…" autocomplete="off"><button class="btn primario chico">Enviar</button></form>
        <div class="sugerencias">${['¿Cómo afilo un formón?', '¿Qué lija uso?', '¿Cuánto me falta?', '¿Qué es el retroceso?'].map((s) => `<button class="btn chico" data-s="${esc(s)}">${esc(s)}</button>`).join('')}</div>
      </section>
      <section class="card"><h3>🧮 Calculadoras de taller</h3>
        <details><summary>Pies madereros</summary>
          <div class="calc" id="cPie">
            <label>Espesor (pulg)<input type="number" step="0.5" value="2" name="e"></label>
            <label>Ancho (pulg)<input type="number" step="0.5" value="4" name="a"></label>
            <label>Largo (m)<input type="number" step="0.1" value="3.2" name="l"></label>
            <label>Cantidad<input type="number" value="1" name="n"></label>
            <p class="resultado"></p></div></details>
        <details><summary>Escalera (regla de Blondel)</summary>
          <div class="calc" id="cEsc">
            <label>Altura piso a piso (cm)<input type="number" value="270" name="h"></label>
            <p class="resultado"></p></div></details>
        <details><summary>Cotización rápida</summary>
          <div class="calc" id="cCot">
            <label>Materiales ($)<input type="number" value="100000" name="m"></label>
            <label>Horas de trabajo<input type="number" value="10" name="h"></label>
            <label>Valor hora ($)<input type="number" value="10000" name="v"></label>
            <label>Gastos (transporte, etc.) ($)<input type="number" value="20000" name="g"></label>
            <label>Utilidad (%)<input type="number" value="20" name="u"></label>
            <label>Impuesto (%)<input type="number" value="19" name="i"></label>
            <p class="resultado"></p></div></details>
        <details><summary>Cajón con correderas</summary>
          <div class="calc" id="cCaj">
            <label>Ancho interior del mueble (mm)<input type="number" value="500" name="w"></label>
            <label>Holgura por lado de la corredera (mm)<input type="number" step="0.1" value="12.7" name="c"></label>
            <label>Espesor del material del cajón (mm)<input type="number" value="15" name="t"></label>
            <p class="resultado"></p></div></details>
      </section>
      <section class="card"><h3>📘 Glosario</h3>
        <input id="fGlos" placeholder="Filtrar términos…">
        <dl id="glos">${GLOSARIO.map(([t, d]) => `<div><dt>${esc(t)}</dt><dd>${esc(d)}</dd></div>`).join('')}</dl></section>
    `, 'Maestro', false);

    const enviar = (txt) => {
      txt = txt.trim(); if (!txt) return;
      S.chat = (S.chat || []).concat([{ yo: true, t: txt }, { yo: false, t: responder(txt) }]).slice(-30);
      guardar(); vistaMaestro(); const c = $('#chat'); c.scrollTop = c.scrollHeight; $('#qChat').focus();
    };
    $('#fChat').onsubmit = (e) => { e.preventDefault(); enviar($('#qChat').value); };
    app().querySelectorAll('[data-s]').forEach((b) => b.onclick = () => enviar(b.dataset.s));
    const c = $('#chat'); c.scrollTop = c.scrollHeight;

    calc('#cPie', (v) => { const p = v.e * v.a * (v.l / 0.3048) / 12 * v.n; return `<b>${p.toFixed(2)}</b> pies madereros (${(v.l / 0.3048).toFixed(2)} pies de largo c/u).`; });
    calc('#cEsc', (v) => {
      if (!(v.h > 0)) return '';
      const n = Math.round(v.h / 17.5); const c = v.h / n; const hu = 63 - 2 * c;
      const ok = c <= 18.5 && c >= 15;
      return `${n} contrahuellas de <b>${c.toFixed(1)} cm</b> · huella ≈ <b>${hu.toFixed(1)} cm</b> · largo horizontal ≈ ${((n - 1) * hu / 100).toFixed(2)} m.${ok ? '' : ' ⚠️ Revisa: contrahuella fuera del rango cómodo.'} Verifica la normativa local.`;
    });
    calc('#cCot', (v) => {
      const neto = (v.m * 1.15 + v.h * v.v + v.g) * (1 + v.u / 100); const tot = neto * (1 + v.i / 100);
      const f = (x) => '$' + Math.round(x).toLocaleString('es-CL');
      return `Neto: <b>${f(neto)}</b> · Con impuesto: <b>${f(tot)}</b> · Anticipo 50 %: ${f(tot / 2)}<br><small>Incluye 15 % de merma sobre materiales.</small>`;
    });
    calc('#cCaj', (v) => { const w = v.w - 2 * v.c; return `Ancho exterior de la caja: <b>${w.toFixed(1)} mm</b> · Frente y trasera (entre costados): <b>${(w - 2 * v.t).toFixed(1)} mm</b>. Confirma con la ficha de tu corredera.`; });

    $('#fGlos').oninput = (e) => {
      const f = norm(e.target.value);
      app().querySelectorAll('#glos > div').forEach((d) => { d.hidden = f && !norm(d.textContent).includes(f); });
    };
  }
  function calc(sel, fn) {
    const el = $(sel); const out = el.querySelector('.resultado');
    const upd = () => { const v = {}; el.querySelectorAll('input').forEach((i) => { v[i.name] = parseFloat(i.value) || 0; }); out.innerHTML = fn(v); };
    el.querySelectorAll('input').forEach((i) => { i.oninput = upd; }); upd();
  }

  // ---------------------------------------------------------------- progreso y ajustes
  function vistaProgreso() {
    const p = S.perfil;
    const dias = Object.keys(S.log).filter((d) => S.log[d] >= 10).length;
    const completas = LECCIONES.filter((l) => L(l.id).done).length;
    const ultimos = [...Array(14)].map((_, i) => { const d = new Date(); d.setDate(d.getDate() - 13 + i); const m = S.log[fecha(d)] || 0; return { d, m }; });
    const maxM = Math.max(p.minDia, ...ultimos.map((x) => x.m));
    const certs = CURRICULUM.filter((n) => S.niveles[n.id] && S.niveles[n.id].aprobado);
    setVista(`
      <section class="card stats">
        <div><b>${(totalMin() / 60).toFixed(1)}</b><small>horas totales</small></div>
        <div><b>${dias}</b><small>días de taller</small></div>
        <div><b>${racha()}</b><small>racha 🔥</small></div>
        <div><b>${completas}/${LECCIONES.length}</b><small>lecciones</small></div>
      </section>
      <section class="card"><h3>Últimos 14 días</h3>
        <div class="grafico">${ultimos.map((x) => `<div title="${fecha(x.d)}: ${x.m} min"><span style="height:${Math.round(100 * x.m / maxM)}%"></span><small>${'LMMJVSD'[(x.d.getDay() + 6) % 7]}</small></div>`).join('')}</div>
        <p class="nota">Meta diaria: ${fmtMin(p.minDia)} · ${p.diasSemana} días por semana</p></section>
      <section class="card"><h3>Camino a profesional</h3>
        ${CURRICULUM.map((n) => `<div class="fila"><span>${n.icono} ${esc(n.nombre)}</span><span class="nota">${S.niveles[n.id] && S.niveles[n.id].aprobado ? '🎓 aprobado' : fmtSemanas(semanasPara(n.id))}</span></div>${barra(porcentajeNivel(n.id))}`).join('')}
        <p class="nota">Estimación según tu ritmo actual. Si practicas más días, el plan se acorta automáticamente.</p></section>
      ${certs.length ? `<section class="card"><h3>🎓 Certificados</h3>${certs.map((n) => `
        <div class="certificado"><small>Maestro Carpintero certifica que</small><b>${esc(p.nombre)}</b>
        <small>aprobó el nivel</small><b>${esc(n.nombre)} — ${esc(n.titulo)}</b>
        <small>${S.niveles[n.id].fecha} · nota ${S.niveles[n.id].nota} %${S.niveles[n.id].convalidado ? ' · por convalidación' : ''}</small></div>`).join('')}
        <p class="nota">Este certificado es de autoaprendizaje. Para acreditar formalmente tu oficio, busca certificación de competencias laborales o cursos técnicos reconocidos.</p></section>` : ''}
      ${(S.notas || []).length ? `<section class="card"><h3>📓 Libreta de taller</h3><ul>${S.notas.slice(0, 10).map((x) => `<li><small>${x.f}</small> ${esc(x.t)}</li>`).join('')}</ul></section>` : ''}
      <form class="card" id="fAj"><h3>⚙️ Ajustes</h3>
        <label>Nombre<input name="nombre" value="${esc(p.nombre)}" maxlength="30"></label>
        <label>Tiempo diario: <b id="vMin">${fmtMin(p.minDia)}</b><input type="range" name="minDia" min="30" max="120" step="15" value="${p.minDia}"></label>
        <label>Días por semana<select name="diasSemana">${[3, 4, 5, 6, 7].map((d) => `<option ${d === p.diasSemana ? 'selected' : ''}>${d}</option>`).join('')}</select></label>
        <button class="btn primario">Guardar y recalcular plan</button>
        <button type="button" class="btn peligro" id="bReset">Borrar todo mi progreso</button>
      </form>`, 'Progreso', false);
    const f = $('#fAj');
    f.minDia.oninput = () => { $('#vMin').textContent = fmtMin(+f.minDia.value); };
    f.onsubmit = (e) => {
      e.preventDefault();
      Object.assign(p, { nombre: f.nombre.value.trim() || p.nombre, minDia: +f.minDia.value, diasSemana: +f.diasSemana.value });
      // conservar lo ya hecho hoy y recalcular lo pendiente
      const hechos = S.plan ? S.plan.bloques.filter((b) => b.hecho) : [];
      generarPlan(); if (hechos.length) S.plan.bloques = hechos.concat(S.plan.bloques);
      guardar(); toast('Plan actualizado'); vistaProgreso();
    };
    $('#bReset').onclick = () => {
      if (confirm('¿Seguro? Se borrará todo tu progreso.')) { S = nuevoEstado(); guardar(); location.hash = ''; render(); }
    };
  }

  // ---------------------------------------------------------------- router
  function render() {
    if (!S.perfil) return vistaBienvenida();
    const [ruta, arg] = location.hash.slice(1).split('/');
    switch (ruta) {
      case 'bloque': return vistaBloque(+arg);
      case 'leccion': return vistaLeccion(arg, null);
      case 'examen': return puedeRendirExamen(arg) ? vistaExamen(arg, null, false) : vistaRuta();
      case 'convalidar': return vistaExamen(arg, null, true);
      case 'ruta': return vistaRuta();
      case 'maestro': return vistaMaestro();
      case 'progreso': return vistaProgreso();
      default: return vistaHoy();
    }
  }
  window.addEventListener('hashchange', render);
  $('#atras').onclick = () => history.back();

  // Llamado por la app Android al presionar "atrás": true si la web lo manejó
  window.manejarAtras = function () {
    const ruta = location.hash.slice(1).split('/')[0];
    if (['bloque', 'leccion', 'examen', 'convalidar'].includes(ruta)) { history.back(); return true; }
    if (ruta && ruta !== 'hoy') { location.hash = '#hoy'; return true; }
    return false;
  };

  // utilidades expuestas para pruebas
  window.__mc = { get estado() { return S; }, generarPlan, LECCIONES };

  render();
})();
