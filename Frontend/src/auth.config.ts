// W3ID only supports implicit flow (id_token, token)
// oidc-client-ts only supports code flow, so we handle the redirect manually

const W3ID_BASE = "https://preprod.login.w3.ibm.com/v1.0/endpoint/default";
const CLIENT_ID = "ZjEzNGEwMGItNDAzYS00";
const REDIRECT_URI = window.location.origin;

export function loginRedirect() {
  const nonce = crypto.randomUUID();
  const state = crypto.randomUUID();
  sessionStorage.setItem("oidc_nonce", nonce);
  sessionStorage.setItem("oidc_state", state);

  const params = new URLSearchParams({
    response_type: "token",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: "openid profile email",
    nonce,
    state,
  });

  window.location.href = `${W3ID_BASE}/authorize?${params}`;
}

export function parseCallback(): string | null {
  const hash = window.location.hash.substring(1);
  if (!hash) return null;

  const params = new URLSearchParams(hash);
  const accessToken = params.get("access_token");
  const state = params.get("state");
  const savedState = sessionStorage.getItem("oidc_state");

  if (state && savedState && state !== savedState) {
    console.error("State mismatch");
    return null;
  }

  sessionStorage.removeItem("oidc_nonce");
  sessionStorage.removeItem("oidc_state");

  if (accessToken) {
    sessionStorage.setItem("access_token", accessToken);
  }

  return accessToken;
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem("access_token");
}

export function clearAuth() {
  sessionStorage.removeItem("access_token");
  sessionStorage.removeItem("oidc_nonce");
  sessionStorage.removeItem("oidc_state");
}
