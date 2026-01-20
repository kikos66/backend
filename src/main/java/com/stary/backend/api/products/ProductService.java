package com.stary.backend.api.products;

import com.stary.backend.api.products.repositories.ProductImageRepository;
import com.stary.backend.api.products.repositories.ProductRepository;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import com.stary.backend.api.users.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final UserRepository userRepository;
    private final Path rootUploadPath;

    private static final Set<String> ALLOWED = Set.of("image/png","image/jpeg","image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int MAX_IMAGES_PER_PRODUCT = 5;

    public ProductService(ProductRepository productRepository,
                          ProductImageRepository imageRepository,
                          UserRepository userRepository,
                          @Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootUploadPath.resolve("products"));
        Files.createDirectories(rootUploadPath.resolve("profiles"));
    }

    @Transactional
    public Product createProduct(Product p, MultipartFile[] images) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated create not allowed");
        }
        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        p.setOwner(owner);

        Product saved = productRepository.save(p);
        if (images != null && images.length > 0) {
            saveImagesForProduct(saved, images);
        }
        return saved;
    }

    @Transactional
    public void saveImagesForProduct(Product product, MultipartFile[] images) throws IOException {
        List<ProductImage> current = imageRepository.findByProductId(product.getId());
        int existing = current.size();
        if (existing + images.length > MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("Exceeds max images per product");
        }

        for (MultipartFile file : images) {
            validateImage(file);
            String newName = storeProductFile(file);
            ProductImage img = new ProductImage();
            img.setFilename(newName);
            img.setContentType(file.getContentType());
            img.setProduct(product);
            imageRepository.save(img);
            product.getImages().add(img);
        }
        productRepository.save(product);
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() <= 0) throw new IllegalArgumentException("Empty file");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("File too large (>5MB)");
        if (!ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("Invalid image type");
    }

    private String storeProductFile(MultipartFile file) throws IOException {
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.')))
                .orElse("");
        String name = UUID.randomUUID().toString() + ext;
        Path target = rootUploadPath.resolve("products").resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @PostConstruct
    public void debugFileAccess() {
        Path test = Paths.get("uploads/products").toAbsolutePath();
        System.out.println(">>> Upload folder exists: " + Files.exists(test));
        System.out.println(">>> Upload folder readable: " + Files.isReadable(test));
    }

    public List<Product> searchFiltered(
            String search,
            String category,
            String condition,
            Double minPrice,
            Double maxPrice
    ) {
        Long ownerId = null;
        try {
            ownerId = getAuthenticatedUser().getId();
        } catch (Exception ignored) {}

        search = (search == null || search.isBlank()) ? null : search;
        category = (category == null || category.isBlank()) ? null : category;
        condition = (condition == null || condition.isBlank()) ? null : condition;

        return productRepository.searchFiltered(
                search,
                category,
                condition,
                minPrice,
                maxPrice,
                ownerId
        );
    }

    public List<Product> suggest(String q) {
        return productRepository.suggest(q, PageRequest.of(0, 5));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        User current = getAuthenticatedUser();
        if (!product.getOwner().getId().equals(current.getId()) &&
                current.getRole() != Role.ROLE_ADMIN && current.getRole() != Role.ROLE_MODERATOR) {
            throw new SecurityException("Not owner of this product");
        }

        Path productsDir = rootUploadPath.resolve("products");
        if (product.getImages() != null) {
            for (var img : product.getImages()) {
                try {
                    Path file = productsDir.resolve(img.getFilename());
                    Files.deleteIfExists(file);
                } catch (Exception e) {
                    System.err.println("Failed to delete file: " + img.getFilename());
                }
            }
        }

        productRepository.delete(product);
    }

    public void assertOwner(Product product) {
        User current = getAuthenticatedUser();
        if (!product.getOwner().getId().equals(current.getId())) {
            throw new SecurityException("Not owner");
        }
    }

    public List<Product> findMine() {
        User user = getAuthenticatedUser();
        return productRepository.findByOwnerId(user.getId());
    }
}
