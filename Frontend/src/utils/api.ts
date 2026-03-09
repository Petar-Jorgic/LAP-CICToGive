import { getAccessToken, loginRedirect } from "../auth.config";

interface ProfileResponse {
  id: number;
  username: string;
  email: string;
  avatarUrl?: string;
}

interface AvatarUploadResponse {
  avatarUrl: string;
  message: string;
}

interface SuccessResponse {
  message: string;
  status: string;
}

export const api = {
  fetch: async (url: string, options: RequestInit = {}): Promise<Response> => {
    const token = getAccessToken();

    const config: RequestInit = {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
        ...(token && { Authorization: `Bearer ${token}` }),
      },
    };

    if (options.body instanceof FormData) {
      delete (config.headers as Record<string, string>)["Content-Type"];
    }

    const response = await fetch(url, config);

    if (response.status === 401) {
      loginRedirect();
      throw new Error("Unauthorized");
    }

    return response;
  },

  get: async (url: string): Promise<Response> => {
    return api.fetch(url, { method: "GET" });
  },

  post: async (
    url: string,
    data: FormData | Record<string, unknown>,
  ): Promise<Response> => {
    return api.fetch(url, {
      method: "POST",
      body: data instanceof FormData ? data : JSON.stringify(data),
    });
  },

  put: async (
    url: string,
    data: Record<string, unknown>,
  ): Promise<Response> => {
    return api.fetch(url, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  delete: async (url: string): Promise<Response> => {
    return api.fetch(url, { method: "DELETE" });
  },
};

export const profileApi = {
  getProfile: async (): Promise<ProfileResponse> => {
    const response = await api.get("/api/profile");
    if (!response.ok) {
      let errorMessage = "Failed to get profile";
      try {
        const error = await response.json();
        errorMessage = error.message || errorMessage;
      } catch {
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }
    return response.json();
  },

  uploadAvatar: async (file: File): Promise<AvatarUploadResponse> => {
    const formData = new FormData();
    formData.append("avatar", file);
    const response = await api.post("/api/profile/avatar", formData);
    if (!response.ok) {
      let errorMessage = "Failed to upload avatar";
      try {
        const error = await response.json();
        errorMessage = error.message || errorMessage;
      } catch {
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }
    return response.json();
  },

  removeAvatar: async (): Promise<SuccessResponse> => {
    const response = await api.delete("/api/profile/avatar");
    if (!response.ok) {
      let errorMessage = "Failed to remove avatar";
      try {
        const error = await response.json();
        errorMessage = error.message || errorMessage;
      } catch {
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }
    return response.json();
  },
};

export const authApi = {
  getCurrentUser: async (): Promise<{
    id: number;
    username: string;
    email: string;
    avatarUrl?: string;
  }> => {
    const response = await api.get("/api/auth/me");
    if (!response.ok) {
      throw new Error("Failed to get current user");
    }
    return response.json();
  },
};
