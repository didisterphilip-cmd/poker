// Local persistence. Everything lives in this device's localStorage for now;
// swap this module for a backend (e.g. Supabase/Firebase) to sync accounts.
const Store = (() => {
  const KEY = 'kettlefire.v1';

  function load() {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) { /* storage unavailable or corrupt */ }
    return { profile: null, sessions: [] };
  }

  let state = load();

  function save() {
    try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { /* ignore */ }
  }

  return {
    get profile() { return state.profile; },
    get sessions() { return state.sessions; },
    setProfile(profile) { state.profile = profile; save(); },
    addSession(session) { state.sessions.push(session); save(); },
    reset() { state = { profile: null, sessions: [] }; save(); },
  };
})();

// Date helpers working in the user's local time zone.
const Dates = {
  key(d) {
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${day}`;
  },
  today() {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), d.getDate());
  },
  addDays(d, n) {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
  },
  WEEKDAYS: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
  WEEKDAYS_LONG: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
  MONTHS: ['January', 'February', 'March', 'April', 'May', 'June', 'July',
    'August', 'September', 'October', 'November', 'December'],
};
