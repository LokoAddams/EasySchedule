import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

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

  constructor(private fb: FormBuilder) {

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

    console.log(this.form.value);

  }

}