package com.grupo140.turnos.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlugGeneratorTest {

    @Mock
    private CompanyRepository companyRepository;

    private SlugGenerator slugGenerator;

    @BeforeEach
    void setUp() {
        slugGenerator = new SlugGenerator(companyRepository);
    }

    @Test
    void removesAccentsAndSymbols() {
        assertThat(slugGenerator.generate("Peluquería Ñandú & Co.")).isEqualTo("peluqueria-nandu-co");
    }

    @Test
    void takenSlugGetsNextFreeSuffix() {
        when(companyRepository.existsBySlug("barberia-central")).thenReturn(true);
        when(companyRepository.existsBySlug("barberia-central-2")).thenReturn(true);

        assertThat(slugGenerator.generate("Barbería Central")).isEqualTo("barberia-central-3");
    }

    @Test
    void reservedWordGetsSuffix() {
        assertThat(slugGenerator.generate("Login")).isEqualTo("login-2");
    }

    @Test
    void nameWithoutLettersOrDigitsUsesFallback() {
        assertThat(slugGenerator.generate("!!!")).isEqualTo("empresa");
    }

    @Test
    void longNameIsCutToColumnLength() {
        String longName = "a".repeat(80);
        when(companyRepository.existsBySlug("a".repeat(60))).thenReturn(true);

        String slug = slugGenerator.generate(longName);

        assertThat(slug).hasSize(60).endsWith("-2");
    }
}
