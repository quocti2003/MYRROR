package com.mirror.product.service;

import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.repository.MirrorProductRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkuSearchService {

    private final MirrorProductRepository mirrorProductRepository;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    // Synonym dictionary (ported from Python app.py)
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
        // Materials
        Map.entry("gold", List.of("18kgold", "whitegold", "yellowgold", "rosegold", "18k", "wg", "yg", "rg")),
        Map.entry("diamond", List.of("diam", "dm", "labgrown", "natural", "lg", "nat")),
        Map.entry("microfiber", List.of("micro", "fiber", "mf")),
        Map.entry("acrylic", List.of("acryl", "acr")),

        // Shapes
        Map.entry("pear", List.of("teardrop", "pr")),
        Map.entry("round", List.of("rnd", "circle", "circular")),
        Map.entry("emerald", List.of("em", "emrld")),
        Map.entry("heart", List.of("hrt", "heart-shaped")),
        Map.entry("oval", List.of("ov")),
        Map.entry("princess", List.of("prin", "square")),
        Map.entry("cushion", List.of("cush", "pillow")),

        // Colors
        Map.entry("red", List.of("rd")),
        Map.entry("black", List.of("blk")),
        Map.entry("grey", List.of("gray", "silver")),
        Map.entry("silver", List.of("slv", "bright")),

        // Origins
        Map.entry("labgrown", List.of("lab", "lg", "labcreated", "synthetic")),
        Map.entry("natural", List.of("nat", "mined")),

        // Item types
        Map.entry("ring", List.of("rng", "band")),
        Map.entry("earring", List.of("ear", "earrings")),
        Map.entry("bracelet", List.of("brc", "bangle")),
        Map.entry("necklace", List.of("neck", "nck")),
        Map.entry("box", List.of("bx", "container")),
        Map.entry("pouch", List.of("poc", "bag"))
    );

    /**
     * Fuzzy search for products by code
     */
    public List<SearchResult> searchSkus(String query, double threshold) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Get all products
        List<MirrorProduct> allProducts = mirrorProductRepository.findAll();

        // Split query into words for multi-word search
        String queryLower = query.strip().toLowerCase();
        String[] queryWords = queryLower.split("\\s+");

        // Calculate relevance for each product
        List<SearchResult> results = allProducts.stream()
            .map(product -> {
                double relevance = calculateRelevance(queryLower, queryWords, product);
                return new SearchResult(product, relevance);
            })
            .filter(result -> result.getRelevance() >= threshold)
            .sorted(Comparator.comparingDouble(SearchResult::getRelevance).reversed())
            .collect(Collectors.toList());

        log.debug("Search query '{}' returned {} results", query, results.size());
        return results;
    }

    /**
     * Calculate relevance score (0-1)
     */
    private double calculateRelevance(String query, String[] queryWords, MirrorProduct product) {
        String codeLower = product.getSkuCode() != null ? product.getSkuCode().toLowerCase() : "";
        String nameLower = product.getItemName() != null ? product.getItemName().toLowerCase() : "";
        String descLower = product.getDescription() != null ? product.getDescription().toLowerCase() : "";

        double maxScore = 0.0;

        // Tier 1: Exact substring match (100%)
        if (codeLower.contains(query) || nameLower.contains(query) || descLower.contains(query)) {
            return 1.0;
        }

        // Tier 2: Synonym match (90%)
        double synonymScore = checkSynonymMatch(query, codeLower, nameLower, descLower);
        maxScore = Math.max(maxScore, synonymScore);

        // Tier 3: Fuzzy match on code (variable)
        double fuzzyScore = fuzzyMatchScore(query, codeLower);
        maxScore = Math.max(maxScore, fuzzyScore);

        // Tier 4: Multi-word query matching
        if (queryWords.length > 1) {
            String searchableText = codeLower + " " + nameLower + " " + descLower;
            double multiWordScore = calculateMultiWordScore(queryWords, searchableText);
            maxScore = Math.max(maxScore, multiWordScore);
        }

        return maxScore;
    }

    /**
     * Check synonym matches
     */
    private double checkSynonymMatch(String query, String code, String name, String desc) {
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String canonical = entry.getKey();
            List<String> synonyms = entry.getValue();

            // If query is a synonym
            if (synonyms.contains(query)) {
                // Check if canonical or any synonym appears in target
                if (code.contains(canonical) || name.contains(canonical) || desc.contains(canonical)) {
                    return 0.9;
                }
                for (String syn : synonyms) {
                    if (code.contains(syn) || name.contains(syn) || desc.contains(syn)) {
                        return 0.9;
                    }
                }
            }
        }
        return 0.0;
    }

    /**
     * Fuzzy match score using Levenshtein distance
     */
    private double fuzzyMatchScore(String query, String target) {
        if (query.equals(target)) {
            return 1.0;
        }

        // Substring match
        if (target.contains(query) || query.contains(target)) {
            return 0.95;
        }

        // Calculate Levenshtein distance
        int distance = levenshtein.apply(query, target);
        int maxLength = Math.max(query.length(), target.length());

        if (maxLength == 0) {
            return 0.0;
        }

        double similarity = 1.0 - ((double) distance / maxLength);
        return Math.max(0, similarity);
    }

    /**
     * Calculate multi-word query score
     */
    private double calculateMultiWordScore(String[] queryWords, String searchableText) {
        String[] textWords = searchableText.split("\\s+");

        int matchedWords = 0;
        for (String queryWord : queryWords) {
            boolean found = false;
            for (String textWord : textWords) {
                if (fuzzyMatchScore(queryWord, textWord) > 0.7) {
                    found = true;
                    break;
                }
            }
            if (found) {
                matchedWords++;
            }
        }

        return (double) matchedWords / queryWords.length;
    }

    @Data
    @AllArgsConstructor
    public static class SearchResult {
        private MirrorProduct product;
        private double relevance;

        public String getSkuCode() {
            return product.getSkuCode();
        }

        public String getDescription() {
            return product.getDescription();
        }

        public String getItemName() {
            return product.getItemName();
        }
    }
}
