package com.example.hyperlocal.category.service;

import com.example.hyperlocal.category.dto.CategoryDto;
import com.example.hyperlocal.category.dto.CategoryRequest;
import com.example.hyperlocal.category.entity.Category;
import com.example.hyperlocal.category.repository.CategoryRepository;
import com.example.hyperlocal.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest req) {
        String slug = req.getSlug() != null && !req.getSlug().trim().isEmpty()
                ? req.getSlug().trim().toLowerCase()
                : req.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");

        if (categoryRepository.existsBySlug(slug)) {
            throw new ApiException("CATEGORY_SLUG_EXISTS", "Category with slug '" + slug + "' already exists", HttpStatus.CONFLICT);
        }

        Category category = new Category(req.getName().trim(), slug, req.getImageUrl());
        if (req.getActive() != null) {
            category.setActive(req.getActive());
        }

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.NOT_FOUND));

        category.setName(req.getName().trim());
        if (req.getSlug() != null && !req.getSlug().trim().isEmpty()) {
            String newSlug = req.getSlug().trim().toLowerCase();
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                throw new ApiException("CATEGORY_SLUG_EXISTS", "Category with slug '" + newSlug + "' already exists", HttpStatus.CONFLICT);
            }
            category.setSlug(newSlug);
        }
        if (req.getImageUrl() != null) {
            category.setImageUrl(req.getImageUrl());
        }
        if (req.getActive() != null) {
            category.setActive(req.getActive());
        }

        Category updated = categoryRepository.save(category);
        return CategoryDto.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.NOT_FOUND));
        category.setActive(false);
        categoryRepository.save(category);
    }
}
