package com.example.first.repository;
import com.example.first.models.ArticleEntity;
import com.example.first.models.UserEntity;
//import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, Long>
{
    // Find articles by author ID
    List<ArticleEntity> findByAuthorId(Long authorId);

    // Find articles by status
    org.springframework.data.domain.Page<ArticleEntity> findByStatusOrderByCreatedAtDesc(ArticleEntity.Status status, org.springframework.data.domain.Pageable pageable);

    List<ArticleEntity> findByStatus(ArticleEntity.Status status);

    // Find articles by domain
    List<ArticleEntity> findByDomain(String domain);

    // Find articles by title containing keyword (for search functionality)
    List<ArticleEntity> findByTitleContainingIgnoreCase(String keyword);

    // Find articles by domain and status
    List<ArticleEntity> findByDomainAndStatus(String domain, ArticleEntity.Status status);

    // Find latest articles
    List<ArticleEntity> findTop10ByOrderByCreatedAtDesc();

    org.springframework.data.domain.Page<ArticleEntity> findByAuthorId(Long authorId, Pageable pageable);

    List<ArticleEntity> findByAuthorIdAndIdNotAndStatusOrderByCreatedAtDesc(
            Long authorId,
            Long excludeArticleId,
            ArticleEntity.Status status,
            Pageable pageable
    );

    // In ArticleRepository.java
    long countByAuthor(UserEntity author);
    long countByAuthorAndStatus(UserEntity author, ArticleEntity.Status status);
    List<ArticleEntity> findByAuthor(UserEntity author);



    List<ArticleEntity> findByCreatedAtAfterAndStatus(LocalDateTime date, ArticleEntity.Status status);
    List<ArticleEntity> findByCreatedAtBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, ArticleEntity.Status status);


    List<ArticleEntity> findByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    List<ArticleEntity> findByPublishedAtBetweenAndStatus(
            LocalDateTime startDate,
            LocalDateTime endDate,
            ArticleEntity.Status status
    );


    List<ArticleEntity> findByAuthorNameContainingIgnoreCaseAndStatus(String authorName, ArticleEntity.Status status);

    Page<ArticleEntity> findByDomainAndStatusOrderByCreatedAtDesc(
            String domain,
            ArticleEntity.Status status,
            Pageable pageable
    );

    // Add to ArticleRepository.java
    org.springframework.data.domain.Page<ArticleEntity> findByAuthorNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String authorName,
            ArticleEntity.Status status,
            Pageable pageable
    );

}


