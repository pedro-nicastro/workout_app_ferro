package br.com.ferro.repository;

import br.com.ferro.model.ProfileSettingsRequest;
import br.com.ferro.model.ProfileSettingsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileSettingsRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FerroRepository repository = new FerroRepository(jdbc);

    @Test
    void savesAndReturnsUnitWeightsAndNotificationChoices() {
        long userId = 12L;
        ProfileSettingsRequest request = new ProfileSettingsRequest(
                "lb", new BigDecimal("68.04"), new BigDecimal("63.50"), false, true, true
        );
        ProfileSettingsResponse saved = new ProfileSettingsResponse(
                "lb", new BigDecimal("68.04"), new BigDecimal("63.50"), false, true, true
        );
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbc.queryForObject(
                anyString(), org.mockito.ArgumentMatchers.<RowMapper<ProfileSettingsResponse>>any(),
                any(Object[].class)
        )).thenReturn(saved);

        ProfileSettingsResponse result = repository.updateProfileSettings(userId, request);

        assertEquals(saved, result);
        verify(jdbc).update(
                eq("UPDATE perfis_usuario SET unidade = ?, peso_atual = ?, meta_peso = ?, " +
                        "notificacao_lembrete = ?, notificacao_recorde = ?, notificacao_resumo = ? " +
                        "WHERE usuario_id = ?"),
                eq("lb"), eq(new BigDecimal("68.04")), eq(new BigDecimal("63.50")),
                eq(false), eq(true), eq(true), eq(userId)
        );
    }

    @Test
    void rejectsUnknownUnitsBeforeWriting() {
        ProfileSettingsRequest request = new ProfileSettingsRequest(
                "stone", null, null, true, true, false
        );

        assertThrows(IllegalArgumentException.class,
                () -> repository.updateProfileSettings(12L, request));
        verifyNoInteractions(jdbc);
    }
}
