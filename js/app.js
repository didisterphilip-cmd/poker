// UI: onboarding, tabs, guided workout player.
(() => {
  const app = document.getElementById('app');
  const tabbar = document.getElementById('tabbar');
  let currentTab = 'train';
  let planView = Dates.today();
  let streakView = Dates.today();

  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const fmtDate = (d) => `${Dates.WEEKDAYS_LONG[d.getDay()]}, ${d.getDate()} ${Dates.MONTHS[d.getMonth()]}`;
  const fmtClock = (sec) => `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;

  // ---------------------------------------------------------------- onboarding
  const STEPS = ['account', 'body', 'schedule', 'training'];

  function startOnboarding(existing) {
    const draft = existing ? { ...existing, trainingDays: [...existing.trainingDays] } : {
      name: '', email: '', age: '', sex: '', weight: '', height: '',
      perWeek: 3, trainingDays: [1, 3, 5], restSeconds: 60, level: '',
    };
    tabbar.hidden = true;
    renderStep(draft, 0, !!existing);
  }

  function renderStep(draft, i, editing) {
    const step = STEPS[i];
    const progress = `<div class="progress"><div style="width:${((i + 1) / STEPS.length) * 100}%"></div></div>`;
    const back = i > 0 ? '<button type="button" class="btn ghost" data-back>Back</button>' : '';
    const nextLabel = i === STEPS.length - 1 ? (editing ? 'Save' : "Let's go") : 'Next';
    let body = '';

    if (step === 'account') {
      body = `
        <h1 class="brand">Kettle<span>Fire</span></h1>
        <h2>${editing ? 'Your account' : 'Create your account'}</h2>
        <p class="muted">Your details stay on this device.</p>
        <label>Full name<input name="name" required autocomplete="name" value="${esc(draft.name)}"></label>
        <label>Email<input name="email" type="email" required autocomplete="email" value="${esc(draft.email)}"></label>`;
    } else if (step === 'body') {
      body = `
        <h2>About you</h2>
        <p class="muted">Used to size your workouts and suggest a kettlebell weight.</p>
        <div class="row2">
          <label>Age<input name="age" type="number" min="10" max="100" required inputmode="numeric" value="${esc(draft.age)}"></label>
          <label>Sex<select name="sex" required>
            <option value="" disabled ${!draft.sex ? 'selected' : ''}>Select</option>
            <option value="male" ${draft.sex === 'male' ? 'selected' : ''}>Male</option>
            <option value="female" ${draft.sex === 'female' ? 'selected' : ''}>Female</option>
          </select></label>
        </div>
        <div class="row2">
          <label>Weight (kg)<input name="weight" type="number" min="25" max="300" step="0.1" required inputmode="decimal" value="${esc(draft.weight)}"></label>
          <label>Height (cm)<input name="height" type="number" min="100" max="250" required inputmode="numeric" value="${esc(draft.height)}"></label>
        </div>`;
    } else if (step === 'schedule') {
      body = `
        <h2>Your schedule</h2>
        <p class="muted">How many workouts a week?</p>
        <div class="chips" data-group="perWeek">
          ${[1, 2, 3, 4, 5, 6, 7].map((n) => `<button type="button" class="chip ${draft.perWeek === n ? 'on' : ''}" data-val="${n}">${n}</button>`).join('')}
        </div>
        <p class="muted">Which days can you train? <span id="dayHint"></span></p>
        <div class="chips days" data-group="days">
          ${[1, 2, 3, 4, 5, 6, 0].map((d) => `<button type="button" class="chip ${draft.trainingDays.includes(d) ? 'on' : ''}" data-val="${d}">${Dates.WEEKDAYS[d]}</button>`).join('')}
        </div>
        <p class="error" id="err"></p>`;
    } else if (step === 'training') {
      body = `
        <h2>Your training</h2>
        <p class="muted">Rest between exercises</p>
        <div class="chips" data-group="rest">
          ${[30, 45, 60, 90, 120].map((s) => `<button type="button" class="chip ${draft.restSeconds === s ? 'on' : ''}" data-val="${s}">${s < 60 ? `${s}s` : `${s / 60} min`}</button>`).join('')}
        </div>
        <p class="muted">How is your shape right now?</p>
        <div class="levels" data-group="level">
          ${Object.entries(Workouts.LEVELS).map(([key, l]) => `
            <button type="button" class="level ${draft.level === key ? 'on' : ''}" data-val="${key}">
              <strong>${l.label}</strong>
              <span>${l.rounds} rounds · ${key === 'beginner' ? 'fewer reps, lighter bell' : key === 'advanced' ? 'more reps, heavier bell' : 'standard reps'}</span>
            </button>`).join('')}
        </div>
        <p class="error" id="err"></p>`;
    }

    app.innerHTML = `
      <form class="screen onboard" novalidate>
        ${progress}
        ${body}
        <div class="actions">${back}<button class="btn primary">${nextLabel}</button></div>
      </form>`;

    const form = app.querySelector('form');
    const err = form.querySelector('#err');

    // Chip groups
    form.querySelectorAll('[data-group]').forEach((group) => {
      group.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-val]');
        if (!btn) return;
        const val = btn.dataset.val;
        const g = group.dataset.group;
        if (g === 'days') {
          const d = Number(val);
          const idx = draft.trainingDays.indexOf(d);
          if (idx >= 0) draft.trainingDays.splice(idx, 1);
          else draft.trainingDays.push(d);
          btn.classList.toggle('on');
        } else {
          group.querySelectorAll('[data-val]').forEach((b) => b.classList.remove('on'));
          btn.classList.add('on');
          if (g === 'perWeek') draft.perWeek = Number(val);
          if (g === 'rest') draft.restSeconds = Number(val);
          if (g === 'level') draft.level = val;
        }
        updateDayHint();
      });
    });

    function updateDayHint() {
      const hint = form.querySelector('#dayHint');
      if (hint) hint.textContent = `(${draft.trainingDays.length}/${draft.perWeek} picked)`;
    }
    updateDayHint();

    form.querySelector('[data-back]')?.addEventListener('click', () => {
      collectInputs(form, draft);
      renderStep(draft, i - 1, editing);
    });

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      collectInputs(form, draft);
      const inputs = [...form.querySelectorAll('input, select')];
      const invalid = inputs.find((el) => !el.checkValidity());
      if (invalid) { invalid.reportValidity(); return; }
      if (step === 'schedule' && draft.trainingDays.length !== draft.perWeek) {
        err.textContent = `Pick exactly ${draft.perWeek} day${draft.perWeek > 1 ? 's' : ''}.`;
        return;
      }
      if (step === 'training' && !draft.level) {
        err.textContent = 'Choose how in shape you are.';
        return;
      }
      if (i < STEPS.length - 1) {
        renderStep(draft, i + 1, editing);
      } else {
        draft.trainingDays.sort((a, b) => a - b);
        draft.createdAt = draft.createdAt || new Date().toISOString();
        Store.setProfile(draft);
        showMain('train');
      }
    });
  }

  function collectInputs(form, draft) {
    form.querySelectorAll('input, select').forEach((el) => {
      draft[el.name] = el.type === 'number' ? (el.value === '' ? '' : Number(el.value)) : el.value.trim();
    });
  }

  // ---------------------------------------------------------------- main tabs
  function showMain(tab) {
    currentTab = tab;
    tabbar.hidden = false;
    tabbar.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === tab));
    if (tab === 'train') renderTrain();
    if (tab === 'calendar') renderCalendar();
    if (tab === 'profile') renderProfile();
    window.scrollTo(0, 0);
  }

  tabbar.addEventListener('click', (e) => {
    const btn = e.target.closest('.tab');
    if (btn) showMain(btn.dataset.tab);
  });

  function nextWorkout(profile) {
    return Workouts.forSessionNumber(profile, Store.sessions.length);
  }

  function renderTrain() {
    const p = Store.profile;
    const today = Dates.today();
    const doneToday = Cal.completedSet().has(Dates.key(today));
    const isTrainingDay = Cal.isTrainingDay(p, today);
    const streak = Cal.streak(p);
    const w = nextWorkout(p);
    const next = Cal.nextTrainingDay(p, Dates.addDays(today, doneToday ? 1 : 0));

    let status;
    if (doneToday) status = `<div class="banner ok">Workout done for today 🔥 Next: <strong>${next ? fmtDate(next) : '—'}</strong></div>`;
    else if (isTrainingDay) status = '<div class="banner">Today is a training day 💪</div>';
    else status = `<div class="banner rest">Rest day. Next workout: <strong>${next ? fmtDate(next) : '—'}</strong></div>`;

    app.innerHTML = `
      <section class="screen">
        <header class="top">
          <div>
            <p class="muted small">Hi ${esc(p.name.split(' ')[0])}</p>
            <h1 class="brand">Kettle<span>Fire</span></h1>
          </div>
          <div class="streak-pill" title="Current streak">🔥 ${streak}</div>
        </header>
        ${status}
        <article class="card workout">
          <p class="eyebrow">${doneToday || !isTrainingDay ? 'Next workout' : "Today's workout"}</p>
          <h2>${w.title}</h2>
          <p class="focus">${w.focus}</p>
          <p>${w.intro}</p>
          <div class="stats">
            <div><strong>~${w.minutes}</strong><span>min</span></div>
            <div><strong>${w.rounds}</strong><span>rounds</span></div>
            <div><strong>${w.rest}s</strong><span>rest</span></div>
            <div><strong>${w.bell} kg</strong><span>bell</span></div>
          </div>
          <h3>Exercises</h3>
          <ol class="ex-list">
            ${w.items.map((ex) => `
              <li>
                <details>
                  <summary><span>${ex.name}</span><span class="reps">${ex.reps}${ex.perSide ? '/side' : ''}</span></summary>
                  <p>${ex.desc}</p>
                </details>
              </li>`).join('')}
          </ol>
          <p class="muted small">Warm up for 5 minutes first: march in place, hip circles, and a few light swings. Do all exercises in order, then repeat for ${w.rounds} rounds.</p>
          <button class="btn primary block" id="start">${doneToday ? 'Train again' : isTrainingDay ? 'Start workout' : 'Train anyway'}</button>
        </article>
      </section>`;

    app.querySelector('#start').addEventListener('click', () => startPlayer(w));
  }

  function renderCalendar() {
    const p = Store.profile;
    const today = Dates.today();
    const done = Cal.completedSet();
    const next = Cal.nextTrainingDay(p);
    const streak = Cal.streak(p);

    app.innerHTML = `
      <section class="screen">
        <h1 class="page-title">Calendar</h1>
        <article class="card">
          <p class="eyebrow">Next workout</p>
          <h2>${next ? fmtDate(next) : 'No upcoming days'}</h2>
          <p class="muted small">Training days: ${p.trainingDays.map((d) => Dates.WEEKDAYS[d]).join(', ')}</p>
          <div id="planCal" class="cal"></div>
          <div class="legend"><span class="dot plan"></span>Training day <span class="dot next"></span>Next workout</div>
        </article>
        <article class="card">
          <p class="eyebrow">Your streak</p>
          <h2 class="streak-big">🔥 ${streak} <small>workout${streak === 1 ? '' : 's'} in a row</small></h2>
          <div id="streakCal" class="cal"></div>
          <div class="legend"><span class="dot fire"></span>Workout completed · ${Store.sessions.length} total</div>
        </article>
      </section>`;

    const nextKey = next ? Dates.key(next) : null;
    const drawPlan = () => Cal.render(app.querySelector('#planCal'), planView, (d) => {
      const k = Dates.key(d);
      if (k === nextKey) return { cls: 'next', mark: '💪' };
      if (done.has(k)) return { cls: 'done-plain', mark: '✓' };
      if (d >= today && Cal.isTrainingDay(p, d)) return { cls: 'plan' };
      return {};
    }, (v) => { planView = v; drawPlan(); });

    const drawStreak = () => Cal.render(app.querySelector('#streakCal'), streakView, (d) => {
      const k = Dates.key(d);
      if (done.has(k)) return { cls: 'fire', mark: '🔥' };
      if (d < today && Cal.isTrainingDay(p, d) && d >= new Date(p.createdAt).setHours(0, 0, 0, 0)) return { cls: 'missed' };
      return {};
    }, (v) => { streakView = v; drawStreak(); });

    drawPlan();
    drawStreak();
  }

  function renderProfile() {
    const p = Store.profile;
    const bmi = p.weight && p.height ? (p.weight / ((p.height / 100) ** 2)).toFixed(1) : '—';
    const row = (k, v) => `<div class="kv"><span>${k}</span><strong>${v}</strong></div>`;
    app.innerHTML = `
      <section class="screen">
        <h1 class="page-title">Profile</h1>
        <article class="card">
          <h2>${esc(p.name)}</h2>
          <p class="muted">${esc(p.email)}</p>
          ${row('Age', esc(p.age))}
          ${row('Sex', p.sex === 'male' ? 'Male' : 'Female')}
          ${row('Weight', `${esc(p.weight)} kg`)}
          ${row('Height', `${esc(p.height)} cm`)}
          ${row('BMI', bmi)}
        </article>
        <article class="card">
          ${row('Workouts per week', p.perWeek)}
          ${row('Training days', p.trainingDays.map((d) => Dates.WEEKDAYS[d]).join(', '))}
          ${row('Rest between exercises', `${p.restSeconds}s`)}
          ${row('Shape', Workouts.LEVELS[p.level].label)}
          ${row('Workouts completed', Store.sessions.length)}
        </article>
        <button class="btn primary block" id="edit">Edit profile & plan</button>
        <button class="btn ghost block" id="reset">Log out & delete data</button>
      </section>`;
    app.querySelector('#edit').addEventListener('click', () => startOnboarding(Store.profile));
    app.querySelector('#reset').addEventListener('click', () => {
      if (confirm('This deletes your profile and workout history from this device. Continue?')) {
        Store.reset();
        startOnboarding(null);
      }
    });
  }

  // ---------------------------------------------------------------- player
  function startPlayer(w) {
    tabbar.hidden = true;
    const steps = [];
    for (let r = 1; r <= w.rounds; r++) {
      w.items.forEach((ex, i) => steps.push({ ex, round: r, idx: i }));
    }
    const started = Date.now();
    let pos = 0;
    let restTimer = null;
    let clockTimer = setInterval(updateClock, 1000);
    let wakeLock = null;
    navigator.wakeLock?.request('screen').then((l) => { wakeLock = l; }).catch(() => {});

    function cleanup() {
      clearInterval(clockTimer);
      clearInterval(restTimer);
      wakeLock?.release().catch(() => {});
    }

    function updateClock() {
      const el = app.querySelector('#clock');
      if (el) el.textContent = fmtClock(Math.floor((Date.now() - started) / 1000));
    }

    function shell(inner) {
      app.innerHTML = `
        <section class="player">
          <header class="player-top">
            <button class="icon-btn" id="quit" aria-label="Quit workout">✕</button>
            <div class="muted">${w.title}</div>
            <div id="clock" class="clock">0:00</div>
          </header>
          <div class="progress"><div style="width:${(pos / steps.length) * 100}%"></div></div>
          ${inner}
        </section>`;
      updateClock();
      app.querySelector('#quit').addEventListener('click', () => {
        if (confirm('Quit this workout? It will not be saved.')) { cleanup(); showMain('train'); }
      });
    }

    function work() {
      const { ex, round, idx } = steps[pos];
      shell(`
        <div class="player-body">
          <p class="eyebrow">Round ${round} of ${w.rounds} · Exercise ${idx + 1} of ${w.items.length}</p>
          <h2 class="ex-name">${ex.name}</h2>
          <div class="big-reps">${ex.reps}<small>${ex.perSide ? 'reps / side' : 'reps'}</small></div>
          <p class="ex-desc">${ex.desc}</p>
        </div>
        <button class="btn primary block huge" id="done">Done ✓</button>`);
      app.querySelector('#done').addEventListener('click', () => {
        pos++;
        if (pos >= steps.length) finish();
        else rest();
      });
    }

    function rest() {
      const roundBreak = steps[pos].idx === 0;
      let left = roundBreak ? w.rest * 2 : w.rest;
      const upcoming = steps[pos];
      shell(`
        <div class="player-body center">
          <p class="eyebrow">${roundBreak ? `Round ${upcoming.round - 1} complete` : 'Rest'}</p>
          <div class="countdown" id="cd">${fmtClock(left)}</div>
          <p class="muted">Next up</p>
          <h3>${upcoming.ex.name} · ${upcoming.ex.reps}${upcoming.ex.perSide ? '/side' : ''}</h3>
        </div>
        <button class="btn ghost block" id="skip">Skip rest</button>`);
      app.querySelector('#skip').addEventListener('click', () => { clearInterval(restTimer); work(); });
      restTimer = setInterval(() => {
        left--;
        const cd = app.querySelector('#cd');
        if (cd) cd.textContent = fmtClock(Math.max(0, left));
        if (left <= 3 && left > 0) beep(600);
        if (left <= 0) {
          clearInterval(restTimer);
          beep(900);
          navigator.vibrate?.(300);
          work();
        }
      }, 1000);
    }

    function finish() {
      const durationSec = Math.floor((Date.now() - started) / 1000);
      cleanup();
      Store.addSession({
        date: Dates.key(Dates.today()),
        workoutId: w.id,
        durationSec,
        completedAt: new Date().toISOString(),
      });
      const streak = Cal.streak(Store.profile);
      app.innerHTML = `
        <section class="player center finish">
          <div class="fire-big">🔥</div>
          <h1>Workout complete!</h1>
          <p class="muted">${w.title} · ${fmtClock(durationSec)}</p>
          <div class="streak-pill large">🔥 ${streak} streak</div>
          <button class="btn primary block" id="home">Back to home</button>
        </section>`;
      app.querySelector('#home').addEventListener('click', () => showMain('train'));
    }

    work();
  }

  let audioCtx;
  function beep(freq) {
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      const o = audioCtx.createOscillator();
      const g = audioCtx.createGain();
      o.frequency.value = freq;
      g.gain.setValueAtTime(0.2, audioCtx.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.25);
      o.connect(g).connect(audioCtx.destination);
      o.start();
      o.stop(audioCtx.currentTime + 0.25);
    } catch (e) { /* audio unavailable */ }
  }

  // ---------------------------------------------------------------- boot
  if (Store.profile) showMain('train');
  else startOnboarding(null);

  if ('serviceWorker' in navigator && location.protocol !== 'file:') {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
})();
