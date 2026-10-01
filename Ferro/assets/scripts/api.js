(function () {
  var API_BASE = window.FERRO_API_BASE || "http://127.0.0.1:8081/api";
  var TOKEN_KEY = "ferro_token";
  var USER_KEY = "ferro_user";

  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
  }

  function headers() {
    var result = { "Content-Type": "application/json" };
    var token = getToken();
    if (token) result.Authorization = "Bearer " + token;
    return result;
  }

  async function request(path, options) {
    options = options || {};
    var controller = new AbortController();
    var timeout = setTimeout(function () {
      controller.abort();
    }, options.timeout || 16000);

    var config = Object.assign({}, options, {
      signal: controller.signal,
      headers: Object.assign(headers(), options.headers || {}),
    });
    delete config.timeout;

    var response;
    try {
      response = await fetch(API_BASE + path, config);
    } catch (error) {
      if (error && error.name === "AbortError") {
        throw new Error(
          "The Java server took too long to respond. Check that the backend is running on port 8081.",
        );
      }
      throw new Error(
        "Could not connect to the Java server. Check that the backend is running on port 8081.",
      );
    } finally {
      clearTimeout(timeout);
    }

    var data = null;
    var texto = await response.text();
    if (texto) {
      try {
        data = JSON.parse(texto);
      } catch (_) {
        data = texto;
      }
    }

    if (!response.ok) {
      var message =
        data && data.error ? data.error : "HTTP error " + response.status;
      if (response.status === 401) clearSession();
      throw new Error(message);
    }

    return data;
  }

  function saveSession(data) {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(
      USER_KEY,
      JSON.stringify({
        id: data.id,
        name: data.name,
        email: data.email,
      }),
    );
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function localUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || "null");
    } catch (_) {
      return null;
    }
  }

  function requireLogin() {
    if (!getToken()) {
      window.location.href = "login.html";
      return false;
    }
    return true;
  }

  window.FerroApi = {
    base: API_BASE,
    hasSession: function () {
      return !!getToken();
    },
    localUser: localUser,
    requireLogin: requireLogin,
    clearSession: clearSession,

    register: async function (data) {
      var response = await request("/auth/register", {
        method: "POST",
        body: JSON.stringify(data),
      });
      saveSession(response);
      return response;
    },

    login: async function (data) {
      var response = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify(data),
      });
      saveSession(response);
      return response;
    },

    me: async function () {
      return request("/auth/me");
    },

    logout: async function () {
      try {
        await request("/auth/logout", { method: "POST" });
      } finally {
        clearSession();
      }
    },

    listPlans: async function () {
      return request("/plans");
    },

    findPlan: async function (id) {
      return request("/plans/" + encodeURIComponent(id));
    },

    activePlan: async function () {
      return request("/plans/active");
    },

    createPlan: async function (plan) {
      return request("/plans", {
        method: "POST",
        body: JSON.stringify(plan),
      });
    },

    updatePlan: async function (id, plan) {
      return request("/plans/" + encodeURIComponent(id), {
        method: "PUT",
        body: JSON.stringify(plan),
      });
    },

    activatePlan: async function (id) {
      return request("/plans/" + encodeURIComponent(id) + "/activate", {
        method: "PUT",
      });
    },

    deletePlan: async function (id) {
      return request("/plans/" + encodeURIComponent(id), {
        method: "DELETE",
      });
    },

    updateProfile: async function (data) {
      var response = await request("/profile", {
        method: "PUT",
        body: JSON.stringify(data),
      });
      localStorage.setItem(USER_KEY, JSON.stringify(response));
      return response;
    },
  };
})();
