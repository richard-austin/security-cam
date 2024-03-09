import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OnvifCredentialsComponent } from './onvif-credentials.component';

describe('CredentialsForCameraAccessComponent', () => {
  let component: OnvifCredentialsComponent;
  let fixture: ComponentFixture<OnvifCredentialsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ OnvifCredentialsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OnvifCredentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
