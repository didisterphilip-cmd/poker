// Month calendar rendering plus schedule / streak logic.
const Cal = (() => {
  function isTrainingDay(profile, date) {
    return profile.trainingDays.includes(date.getDay());
  }

  function completedSet() {
    return new Set(Store.sessions.map((s) => s.date));
  }

  function nextTrainingDay(profile, from = Dates.today()) {
    const done = completedSet();
    for (let i = 0; i < 14; i++) {
      const d = Dates.addDays(from, i);
      if (isTrainingDay(profile, d) && !done.has(Dates.key(d))) return d;
    }
    return null;
  }

  // Streak = completed workouts in a row; only a missed *scheduled* day breaks it.
  // Today does not break the streak until the day is over.
  function streak(profile) {
    const done = completedSet();
    const today = Dates.today();
    let count = 0;
    for (let i = 0; i < 730; i++) {
      const d = Dates.addDays(today, -i);
      const k = Dates.key(d);
      if (done.has(k)) count++;
      else if (i > 0 && isTrainingDay(profile, d)) break;
    }
    return count;
  }

  // decorate(date) -> { cls: string, mark: string }
  function render(container, viewDate, decorate, onNavigate) {
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();
    const first = new Date(year, month, 1);
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const todayKey = Dates.key(Dates.today());

    let cells = '';
    for (let i = 0; i < first.getDay(); i++) cells += '<div class="cal-cell empty"></div>';
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, month, day);
      const { cls = '', mark = '' } = decorate(d) || {};
      const isToday = Dates.key(d) === todayKey ? ' today' : '';
      cells += `<div class="cal-cell ${cls}${isToday}"><span class="cal-num">${day}</span>${mark ? `<span class="cal-mark">${mark}</span>` : ''}</div>`;
    }

    container.innerHTML = `
      <div class="cal-head">
        <button class="icon-btn" data-nav="-1" aria-label="Previous month">‹</button>
        <div class="cal-title">${Dates.MONTHS[month]} ${year}</div>
        <button class="icon-btn" data-nav="1" aria-label="Next month">›</button>
      </div>
      <div class="cal-grid">
        ${Dates.WEEKDAYS.map((w) => `<div class="cal-dow">${w[0]}</div>`).join('')}
        ${cells}
      </div>`;

    container.querySelectorAll('[data-nav]').forEach((btn) => {
      btn.addEventListener('click', () => {
        onNavigate(new Date(year, month + Number(btn.dataset.nav), 1));
      });
    });
  }

  return { isTrainingDay, completedSet, nextTrainingDay, streak, render };
})();
