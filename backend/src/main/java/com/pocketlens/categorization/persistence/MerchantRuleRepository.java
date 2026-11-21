package com.pocketlens.categorization.persistence;

import com.pocketlens.categorization.domain.MerchantRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MerchantRuleRepository extends JpaRepository<MerchantRule, UUID> {

    /** User rules and global rules that apply to the user. */
    @Query("select r from MerchantRule r where r.userId = :userId or r.userId is null")
    List<MerchantRule> findApplicableTo(UUID userId);

    List<MerchantRule> findByUserIdOrderByPattern(UUID userId);

    List<MerchantRule> findByUserIdIsNullOrderByPattern();

    Optional<MerchantRule> findByUserIdAndPattern(UUID userId, String pattern);

    Optional<MerchantRule> findByUserIdIsNullAndPattern(String pattern);

    Optional<MerchantRule> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("delete from MerchantRule r where r.category.id = :categoryId")
    int deleteByCategoryId(UUID categoryId);
}
