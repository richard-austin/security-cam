import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CameraCredentialsComponent } from './camera-credentials.component';

describe('CredentialsForCameraAccessComponent', () => {
  let component: CameraCredentialsComponent;
  let fixture: ComponentFixture<CameraCredentialsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CameraCredentialsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CameraCredentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
