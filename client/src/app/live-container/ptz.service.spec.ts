import { TestBed } from '@angular/core/testing';

import { PTZService } from './ptz.service';

describe('PTZService', () => {
  let service: PTZService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PTZService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
