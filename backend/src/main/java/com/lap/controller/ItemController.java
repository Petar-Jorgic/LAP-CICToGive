package com.lap.controller;

import com.lap.entity.Item;
import com.lap.entity.User;
import com.lap.repository.ItemRepository;
import com.lap.repository.UserRepository;
import com.lap.service.FileStorageService;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createItem(
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam("category") String category,
        @RequestParam("location") String location,
        @RequestParam("condition") String condition,
        @RequestParam(
            value = "images",
            required = false
        ) MultipartFile[] images,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Benutzer nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userOpt.get();

            // Create item
            Item item = new Item(
                title,
                description,
                category,
                location,
                condition,
                user
            );

            // Handle images
            List<String> imageUrls = new ArrayList<>();
            if (images != null && images.length > 0) {
                try {
                    for (MultipartFile img : images) {
                        if (!img.isEmpty()) {
                            String fileName =
                                "item_" +
                                System.currentTimeMillis() +
                                "_" +
                                img.getOriginalFilename();
                            fileStorageService.uploadFile(img, fileName);
                            String imageUrl = fileStorageService.getDownloadUrl(
                                fileName
                            );
                            imageUrls.add(imageUrl);
                        }
                    }
                } catch (Exception e) {
                    response.put("success", false);
                    response.put(
                        "error",
                        "Fehler beim Hochladen der Bilder: " + e.getMessage()
                    );
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Set all image URLs
            if (!imageUrls.isEmpty()) {
                item.setImageUrls(imageUrls);
            }

            // Save item
            Item savedItem = itemRepository.save(item);

            response.put("success", true);
            response.put("message", "Artikel erfolgreich erstellt");
            response.put("data", createItemResponse(savedItem, user));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Erstellen des Artikels: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String currentUsername = authentication != null
                ? authentication.getName()
                : null;
            List<Item> items;

            if (
                search != null &&
                !search.isEmpty() &&
                category != null &&
                !category.isEmpty()
            ) {
                items = itemRepository.findByCategoryAndKeyword(
                    category,
                    search
                );
            } else if (search != null && !search.isEmpty()) {
                items = itemRepository.findByKeyword(search);
            } else if (category != null && !category.isEmpty()) {
                items = itemRepository.findByCategory(category);
            } else {
                items = itemRepository.findAllByOrderByCreatedAtDesc();
            }

            List<Map<String, Object>> itemResponses = items
                .stream()
                .map(item -> createItemResponse(item, currentUsername))
                .toList();

            // Create paginated response
            Map<String, Object> paginatedResponse = new HashMap<>();
            paginatedResponse.put("items", itemResponses);
            paginatedResponse.put("currentPage", page);
            paginatedResponse.put("totalItems", itemResponses.size());
            paginatedResponse.put(
                "totalPages",
                (int) Math.ceil((double) itemResponses.size() / size)
            );

            response.put("success", true);
            response.put("data", paginatedResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to fetch items: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllItems(
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String currentUsername = authentication != null
                ? authentication.getName()
                : null;
            List<Item> items = itemRepository.findAllByOrderByCreatedAtDesc();

            List<Map<String, Object>> itemResponses = items
                .stream()
                .map(item -> createItemResponse(item, currentUsername))
                .toList();

            response.put("success", true);
            response.put("data", itemResponses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to fetch items: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchItems(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String currentUsername = authentication != null
                ? authentication.getName()
                : null;
            List<Item> items;

            if (
                keyword != null &&
                !keyword.isEmpty() &&
                category != null &&
                !category.isEmpty()
            ) {
                items = itemRepository.findByCategoryAndKeyword(
                    category,
                    keyword
                );
            } else if (keyword != null && !keyword.isEmpty()) {
                items = itemRepository.findByKeyword(keyword);
            } else if (category != null && !category.isEmpty()) {
                items = itemRepository.findByCategory(category);
            } else {
                items = itemRepository.findAllByOrderByCreatedAtDesc();
            }

            List<Map<String, Object>> itemResponses = items
                .stream()
                .map(item -> createItemResponse(item, currentUsername))
                .toList();

            response.put("success", true);
            response.put("data", itemResponses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to search items: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Map<String, Object>> toggleReservation(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = authentication.getName();
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            // Check if user owns the item
            if (!item.getUser().getUsername().equals(username)) {
                response.put("success", false);
                response.put(
                    "error",
                    "Sie können nur Ihre eigenen Artikel reservieren/freigeben"
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Toggle reservation
            item.setIsReserved(!item.getIsReserved());
            Item savedItem = itemRepository.save(item);

            response.put("success", true);
            response.put(
                "message",
                savedItem.getIsReserved()
                    ? "Artikel reserviert"
                    : "Reservierung entfernt"
            );
            response.put("data", createItemResponse(savedItem, username));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Ändern der Reservierung: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-items")
    public ResponseEntity<Map<String, Object>> getMyItems(
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || authentication.getName() == null) {
                response.put("success", false);
                response.put("error", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String currentUsername = authentication.getName();
            List<Item> items =
                itemRepository.findByUserUsernameOrderByCreatedAtDesc(
                    currentUsername
                );

            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (Item item : items) {
                itemsList.add(createItemResponse(item, currentUsername));
            }

            response.put("success", true);
            response.put("data", itemsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Failed to retrieve items: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getItem(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String currentUsername = authentication != null
                ? authentication.getName()
                : null;
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();
            response.put("success", true);
            response.put("data", createItemResponse(item, currentUsername));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Abrufen des Artikels: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/refresh-image")
    public ResponseEntity<Map<String, Object>> refreshImageUrl(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Item not found");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                try {
                    List<String> refreshedUrls = new ArrayList<>();
                    for (String imageUrl : item.getImageUrls()) {
                        // Extract filename from URL
                        String fileName = imageUrl.substring(
                            imageUrl.lastIndexOf('/') + 1
                        );
                        if (fileName.contains("?")) {
                            fileName = fileName.substring(
                                0,
                                fileName.indexOf("?")
                            );
                        }

                        // Generate new authorized URL
                        String newImageUrl = fileStorageService.getDownloadUrl(
                            fileName,
                            3600
                        );
                        refreshedUrls.add(newImageUrl);
                    }
                    item.setImageUrls(refreshedUrls);
                    itemRepository.save(item);

                    response.put("success", true);
                    response.put("imageUrls", refreshedUrls);
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put(
                        "error",
                        "Failed to refresh image URLs: " + e.getMessage()
                    );
                    return ResponseEntity.badRequest().body(response);
                }
            }

            response.put("success", false);
            response.put("error", "No image URLs to refresh");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Failed to refresh image URL: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> createItemResponse(Item item, String username) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("id", item.getId());
        itemMap.put("title", item.getTitle());
        itemMap.put("description", item.getDescription());
        itemMap.put("category", item.getCategory());
        itemMap.put("location", item.getLocation());
        itemMap.put("condition", item.getCondition());

        itemMap.put("imageUrls", item.getImageUrls() != null ? item.getImageUrls() : new ArrayList<>());

        itemMap.put("isReserved", item.getIsReserved());
        itemMap.put(
            "isMyItem",
            username != null && username.equals(item.getUser().getUsername())
        );
        itemMap.put("datePosted", item.getCreatedAt().toString());
        return itemMap;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = authentication.getName();
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            // Check if user owns the item
            if (!item.getUser().getUsername().equals(username)) {
                response.put("success", false);
                response.put("error", "You can only delete your own items");
                return ResponseEntity.badRequest().body(response);
            }

            // Delete images from B2 if exist
            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                for (String imageUrl : item.getImageUrls()) {
                    try {
                        // Extract filename from URL
                        String fileName = imageUrl.substring(
                            imageUrl.lastIndexOf('/') + 1
                        );
                        if (fileName.contains("?")) {
                            fileName = fileName.substring(
                                0,
                                fileName.indexOf("?")
                            );
                        }
                        fileStorageService.deleteFile(fileName);
                    } catch (Exception e) {
                        // Log error but don't fail the deletion
                        System.err.println(
                            "Failed to delete image from B2: " + e.getMessage()
                        );
                    }
                }
            }

            // Delete item from database
            itemRepository.delete(item);

            response.put("success", true);
            response.put("message", "Artikel erfolgreich gelöscht");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Löschen des Artikels: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(
        @PathVariable Long id,
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam("category") String category,
        @RequestParam("location") String location,
        @RequestParam("condition") String condition,
        @RequestParam(
            value = "images",
            required = false
        ) MultipartFile[] images,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = authentication.getName();
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            // Check if user owns the item
            if (!item.getUser().getUsername().equals(username)) {
                response.put("success", false);
                response.put("error", "You can only edit your own items");
                return ResponseEntity.badRequest().body(response);
            }

            // Update item fields
            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(category);
            item.setLocation(location);
            item.setCondition(condition);

            // Handle image updates (add new images to existing ones)
            if (images != null && images.length > 0) {
                // Get current images
                List<String> currentImageUrls = item.getImageUrls() != null
                    ? new ArrayList<>(item.getImageUrls())
                    : new ArrayList<>();

                // Check total image limit (max 5 images)
                if (currentImageUrls.size() + images.length > 5) {
                    response.put("success", false);
                    response.put("error", "Maximum 5 images allowed per item");
                    return ResponseEntity.badRequest().body(response);
                }

                // Upload new images
                List<String> newImageUrls = new ArrayList<>();
                try {
                    for (MultipartFile img : images) {
                        if (!img.isEmpty()) {
                            String fileName =
                                "item_" +
                                System.currentTimeMillis() +
                                "_" +
                                img.getOriginalFilename();
                            fileStorageService.uploadFile(img, fileName);
                            String imageUrl = fileStorageService.getDownloadUrl(
                                fileName
                            );
                            newImageUrls.add(imageUrl);
                        }
                    }
                } catch (Exception e) {
                    response.put("success", false);
                    response.put(
                        "error",
                        "Fehler beim Hochladen der Bilder: " + e.getMessage()
                    );
                    return ResponseEntity.badRequest().body(response);
                }

                // Add new images to existing ones
                currentImageUrls.addAll(newImageUrls);
                item.setImageUrls(currentImageUrls);
            }

            // Save updated item
            Item savedItem = itemRepository.save(item);

            response.put("success", true);
            response.put("message", "Artikel erfolgreich aktualisiert");
            response.put("data", createItemResponse(savedItem, username));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Aktualisieren des Artikels: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<Map<String, Object>> addImagesToItem(
        @PathVariable Long id,
        @RequestParam("images") MultipartFile[] images,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = authentication.getName();
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            // Check if user owns the item
            if (!item.getUser().getUsername().equals(username)) {
                response.put("success", false);
                response.put(
                    "error",
                    "Sie können nur zu Ihren eigenen Artikeln Bilder hinzufügen"
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Get current images
            List<String> currentImageUrls = item.getImageUrls() != null
                ? new ArrayList<>(item.getImageUrls())
                : new ArrayList<>();

            // Check total image limit (max 5 images)
            if (currentImageUrls.size() + images.length > 5) {
                response.put("success", false);
                response.put("error", "Maximum 5 images allowed per item");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload new images
            List<String> newImageUrls = new ArrayList<>();
            try {
                for (MultipartFile img : images) {
                    if (!img.isEmpty()) {
                        String fileName =
                            "item_" +
                            System.currentTimeMillis() +
                            "_" +
                            img.getOriginalFilename();
                        fileStorageService.uploadFile(img, fileName);
                        String imageUrl = fileStorageService.getDownloadUrl(
                            fileName
                        );
                        newImageUrls.add(imageUrl);
                    }
                }
            } catch (Exception e) {
                response.put("success", false);
                response.put(
                    "error",
                    "Fehler beim Hochladen der Bilder: " + e.getMessage()
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Add new images to existing ones
            currentImageUrls.addAll(newImageUrls);
            item.setImageUrls(currentImageUrls);

            // Save updated item
            Item savedItem = itemRepository.save(item);

            response.put("success", true);
            response.put("message", "Bilder erfolgreich hinzugefügt");
            response.put("data", createItemResponse(savedItem, username));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Hinzufügen der Bilder: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Map<String, Object>> deleteImage(
        @PathVariable Long id,
        @RequestParam("imageUrl") String imageUrl,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = authentication.getName();
            Optional<Item> itemOpt = itemRepository.findById(id);

            if (!itemOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Artikel nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }

            Item item = itemOpt.get();

            // Check if user owns the item
            if (!item.getUser().getUsername().equals(username)) {
                response.put("success", false);
                response.put(
                    "error",
                    "Sie können nur Bilder von Ihren eigenen Artikeln löschen"
                );
                return ResponseEntity.badRequest().body(response);
            }

            // Remove image from list
            List<String> currentUrls = item.getImageUrls();
            if (currentUrls != null && currentUrls.contains(imageUrl)) {
                currentUrls.remove(imageUrl);
                item.setImageUrls(currentUrls);
                itemRepository.save(item);

                // Delete from B2 storage
                try {
                    String fileName = imageUrl.substring(
                        imageUrl.lastIndexOf('/') + 1
                    );
                    if (fileName.contains("?")) {
                        fileName = fileName.substring(0, fileName.indexOf("?"));
                    }
                    fileStorageService.deleteFile(fileName);
                } catch (Exception e) {
                    System.err.println(
                        "Failed to delete image from B2: " + e.getMessage()
                    );
                }

                response.put("success", true);
                response.put("message", "Bild erfolgreich gelöscht");
                response.put("data", createItemResponse(item, username));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Bild nicht gefunden");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put(
                "error",
                "Fehler beim Löschen des Bildes: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> createItemResponse(
        Item item,
        User currentUser
    ) {
        return createItemResponse(
            item,
            currentUser != null ? currentUser.getUsername() : null
        );
    }
}
