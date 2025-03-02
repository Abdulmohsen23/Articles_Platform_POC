package com.example.first.controllers;

import com.example.first.models.ArticleEntity;
import com.example.first.models.UserEntity;
import com.example.first.repository.ArticleRepository;
import com.example.first.repository.UserRepository;
import com.example.first.service.ArticleService;
import com.example.first.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Value("${article.cover.upload.dir}")
    private String uploadDir;

    // Available domains for articles
    private final List<String> AVAILABLE_DOMAINS = Arrays.asList(
            "Artificial Intelligence",
            "Data Science",
            "Web Development",
            "Mobile Development",
            "Cloud Computing",
            "Cybersecurity",
            "Machine Learning",
            "DevOps",
            "Blockchain",
            "Internet of Things"
    );
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserRepository userRepository;

    // Helper method to check if user is logged in
    private UserEntity getLoggedInUser(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }
        return user;
    }

    // Show article creation form
    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Check if user is logged in
            UserEntity user = getLoggedInUser(session);

            model.addAttribute("article", new ArticleEntity());
            model.addAttribute("domains", AVAILABLE_DOMAINS);
            return "articles/create";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to create an article");
            return "redirect:/users/login";
        }
    }

    @PostMapping("/new")
    public String createArticle(@ModelAttribute ArticleEntity article,
                                @RequestParam("coverImage") MultipartFile coverImage,
                                @RequestParam(value = "attachments", required = false) MultipartFile file,
                                @RequestParam("keywordsInput") String keywordsInput,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            // Check if user is logged in
            UserEntity user = getLoggedInUser(session);

            // Handle cover image upload
            if (!coverImage.isEmpty()) {
                // Create uploads directory if it doesn't exist
                String uploadPath = System.getProperty("user.dir") + File.separator + uploadDir;
                File uploadsDir = new File(uploadPath);
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }

                // Generate unique filename
                String fileName = UUID.randomUUID().toString() + getFileExtension(coverImage.getOriginalFilename());

                // Create full path
                Path filePath = Paths.get(uploadPath, fileName);
                Files.copy(coverImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Set the path that will be used to serve the image
                article.setCoverImagePath("/uploads/" + fileName);
            }

            // Handle file upload
            if (file != null && !file.isEmpty()) {
                // Create files directory if it doesn't exist
                String filesPath = System.getProperty("user.dir") + File.separator + "uploads/files";
                File filesDir = new File(filesPath);
                if (!filesDir.exists()) {
                    filesDir.mkdirs();
                }

                // Generate unique filename
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(filesPath, fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Set file details
                article.setFileName(file.getOriginalFilename());
                article.setFilePath("/uploads/files/" + fileName);
                article.setFileType(file.getContentType());
                article.setFileSize(file.getSize());
            }

            // Handle keywords
            if (keywordsInput != null && !keywordsInput.trim().isEmpty()) {
                List<String> keywords = Arrays.asList(keywordsInput.split(","));
                article.setKeywords(keywords);
            }

            // Save article with logged-in user
            ArticleEntity savedArticle = articleService.createArticle(article, user);

            redirectAttributes.addFlashAttribute("successMessage",
                    user.getRole() == UserEntity.Role.ADMIN ? "Article published successfully!" : "Article submitted for review!");
            return "redirect:/articles/" + savedArticle.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create article: " + e.getMessage());
            redirectAttributes.addFlashAttribute("article", article);
            return "redirect:/articles/new";
        }
    }



    // Utility method to get file extension
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // Default extension if none found
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // Show article edit form - with authentication
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            UserEntity user = getLoggedInUser(session);
            ArticleEntity article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Article not found"));

            if (!article.getAuthor().getId().equals(user.getId()) && user.getRole() != UserEntity.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this article");
                return "redirect:/articles/" + id;
            }

            model.addAttribute("article", article);
            model.addAttribute("domains", AVAILABLE_DOMAINS);
            model.addAttribute("isAdmin", user.getRole() == UserEntity.Role.ADMIN);
            return "articles/edit";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to edit articles");
            return "redirect:/users/login";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateArticle(@PathVariable Long id,
                                @ModelAttribute ArticleEntity article,
                                @RequestParam(required = false) MultipartFile coverImage,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            UserEntity user = getLoggedInUser(session);

            // Get existing article to preserve status for admin
            ArticleEntity existingArticle = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Article not found"));

            // Set appropriate status
            if (user.getRole() != UserEntity.Role.ADMIN) {
                article.setStatus(ArticleEntity.Status.PENDING);
            } else {
                article.setStatus(existingArticle.getStatus());
            }

            // If no new cover image is uploaded, preserve the existing one
            if (coverImage == null || coverImage.isEmpty()) {
                article.setCoverImagePath(existingArticle.getCoverImagePath());
            }

            // Update article with cover image
            articleService.updateArticle(id, article, user, coverImage);

            redirectAttributes.addFlashAttribute("successMessage", "Article updated successfully");
            return "redirect:/articles/" + id;
        } catch (RuntimeException e) {
            if (e.getMessage().equals("User not authenticated")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please login to edit articles");
                return "redirect:/users/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update article: " + e.getMessage());
            return "redirect:/articles/edit/" + id;
        }
    }

    // List user's articles - with authentication
    @GetMapping("/myarticles")
    public String listUserArticles(Model model,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        try {
            UserEntity user = getLoggedInUser(session);
            Pageable pageable = PageRequest.of(page, size);
            Page<ArticleEntity> articlePage = articleService.getUserArticles(user.getId(), pageable);

            model.addAttribute("articles", articlePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", articlePage.getTotalPages());
            return "articles/myarticles";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your articles");
            return "redirect:/users/login";
        }
    }

    // Admin methods - with role check
    @GetMapping("/admin/pending")
    public String listPendingArticles(Model model,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        try {
            // Check if user is logged in and is admin
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied. Admin rights required.");
                return "redirect:/articles";
            }

            // Get paginated pending articles
            Pageable pageable = PageRequest.of(page, size);
            Page<ArticleEntity> articlePage = articleService.getPendingArticles(pageable);

            // Add pagination attributes to model
            model.addAttribute("articles", articlePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", articlePage.getTotalPages());

            return "articles/admin/pending";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to access admin features");
            return "redirect:/users/login";
        }
    }

    // Admin review - with role check
    @PostMapping("/admin/review/{id}")
    public String reviewArticle(@PathVariable Long id,
                                @RequestParam ArticleEntity.Status status,
                                @RequestParam(required = false) String comments,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            // Check if user is logged in and is admin
            UserEntity admin = getLoggedInUser(session);
            if (admin.getRole() != UserEntity.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied. Admin rights required.");
                return "redirect:/articles";
            }

            // Update article status
            ArticleEntity article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Article not found"));



            // Add success message
            String statusMessage = switch(status) {
                case APPROVED -> "Article has been approved";
                case REJECTED -> "Article has been rejected";
                case REVISION_NEEDED -> "Article has been returned for revision";
                default -> "Article status updated";
            };

            article.setStatus(status);
            article.setAdminComments(comments);
            article.setReviewedBy(admin);
            article.setReviewedAt(LocalDateTime.now());

            // Set publishedAt when article is approved
            if (status == ArticleEntity.Status.APPROVED) {
                article.setPublishedAt(LocalDateTime.now());
            }

            articleService.reviewArticle(id,status, comments,admin);

            redirectAttributes.addFlashAttribute("successMessage", statusMessage);
            return "redirect:/articles/admin/pending";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to review article: " + e.getMessage());
            return "redirect:/articles/admin/pending";
        }
    }

    // Public methods (no authentication required)
    @GetMapping("/{id}")
    public String viewArticle(@PathVariable Long id, Model model) {
        ArticleEntity article = articleService.getArticleById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        // Get recent articles by the same author (limit to 3)
        List<ArticleEntity> recentArticles = articleService.getRecentArticlesByAuthor(
                article.getAuthor().getId(),
                id,  // Exclude current article
                3    // Limit
        );

        model.addAttribute("article", article);
        model.addAttribute("recentArticles", recentArticles);
        return "articles/view";
    }


    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getArticleDetails(@PathVariable Long id, HttpSession session) {
        try {
            // Verify admin access
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            ArticleEntity article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Article not found"));

            // Create a map with the article details
            Map<String, Object> response = new HashMap<>();
            response.put("id", article.getId());
            response.put("title", article.getTitle());
            response.put("brief", article.getBrief());
            response.put("content", article.getContent());
            response.put("domain", article.getDomain());
            response.put("createdAt", article.getCreatedAt());
            response.put("keywords", article.getKeywords());
            response.put("author", Map.of(
                    "id", article.getAuthor().getId(),
                    "name", article.getAuthor().getName()
            ));
            response.put("coverImagePath", article.getCoverImagePath());
            response.put("fileName", article.getFileName());
            response.put("filePath", article.getFilePath());
            response.put("fileSize", article.getFileSize());
            response.put("fileType", article.getFileType());


            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching article details: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public String searchArticles(@RequestParam String keyword, Model model) {
        List<ArticleEntity> searchResults = articleService.searchArticles(keyword);
        model.addAttribute("articles", searchResults);
        model.addAttribute("keyword", keyword);
        return "articles/search";
    }

    @GetMapping
    public String listArticles(Model model,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size) {

        // Get paginated articles
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleEntity> articlePage = articleService.getApprovedArticles(pageable);

        // Add pagination attributes to model
        model.addAttribute("articles", articlePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articlePage.getTotalPages());
        return "articles/list";
    }

    @PostMapping("/api/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
            String uploadPath = System.getProperty("user.dir") + File.separator + uploadDir;
            Path filePath = Paths.get(uploadPath, fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of(
                    "location", "/uploads/" + fileName
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }



    // Add to ArticleController.java
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getArticleStatistics(HttpSession session) {
        try {
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            Map<String, Object> statistics = new HashMap<>();

            // Total articles (all statuses)
            statistics.put("totalArticles", articleRepository.count());

            // Articles by status
            Map<String, Long> articlesByStatus = articleService.getArticleCountByStatus();
            statistics.put("articlesByStatus", articlesByStatus);

            // Only approved articles by domain
            Map<String, Long> approvedArticlesByDomain = articleService.getApprovedArticleCountByDomain();
            statistics.put("approvedArticlesByDomain", approvedArticlesByDomain);

            // Get domain statistics with status breakdown
            Map<String, Map<String, Long>> domainStatistics = articleService.getDomainStatistics();
            statistics.put("domainStatistics", domainStatistics);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Check if user is logged in and is admin
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied. Admin rights required.");
                return "redirect:/articles";
            }

            return "articles/admin/dashboard"; // Updated path
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to access admin features");
            return "redirect:/users/login";
        }
    }

    // In ArticleController.java
    @GetMapping("/api/user-statistics")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserPublicationStatistics(HttpSession session) {
        try {
            UserEntity admin = getLoggedInUser(session);
            if (admin.getRole() != UserEntity.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(null);
            }

            List<Map<String, Object>> userStats = articleService.getUserPublicationStatistics();
            return ResponseEntity.ok(userStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/api/articles/analytics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getArticleAnalytics(
            @RequestParam(required = false, defaultValue = "1") Integer months,
            HttpSession session) {
        try {
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            Map<String, Object> analytics = articleService.getArticleAnalytics(months);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/articles/analytics/period")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getArticleAnalyticsByPeriod(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth,
            HttpSession session) {
        try {
            UserEntity user = getLoggedInUser(session);
            if (user.getRole() != UserEntity.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            Map<String, Object> analytics = articleService.getArticleAnalyticsByPeriod(year, startMonth, endMonth);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }



    @GetMapping("/api/chatbot/articles/by-domain")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChatbotArticles(
            @RequestParam String domain,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ArticleEntity> articlesPage = articleService.getArticlesByDomainPaged(domain, pageable);

        List<ArticleEntity> articles = articlesPage.getContent()
                .stream()
                .filter(article -> article.getStatus() == ArticleEntity.Status.APPROVED)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("articles", formatArticlesForChatbot(articles));
        response.put("hasMore", !articlesPage.isLast() && articlesPage.hasNext());
        response.put("totalPages", articlesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
    //new method for showing all contributers
    @GetMapping("/api/chatbot/authors")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuthors() {
        List<UserEntity> users = userRepository.findByRoleOrderByNameAsc(UserEntity.Role.USER);

        Map<String, Object> response = new HashMap<>();
        response.put("authors", users.stream()
                .map(user -> Map.of(
                        "id", user.getId(),
                        "name", user.getName()
                ))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }


    @GetMapping("/api/chatbot/articles/latest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLatestArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ArticleEntity> articlesPage = articleRepository.findByStatusOrderByCreatedAtDesc(
                ArticleEntity.Status.APPROVED,
                pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("articles", formatArticlesForChatbot(articlesPage.getContent()));
        response.put("hasMore", !articlesPage.isLast() && articlesPage.hasNext());
        response.put("totalPages", articlesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
    // search by keywords
    @GetMapping("/api/chatbot/articles/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchArticles(@RequestParam String keywords) {
        List<ArticleEntity> articles = articleService.searchArticles(keywords)
                .stream()
                .filter(article -> article.getStatus() == ArticleEntity.Status.APPROVED)
                .limit(5)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("articles", formatArticlesForChatbot(articles)));
    }

    @GetMapping("/api/chatbot/articles/by-author")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getArticlesByAuthor(
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ArticleEntity> articlesPage = articleRepository
                .findByAuthorNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        name,
                        ArticleEntity.Status.APPROVED,
                        pageable
                );

        Map<String, Object> response = new HashMap<>();
        response.put("articles", formatArticlesForChatbot(articlesPage.getContent()));
        response.put("hasMore", !articlesPage.isLast() && articlesPage.hasNext());
        response.put("totalPages", articlesPage.getTotalPages());
        response.put("authorName", name);

        return ResponseEntity.ok(response);
    }

    // Helper method to format articles for chatbot response
    private List<Map<String, Object>> formatArticlesForChatbot(List<ArticleEntity> articles) {
        return articles.stream()
                .map(article -> {
                    Map<String, Object> articleMap = new HashMap<>();
                    articleMap.put("id", article.getId());
                    articleMap.put("title", article.getTitle());
                    articleMap.put("brief", article.getBrief());
                    articleMap.put("author", article.getAuthor().getName());
                    articleMap.put("domain", article.getDomain());
                    return articleMap;
                })
                .collect(Collectors.toList());
    }

}