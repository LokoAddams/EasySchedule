import { NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { FeatureToggleService } from '../../services/feature-toggle.service';

@Component({
  selector: 'app-malla',
  imports: [RouterLink, NgIf, TranslatePipe],
  templateUrl: './malla.html',
  styleUrl: './malla.scss',
})
export class Malla implements OnInit {
  protected mallaEnabled = false;

  constructor(private readonly featureService: FeatureToggleService) {}

  ngOnInit(): void {
    this.mallaEnabled = this.featureService.isEnabled('malla');
  }
}
