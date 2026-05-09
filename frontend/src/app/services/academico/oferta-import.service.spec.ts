import { TestBed } from '@angular/core/testing';

import { OfertaImportService } from './oferta-import.service';

describe('OfertaImportService', () => {
  let service: OfertaImportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OfertaImportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
