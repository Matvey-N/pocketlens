package com.pocketlens.importing.service;

import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ConflictException;
import com.pocketlens.common.error.NotFoundException;
import com.pocketlens.importing.domain.ImportProfile;
import com.pocketlens.importing.persistence.ImportProfileRepository;
import com.pocketlens.importing.service.format.ColumnMapping;
import com.pocketlens.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportProfileServiceTest {

    private final ImportProfileRepository profiles = mock(ImportProfileRepository.class);
    private final ImportProfileService service = new ImportProfileService(profiles, TestData.clock());
    private final ColumnMapping valid = new ColumnMapping(';', "Date", "dd.MM.yyyy", "Text", "Amount", null, null,
            null, "EUR", ',');

    @Test
    void createsAndListsProfiles() {
        when(profiles.save(any())).thenAnswer(i -> i.getArgument(0));
        ImportProfile created = service.create(USER_ID, " My bank ", valid);
        assertThat(created.getName()).isEqualTo("My bank");
        assertThat(created.toMapping()).isEqualTo(valid);

        when(profiles.findByUserIdOrderByName(USER_ID)).thenReturn(List.of(created));
        assertThat(service.list(USER_ID)).containsExactly(created);
    }

    @Test
    void rejectsDuplicateNamesAndInvalidMappings() {
        when(profiles.existsByUserIdAndNameIgnoreCase(USER_ID, "Dup")).thenReturn(true);
        assertThatThrownBy(() -> service.create(USER_ID, "Dup", valid)).isInstanceOf(ConflictException.class);

        ColumnMapping both = new ColumnMapping(';', "D", "dd.MM.yyyy", "T", "A", "Debit", "Credit", null, "EUR", ',');
        ColumnMapping noCurrency = new ColumnMapping(';', "D", "dd.MM.yyyy", "T", "A", null, null, null, null, ',');
        ColumnMapping badDate = new ColumnMapping(';', "D", "{bad", "T", "A", null, null, "C", null, ',');
        assertThatThrownBy(() -> service.validate(both)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.validate(noCurrency)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.validate(badDate)).isInstanceOf(ApiException.class);
    }

    @Test
    void deletesOwnProfilesOnly() {
        ImportProfile profile = TestData.withId(new ImportProfile(USER_ID, "P", valid, TestData.NOW), UUID.randomUUID());
        when(profiles.findByIdAndUserId(profile.getId(), USER_ID)).thenReturn(Optional.of(profile));
        service.delete(USER_ID, profile.getId());
        verify(profiles).delete(profile);

        UUID other = UUID.randomUUID();
        when(profiles.findByIdAndUserId(other, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(USER_ID, other)).isInstanceOf(NotFoundException.class);
    }
}
