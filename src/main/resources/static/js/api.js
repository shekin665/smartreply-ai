// Thin fetch wrapper shared by login.js and app.js. Every SmartReply REST
// endpoint returns either a JSON body or a JSON ErrorResponse with a
// "message" field, so this is the one place that needs to know that shape.

function csrfHeaders() {
  const tokenMeta = document.querySelector('meta[name="_csrf"]');
  const headerMeta = document.querySelector('meta[name="_csrf_header"]');
  if (!tokenMeta || !headerMeta) return {};
  return { [headerMeta.content]: tokenMeta.content };
}

async function apiRequest(method, url, body) {
  const needsCsrf = method !== "GET";
  const options = {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(needsCsrf ? csrfHeaders() : {}),
    },
    credentials: "same-origin",
  };
  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : null;

  if (!response.ok) {
    const message = (data && data.message) || `Request failed (${response.status})`;
    throw new Error(message);
  }
  return data;
}

const api = {
  get: (url) => apiRequest("GET", url),
  post: (url, body) => apiRequest("POST", url, body ?? {}),
  put: (url, body) => apiRequest("PUT", url, body ?? {}),
  delete: (url) => apiRequest("DELETE", url),
};
