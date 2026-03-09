import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { Item, ItemComment } from "../types";
import { api } from "../services/api";

// Types

export interface PaginatedResponse<T> {
  items: T[];

  currentPage: number;

  totalItems: number;

  totalPages: number;
}

// Query keys

const queryKeys = {
  items: (username?: string) => ["items", { username }] as const,

  item: (id: number, username?: string) => ["items", id, { username }] as const,

  myItems: (username?: string) => ["items", "my", { username }] as const,

  comments: (itemId: number) => ["comments", itemId] as const,

  user: ["user"] as const,
};

// Items hooks

export const useItems = (
  page: number = 0,

  size: number = 10,

  category?: string,

  search?: string,

  sortBy: string = "createdAt",

  sortDir: string = "desc",
) => {
  const currentUser = null as { username?: string } | null;
  return useQuery({
    queryKey: [
      ...queryKeys.items(currentUser?.username),

      { page, size, category, search, sortBy, sortDir },
    ],

    queryFn: async (): Promise<PaginatedResponse<Item>> => {
      const params = new URLSearchParams({
        page: page.toString(),

        size: size.toString(),

        sortBy,

        sortDir,
      });

      if (category) params.append("category", category);

      if (search) params.append("search", search);

      const response = await api.get<{
        success: boolean;
        data: PaginatedResponse<Item>;
      }>(`/items?${params}`);

      return response.data.data;
    },
  });
};

export const useItem = (id: number, options?: { enabled?: boolean }) => {
  const currentUser = null as { username?: string } | null;
  return useQuery({
    queryKey: queryKeys.item(id, currentUser?.username),

    queryFn: async (): Promise<Item> => {
      const response = await api.get(`/items/${id}`);
      return response.data.data;
    },

    enabled: options?.enabled !== false,
  });
};

export const useMyItems = () => {
  const currentUser = null as { username?: string } | null;
  return useQuery({
    queryKey: queryKeys.myItems(currentUser?.username),

    queryFn: async (): Promise<Item[]> => {
      const response = await api.get("/items/my-items");

      return response.data.data || response.data;
    },

    enabled: true,
  });
};

export const useUpdateItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      id,
      title,
      description,
      category,
      location,
      condition,
      images,
    }: {
      id: number;
      title: string;
      description: string;
      category: string;
      location: string;
      condition: string;
      images?: File[];
    }): Promise<Item> => {
      const formData = new FormData();
      formData.append("title", title);
      formData.append("description", description);
      formData.append("category", category);
      formData.append("location", location);
      formData.append("condition", condition);

      if (images && images.length > 0) {
        images.forEach((image) => {
          formData.append("images", image);
        });
      }

      const response = await api.put(`/items/${id}`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      return response.data;
    },

    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", id] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
    },
  });
};

export const useDeleteItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await api.delete(`/items/${id}`);
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
    },
  });
};

// Comments hooks

export const useComments = (
  itemId: number,
  options?: { enabled?: boolean },
) => {
  return useQuery({
    queryKey: queryKeys.comments(itemId),

    queryFn: async (): Promise<ItemComment[]> => {
      const response = await api.get(`/comments/item/${itemId}`);
      return response.data.data || [];
    },

    enabled: options?.enabled !== false,
  });
};

export const useAddComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      itemId,

      text,
    }: {
      itemId: number;

      text: string;
    }) => {
      const formData = new FormData();
      formData.append("text", text);
      formData.append("itemId", itemId.toString());

      const response = await api.post("/comments/create", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      return response.data;
    },

    onSuccess: (_, { itemId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments(itemId) });
    },
  });
};

// Authenticated Item Creation Hook
export const useCreateItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      title: string;
      description: string;
      category: string;
      location: string;
      condition: string;
      images?: File[];
    }): Promise<{
      success: boolean;
      data?: Item;
      error?: string;
      message?: string;
    }> => {
      const formData = new FormData();
      formData.append("title", data.title);
      formData.append("description", data.description);
      formData.append("category", data.category);
      formData.append("location", data.location);
      formData.append("condition", data.condition);

      if (data.images && data.images.length > 0) {
        data.images.forEach((image) => {
          formData.append("images", image);
        });
      }

      const response = await api.post("/items/create", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
    },
  });
};

// Simple hook to get all items (matches backend /items/all)
export const useAllItems = () => {
  const currentUser = null as { username?: string } | null;
  return useQuery({
    queryKey: ["items", "all", { username: currentUser?.username }],
    queryFn: async (): Promise<Item[]> => {
      const response = await api.get("/items/all");
      return response.data.data || response.data || [];
    },
    enabled: true,
  });
};

// Delete single image hook
export const useDeleteImage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      itemId,
      imageUrl,
    }: {
      itemId: number;
      imageUrl: string;
    }) => {
      const response = await api.delete(`/items/${itemId}/image`, {
        params: { imageUrl },
      });
      return response.data;
    },

    onSuccess: (_, { itemId }) => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
      queryClient.invalidateQueries({ queryKey: ["items", itemId] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
    },
  });
};

// Upload hooks

// Toggle reservation hook
export const useToggleReservation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (itemId: number) => {
      const response = await api.post(`/items/${itemId}/reserve`);
      return response.data;
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
    },
  });
};

export const useUploadImages = () => {
  return useMutation({
    mutationFn: async (files: File[]): Promise<string[]> => {
      const formData = new FormData();

      files.forEach((file) => formData.append("files", file));

      const response = await api.post<string[]>("/upload/images", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      return response.data;
    },
  });
};

// Add images to existing item hook
export const useAddImagesToItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      itemId,
      images,
    }: {
      itemId: number;
      images: File[];
    }) => {
      const formData = new FormData();

      images.forEach((image) => {
        formData.append("images", image);
      });

      const response = await api.post(`/items/${itemId}/images`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      return response.data;
    },

    onSuccess: (_, { itemId }) => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      queryClient.invalidateQueries({ queryKey: ["items", itemId] });
      queryClient.invalidateQueries({ queryKey: ["items", "my"] });
      queryClient.invalidateQueries({ queryKey: ["items", "all"] });
    },
  });
};
