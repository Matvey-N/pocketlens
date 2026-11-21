package com.pocketlens.category.service;

import static com.pocketlens.support.TestData.USER_ID;
import static com.pocketlens.support.TestData.OTHER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.category.domain.Category;
import com.pocketlens.category.domain.CategoryKind;
import com.pocketlens.category.persistence.CategoryRepository;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final CategoryUsageCleaner cleaner = mock(CategoryUsageCleaner.class);
    private final CategoryService service = new CategoryService(categories, List.of(cleaner));
    private final CategoryCommand pets = new CategoryCommand(" Pets ", "#AA0000", CategoryKind.EXPENSE);

    @Test
    void createsCustomCategoryWithUniqueName() {
        when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
        Category created = service.create(USER_ID, pets);
        assertThat(created.getName()).isEqualTo("Pets");
        assertThat(created.getUserId()).isEqualTo(USER_ID);
        assertThat(created.isDefault()).isFalse();

        when(categories.nameTaken(USER_ID, "Pets", CategoryRepository.NO_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.create(USER_ID, pets)).isInstanceOf(ConflictException.class);
    }

    @Test
    void visibleCategoriesIncludeDefaultsAndOwnOnly() {
        Category own = TestData.userCategory(USER_ID, "Pets");
        Category foreign = TestData.userCategory(OTHER_USER_ID, "Boat");
        Category global = TestData.expenseCategory("Groceries");
        when(categories.findById(own.getId())).thenReturn(Optional.of(own));
        when(categories.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        when(categories.findById(global.getId())).thenReturn(Optional.of(global));
        when(categories.findVisibleTo(USER_ID)).thenReturn(List.of(global, own));

        assertThat(service.listVisible(USER_ID)).hasSize(2);
        assertThat(service.getVisible(USER_ID, own.getId())).isEqualTo(own);
        assertThat(service.getVisible(USER_ID, global.getId())).isEqualTo(global);
        assertThatThrownBy(() -> service.getVisible(USER_ID, foreign.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesAndDeletesOwnCategoriesOnly() {
        Category own = TestData.userCategory(USER_ID, "Pets");
        Category global = TestData.expenseCategory("Groceries");
        when(categories.findById(own.getId())).thenReturn(Optional.of(own));
        when(categories.findById(global.getId())).thenReturn(Optional.of(global));

        Category updated = service.update(USER_ID, own.getId(), new CategoryCommand("Animals", "#00AA00",
                CategoryKind.EXPENSE));
        assertThat(updated.getName()).isEqualTo("Animals");
        assertThat(updated.getColor()).isEqualTo("#00AA00");
        when(categories.nameTaken(USER_ID, "Taken", own.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.update(USER_ID, own.getId(),
                new CategoryCommand("Taken", "#000000", CategoryKind.EXPENSE))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.update(USER_ID, global.getId(), pets)).isInstanceOf(NotFoundException.class);

        service.delete(USER_ID, own.getId());
        verify(cleaner).releaseCategory(own.getId());
        verify(categories).delete(own);
    }

    @Test
    void managesDefaultCategories() {
        Category global = TestData.expenseCategory("Groceries");
        Category own = TestData.userCategory(USER_ID, "Pets");
        when(categories.findByUserIdIsNullOrderByName()).thenReturn(List.of(global));
        when(categories.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categories.findById(global.getId())).thenReturn(Optional.of(global));
        when(categories.findById(own.getId())).thenReturn(Optional.of(own));
        when(categories.findBySystemKey("other")).thenReturn(Optional.of(global));

        assertThat(service.listDefaults()).containsExactly(global);
        assertThat(service.createDefault(pets).isDefault()).isTrue();
        assertThat(service.updateDefault(global.getId(), pets).getName()).isEqualTo("Pets");
        assertThatThrownBy(() -> service.updateDefault(own.getId(), pets)).isInstanceOf(NotFoundException.class);
        when(categories.defaultNameTaken(eq("Pets"), any(UUID.class))).thenReturn(true);
        assertThatThrownBy(() -> service.createDefault(pets)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.updateDefault(global.getId(), pets)).isInstanceOf(ConflictException.class);
        assertThat(service.getBySystemKey("other")).isEqualTo(global);
        when(categories.findBySystemKey("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBySystemKey("missing")).isInstanceOf(IllegalStateException.class);
    }
}
