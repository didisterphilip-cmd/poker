// Exercise library and workout generation.
const Workouts = (() => {
  const EXERCISES = {
    swing: {
      name: 'Two-Hand Swing',
      desc: 'Hinge at the hips, hike the bell back, then snap the hips forward to float it to chest height. Arms stay loose — the power comes from the hips.',
      reps: 15,
    },
    goblet: {
      name: 'Goblet Squat',
      desc: 'Hold the bell by the horns at your chest. Sit down between your heels, chest tall, then drive up through the whole foot.',
      reps: 10,
    },
    halo: {
      name: 'Halo',
      desc: 'Hold the bell upside down by the horns and circle it slowly around your head. Keep ribs down and glutes tight.',
      reps: 5, perSide: true,
    },
    deadlift: {
      name: 'Kettlebell Deadlift',
      desc: 'Bell between your feet. Push the hips back, flat back, grab the handle and stand tall by squeezing the glutes.',
      reps: 10,
    },
    clean: {
      name: 'Single-Arm Clean',
      desc: 'Swing the bell back, then pull it close to the body into the rack position at your chest. Soft catch — no banging the forearm.',
      reps: 5, perSide: true,
    },
    press: {
      name: 'Overhead Press',
      desc: 'From the rack, brace your core and press the bell overhead until the arm is locked out by your ear. Lower with control.',
      reps: 5, perSide: true,
    },
    row: {
      name: 'Bent-Over Row',
      desc: 'Hinge forward with a flat back. Pull the bell to your hip, elbow close to the body, then lower slowly.',
      reps: 8, perSide: true,
    },
    singleSwing: {
      name: 'Single-Arm Swing',
      desc: 'Same as the two-hand swing with one hand. Resist the rotation and keep the shoulder packed.',
      reps: 10, perSide: true,
    },
    lunge: {
      name: 'Goblet Reverse Lunge',
      desc: 'Hold the bell at your chest and step back into a lunge until the back knee nearly touches the floor. Drive through the front foot.',
      reps: 8, perSide: true,
    },
    pushup: {
      name: 'Push-Up',
      desc: 'Hands on the floor (or on the bell handles for depth). Body in a straight line, chest to the floor and back up.',
      reps: 10,
    },
    getup: {
      name: 'Turkish Get-Up',
      desc: 'Lying down with the bell locked out overhead, stand up step by step while keeping your eyes and arm on the bell. Reverse to return.',
      reps: 1, perSide: true,
    },
    highPull: {
      name: 'High Pull',
      desc: 'Swing, then at the top pull the elbow back so the bell comes to chest height close to the body.',
      reps: 8, perSide: true,
    },
  };

  const TEMPLATES = [
    {
      id: 'foundation',
      title: 'Swing Foundations',
      focus: 'Full body · Hip power',
      intro: 'Builds the hip hinge — the base of all kettlebell training. Focus on clean form and strong, snappy hips.',
      exercises: ['halo', 'deadlift', 'swing', 'goblet'],
    },
    {
      id: 'strength',
      title: 'Strength & Press',
      focus: 'Upper body · Legs',
      intro: 'Slower, heavier movements for strength. Own every rep and keep tension through the whole body.',
      exercises: ['clean', 'press', 'goblet', 'row'],
    },
    {
      id: 'conditioning',
      title: 'Fire Conditioning',
      focus: 'Cardio · Core',
      intro: 'Higher pace to raise the heart rate. Keep the rest short and the movement quality high.',
      exercises: ['singleSwing', 'lunge', 'pushup', 'highPull', 'getup'],
    },
  ];

  const LEVELS = {
    beginner: { label: 'Just starting', rounds: 2, repScale: 0.7 },
    intermediate: { label: 'In decent shape', rounds: 3, repScale: 1 },
    advanced: { label: 'In great shape', rounds: 4, repScale: 1.3 },
  };

  const SECONDS_PER_REP = 3;

  function suggestedBell(profile) {
    const male = profile.sex === 'male';
    const table = {
      beginner: male ? 12 : 8,
      intermediate: male ? 16 : 12,
      advanced: male ? 24 : 16,
    };
    return table[profile.level] || 12;
  }

  // Rotates through templates based on how many workouts are already done.
  function forSessionNumber(profile, n) {
    const tpl = TEMPLATES[n % TEMPLATES.length];
    const lvl = LEVELS[profile.level] || LEVELS.intermediate;
    const rest = Number(profile.restSeconds) || 60;

    const items = tpl.exercises.map((key) => {
      const ex = EXERCISES[key];
      const reps = Math.max(1, Math.round(ex.reps * lvl.repScale));
      return { key, ...ex, reps };
    });

    const workSeconds = items.reduce(
      (sum, ex) => sum + ex.reps * (ex.perSide ? 2 : 1) * SECONDS_PER_REP, 0) * lvl.rounds;
    const restCount = items.length * lvl.rounds - 1;
    const totalSeconds = workSeconds + restCount * rest + 5 * 60; // + warm-up

    return {
      ...tpl,
      rounds: lvl.rounds,
      rest,
      bell: suggestedBell(profile),
      items,
      minutes: Math.round(totalSeconds / 60),
    };
  }

  return { EXERCISES, TEMPLATES, LEVELS, forSessionNumber };
})();
