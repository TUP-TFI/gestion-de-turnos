package com.grupo140.turnos.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a {@link Company}. Extiende {@link Repository} y no {@code JpaRepository}, igual que
 * {@code UserRepository}: solo expone los métodos que se usan.
 */
public interface CompanyRepository extends Repository<Company, UUID> {

    Company save(Company company);

    // Guarda y ejecuta el INSERT en el momento, sin esperar al final de la transacción: recién ahí
    // quedan cargados createdAt y updatedAt.
    Company saveAndFlush(Company company);

    boolean existsBySlug(String slug);

    // Listado y edición del superadmin (US-04.2 y US-04.3). No llevan companyId: la empresa es el propio
    // tenant, y solo el superadmin llega a estas consultas.
    List<Company> findAllByOrderByNameAsc();

    Optional<Company> findById(UUID id);

    @Query("select c.status from Company c where c.id = :id")
    Optional<CompanyStatus> findStatusById(@Param("id") UUID id);

    // Dashboard del superadmin (US-04.6): cuenta empresas de toda la plataforma, no datos de una empresa.
    long countByStatus(CompanyStatus status);
}
