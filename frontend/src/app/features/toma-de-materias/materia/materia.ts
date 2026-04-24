import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-materia',
  standalone: true,
  imports: [TranslatePipe, ReactiveFormsModule, CommonModule],
  templateUrl: './materia.html',
  styleUrl: './materia.scss',
})
export class Materia {
  materiaForm: FormGroup;
  dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  bloquesHorarios = ['07:00 - 08:30', '08:30 - 10:00', '10:00 - 11:30', '11:30 - 13:00', '13:00 - 14:30', '14:30 - 16:00', '16:00 - 17:30', '17:30 - 19:00', '19:00 - 20:30', '20:30 - 22:00'];
  
  successMessage = '';
  errorMessage = '';

  constructor(private fb: FormBuilder) {
    this.materiaForm = this.fb.group({
      bloques: this.fb.formArray([])
    });
    this.addBloque();
  }

  get bloques(): FormArray {
    return this.materiaForm.get('bloques') as FormArray;
  }

  addBloque() {
    const bloqueForm = this.fb.group({
      dia: ['', Validators.required],
      bloque: ['', Validators.required],
      docente: ['', Validators.required],
      aula: ['', Validators.required]
    });
    this.bloques.push(bloqueForm);
    this.errorMessage = '';
    this.successMessage = '';
  }

  removeBloque(index: number) {
    this.bloques.removeAt(index);
    this.errorMessage = '';
    this.successMessage = '';
  }

  clearBloques() {
    this.bloques.clear();
    this.errorMessage = '';
    this.successMessage = '';
  }

  save() {
    this.errorMessage = '';
    this.successMessage = '';
    
    if (this.materiaForm.invalid) {
      if (this.bloques.length === 0) {
        // Technically allowing form to save no blocks means emptying the configuration
        this.successMessage = 'Horario guardado correctamente. (Sin bloques asignados)';
      } else {
        this.errorMessage = 'Por favor, complete todos los campos de cada bloque.';
      }
      return;
    }

    const blocksValue = this.bloques.value;
    const seen = new Set();
    for (let i = 0; i < blocksValue.length; i++) {
        const key = blocksValue[i].dia + '-' + blocksValue[i].bloque;
        if (seen.has(key)) {
             this.errorMessage = 'Hay un conflicto: múltiples bloques en el mismo día y horario.';
             return;
        }
        seen.add(key);
    }

    this.successMessage = 'Horario guardado correctamente.';
  }
}
