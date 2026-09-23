import { TestBed } from '@angular/core/testing';

import { BiographyServiceService } from './biography-service.service';

describe('BiographyServiceService', () => {
  let service: BiographyServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BiographyServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
