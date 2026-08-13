/* ניהול אירועים ומלצרים — לוגיקת האפליקציה
   כל הנתונים נשמרים מקומית במכשיר (localStorage). אין שרת, אין אינטרנט נדרש. */
'use strict';

const STORAGE_KEY = 'events_app_v1';

/* ---------- State ---------- */
let events = load();
let editingId = null;
let calCursor = new Date(); // month currently shown in calendar
let calSelectedKey = null;  // "YYYY-MM-DD" selected in calendar

/* ---------- Storage ---------- */
function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error('load failed', e);
    return [];
  }
}
function save() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
  } catch (e) {
    alert('שגיאה בשמירה: ייתכן שאין מקום פנוי במכשיר.');
    console.error(e);
  }
}

/* ---------- Helpers ---------- */
const $ = (id) => document.getElementById(id);
const HE_MONTHS = ['ינואר','פברואר','מרץ','אפריל','מאי','יוני','יולי','אוגוסט','ספטמבר','אוקטובר','נובמבר','דצמבר'];
const HE_MONTHS_SHORT = ['ינו','פבר','מרץ','אפר','מאי','יונ','יול','אוג','ספט','אוק','נוב','דצמ'];
const HE_DOW = ['א','ב','ג','ד','ה','ו','ש'];
const HE_DOW_LONG = ['ראשון','שני','שלישי','רביעי','חמישי','שישי','שבת'];

function uid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 7); }
function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function num(v) { const n = parseFloat(v); return isNaN(n) ? 0 : n; }
function money(n) {
  const v = num(n);
  return '₪' + v.toLocaleString('he-IL', { maximumFractionDigits: 2 });
}
function parseDateKey(key) {
  if (!key) return null;
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d);
}
function dateKey(dt) {
  return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' + String(dt.getDate()).padStart(2, '0');
}
function todayKey() { return dateKey(new Date()); }
function formatDateHe(key) {
  const dt = parseDateKey(key);
  if (!dt) return '';
  return HE_DOW_LONG[dt.getDay()] + ', ' + dt.getDate() + ' ב' + HE_MONTHS[dt.getMonth()] + ' ' + dt.getFullYear();
}

function toast(msg) {
  const t = $('toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast._t);
  toast._t = setTimeout(() => t.classList.remove('show'), 2200);
}

/* ---------- Confirm modal ---------- */
function confirmDialog(text, okLabel = 'מחק') {
  return new Promise((resolve) => {
    $('modal-text').textContent = text;
    $('modal-ok').textContent = okLabel;
    $('modal').classList.add('show');
    const cleanup = (result) => {
      $('modal').classList.remove('show');
      $('modal-ok').onclick = null;
      $('modal-cancel').onclick = null;
      resolve(result);
    };
    $('modal-ok').onclick = () => cleanup(true);
    $('modal-cancel').onclick = () => cleanup(false);
  });
}

/* ================= VIEW SWITCHING ================= */
const VIEW_TITLES = { list: 'העבודות שלי', form: 'עבודה חדשה', calendar: 'לוח שנה' };
function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  $('view-' + name).classList.add('active');
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.view === name));
  $('header-title').textContent = VIEW_TITLES[name] || '';
  window.scrollTo(0, 0);
  if (name === 'list') renderList();
  if (name === 'calendar') renderCalendar();
}
document.querySelectorAll('.nav-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    if (btn.dataset.view === 'form' && !editingId) resetForm();
    showView(btn.dataset.view);
  });
});

/* ================= FORM ================= */
function selectOption(selectEl, otherInputEl, value) {
  // set a select whose value might be a custom "אחר" value
  const opts = Array.from(selectEl.options).map(o => o.value);
  if (value && !opts.includes(value)) {
    selectEl.value = '__other__';
    otherInputEl.value = value;
    otherInputEl.classList.remove('hidden');
  } else {
    selectEl.value = value || '';
    otherInputEl.value = '';
    otherInputEl.classList.toggle('hidden', value !== '__other__');
  }
}
function readSelect(selectEl, otherInputEl) {
  if (selectEl.value === '__other__') return otherInputEl.value.trim();
  return selectEl.value;
}

function wireOtherToggle(selectId, otherId) {
  $(selectId).addEventListener('change', () => {
    $(otherId).classList.toggle('hidden', $(selectId).value !== '__other__');
    updateCalc();
  });
}
wireOtherToggle('f-eventtype', 'f-eventtype-other');
wireOtherToggle('f-hall', 'f-hall-other');
wireOtherToggle('f-manager', 'f-manager-other');

// Waiter name boxes react to count
function renderWaiterNames(count, existing = []) {
  const c = $('waiter-names-container');
  // preserve currently typed values
  const current = Array.from(c.querySelectorAll('input')).map(i => i.value);
  const values = existing.length ? existing : current;
  c.innerHTML = '';
  count = Math.max(0, Math.min(60, count | 0));
  $('waiter-names-hint').classList.toggle('hidden', count > 0);
  for (let i = 0; i < count; i++) {
    const row = document.createElement('div');
    row.className = 'waiter-name-row';
    row.innerHTML = `<span class="idx">${i + 1}</span>` +
      `<input type="text" placeholder="שם מלצר ${i + 1}" value="${esc(values[i] || '')}" />`;
    c.appendChild(row);
  }
}
$('f-numwaiters').addEventListener('input', () => {
  renderWaiterNames(num($('f-numwaiters').value));
  updateCalc();
});

// Special waiter toggle
$('f-special-on').addEventListener('change', () => {
  $('special-fields').classList.toggle('hidden', !$('f-special-on').checked);
  updateCalc();
});

// Live cost calculator
['f-esthours', 'f-numwaiters', 'f-waiterrate', 'f-special-rate', 'f-total'].forEach(id => {
  $(id).addEventListener('input', updateCalc);
});
function updateCalc() {
  const hours = num($('f-esthours').value);
  const nw = num($('f-numwaiters').value);
  const rate = num($('f-waiterrate').value);
  const specialOn = $('f-special-on').checked;
  const specialRate = specialOn ? num($('f-special-rate').value) : 0;
  const total = num($('f-total').value);

  const waitersCost = nw * rate * hours;
  const specialCost = specialRate * hours;
  const cost = waitersCost + specialCost;
  const box = $('calc-box');

  if (!hours || (!cost && !total)) { box.classList.add('hidden'); box.innerHTML = ''; return; }
  box.classList.remove('hidden');
  const profit = total - cost;
  let html = '';
  if (waitersCost) html += `<div class="line"><span>עלות מלצרים (${nw} × ${money(rate)} × ${hours} ש')</span><span>${money(waitersCost)}</span></div>`;
  if (specialCost) html += `<div class="line"><span>מלצר מיוחד (${money(specialRate)} × ${hours} ש')</span><span>${money(specialCost)}</span></div>`;
  html += `<div class="line total"><span>סה"כ עלות משוערת</span><span>${money(cost)}</span></div>`;
  if (total) {
    html += `<div class="line"><span>סכום מהלקוח</span><span>${money(total)}</span></div>`;
    html += `<div class="line total"><span>רווח משוער</span><span class="${profit >= 0 ? 'profit-pos' : 'profit-neg'}">${money(profit)}</span></div>`;
  }
  box.innerHTML = html;
}

function resetForm() {
  editingId = null;
  $('job-form').reset();
  $('f-id').value = '';
  ['f-eventtype-other', 'f-hall-other', 'f-manager-other'].forEach(id => $(id).classList.add('hidden'));
  $('special-fields').classList.add('hidden');
  $('waiter-names-container').innerHTML = '';
  $('waiter-names-hint').classList.remove('hidden');
  $('calc-box').classList.add('hidden');
  $('form-mode-title').textContent = 'רישום עבודה חדשה';
  $('btn-save').textContent = 'שמור עבודה';
  $('btn-cancel-edit').classList.add('hidden');
}

function fillForm(ev) {
  editingId = ev.id;
  $('f-id').value = ev.id;
  $('f-client').value = ev.client || '';
  $('f-date').value = ev.date || '';
  $('f-daytype').value = ev.dayType || '';
  $('f-guests').value = ev.guests ?? '';
  selectOption($('f-eventtype'), $('f-eventtype-other'), ev.eventType || '');
  $('f-esthours').value = ev.estHours ?? '';
  $('f-location').value = ev.location || '';
  selectOption($('f-hall'), $('f-hall-other'), ev.hallType || '');
  $('f-plates').value = ev.plateType || '';
  selectOption($('f-manager'), $('f-manager-other'), ev.manager || '');
  $('f-numwaiters').value = ev.numWaiters ?? '';
  $('f-waiterrate').value = ev.waiterRate ?? '';
  $('f-special-on').checked = !!ev.specialOn;
  $('special-fields').classList.toggle('hidden', !ev.specialOn);
  $('f-special-name').value = ev.specialName || '';
  $('f-special-rate').value = ev.specialRate ?? '';
  renderWaiterNames(num(ev.numWaiters), ev.waiterNames || []);
  $('f-total').value = ev.total ?? '';
  $('f-paid').checked = !!ev.paid;
  $('f-notes').value = ev.notes || '';
  updateCalc();
  $('form-mode-title').textContent = 'עריכת עבודה';
  $('btn-save').textContent = 'עדכן עבודה';
  $('btn-cancel-edit').classList.remove('hidden');
}

$('btn-cancel-edit').addEventListener('click', () => {
  resetForm();
  showView('list');
});

$('job-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const client = $('f-client').value.trim();
  const date = $('f-date').value;
  if (!client && !date) {
    toast('נא למלא לפחות שם לקוח או תאריך');
    return;
  }
  const waiterNames = Array.from($('waiter-names-container').querySelectorAll('input'))
    .map(i => i.value.trim());
  const record = {
    id: editingId || uid(),
    client,
    date,
    dayType: $('f-daytype').value,
    eventType: readSelect($('f-eventtype'), $('f-eventtype-other')),
    guests: $('f-guests').value,
    estHours: $('f-esthours').value,
    location: $('f-location').value.trim(),
    hallType: readSelect($('f-hall'), $('f-hall-other')),
    plateType: $('f-plates').value,
    manager: readSelect($('f-manager'), $('f-manager-other')),
    numWaiters: $('f-numwaiters').value,
    waiterRate: $('f-waiterrate').value,
    specialOn: $('f-special-on').checked,
    specialName: $('f-special-name').value.trim(),
    specialRate: $('f-special-rate').value,
    waiterNames,
    total: $('f-total').value,
    paid: $('f-paid').checked,
    notes: $('f-notes').value.trim(),
    createdAt: editingId ? (events.find(e => e.id === editingId)?.createdAt || Date.now()) : Date.now(),
  };

  if (editingId) {
    const i = events.findIndex(e => e.id === editingId);
    if (i >= 0) events[i] = record;
    toast('העבודה עודכנה ✓');
  } else {
    events.push(record);
    toast('העבודה נשמרה ✓');
  }
  save();
  resetForm();
  showView('list');
});

/* ================= LIST ================= */
function eventsSorted(mode) {
  const arr = events.slice();
  const tk = todayKey();
  if (mode === 'date-asc') arr.sort((a, b) => (a.date || '').localeCompare(b.date || ''));
  else if (mode === 'upcoming') {
    arr.sort((a, b) => {
      const au = (a.date || '') >= tk, bu = (b.date || '') >= tk;
      if (au !== bu) return au ? -1 : 1;              // upcoming first
      if (au) return (a.date || '').localeCompare(b.date || ''); // nearest upcoming first
      return (b.date || '').localeCompare(a.date || '');         // recent past first
    });
  } else arr.sort((a, b) => (b.date || '').localeCompare(a.date || '')); // date-desc default
  return arr;
}

function renderList(openId = null) {
  const q = $('search-input').value.trim().toLowerCase();
  const mode = $('sort-select').value;
  let list = eventsSorted(mode);
  if (q) {
    list = list.filter(ev =>
      [ev.client, ev.location, ev.eventType, ev.manager, ev.dayType, (ev.waiterNames || []).join(' ')]
        .join(' ').toLowerCase().includes(q));
  }
  const c = $('list-container');
  if (list.length === 0) {
    c.innerHTML = `<div class="empty"><div class="big">🍽️</div>` +
      (events.length === 0
        ? `עדיין לא רשמת עבודות.<br>לחץ על <b>עבודה חדשה</b> כדי להתחיל.`
        : `לא נמצאו עבודות שמתאימות לחיפוש.`) + `</div>`;
    return;
  }
  c.innerHTML = list.map(renderEventCard).join('');
  // wire up
  c.querySelectorAll('.event-head').forEach(h => {
    h.addEventListener('click', () => h.closest('.event-card').classList.toggle('open'));
  });
  c.querySelectorAll('[data-edit]').forEach(b =>
    b.addEventListener('click', (e) => { e.stopPropagation(); startEdit(b.dataset.edit); }));
  c.querySelectorAll('[data-del]').forEach(b =>
    b.addEventListener('click', async (e) => {
      e.stopPropagation();
      const ev = events.find(x => x.id === b.dataset.del);
      const ok = await confirmDialog(`למחוק את העבודה של "${ev?.client || 'ללא שם'}"?`);
      if (ok) { events = events.filter(x => x.id !== b.dataset.del); save(); renderList(); toast('נמחק'); }
    }));

  if (openId) {
    const card = c.querySelector(`.event-card[data-id="${openId}"]`);
    if (card) {
      card.classList.add('open');
      card.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}

function calChip(dateStr) {
  const dt = parseDateKey(dateStr);
  if (!dt) return `<div class="cal-chip"><div class="d">–</div><div class="m">?</div></div>`;
  return `<div class="cal-chip"><div class="d">${dt.getDate()}</div><div class="m">${HE_MONTHS_SHORT[dt.getMonth()]}</div></div>`;
}

function detailItem(k, v, full = false) {
  const empty = v === '' || v == null;
  return `<div class="detail-item ${full ? 'full' : ''}"><div class="k">${esc(k)}</div>` +
    `<div class="v ${empty ? 'empty-v' : ''}">${empty ? '—' : esc(v)}</div></div>`;
}

function renderEventCard(ev) {
  const tags = [];
  if (ev.eventType) tags.push(`<span class="tag type">${esc(ev.eventType)}</span>`);
  if (ev.dayType) tags.push(`<span class="tag ${ev.dayType === 'שבת' ? 'shabbat' : ''}">${esc(ev.dayType)}</span>`);
  if (num(ev.total) > 0) tags.push(`<span class="tag ${ev.paid ? 'paid' : ''}">${ev.paid ? 'שולם' : 'לא שולם'} · ${money(ev.total)}</span>`);

  const metaBits = [];
  if (ev.location) metaBits.push('📍 ' + esc(ev.location));
  if (ev.guests) metaBits.push('👥 ' + esc(ev.guests));

  // waiter list
  let waitersHtml = '';
  const names = (ev.waiterNames || []).filter(Boolean);
  if (names.length || ev.numWaiters || ev.specialOn) {
    waitersHtml = `<div class="detail-item full"><div class="k">מלצרים${ev.numWaiters ? ' (' + esc(ev.numWaiters) + ')' : ''}</div><ul class="waiters-list">`;
    if (names.length) names.forEach(n => waitersHtml += `<li>• ${esc(n)}</li>`);
    else if (ev.numWaiters) waitersHtml += `<li class="empty-v">${esc(ev.numWaiters)} מלצרים (ללא שמות)</li>`;
    if (ev.specialOn && (ev.specialName || ev.specialRate))
      waitersHtml += `<li><span class="star">★</span> ${esc(ev.specialName || 'מלצר מיוחד')}${ev.specialRate ? ' — ' + money(ev.specialRate) + ' לשעה' : ''}</li>`;
    waitersHtml += `</ul></div>`;
  }

  return `<div class="event-card" data-id="${ev.id}">
    <div class="event-head">
      ${calChip(ev.date)}
      <div class="info">
        <div class="name">${esc(ev.client || 'ללא שם')}</div>
        <div class="meta">${ev.date ? formatDateHe(ev.date) : 'ללא תאריך'}${metaBits.length ? ' · ' + metaBits.join(' · ') : ''}</div>
        ${tags.length ? `<div class="tags">${tags.join('')}</div>` : ''}
      </div>
      <span class="chevron">▾</span>
    </div>
    <div class="event-body">
      <div class="detail-grid">
        ${detailItem('שם הלקוח', ev.client)}
        ${detailItem('תאריך', ev.date ? formatDateHe(ev.date) : '')}
        ${detailItem('שבת / יום חול', ev.dayType)}
        ${detailItem('סוג האירוע', ev.eventType)}
        ${detailItem('כמות אורחים', ev.guests)}
        ${detailItem('שעות משוערות', ev.estHours)}
        ${detailItem('מיקום', ev.location, true)}
        ${detailItem('סוג המקום', ev.hallType)}
        ${detailItem('סוג צלחות', ev.plateType)}
        ${detailItem('מנהל האירוע', ev.manager)}
        ${detailItem('תשלום למלצר לשעה', ev.waiterRate ? money(ev.waiterRate) : '')}
        ${waitersHtml}
        ${detailItem('סכום מהלקוח', ev.total ? money(ev.total) : '')}
        ${detailItem('סטטוס תשלום', ev.total ? (ev.paid ? 'שולם ✓' : 'טרם שולם') : '')}
        ${ev.notes ? detailItem('הערות', ev.notes, true) : ''}
      </div>
      <div class="detail-actions">
        <button class="btn secondary" data-edit="${ev.id}">✏️ עריכה</button>
        <button class="btn danger" data-del="${ev.id}">🗑️ מחיקה</button>
      </div>
    </div>
  </div>`;
}

function startEdit(id) {
  const ev = events.find(e => e.id === id);
  if (!ev) return;
  fillForm(ev);
  showView('form');
}

$('search-input').addEventListener('input', () => renderList());
$('sort-select').addEventListener('change', () => renderList());

/* ================= CALENDAR ================= */
function eventsByDay() {
  const map = {};
  events.forEach(ev => { if (ev.date) (map[ev.date] = map[ev.date] || []).push(ev); });
  return map;
}

function renderCalendar() {
  const y = calCursor.getFullYear(), m = calCursor.getMonth();
  $('cal-title').textContent = HE_MONTHS[m] + ' ' + y;

  // day-of-week header (Sunday-first; RTL grid puts Sunday on the right)
  $('cal-dow-row').innerHTML = HE_DOW.map(d => `<div class="cal-dow">${d}</div>`).join('');

  const first = new Date(y, m, 1);
  const startBlanks = first.getDay();          // 0=Sunday
  const daysInMonth = new Date(y, m + 1, 0).getDate();
  const byDay = eventsByDay();
  const tk = todayKey();

  let cells = '';
  for (let i = 0; i < startBlanks; i++) cells += `<div class="cal-cell blank"></div>`;
  for (let d = 1; d <= daysInMonth; d++) {
    const key = y + '-' + String(m + 1).padStart(2, '0') + '-' + String(d).padStart(2, '0');
    const evs = byDay[key] || [];
    const classes = ['cal-cell'];
    if (key === tk) classes.push('today');
    if (evs.length) classes.push('has-events');
    if (key === calSelectedKey) classes.push('selected');
    const dot = evs.length ? `<span class="dot">${evs.length}</span>` : '';
    cells += `<div class="${classes.join(' ')}" data-key="${key}">${d}${dot}</div>`;
  }
  $('cal-grid').innerHTML = cells;

  $('cal-grid').querySelectorAll('.cal-cell.has-events').forEach(cell => {
    cell.addEventListener('click', () => {
      calSelectedKey = cell.dataset.key;
      renderCalendar();
      renderCalDayEvents(calSelectedKey);
    });
  });

  if (calSelectedKey && calSelectedKey.startsWith(y + '-' + String(m + 1).padStart(2, '0'))) {
    renderCalDayEvents(calSelectedKey);
  } else {
    $('cal-day-events').innerHTML = '';
  }
}

function renderCalDayEvents(key) {
  const evs = (eventsByDay()[key] || []);
  const box = $('cal-day-events');
  if (!evs.length) { box.innerHTML = ''; return; }
  box.innerHTML = `<h3>${formatDateHe(key)} — ${evs.length} אירועים</h3>` +
    evs.map(ev => `<div class="event-card"><div class="event-head" data-goto="${ev.id}">
        ${calChip(ev.date)}
        <div class="info">
          <div class="name">${esc(ev.client || 'ללא שם')}</div>
          <div class="meta">${ev.location ? '📍 ' + esc(ev.location) : ''}${ev.eventType ? ' · ' + esc(ev.eventType) : ''}</div>
        </div>
        <span class="chevron" style="transform:rotate(-90deg)">▾</span>
      </div></div>`).join('');
  box.querySelectorAll('[data-goto]').forEach(h =>
    h.addEventListener('click', () => {
      showView('list');
      renderList(h.dataset.goto);
    }));
}

$('cal-prev').addEventListener('click', () => { calCursor.setMonth(calCursor.getMonth() - 1); renderCalendar(); });
$('cal-next').addEventListener('click', () => { calCursor.setMonth(calCursor.getMonth() + 1); renderCalendar(); });

/* ================= BACKUP / RESTORE ================= */
$('btn-backup').addEventListener('click', async () => {
  const choice = await backupMenu();
  if (choice === 'export') exportData();
  if (choice === 'import') $('import-file').click();
});
function backupMenu() {
  return new Promise((resolve) => {
    $('modal-text').innerHTML = `<b>גיבוי ושחזור</b><br><span style="font-size:14px;color:#64748b">שמור קובץ גיבוי של כל העבודות, או שחזר מקובץ קיים.</span>`;
    const modal = $('modal');
    const row = modal.querySelector('.btn-row');
    row.innerHTML = `<button class="btn" id="mb-export">⬇️ שמור גיבוי</button>
                     <button class="btn secondary" id="mb-import">⬆️ שחזר</button>`;
    modal.classList.add('show');
    const done = (v) => {
      modal.classList.remove('show');
      row.innerHTML = `<button class="btn secondary" id="modal-cancel">ביטול</button><button class="btn danger" id="modal-ok">מחק</button>`;
      resolve(v);
    };
    $('mb-export').onclick = () => done('export');
    $('mb-import').onclick = () => done('import');
    modal.onclick = (e) => { if (e.target === modal) done(null); };
  });
}
function exportData() {
  const blob = new Blob([JSON.stringify(events, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'גיבוי-אירועים-' + todayKey() + '.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  toast('נשמר קובץ גיבוי');
}
$('import-file').addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = async () => {
    try {
      const data = JSON.parse(reader.result);
      if (!Array.isArray(data)) throw new Error('bad format');
      const ok = await confirmDialog(`הקובץ מכיל ${data.length} עבודות. לשחזר ולהחליף את הנתונים הנוכחיים?`, 'שחזר');
      if (ok) {
        events = data;
        save();
        toast('שוחזר בהצלחה');
        showView('list');
      }
    } catch (err) {
      toast('קובץ לא תקין');
      console.error(err);
    }
    $('import-file').value = '';
  };
  reader.readAsText(file, 'utf-8');
});

/* ================= INIT ================= */
resetForm();
showView('list');

// register service worker for offline use
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch(err => console.warn('SW failed', err));
  });
}
