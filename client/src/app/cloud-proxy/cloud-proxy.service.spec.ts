import { TestBed } from '@angular/core/testing';

import { CloudProxyService } from './cloud-proxy.service';

describe('CloudProxyService', () => {
  let service: CloudProxyService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CloudProxyService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
