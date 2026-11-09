(function () {
  var CACHE_PREFIX = "ferro_preferences_";
  var KG_TO_LB = 2.2046226218;
  var settings = defaultSettings();
  var loaded = false;
  var loadPromise = null;
  var saveQueue = Promise.resolve();
  var saveVersion = 0;
  var dailyTimer = null;
  var weeklyTimer = null;

  function defaultSettings() {
    return {
      unit: "kg",
      currentWeightKg: null,
      goalWeightKg: null,
      dailyWorkoutReminder: true,
      personalRecordNotification: true,
      weeklySummaryNotification: false,
    };
  }

  function cacheKey() {
    var user = window.FerroApi && FerroApi.localUser();
    return CACHE_PREFIX + (user && user.id ? user.id : "current");
  }

  function remember(value) {
    settings = Object.assign(defaultSettings(), value || {});
    localStorage.setItem(cacheKey(), JSON.stringify(settings));
  }

  function readCache() {
    try {
      return Object.assign(defaultSettings(), JSON.parse(localStorage.getItem(cacheKey()) || "{}"));
    } catch (_) {
      return defaultSettings();
    }
  }

  function load() {
    if (loaded) return Promise.resolve(settings);
    if (loadPromise) return loadPromise;

    loadPromise = FerroApi.profileSettings().then(function (result) {
      remember(result);
      loaded = true;
      armNotifications();
      return settings;
    }).catch(function (error) {
      settings = readCache();
      loaded = true;
      settings.loadError = error.message;
      armNotifications();
      return settings;
    }).finally(function () {
      loadPromise = null;
    });
    return loadPromise;
  }

  function save(changes) {
    return load().then(function () {
      var previous = Object.assign({}, settings);
      settings = Object.assign({}, settings, changes || {});
      delete settings.loadError;
      var snapshot = Object.assign({}, settings);
      var version = ++saveVersion;
      var task = saveQueue.catch(function () {}).then(function () {
        return FerroApi.updateProfileSettings(snapshot);
      }).then(function (result) {
        if (version === saveVersion) {
          remember(result);
          armNotifications();
          window.dispatchEvent(new CustomEvent("ferro:settings", { detail: settings }));
        }
        return result;
      }).catch(function (error) {
        if (version === saveVersion) {
          settings = previous;
          armNotifications();
        }
        throw error;
      });
      saveQueue = task;
      return task;
    });
  }

  function unit() {
    return settings.unit === "lb" ? "lb" : "kg";
  }

  function fromKg(valueKg) {
    var value = Number(valueKg);
    return unit() === "lb" ? value * KG_TO_LB : value;
  }

  function toKg(value) {
    var number = Number(value);
    return unit() === "lb" ? number / KG_TO_LB : number;
  }

  function round(value, places) {
    var factor = Math.pow(10, places);
    return Math.round((Number(value) + Number.EPSILON) * factor) / factor;
  }

  function formatNumber(value, places) {
    return round(value, places == null ? 2 : places).toLocaleString("en-US", {
      maximumFractionDigits: places == null ? 2 : places,
    });
  }

  function formatMass(valueKg, withUnit) {
    var displayed = fromKg(valueKg);
    return formatNumber(displayed) + (withUnit === false ? "" : unit());
  }

  function parseMass(value, assumedUnit) {
    var match = String(value == null ? "" : value).trim().match(/^(-?\d+(?:[.,]\d+)?)\s*(kg|lb)?$/i);
    if (!match) return null;
    var number = Number(match[1].replace(",", "."));
    var valueUnit = (match[2] || assumedUnit || unit()).toLowerCase();
    if (!Number.isFinite(number)) return null;
    return valueUnit === "lb" ? number / KG_TO_LB : number;
  }

  function inputMass(valueKg) {
    var kilograms = parseMass(valueKg, "kg");
    return kilograms == null ? "" : String(round(fromKg(kilograms), 2));
  }

  function planWeight(value) {
    if (!String(value == null ? "" : value).trim()) return "";
    var kilograms = parseMass(value, unit());
    return kilograms == null ? String(value).trim() : String(round(kilograms, 2)) + "kg";
  }

  function notificationPermission() {
    if (!("Notification" in window)) return Promise.resolve("unsupported");
    if (Notification.permission !== "default") return Promise.resolve(Notification.permission);
    return Notification.requestPermission();
  }

  function notify(title, body, tag) {
    if (!("Notification" in window) || Notification.permission !== "granted") return false;
    new Notification(title, { body: body, tag: tag || "ferro" });
    return true;
  }

  function clearNotificationTimers() {
    if (dailyTimer) clearTimeout(dailyTimer);
    if (weeklyTimer) clearTimeout(weeklyTimer);
    dailyTimer = null;
    weeklyTimer = null;
  }

  function nextOccurrence(hour, minute, weekday) {
    var now = new Date();
    var next = new Date(now);
    next.setHours(hour, minute, 0, 0);
    if (weekday != null) {
      var days = (weekday - next.getDay() + 7) % 7;
      next.setDate(next.getDate() + days);
      if (next <= now) next.setDate(next.getDate() + 7);
    } else if (next <= now) {
      next.setDate(next.getDate() + 1);
    }
    return next;
  }

  function isoDate(date) {
    return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") +
      "-" + String(date.getDate()).padStart(2, "0");
  }

  async function sendDailyReminder() {
    var today = new Date();
    var date = isoDate(today);
    if (localStorage.getItem("ferro_daily_reminder_date") === date) return;

    var plan = await FerroApi.activePlan();
    var todayIndex = today.getDay();
    var hasWorkout = (plan && plan.days || []).some(function (day, index) {
      return (Number.isInteger(day.weekday) ? day.weekday : index) === todayIndex;
    });
    if (!hasWorkout) return;

    var history = await FerroApi.workoutHistory();
    var alreadyDone = (history || []).some(function (item) {
      return String(item.date || "").slice(0, 10) === date;
    });
    if (!alreadyDone && notify("FERRO workout reminder", "Your scheduled workout is ready when you are.", "ferro-daily")) {
      localStorage.setItem("ferro_daily_reminder_date", date);
    }
  }

  async function sendWeeklySummary() {
    var today = new Date();
    var todayKey = isoDate(today);
    if (localStorage.getItem("ferro_weekly_summary_date") === todayKey) return;

    var weekStart = new Date(today);
    weekStart.setHours(0, 0, 0, 0);
    weekStart.setDate(weekStart.getDate() - weekStart.getDay());
    var startKey = isoDate(weekStart);
    var history = await FerroApi.workoutHistory();
    var workouts = (history || []).filter(function (item) {
      var date = String(item.date || "").slice(0, 10);
      return date >= startKey && date <= todayKey;
    });
    var volumeKg = workouts.reduce(function (total, item) {
      return total + (Number(item.volumeValue) || 0);
    }, 0);
    var summary = workouts.length + (workouts.length === 1 ? " workout" : " workouts") +
      " · " + formatMass(volumeKg) + " volume this week.";
    if (notify("Your FERRO weekly summary", summary, "ferro-weekly")) {
      localStorage.setItem("ferro_weekly_summary_date", todayKey);
    }
  }

  function armNotifications() {
    clearNotificationTimers();
    if (!("Notification" in window) || Notification.permission !== "granted") return;

    if (settings.dailyWorkoutReminder) {
      var dailyAt = nextOccurrence(9, 0);
      dailyTimer = setTimeout(function () {
        sendDailyReminder().catch(function () {});
        armNotifications();
      }, dailyAt.getTime() - Date.now());
    }
    if (settings.weeklySummaryNotification) {
      var weeklyAt = nextOccurrence(18, 0, 0);
      weeklyTimer = setTimeout(function () {
        sendWeeklySummary().catch(function () {});
        armNotifications();
      }, weeklyAt.getTime() - Date.now());
    }
  }

  function notifyNewRecords(before, after) {
    var oldRecords = {};
    (before && before.personalRecords || []).forEach(function (record) {
      oldRecords[String(record.exercise).toLowerCase()] = record;
    });
    var newRecords = (after && after.personalRecords || []).filter(function (record) {
      var old = oldRecords[String(record.exercise).toLowerCase()];
      return !old || Number(record.weight) > Number(old.weight) ||
        (Number(record.weight) === Number(old.weight) && Number(record.reps) > Number(old.reps));
    });
    if (!newRecords.length) return false;
    var names = newRecords.slice(0, 3).map(function (record) { return record.exercise; }).join(", ");
    return notify("New personal record", names + (newRecords.length > 1 ? " set new records." : " set a new record."), "ferro-pr");
  }

  window.FerroPreferences = {
    load: load,
    save: save,
    current: function () { return settings; },
    unit: unit,
    fromKg: fromKg,
    toKg: toKg,
    round: round,
    formatNumber: formatNumber,
    formatMass: formatMass,
    parseMass: parseMass,
    inputMass: inputMass,
    planWeight: planWeight,
    notificationPermission: notificationPermission,
    refreshNotifications: armNotifications,
    notify: notify,
    notifyNewRecords: notifyNewRecords,
  };

  if (window.FerroApi && FerroApi.hasSession()) load();
})();
