import axios from "axios";
import { getAccessToken, loginRedirect } from "../auth.config";

export const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
});

// Add Bearer token to all requests
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Redirect to login on 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      loginRedirect();
    }
    return Promise.reject(error);
  },
);
