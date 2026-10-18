package com.grupo140.turnos.catalog;

import com.grupo140.turnos.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del catálogo de servicios, y API de {@code catalog} para el resto de los features.
 */
@org.springframework.stereotype.Service
public class CatalogService {

    private final ServiceRepository serviceRepository;

    public CatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /** Todos los servicios de la empresa, activos e inactivos, ordenados por nombre (US-05.1). */
    @Transactional(readOnly = true)
    public List<ServiceResponse> list(UUID companyId) {
        return serviceRepository.findAllByCompanyIdOrderByNameAsc(companyId).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    /**
     * Detalle de un servicio de la empresa (US-05.1).
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public ServiceResponse get(UUID companyId, UUID serviceId) {
        return ServiceResponse.from(requireOwnedService(companyId, serviceId));
    }

    /** Alta de un servicio, siempre {@link ServiceStatus#ACTIVE} (US-05.1). */
    @Transactional
    public ServiceResponse create(UUID companyId, ServiceRequest request) {
        Service service = new Service(companyId, request.name(), request.price(), request.durationMinutes());
        service.setDescription(blankToNull(request.description()));
        return ServiceResponse.from(serviceRepository.saveAndFlush(service));
    }

    /**
     * Edición de los datos de un servicio (US-05.1). Los turnos ya reservados no cambian: guardan su
     * propio precio y duración.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404) o {@code VALIDATION_ERROR} (400).
     */
    @Transactional
    public ServiceResponse update(UUID companyId, UUID serviceId, ServiceRequest request) {
        Service service = requireOwnedService(companyId, serviceId);
        service.setName(request.name());
        service.setDescription(blankToNull(request.description()));
        service.setPrice(request.price());
        service.setDurationMinutes(request.durationMinutes());
        return ServiceResponse.from(serviceRepository.saveAndFlush(service));
    }

    /**
     * Desactiva un servicio (US-05.2). Es idempotente: si ya estaba inactivo, no falla.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404).
     */
    @Transactional
    public ServiceResponse deactivate(UUID companyId, UUID serviceId) {
        return changeStatus(companyId, serviceId, ServiceStatus.INACTIVE);
    }

    /**
     * Vuelve a activar un servicio desactivado (US-05.2). Es idempotente.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404).
     */
    @Transactional
    public ServiceResponse activate(UUID companyId, UUID serviceId) {
        return changeStatus(companyId, serviceId, ServiceStatus.ACTIVE);
    }

    /**
     * Devuelve el servicio si es de la empresa. Es la API de {@code catalog} para otros features: un
     * servicio inexistente y uno de otra empresa dan el mismo 404.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public Service requireOwnedService(UUID companyId, UUID serviceId) {
        return serviceRepository
                .findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(
                        () -> new ApiException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "El servicio no existe"));
    }

    private ServiceResponse changeStatus(UUID companyId, UUID serviceId, ServiceStatus status) {
        Service service = requireOwnedService(companyId, serviceId);
        service.setStatus(status);
        return ServiceResponse.from(serviceRepository.saveAndFlush(service));
    }

    // Una descripción vacía o con solo espacios se guarda como "sin descripción".
    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
