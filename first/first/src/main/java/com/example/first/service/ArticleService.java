package com.example.first.service;
import com.example.first.models.ArticleEntity;
import com.example.first.models.UserEntity;
import com.example.first.repository.ArticleRepository;
import com.example.first.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Service
public class ArticleService
{
    @Autowired
    private ArticleRepository articleRepository;

    @Value("${article.cover.upload.dir}")
    private String uploadDir;
    @Autowired
    private UserRepository userRepository;

    //     Create new article
    @Transactional
    public ArticleEntity createArticle(ArticleEntity article, UserEntity author) {
        // Set the author
        article.setAuthor(author);

        // Set initial status
        article.setStatus(ArticleEntity.Status.PENDING);

        // Save and return the article
        return articleRepository.save(article);
    }


    // Get article by ID
    public Optional<ArticleEntity> getArticleById(Long id) {
        return articleRepository.findById(id);
    }

    // Add this method for pagination
    public org.springframework.data.domain.Page<ArticleEntity> getPendingArticles(org.springframework.data.domain.Pageable pageable) {
        return articleRepository.findByStatusOrderByCreatedAtDesc(ArticleEntity.Status.PENDING, pageable);
    }
    // For non-paginated version (keep this one)
    public List<ArticleEntity> getPendingArticles() {
        return articleRepository.findByStatus(ArticleEntity.Status.PENDING);
    }

    // Update article status (for admin)
    @Transactional
    public ArticleEntity updateArticleStatus(Long articleId,
                                             ArticleEntity.Status newStatus,
                                             String adminComments) {
        Optional<ArticleEntity> articleOpt = articleRepository.findById(articleId);

        if (articleOpt.isPresent()) {
            ArticleEntity article = articleOpt.get();
            article.setStatus(newStatus);

            if (adminComments != null && !adminComments.trim().isEmpty()) {
                article.setAdminComments(adminComments);
            }

            return articleRepository.save(article);
        }

        throw new RuntimeException("Article not found with id: " + articleId);
    }

    // Get user's articles
    public List<ArticleEntity> getUserArticles(Long userId) {
        return articleRepository.findByAuthorId(userId);
    }

    // Get articles by domain
    public List<ArticleEntity> getArticlesByDomain(String domain) {
        return articleRepository.findByDomain(domain);
    }

    // Search articles by title
    public List<ArticleEntity> searchArticles(String keyword) {
        return articleRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // Get latest approved articles
    public List<ArticleEntity> getLatestApprovedArticles() {
        return articleRepository.findByStatus(ArticleEntity.Status.APPROVED)
                .stream()
                .limit(10)
                .toList();
    }



    @Transactional
    public ArticleEntity updateArticle(Long articleId, ArticleEntity updatedArticle, UserEntity user, MultipartFile coverImage) {
        Optional<ArticleEntity> existingArticleOpt = articleRepository.findById(articleId);

        if (existingArticleOpt.isPresent()) {
            ArticleEntity existingArticle = existingArticleOpt.get();

            // Check if user is author
            if (!existingArticle.getAuthor().getId().equals(user.getId()) &&
                    user.getRole() != UserEntity.Role.ADMIN) {
                throw new RuntimeException("Unauthorized to update this article");
            }

            // Handle cover image upload if new image is provided
            if (coverImage != null && !coverImage.isEmpty()) {
                try {
                    // Create uploads directory if it doesn't exist
                    String uploadPath = System.getProperty("user.dir") + File.separator + uploadDir;
                    File uploadsDir = new File(uploadPath);
                    if (!uploadsDir.exists()) {
                        uploadsDir.mkdirs();
                    }

                    // Generate unique filename
                    String fileName = UUID.randomUUID().toString() + getFileExtension1(coverImage.getOriginalFilename());
                    Path filePath = Paths.get(uploadPath, fileName);

                    // Save the file
                    Files.copy(coverImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    // Update the cover image path
                    existingArticle.setCoverImagePath("/uploads/" + fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save cover image: " + e.getMessage());
                }
            }

            // Update fields but preserve certain ones
            existingArticle.setTitle(updatedArticle.getTitle());
            existingArticle.setBrief(updatedArticle.getBrief());
            existingArticle.setContent(updatedArticle.getContent());
            existingArticle.setKeywords(updatedArticle.getKeywords());
            existingArticle.setDomain(updatedArticle.getDomain());
            existingArticle.setStatus(updatedArticle.getStatus());

            // If it was previously rejected and now being resubmitted
            if (existingArticle.getStatus() == ArticleEntity.Status.REJECTED ||
                    existingArticle.getStatus() == ArticleEntity.Status.REVISION_NEEDED) {
                existingArticle.setStatus(ArticleEntity.Status.PENDING);
                existingArticle.setAdminComments(null); // Clear previous comments
            }

            return articleRepository.save(existingArticle);
        }

        throw new RuntimeException("Article not found with id: " + articleId);
    }

    // Utility method to get file extension
    private String getFileExtension1(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // Default extension if none found
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // Delete article
    @Transactional
    public void deleteArticle(Long articleId, UserEntity user) {
        Optional<ArticleEntity> articleOpt = articleRepository.findById(articleId);

        if (articleOpt.isPresent()) {
            ArticleEntity article = articleOpt.get();

            // Check if user is author or admin
            if (article.getAuthor().getId().equals(user.getId()) ||
                    user.getRole() == UserEntity.Role.ADMIN) {
                articleRepository.delete(article);
            } else {
                throw new RuntimeException("Unauthorized to delete this article");
            }
        } else {
            throw new RuntimeException("Article not found with id: " + articleId);
        }
    }

    @Transactional
    public ArticleEntity reviewArticle(Long articleId, ArticleEntity.Status status,
                                       String comments, UserEntity admin) {
        ArticleEntity article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        // Verify the reviewer is an admin
        if (admin.getRole() != UserEntity.Role.ADMIN) {
            throw new RuntimeException("Only admins can review articles");
        }

        // Update article status
        article.setStatus(status);
        article.setAdminComments(comments);
        article.setReviewedBy(admin);
        article.setReviewedAt(LocalDateTime.now());


        return articleRepository.save(article);
    }

    // Get articles requiring revision
    public List<ArticleEntity> getArticlesNeedingRevision() {
        return articleRepository.findByStatus(ArticleEntity.Status.REVISION_NEEDED);
    }

    // Get approved articles
    public List<ArticleEntity> getApprovedArticles() {
        return articleRepository.findByStatus(ArticleEntity.Status.APPROVED);
    }
    public org.springframework.data.domain.Page<ArticleEntity> getApprovedArticles(org.springframework.data.domain.Pageable pageable) {
        return articleRepository.findByStatusOrderByCreatedAtDesc(ArticleEntity.Status.APPROVED, pageable);
    }

    // Get articles by status
    public List<ArticleEntity> getArticlesByStatus(ArticleEntity.Status status) {
        return articleRepository.findByStatus(status);
    }

    // Method to handle file upload
    private String saveImage(MultipartFile file) throws IOException {
        // Create uploads directory if it doesn't exist
        File uploadsDir = new File(uploadDir);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
        Path filePath = Paths.get(uploadDir, fileName);

        // Save the file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return the path that will be stored in the database
        return "/uploads/" + fileName;
    }

    // Helper method to get file extension
    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    // Update in your createArticle method
    @Transactional
    public ArticleEntity createArticle(ArticleEntity article, MultipartFile coverImage, UserEntity author) throws IOException {
        if (coverImage != null && !coverImage.isEmpty()) {
            String imagePath = saveImage(coverImage);
            article.setCoverImagePath(imagePath);
        }

        article.setAuthor(author);
        return articleRepository.save(article);
    }

    public Page<ArticleEntity> getUserArticles(Long userId, Pageable pageable) {
        return articleRepository.findByAuthorId(userId, pageable);
    }

    public List<ArticleEntity> getRecentArticlesByAuthor(Long authorId, Long excludeArticleId, int limit) {
        return articleRepository.findByAuthorIdAndIdNotAndStatusOrderByCreatedAtDesc(
                authorId,
                excludeArticleId,
                ArticleEntity.Status.APPROVED,
                PageRequest.of(0, limit)
        );
    }

    // Add to ArticleService.java
    public Map<String, Long> getArticleCountByDomain() {
        List<ArticleEntity> articles = articleRepository.findAll();
        return articles.stream()
                .collect(Collectors.groupingBy(
                        ArticleEntity::getDomain,
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getArticleCountByStatus() {
        List<ArticleEntity> articles = articleRepository.findAll();
        return articles.stream()
                .collect(Collectors.groupingBy(
                        article -> article.getStatus().name(),
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getApprovedArticleCountByDomain() {
        List<ArticleEntity> articles = articleRepository.findAll();
        return articles.stream()
                .filter(article -> article.getStatus() == ArticleEntity.Status.APPROVED)
                .collect(Collectors.groupingBy(
                        ArticleEntity::getDomain,
                        Collectors.counting()
                ));
    }

    // In ArticleService.java
    public List<Map<String, Object>> getUserPublicationStatistics() {
        List<UserEntity> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    Map<String, Object> stats = new HashMap<>();
                    List<ArticleEntity> userArticles = articleRepository.findByAuthor(user);

                    stats.put("userName", user.getName());
                    stats.put("totalArticles", userArticles.size());
                    stats.put("publishedArticles",
                            userArticles.stream().filter(a -> a.getStatus() == ArticleEntity.Status.APPROVED).count());
                    stats.put("pendingArticles",
                            userArticles.stream().filter(a -> a.getStatus() == ArticleEntity.Status.PENDING).count());
                    stats.put("revisionNeededArticles",
                            userArticles.stream().filter(a -> a.getStatus() == ArticleEntity.Status.REVISION_NEEDED).count());
                    stats.put("rejectedArticles",
                            userArticles.stream().filter(a -> a.getStatus() == ArticleEntity.Status.REJECTED).count());

                    return stats;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, Long>> getDomainStatistics() {
        List<ArticleEntity> articles = articleRepository.findAll();

        // Group articles by domain and count by status
        Map<String, Map<String, Long>> domainStats = new HashMap<>();

        for (ArticleEntity article : articles) {
            String domain = article.getDomain();
            domainStats.putIfAbsent(domain, new HashMap<>());

            Map<String, Long> stats = domainStats.get(domain);
            // Increment total count
            stats.merge("total", 1L, Long::sum);
            // Increment status count
            stats.merge(article.getStatus().name(), 1L, Long::sum);
        }

        return domainStats;
    }

    public Map<String, Object> getArticleAnalytics(Integer months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<ArticleEntity> articles = articleRepository.findByCreatedAtAfterAndStatus(
                startDate, ArticleEntity.Status.APPROVED);

        Map<String, Object> analytics = new HashMap<>();

        // Get published articles count by month
        Map<String, Long> monthlyPublications = articles.stream()
                .collect(Collectors.groupingBy(
                        article -> article.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()
                ));

        // Get domain distribution for the period
        Map<String, Long> domainDistribution = articles.stream()
                .collect(Collectors.groupingBy(
                        ArticleEntity::getDomain,
                        Collectors.counting()
                ));

        // Calculate average articles per month
        double averagePerMonth = articles.size() / (double) months;

        // Calculate growth rate (compared to previous period)
        LocalDateTime previousStartDate = startDate.minusMonths(months);
        List<ArticleEntity> previousArticles = articleRepository.findByCreatedAtBetweenAndStatus(
                previousStartDate, startDate, ArticleEntity.Status.APPROVED);
        double growthRate = calculateGrowthRate(previousArticles.size(), articles.size());

        // Find peak publishing months
        Optional<Map.Entry<String, Long>> peakMonth = monthlyPublications.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        analytics.put("monthlyPublications", monthlyPublications);
        analytics.put("domainDistribution", domainDistribution);
        analytics.put("totalPublished", articles.size());
        analytics.put("averagePerMonth", averagePerMonth);
        analytics.put("growthRate", growthRate);
        analytics.put("peakMonth", peakMonth.orElse(null));

        return analytics;
    }

    private double calculateGrowthRate(int previous, int current) {
        if (previous == 0) return 100.0;
        return ((current - previous) / (double) previous) * 100.0;
    }

    public Map<String, Object> getArticleAnalyticsByPeriod(Integer year, Integer startMonth, Integer endMonth) {
        LocalDateTime startDate;
        LocalDateTime endDate;

        // Validate input
        if (year == null) {
            throw new IllegalArgumentException("Year must be provided");
        }

        // Handle custom period
        if (startMonth != null && endMonth != null) {
            // Validate months
            if (startMonth < 1 || startMonth > 12 || endMonth < 1 || endMonth > 12) {
                throw new IllegalArgumentException("Invalid month values");
            }

            // Handle case where end month is before start month (spans across years)
            if (endMonth < startMonth) {
                endMonth = 12; // Limit to end of year
            }

            startDate = LocalDateTime.of(year, startMonth, 1, 0, 0);
            endDate = LocalDateTime.of(year, endMonth, 1, 0, 0)
                    .plusMonths(1)
                    .minusSeconds(1);
        } else {
            // Full year
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        }

        List<ArticleEntity> publishedArticles = articleRepository.findByPublishedAtBetweenAndStatus(
                startDate, endDate, ArticleEntity.Status.APPROVED);

        // Create sorted monthly breakdown using TreeMap
        Map<String, Long> monthlyBreakdown = new TreeMap<>(publishedArticles.stream()
                .collect(Collectors.groupingBy(
                        article -> article.getPublishedAt().format(DateTimeFormatter.ofPattern("MMM")),
                        Collectors.counting()
                )));

        // Calculate cumulative growth
        List<Long> cumulativeGrowth = new ArrayList<>();
        long runningTotal = 0;
        for (Long count : monthlyBreakdown.values()) {
            runningTotal += count;
            cumulativeGrowth.add(runningTotal);
        }

        // Return analytics data
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("monthlyBreakdown", monthlyBreakdown);
        analytics.put("cumulativeGrowth", cumulativeGrowth);
        analytics.put("total", publishedArticles.size());
        analytics.put("period", Map.of(
                "year", year,
                "startMonth", startDate.getMonthValue(),
                "endMonth", endDate.getMonthValue(),
                "months", monthlyBreakdown.keySet()
        ));

        return analytics;
    }

// Add to ArticleService.java

    public List<ArticleEntity> getArticlesByAuthorName(String authorName) {
        return articleRepository.findByAuthorNameContainingIgnoreCaseAndStatus(
                authorName,
                ArticleEntity.Status.APPROVED
        );
    }

    public Page<ArticleEntity> getArticlesByDomainPaged(String domain, Pageable pageable) {
        return articleRepository.findByDomainAndStatusOrderByCreatedAtDesc(
                domain,
                ArticleEntity.Status.APPROVED,
                pageable
        );
    }

}
