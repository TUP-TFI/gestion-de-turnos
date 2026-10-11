package com.grupo140.turnos.company;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API de {@code company} para el resto de los features. Mínimo para auth; se extiende en el Paso 4.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /** Indica si la empresa existe y está {@link CompanyStatus#ACTIVE}. */
    @Transactional(readOnly = true)
    public boolean isActive(UUID companyId) {
        return companyRepository
                .findStatusById(companyId)
                .map(status -> status == CompanyStatus.ACTIVE)
                .orElse(false);
    }
}
