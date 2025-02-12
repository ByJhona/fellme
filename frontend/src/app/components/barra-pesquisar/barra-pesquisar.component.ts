import { Component } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { query } from 'express';

@Component({
  selector: 'app-barra-pesquisar',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './barra-pesquisar.component.html',
  styleUrl: './barra-pesquisar.component.scss'
})
export class BarraPesquisarComponent {
  pesquisaForm = new FormControl('')

  constructor(private router: Router) { }
  value = 'Clear me';

  pesquisar() {
    this.router.navigate(['/pesquisar'], {queryParams: {titulo:this.pesquisaForm.value}});
  }
}
