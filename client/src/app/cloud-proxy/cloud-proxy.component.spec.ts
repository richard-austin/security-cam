import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CloudProxyComponent } from './cloud-proxy.component';

describe('CloudProxyComponent', () => {
  let component: CloudProxyComponent;
  let fixture: ComponentFixture<CloudProxyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CloudProxyComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CloudProxyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
