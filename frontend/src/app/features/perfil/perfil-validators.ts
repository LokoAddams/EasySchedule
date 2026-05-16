import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

type DateStruct = {
  year: number;
  month: number;
  day: number;
};

/**
 * Validador personalizado para el carnet de identidad
 * - Máximo 8 caracteres
 * - Sin caracteres especiales (solo letras y números)
 * - No puede estar vacío
 */
export function carnetIdentidadValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Dejar que required validator maneje esto
    }

    const value = control.value.trim();

    // Validar longitud máxima
    if (value.length > 8) {
      return { carnetMaxLength: { requiredLength: 8, actualLength: value.length } };
    }

    // Validar que no contenga caracteres especiales (solo letras y números)
    if (!/^[a-zA-Z0-9]+$/.test(value)) {
      return { carnetInvalidChars: true };
    }

    return null;
  };
}

/**
 * Validador para campos de nombres (nombre y apellido)
 * - Máximo 50 caracteres
 * - Solo letras, espacios y acentos
 * - Mínimo 2 caracteres
 */
export function nombreValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Dejar que required validator maneje esto
    }

    const value = control.value.trim();

    // Validar longitud mínima
    if (value.length < 2) {
      return { nombreMinLength: { requiredLength: 2, actualLength: value.length } };
    }

    // Validar longitud máxima
    if (value.length > 50) {
      return { nombreMaxLength: { requiredLength: 50, actualLength: value.length } };
    }

    // Validar que solo contenga letras, espacios y acentos
    if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(value)) {
      return { nombreInvalidChars: true };
    }

    return null;
  };
}

/**
 * Validador para el username
 * - Máximo 30 caracteres
 * - Mínimo 3 caracteres
 * - Solo letras, números, puntos y guiones
 */
export function usernameValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Dejar que required validator maneje esto
    }

    const value = control.value.trim();

    // Validar longitud mínima
    if (value.length < 3) {
      return { usernameMinLength: { requiredLength: 3, actualLength: value.length } };
    }

    // Validar longitud máxima
    if (value.length > 30) {
      return { usernameMaxLength: { requiredLength: 30, actualLength: value.length } };
    }

    // Validar caracteres permitidos (alfanuméricos, puntos y guiones)
    if (!/^[a-zA-Z0-9._-]+$/.test(value)) {
      return { usernameInvalidChars: true };
    }

    return null;
  };
}

/**
 * Validador para email adicional (complemento a Validators.email)
 * - Máximo 100 caracteres
 */
export function emailValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Dejar que required validator maneje esto
    }

    const value = control.value.trim();

    // Validar longitud máxima
    if (value.length > 100) {
      return { emailMaxLength: { requiredLength: 100, actualLength: value.length } };
    }

    return null;
  };
}

/**
 * Validador de fecha de nacimiento por rango de edad.
 */
export function fechaNacimientoEdadValidator(minAge: number, maxAge: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as DateStruct | null;
    if (!value) {
      return null;
    }

    const birthDate = new Date(value.year, value.month - 1, value.day);
    if (
      !Number.isFinite(birthDate.getTime())
      || birthDate.getFullYear() !== value.year
      || birthDate.getMonth() !== value.month - 1
      || birthDate.getDate() !== value.day
    ) {
      return { fechaNacimientoInvalid: true };
    }

    const today = new Date();
    const normalizedToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    if (birthDate > normalizedToday) {
      return { fechaNacimientoFuture: true };
    }

    let age = normalizedToday.getFullYear() - birthDate.getFullYear();
    const monthDiff = normalizedToday.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && normalizedToday.getDate() < birthDate.getDate())) {
      age--;
    }

    if (age < minAge || age > maxAge) {
      return { fechaNacimientoOutOfRange: true };
    }

    return null;
  };
}
