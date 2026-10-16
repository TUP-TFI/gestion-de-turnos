package com.grupo140.turnos.company;

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

    @Query("select c.status from Company c where c.id = :id")
    Optional<CompanyStatus> findStatusById(@Param("id") UUID id);
}
