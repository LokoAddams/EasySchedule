package com.easyschedule.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "La contrasenia actual es obligatoria")
    @Size(max = 120, message = "La contrasenia actual no puede exceder 120 caracteres")
    private String currentPassword;

    @NotBlank(message = "La nueva contrasenia es obligatoria")
    @Size(min = 8, max = 120, message = "La nueva contrasenia debe tener entre 8 y 120 caracteres")
    private String newPassword;

    @NotBlank(message = "Debes repetir la nueva contrasenia")
    @Size(min = 8, max = 120, message = "La repeticion de contrasenia debe tener entre 8 y 120 caracteres")
    private String confirmNewPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}
