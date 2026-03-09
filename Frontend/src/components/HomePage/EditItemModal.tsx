import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useUpdateItem, useDeleteItem, useDeleteImage, useItem } from "../../hooks/useAPI";
import type { Item } from "../../types";
import { useEffect, useState } from "react";
import { CATEGORIES, CONDITIONS } from "../../schemas/ItemSchemas";

const schema = z.object({
  title: z.string().min(1, "Titel erforderlich"),
  description: z.string().min(1, "Beschreibung erforderlich"),
  category: z.string().min(1, "Kategorie erforderlich"),
  location: z.string().min(1, "Standort erforderlich"),
  condition: z.string().min(1, "Zustand erforderlich"),
  images: z.instanceof(FileList).optional(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  item: Item | null;
  isOpen: boolean;
  onClose: () => void;
}

export default function EditItemModal({ item, isOpen, onClose }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const updateItemMutation = useUpdateItem();
  const deleteItemMutation = useDeleteItem();
  const deleteImageMutation = useDeleteImage();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Use live data from query instead of stale prop
  const { data: currentItem } = useItem(item?.id || 0, {
    enabled: !!item && isOpen,
  });

  // Use current item data from query, fallback to passed item
  const displayItem = currentItem || item;

  // Reset form when item changes
  useEffect(() => {
    if (displayItem) {
      reset({
        title: displayItem.title,
        description: displayItem.description,
        category: displayItem.category,
        location: displayItem.location,
        condition: displayItem.condition,
      });
    }
  }, [displayItem, reset]);

  const handleFormSubmit = async (data: FormData) => {
    if (!displayItem) return;

    try {
      await updateItemMutation.mutateAsync({
        id: displayItem.id,
        title: data.title,
        description: data.description,
        category: data.category,
        location: data.location,
        condition: data.condition,
        images: data.images ? Array.from(data.images) : undefined,
      });
      reset();
      onClose();
    } catch (error) {
      console.error("Error updating item:", error);
    }
  };

  const handleDeleteImage = async (imageUrl: string) => {
    if (!displayItem) return;

    try {
      await deleteImageMutation.mutateAsync({
        itemId: displayItem.id,
        imageUrl,
      });
    } catch (error) {
      console.error("Failed to delete image:", error);
    }
  };

  const handleDeleteItem = async () => {
    if (!displayItem) return;
    try {
      await deleteItemMutation.mutateAsync(displayItem.id);
      setShowDeleteConfirm(false);
      reset();
      onClose();
    } catch (error) {
      console.error("Failed to delete item:", error);
    }
  };

  const handleClose = () => {
    setShowDeleteConfirm(false);
    reset();
    onClose();
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      handleClose();
    }
  };

  if (!isOpen || !displayItem) return null;

  return (
    <div
      className="fixed inset-0 bg-black/20 backdrop-blur-sm flex items-center justify-center z-50 p-4"
      onClick={handleBackdropClick}
    >
      <div className="bg-base-100 rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-xl font-semibold">Artikel bearbeiten</h2>
          <button
            onClick={handleClose}
            className="btn btn-sm btn-circle btn-ghost"
          >
            ✕
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
            <div>
              <fieldset className="fieldset">
                <legend className="fieldset-legend">
                  📷 Bilder hinzufügen (optional)
                </legend>
                <input
                  {...register("images")}
                  type="file"
                  accept="image/*"
                  multiple
                  className="file-input file-input-bordered w-full"
                />
                <label className="label">
                  <span className="label-text-alt">
                    Max. 2MB pro Bild. Neue Bilder werden zu den bestehenden
                    hinzugefügt (max. 5 total).
                  </span>
                </label>
              </fieldset>

              {/* Current Images Preview */}
              {displayItem.imageUrls && displayItem.imageUrls.length > 0 && (
                <div className="mt-2">
                  <label className="label">
                    <span className="label-text">Aktuelle Bilder:</span>
                  </label>
                  <div className="flex gap-2 flex-wrap">
                    {displayItem.imageUrls.map((url, index) => (
                      <div
                        key={index}
                        className="w-24 h-24 bg-base-200 rounded-lg overflow-hidden relative"
                      >
                        <img
                          src={url}
                          alt={`${displayItem.title} - Image ${index + 1}`}
                          className="w-full h-full object-cover"
                        />
                        <button
                          className="btn btn-error btn-xs absolute top-1 right-1 z-10"
                          onClick={() => handleDeleteImage(url)}
                          disabled={deleteImageMutation.isPending}
                          type="button"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <input
                {...register("title")}
                placeholder="Titel"
                className="input input-bordered w-full mt-2"
              />
              {errors.title && (
                <p className="text-error text-xs mt-1">
                  {errors.title.message}
                </p>
              )}
            </div>

            <div>
              <textarea
                {...register("description")}
                placeholder="Beschreibung"
                className="textarea textarea-bordered w-full"
              />
              {errors.description && (
                <p className="text-error text-xs mt-1">
                  {errors.description.message}
                </p>
              )}
            </div>

            <div>
              <select
                {...register("category")}
                className="select select-bordered w-full"
              >
                <option value="">Kategorie auswählen</option>
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
              {errors.category && (
                <p className="text-error text-xs mt-1">
                  {errors.category.message}
                </p>
              )}
            </div>

            <div>
              <input
                {...register("location")}
                placeholder="Standort"
                className="input input-bordered w-full"
              />
              {errors.location && (
                <p className="text-error text-xs mt-1">
                  {errors.location.message}
                </p>
              )}
            </div>

            <div>
              <select
                {...register("condition")}
                className="select select-bordered w-full"
              >
                <option value="">Zustand auswählen</option>
                {CONDITIONS.map((cond) => (
                  <option key={cond} value={cond}>
                    {cond}
                  </option>
                ))}
              </select>
              {errors.condition && (
                <p className="text-error text-xs mt-1">
                  {errors.condition.message}
                </p>
              )}
            </div>

            {showDeleteConfirm ? (
              <div className="bg-error/10 border border-error rounded-lg p-4 mt-4">
                <p className="text-sm mb-3">Artikel wirklich löschen? Das kann nicht rückgängig gemacht werden.</p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setShowDeleteConfirm(false)}
                    className="btn btn-ghost btn-sm flex-1"
                    disabled={deleteItemMutation.isPending}
                  >
                    Abbrechen
                  </button>
                  <button
                    type="button"
                    onClick={handleDeleteItem}
                    className="btn btn-error btn-sm flex-1"
                    disabled={deleteItemMutation.isPending}
                  >
                    {deleteItemMutation.isPending ? "Wird gelöscht..." : "Endgültig löschen"}
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex gap-2 pt-4">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="btn btn-error btn-outline"
                  disabled={updateItemMutation.isPending}
                >
                  Löschen
                </button>
                <button
                  type="button"
                  onClick={handleClose}
                  className="btn btn-ghost flex-1"
                  disabled={updateItemMutation.isPending}
                >
                  Abbrechen
                </button>
                <button
                  type="submit"
                  className="btn btn-primary flex-1"
                  disabled={updateItemMutation.isPending}
                >
                  {updateItemMutation.isPending
                    ? "Wird aktualisiert..."
                    : "Aktualisieren"}
                </button>
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
