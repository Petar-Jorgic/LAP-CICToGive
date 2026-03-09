import { authApi } from "../utils/api";

export const authService = {
  async getCurrentUser() {
    return authApi.getCurrentUser();
  },
};
