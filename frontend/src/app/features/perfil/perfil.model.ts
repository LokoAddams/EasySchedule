export interface PerfilResponse {
  id: number;
  username: string;
  nombre: string | null;
  apellido: string | null;
  email: string | null;
  carnetIdentidad: string | null;
  fechaNacimiento: string | null;
  fechaRegistro: string | null;
  semestreActual: number | null;
  carrera: string | null;
  mallaId: number | null;
  universidad: string | null;
}
