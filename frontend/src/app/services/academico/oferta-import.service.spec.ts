import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiService } from '../api.service';
import {
  OfertaImportResultResponse,
  OfertaImportService,
} from './oferta-import.service';

describe('OfertaImportService', () => {
  let service: OfertaImportService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['post']);

    TestBed.configureTestingModule({
      providers: [
        OfertaImportService,
        {
          provide: ApiService,
          useValue: apiServiceSpy,
        },
      ],
    });

    service = TestBed.inject(OfertaImportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send the selected CSV file to the academic offers import endpoint', (done) => {
    const file = new File(['contenido'], 'ofertas.csv', {
      type: 'text/csv',
    });

    const response: OfertaImportResultResponse = {
      summary: {
        totalRows: 1,
        offersCreated: 1,
        offersUpdated: 0,
        scheduleBlocks: 1,
        skippedRows: 0,
        errorsCount: 0,
        warningsCount: 0,
      },
      offers: [],
      errors: [],
      warnings: [],
    };

    apiServiceSpy.post.and.returnValue(of(response));

    service.importarOfertas(1, file).subscribe((result) => {
      expect(result).toEqual(response);
      expect(apiServiceSpy.post).toHaveBeenCalledTimes(1);

      const [url, body] = apiServiceSpy.post.calls.mostRecent().args;

      expect(url).toBe('/api/academico/ofertas/importar?mallaId=1');
      expect(body instanceof FormData).toBeTrue();
      expect((body as FormData).get('file')).toBe(file);

      done();
    });
  });
});