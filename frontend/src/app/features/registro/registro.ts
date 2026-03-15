import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class Registro {

  form: FormGroup;
  loading = false;

  constructor(private fb: FormBuilder, private http: HttpClient) {

    this.form = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatch });

  }

  passwordMatch(control: AbstractControl) {

    const pass = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;

    if (pass !== confirm) {
      control.get('confirmPassword')?.setErrors({ mismatch: true });
    } else {
      control.get('confirmPassword')?.setErrors(null);
    }

    return null;
  }
  registrar() {

    if (this.form.invalid) return;
  
    this.loading = true;
  
    const payload = {
      username: this.form.value.nombre + this.form.value.apellido,
      email: this.form.value.correo,
      password: this.form.value.password
    };
  
    this.http.post('http://localhost:8080/api/estudiantes/registro', payload)
      .subscribe({
        next: (response) => {
          console.log('Registro exitoso', response);
          this.loading = false;
        },
        error: (error) => {
          console.error('Error en registro', error);
          this.loading = false;
        }
      });
  
  }

}