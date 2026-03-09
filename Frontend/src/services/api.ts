import axios from "axios";
import { userManager } from "../auth.config";

export const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
});

// Add Bearer token to all requests
api.interceptors.request.use(async (config) => {
  const user = await userManager.getUser();
  if (user?.access_token) {
    config.headers.Authorization = `Bearer ${user.access_token}`;
  }
  return config;
});

// Redirect to login on 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      userManager.signinRedirect();
    }
    return Promise.reject(error);
  },
);
