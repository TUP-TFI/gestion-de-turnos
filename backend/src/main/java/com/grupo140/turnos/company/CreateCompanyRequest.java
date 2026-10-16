package com.grupo140.turnos.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/superadmin/companies} (US-04.1): los datos de la empresa y los de su admin.
 * El slug y el estado no vienen en el pedido: los define el backend. Los topes de largo son los de las
 * columnas de la base.
 */
public record CreateCompanyRequest(
        @NotNull(message = "Los datos de la empresa son obligatorios") @Valid
        CompanyData company,

        @NotNull(message = "Los datos del admin son obligatorios") @Valid
        AdminData admin) {

    public record CompanyData(
            @NotBlank(message = "El nombre de la empresa es obligatorio")
            @Size(max = 120, message = "El nombre de la empresa no puede superar los 120 caracteres")
            String name,

            @NotNull(message = "El rubro es obligatorio") CompanyCategory category,

            @NotBlank(message = "La zona horaria es obligatoria")
            @Size(max = 50, message = "La zona horaria no puede superar los 50 caracteres")
            String timezone,

            String description,

            @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
            String address,

            @Size(max = 30, message = "El teléfono de la empresa no puede superar los 30 caracteres")
            String phone,

            @Email(message = "El email de contacto no es válido")
            @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres")
            String contactEmail,

            @Size(max = 500, message = "La dirección del logo no puede superar los 500 caracteres")
            String logoUrl) {}

    public record AdminData(
            @NotBlank(message = "El nombre del admin es obligatorio")
            @Size(max = 80, message = "El nombre del admin no puede superar los 80 caracteres")
            String firstName,

            @NotBlank(message = "El apellido del admin es obligatorio")
            @Size(max = 80, message = "El apellido del admin no puede superar los 80 caracteres")
            String lastName,

            @NotBlank(message = "El email del admin es obligatorio")
            @Email(message = "El email del admin no es válido")
            @Size(max = 150, message = "El email del admin no puede superar los 150 caracteres")
            String email,

            @NotBlank(message = "El teléfono del admin es obligatorio")
            @Size(max = 30, message = "El teléfono del admin no puede superar los 30 caracteres")
            String phone) {}
}
