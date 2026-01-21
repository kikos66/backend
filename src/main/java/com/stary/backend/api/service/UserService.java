package com.stary.backend.api.service;

import com.stary.backend.api.comments.CommentRepository;
import com.stary.backend.api.model.EditRequest;
import com.stary.backend.api.orders.repositories.OrderItemRepository;
import com.stary.backend.api.orders.repositories.PurchaseOrderRepository;
import com.stary.backend.api.products.Product;
import com.stary.backend.api.products.ProductImage;
import com.stary.backend.api.products.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import com.stary.backend.api.model.LoginRequest;
import com.stary.backend.api.model.RegisterRequest;
import com.stary.backend.api.users.repositories.RefreshTokenRepository;
import com.stary.backend.api.users.repositories.UserRepository;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.stary.backend.api.users.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import com.stary.backend.api.reviews.ReviewRepository;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9+&-]+(?:.[a-zA-Z0-9_+&-]+)*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,7}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);
    private final Path rootUploadPath;
    private final ProductRepository productRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, TokenManager tokenManager,
                       @Value("${file.upload-dir}") String uploadDir, ProductRepository productRepository,
                       CommentRepository commentRepository, ReviewRepository reviewRepository,
                       PurchaseOrderRepository purchaseOrderRepository, OrderItemRepository orderItemRepository) throws IOException {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenManager = tokenManager;

        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootUploadPath.resolve("profiles"));
        this.productRepository = productRepository;
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public User registerNewUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email is already registered.");
        }
        if(!PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalStateException("Email is invalid.");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(Role.ROLE_USER);

        System.out.println(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid username/password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid username/password");
        }
        return user;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        //refreshTokenRepository.deleteByUser(user);
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plusMillis(tokenManager.getJwtRefreshExpirationMs()));
        return refreshTokenRepository.save(rt);
    }

    @Transactional
    public boolean edit(Long id, EditRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        boolean updated = false;

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail()) &&
                PATTERN.matcher(req.getEmail()).matches()) {
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                throw new IllegalStateException("Email is already in use.");
            }
            user.setEmail(req.getEmail());
            updated = true;
        }

        if (req.getUsername() != null && !req.getUsername().equals(user.getUsername())
                && !req.getUsername().isEmpty()) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new IllegalStateException("Username is already taken.");
            }
            user.setUsername(req.getUsername());
            updated = true;
        }

        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            String encoded = passwordEncoder.encode(req.getPassword());
            user.setPassword(encoded);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            return true;
        }

        return false; // nothing changed
    }

    public void deleteProfileFile(User user) {
        if (user.getProfilePicture() != null) {
            Path profilesDir = rootUploadPath.resolve("profiles");
            try {
                Files.deleteIfExists(profilesDir.resolve(user.getProfilePicture()));
            } catch (Exception e) {
                System.err.println("Failed to delete profile picture: " + user.getProfilePicture());
            }
        }
    }

    public void deleteProductFiles(Product product) {
        Path productsDir = rootUploadPath.resolve("products");
        if (product.getImages() != null) {
            for (ProductImage img : product.getImages()) {
                try {
                    Path file = productsDir.resolve(img.getFilename());
                    Files.deleteIfExists(file);
                } catch (Exception e) {
                    System.err.println("Failed to delete file: " + img.getFilename());
                }
            }
        }
    }

    @Transactional
    public boolean deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        orderItemRepository.deleteItemsByBuyerId(user.getId());
        purchaseOrderRepository.deleteByBuyerId(user.getId());

        List<Product> products = productRepository.findByOwnerId(user.getId());
        for (Product product : products) {
            commentRepository.deleteByProductId(product.getId());
            deleteProductFiles(product);
            productRepository.delete(product);
        }

        reviewRepository.deleteByAuthorId(user.getId());
        reviewRepository.deleteByTargetId(user.getId());

        deleteProfileFile(user);
        deleteRefreshTokensForUser(user);
        userRepository.delete(user);
        return true;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public void deleteRefreshToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }

    public void deleteRefreshTokensForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public String storeProfileFile(MultipartFile file, String oldFilename) throws IOException {

        Path profilesDir = rootUploadPath.resolve("profiles");
        Files.createDirectories(profilesDir);

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.')))
                .orElse("");

        String filename = UUID.randomUUID() + ext;
        Path target = profilesDir.resolve(filename);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // delete old avatar
        if (oldFilename != null) {
            Files.deleteIfExists(profilesDir.resolve(oldFilename));
        }

        return filename;
    }

    //admin role giver
    /*@Component
    public class AdminSeeder implements CommandLineRunner {

        private final UserRepository userRepository;

        public AdminSeeder(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public void run(String... args) {
            userRepository.findByEmail("admin@admin.com").ifPresent(user -> {
                user.setRole(Role.ROLE_ADMIN);
                userRepository.save(user);
            });
        }
    }*/
}
