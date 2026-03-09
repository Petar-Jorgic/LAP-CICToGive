import { UserManager, WebStorageStateStore } from "oidc-client-ts";

const w3idConfig = {
  authority: "https://preprod.login.w3.ibm.com/oidc/endpoint/default",
  client_id: "ZjEzNGEwMGItNDAzYS00",
  client_secret: "YmVhNGMxOWMtMzNlZC00",
  redirect_uri: window.location.origin + "/",
  post_logout_redirect_uri: window.location.origin + "/",
  response_type: "id_token token",
  scope: "openid profile email",
  userStore: new WebStorageStateStore({ store: sessionStorage }),
  metadata: {
    issuer: "https://preprod.login.w3.ibm.com/oidc/endpoint/default",
    authorization_endpoint:
      "https://preprod.login.w3.ibm.com/v1.0/endpoint/default/authorize",
    token_endpoint:
      "https://preprod.login.w3.ibm.com/v1.0/endpoint/default/token",
    userinfo_endpoint:
      "https://preprod.login.w3.ibm.com/v1.0/endpoint/default/userinfo",
    revocation_endpoint:
      "https://preprod.login.w3.ibm.com/v1.0/endpoint/default/revoke",
    jwks_uri:
      "https://preprod.login.w3.ibm.com/oidc/endpoint/default/jwks",
  },
};

export const userManager = new UserManager(w3idConfig);
